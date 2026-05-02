package com.example.webrtctest.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * webrtc-streamer 代理服务
 * 负责与独立的 webrtc-streamer 进程进行 HTTP 信令交互
 */
@Service
@Slf4j
public class WebRtcProxyService {

    @Value("${webrtc-streamer.base-url:http://127.0.0.1:8000}")
    private String webrtcStreamerBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 向 webrtc-streamer 请求创建 Offer
     *
     * @param peerId  对等连接 ID（每个浏览器客户端唯一）
     * @param rtspUrl RTSP 流地址
     * @return SDP Offer {sdp: "...", type: "offer"}
     */
    public Map<String, Object> createOffer(String peerId, String rtspUrl) {
        String url = UriComponentsBuilder.fromHttpUrl(webrtcStreamerBaseUrl + "/api/call")
                .queryParam("peerid", peerId)
                .queryParam("url", rtspUrl)
                .toUriString();

        log.info("Requesting offer from webrtc-streamer | peerId={} | rtspUrl={}", peerId, rtspUrl);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        log.info("Offer received from webrtc-streamer | peerId={}", peerId);
        return response.getBody();
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
        String url = UriComponentsBuilder.fromHttpUrl(webrtcStreamerBaseUrl + "/api/call")
                .queryParam("peerid", peerId)
                .queryParam("url", rtspUrl)
                .toUriString();

        Map<String, String> body = new HashMap<>();
        body.put("sdp", sdp);
        body.put("type", type);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        log.info("Sending answer to webrtc-streamer | peerId={}", peerId);
        restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        log.info("Answer sent successfully | peerId={}", peerId);
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
        String url = UriComponentsBuilder.fromHttpUrl(webrtcStreamerBaseUrl + "/api/addIceCandidate")
                .queryParam("peerid", peerId)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(candidate, headers);

        restTemplate.postForEntity(url, entity, Void.class);
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
        }
    }
}
