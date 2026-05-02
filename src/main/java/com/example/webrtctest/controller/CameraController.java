package com.example.webrtctest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.webrtctest.entity.HikvisionCamera;
import com.example.webrtctest.mapper.CameraMapper;
import com.example.webrtctest.service.HikvisionService;
import com.example.webrtctest.service.StreamManager;
import com.example.webrtctest.service.StreamSession;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 摄像头API控制器
 */
@RestController
@RequestMapping("/api/cameras")
@Slf4j
@CrossOrigin(origins = "*")
public class CameraController {

    @Autowired
    private CameraMapper cameraMapper;

    @Autowired
    private HikvisionService hikvisionService;

    @Autowired
    private StreamManager streamManager;

    /**
     * 获取所有摄像头列表
     */
    @GetMapping
    public ResponseEntity<List<HikvisionCamera>> getAllCameras() {
        log.info("Fetching all cameras");
        List<HikvisionCamera> cameras = cameraMapper.selectList(null);
        return ResponseEntity.ok(cameras);
    }

    /**
     * 获取在线摄像头列表
     */
    @GetMapping("/online")
    public ResponseEntity<List<HikvisionCamera>> getOnlineCameras() {
        log.info("Fetching online cameras");
        LambdaQueryWrapper<HikvisionCamera> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HikvisionCamera::getStatus, 1);
        List<HikvisionCamera> cameras = cameraMapper.selectList(wrapper);
        return ResponseEntity.ok(cameras);
    }

    /**
     * 根据ID获取摄像头
     */
    @GetMapping("/{id}")
    public ResponseEntity<HikvisionCamera> getCameraById(@PathVariable Long id) {
        HikvisionCamera camera = cameraMapper.selectById(id);
        return camera != null ? ResponseEntity.ok(camera) : ResponseEntity.notFound().build();
    }

    /**
     * 根据编码获取摄像头
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<HikvisionCamera> getCameraByCode(@PathVariable String code) {
        LambdaQueryWrapper<HikvisionCamera> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HikvisionCamera::getCode, code);
        HikvisionCamera camera = cameraMapper.selectOne(wrapper);
        return camera != null ? ResponseEntity.ok(camera) : ResponseEntity.notFound().build();
    }

    /**
     * 请求播放视频流（核心接口）
     * 实现流复用逻辑，返回 webrtc-streamer 的视频 ID
     */
    @PostMapping("/{code}/play")
    public ResponseEntity<Map<String, Object>> playStream(
            @PathVariable String code,
            @RequestBody PlayRequest request) {

        log.info("\n📹 收到播放请求 | 摄像头: {} | 用户: {}", code, request.getUserId());

        // 查询摄像头信息
        LambdaQueryWrapper<HikvisionCamera> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HikvisionCamera::getCode, code);
        HikvisionCamera camera = cameraMapper.selectOne(wrapper);

        if (camera == null) {
            throw new RuntimeException("Camera not found: " + code);
        }

        // 检查摄像头状态
        if (camera.getStatus() != 1) {
            throw new RuntimeException("Camera is offline: " + code);
        }

        // 构建 RTSP 地址
        String rtspUrl = hikvisionService.buildRtspUrl(camera);

        // 通过 StreamManager 获取流（支持复用）
        StreamSession session = streamManager.acquireStream(
                code,
                request.getUserId(),
                rtspUrl
        );

        // 构建响应 - 返回 RTSP 地址和会话信息，前端通过后端信令接口建立 WebRTC 连接
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("sessionId", session.getSessionId());
        response.put("cameraCode", code);
        response.put("cameraName", camera.getName());
        response.put("rtspUrl", rtspUrl);  // RTSP 流地址，供后端与 webrtc-streamer 交互
        response.put("isMockMode", hikvisionService.isMockMode());
        response.put("referenceCount", session.getReferenceCount());

        log.info("✅ 流已准备 | 摄像头: {} | 会话: {} | 引用计数: {}",
                code, session.getSessionId(), session.getReferenceCount());

        return ResponseEntity.ok(response);
    }

    /**
     * 停止播放视频流
     */
    @PostMapping("/{code}/stop")
    public ResponseEntity<Map<String, Object>> stopStream(
            @PathVariable String code,
            @RequestBody StopRequest request) {

        log.info("\n⏹️  收到停止请求 | 摄像头: {} | 用户: {}", code, request.getUserId());

        // 释放流
        streamManager.releaseStream(code, request.getUserId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Stream released");
        response.put("cameraCode", code);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前活跃流统计信息
     */
    @GetMapping("/stats/streams")
    public ResponseEntity<Map<String, Object>> getStreamStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeStreamCount", streamManager.getActiveStreamCount());
        stats.put("maxActiveStreams", 64); // 从配置读取
        stats.put("streams", streamManager.getActiveStreams());

        return ResponseEntity.ok(stats);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("mockMode", hikvisionService.isMockMode());
        health.put("activeStreams", streamManager.getActiveStreamCount());

        return ResponseEntity.ok(health);
    }

    /**
     * 播放请求DTO
     */
    @Data
    public static class PlayRequest {
        private String userId;
    }

    /**
     * 停止请求DTO
     */
    @Data
    public static class StopRequest {
        private String userId;
    }
}
