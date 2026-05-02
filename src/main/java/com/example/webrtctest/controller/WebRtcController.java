package com.example.webrtctest.controller;

import com.example.webrtctest.service.StreamManager;
import com.example.webrtctest.service.StreamSession;
import com.example.webrtctest.service.WebRtcProxyService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * WebRTC 信令控制器
 * 作为前端与 webrtc-streamer 之间的信令代理
 * 前端不再直连 webrtc-streamer，所有信令交互都走后端
 */
@RestController
@RequestMapping("/api/webrtc")
@Slf4j
@CrossOrigin(origins = "*")
public class WebRtcController {

    @Autowired
    private WebRtcProxyService webRtcProxyService;

    @Autowired
    private StreamManager streamManager;

    /**
     * 创建 WebRTC Offer
     * 后端调用 webrtc-streamer 获取 SDP Offer 返回给前端
     */
    @PostMapping("/offer")
    public ResponseEntity<Map<String, Object>> createOffer(@RequestBody OfferRequest request) {
        log.info("WebRTC offer request | cameraCode={} | peerId={}", request.getCameraCode(), request.getPeerId());

        StreamSession session = streamManager.getStreamByCamera(request.getCameraCode());
        if (session == null || !session.isActive()) {
            log.warn("Stream not found or inactive | cameraCode={}", request.getCameraCode());
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> offer = webRtcProxyService.createOffer(request.getPeerId(), session.getRtspUrl());
        return ResponseEntity.ok(offer);
    }

    /**
     * 设置 WebRTC Answer
     * 前端创建 Answer 后，通过此接口由后端转发给 webrtc-streamer
     */
    @PostMapping("/answer")
    public ResponseEntity<Void> setAnswer(@RequestBody AnswerRequest request) {
        log.info("WebRTC answer received | cameraCode={} | peerId={}", request.getCameraCode(), request.getPeerId());

        StreamSession session = streamManager.getStreamByCamera(request.getCameraCode());
        if (session == null || !session.isActive()) {
            return ResponseEntity.notFound().build();
        }

        webRtcProxyService.setAnswer(request.getPeerId(), session.getRtspUrl(), request.getSdp(), request.getType());
        return ResponseEntity.ok().build();
    }

    /**
     * 获取 ICE Candidates（webrtc-streamer 侧）
     */
    @GetMapping("/ice/{peerId}")
    public ResponseEntity<List<Map<String, Object>>> getIceCandidates(@PathVariable String peerId) {
        List<Map<String, Object>> candidates = webRtcProxyService.getIceCandidates(peerId);
        if (candidates == null || candidates.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(candidates);
    }

    /**
     * 添加 ICE Candidate（浏览器侧）
     */
    @PostMapping("/ice")
    public ResponseEntity<Void> addIceCandidate(@RequestBody IceCandidateRequest request) {
        webRtcProxyService.addIceCandidate(request.getPeerId(), request.getCandidate());
        return ResponseEntity.ok().build();
    }

    /**
     * 断开连接
     */
    @PostMapping("/hangup/{peerId}")
    public ResponseEntity<Void> hangup(@PathVariable String peerId) {
        webRtcProxyService.hangup(peerId);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class OfferRequest {
        private String cameraCode;
        private String peerId;
    }

    @Data
    public static class AnswerRequest {
        private String cameraCode;
        private String peerId;
        private String sdp;
        private String type;
    }

    @Data
    public static class IceCandidateRequest {
        private String peerId;
        private Map<String, Object> candidate;
    }
}
