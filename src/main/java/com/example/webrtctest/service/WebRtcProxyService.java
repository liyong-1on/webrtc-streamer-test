package com.example.webrtctest.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * webrtc-streamer 代理服务
 * 负责与独立的 webrtc-streamer 进程进行 HTTP 信令交互
 */
@Service
@Slf4j
public class WebRtcProxyService {

    @Value("${webrtc-streamer.base-url:http://127.0.0.1:8000}")
    private String webrtcStreamerBaseUrl;

    private final RestTemplate restTemplate;

    // ICE candidate 缓存：setAnswer 完成前暂存 candidates，避免时序问题导致 500
    private final Map<String, List<Map<String, Object>>> candidateCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> answerSent = new ConcurrentHashMap<>();

    public WebRtcProxyService() {
        this.restTemplate = new RestTemplate();

        // webrtc-streamer 返回 Content-Type: text/plain，但内容是 JSON
        // 需要让 Jackson 转换器也支持 text/plain
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setSupportedMediaTypes(java.util.Arrays.asList(
                MediaType.APPLICATION_JSON,
                MediaType.TEXT_PLAIN
        ));
        // 将自定义转换器放到第一位，优先使用
        this.restTemplate.getMessageConverters().add(0, jsonConverter);

        // 添加拦截器记录请求/响应详情（仅DEBUG级别，方便排查 webrtc-streamer 问题）
        this.restTemplate.getInterceptors().add((request, body, execution) -> {
            log.debug("[RestTemplate] Request: {} {}", request.getMethod(), request.getURI());
            org.springframework.http.client.ClientHttpResponse response = execution.execute(request, body);
            log.debug("[RestTemplate] Response: {}", response.getStatusCode());
            return response;
        });
    }

    /**
     * 向 webrtc-streamer 请求创建 Offer
     *
     * @param peerId  对等连接 ID（每个浏览器客户端唯一）
     * @param rtspUrl RTSP 流地址
     * @return SDP Offer {sdp: "...", type: "offer"}
     */
    public Map<String, Object> createOffer(String peerId, String rtspUrl) {
        // webrtc-streamer 期望接收原始格式的 RTSP URL
        // UriComponentsBuilder 会对 RTSP URL 中的特殊字符（如 : / @ 等）进行 URL 编码
        // 这可能导致 webrtc-streamer 无法正确解析，因此直接拼接 URL
        String url = webrtcStreamerBaseUrl + "/api/createOffer?peerid=" + peerId + "&url=" + rtspUrl;

        log.info("Requesting offer from webrtc-streamer | peerId={} | rtspUrl={} | fullUrl={}", peerId, rtspUrl, url);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            log.info("Offer received from webrtc-streamer | peerId={}", peerId);
            return response.getBody();
        } catch (HttpServerErrorException e) {
            String curlCmd = String.format("curl -v \"%s\"", url);
            log.error("webrtc-streamer returned server error | peerId={} | status={} | responseBody={} | testWith={}",
                    peerId, e.getStatusCode(), e.getResponseBodyAsString(), curlCmd);
            throw new RuntimeException("webrtc-streamer 服务内部错误 (HTTP " + e.getStatusCode() + "): " + e.getResponseBodyAsString()
                    + " | 可尝试手动执行: " + curlCmd, e);
        } catch (Exception e) {
            log.error("Failed to request offer from webrtc-streamer | peerId={} | url={}", peerId, url, e);
            throw new RuntimeException("调用 webrtc-streamer /api/call 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 测试 webrtc-streamer 连通性
     */
    public boolean healthCheck() {
        try {
            String url = webrtcStreamerBaseUrl + "/api/version";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.info("webrtc-streamer health check OK | status={} | body={}", response.getStatusCode(), response.getBody());
            return true;
        } catch (Exception e) {
            log.error("webrtc-streamer health check FAILED | baseUrl={} | error={}", webrtcStreamerBaseUrl, e.getMessage());
            return false;
        }
    }

    /**
     * 向 webrtc-streamer 发送 Answer
     *
     * @param peerId  对等连接 ID
     * @param rtspUrl RTSP 流地址
     * @param sdp     Answer SDP
     * @param type    "answer"
     */
    public void setAnswer(String peerId, String rtspUrl, String sdp, String type) {
        // 直接拼接 URL，避免对 RTSP URL 进行 URL 编码
        String url = webrtcStreamerBaseUrl + "/api/setAnswer?peerid=" + peerId + "&url=" + rtspUrl;

        Map<String, String> body = new HashMap<>();
        body.put("sdp", sdp);
        body.put("type", type);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        log.info("Sending answer to webrtc-streamer | peerId={} | fullUrl={}", peerId, url);
        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
            log.info("Answer sent successfully | peerId={}", peerId);

            // 标记 answer 已发送，并flush缓存的 ICE candidates
            answerSent.put(peerId, true);
            flushCandidateCache(peerId);
        } catch (HttpServerErrorException e) {
            log.error("webrtc-streamer returned server error on answer | peerId={} | status={} | responseBody={}",
                    peerId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("webrtc-streamer 服务内部错误 (HTTP " + e.getStatusCode() + "): " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Failed to send answer to webrtc-streamer | peerId={} | url={}", peerId, url, e);
            throw new RuntimeException("调用 webrtc-streamer /api/setAnswer (POST) 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送缓存的 ICE candidates
     */
    private void flushCandidateCache(String peerId) {
        List<Map<String, Object>> cached = candidateCache.remove(peerId);
        if (cached != null && !cached.isEmpty()) {
            log.info("Flushing {} cached ICE candidates for peerId={}", cached.size(), peerId);
            for (Map<String, Object> candidate : cached) {
                doSendIceCandidate(peerId, candidate);
            }
        }
    }

    /**
     * 实际发送 ICE Candidate 到 webrtc-streamer
     */
    private void doSendIceCandidate(String peerId, Map<String, Object> candidate) {
        String url = UriComponentsBuilder.fromHttpUrl(webrtcStreamerBaseUrl + "/api/addIceCandidate")
                .queryParam("peerid", peerId)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(candidate, headers);

        log.debug("Sending ICE candidate to webrtc-streamer | peerId={} | candidate={}", peerId, candidate);
        try {
            restTemplate.postForEntity(url, entity, Void.class);
            log.debug("ICE candidate sent successfully | peerId={}", peerId);
        } catch (HttpServerErrorException e) {
            log.warn("webrtc-streamer rejected ICE candidate | peerId={} | status={} | response={} | candidate={}",
                    peerId, e.getStatusCode(), e.getResponseBodyAsString(), candidate);
        } catch (Exception e) {
            log.warn("Failed to send ICE candidate | peerId={} | candidate={} | error={}", peerId, candidate, e.getMessage());
        }
    }

    /**
     * 获取 webrtc-streamer 的 ICE Candidates
     *
     * @param peerId 对等连接 ID
     * @return ICE Candidates 列表 [{candidate: "...", sdpMid: "...", sdpMLineIndex: n}]
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getIceCandidates(String peerId) {
        String url = UriComponentsBuilder.fromHttpUrl(webrtcStreamerBaseUrl + "/api/getIceCandidate")
                .queryParam("peerid", peerId)
                .toUriString();

        try {
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            return response.getBody();
        } catch (Exception e) {
            log.warn("Failed to get ICE candidates | peerId={} | {}", peerId, e.getMessage());
            return null;
        }
    }

    /**
     * 向 webrtc-streamer 发送 ICE Candidate
     *
     * @param peerId    对等连接 ID
     * @param candidate ICE Candidate 信息
     */
    public void addIceCandidate(String peerId, Map<String, Object> candidate) {
        // 如果 answer 还没发送，先缓存 candidate，等 setAnswer 完成后再统一发送
        if (!Boolean.TRUE.equals(answerSent.get(peerId))) {
            log.debug("Caching ICE candidate for peerId={} (answer not yet sent)", peerId);
            candidateCache.computeIfAbsent(peerId, k -> new ArrayList<>()).add(candidate);
            return;
        }

        doSendIceCandidate(peerId, candidate);
    }

    /**
     * 断开与 webrtc-streamer 的连接
     *
     * @param peerId 对等连接 ID
     */
    public void hangup(String peerId) {
        String url = UriComponentsBuilder.fromHttpUrl(webrtcStreamerBaseUrl + "/api/hangup")
                .queryParam("peerid", peerId)
                .toUriString();

        try {
            restTemplate.getForEntity(url, Void.class);
            log.info("Hangup success | peerId={}", peerId);
        } catch (Exception e) {
            log.warn("Hangup failed | peerId={} | {}", peerId, e.getMessage());
        } finally {
            // 清理该 peerId 的缓存，避免内存泄漏
            candidateCache.remove(peerId);
            answerSent.remove(peerId);
            log.debug("Cleaned up cache for peerId={}", peerId);
        }
    }
}
