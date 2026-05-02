package com.example.webrtctest.service;

import com.example.webrtctest.entity.HikvisionCamera;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 海康摄像头服务
 * 支持本地测试模拟和真实设备接入
 */
@Service
@Slf4j
public class HikvisionService {
    
    @Value("${webrtc-streamer.hikvision.mock-mode:true}")
    private boolean mockMode;
    
    @Value("${webrtc-streamer.hikvision.rtsp-base-url:rtsp://admin:password@192.168.1.100:554/Streaming/Channels/}")
    private String rtspBaseUrl;
    
    @Value("${webrtc-streamer.hikvision.username:admin}")
    private String defaultUsername;
    
    @Value("${webrtc-streamer.hikvision.password:password123}")
    private String defaultPassword;
    
    /**
     * 构建RTSP流地址
     * @param camera 摄像头信息
     * @return RTSP地址
     */
    public String buildRtspUrl(HikvisionCamera camera) {
        if (mockMode) {
            // 本地测试模式：返回模拟的RTSP地址
            String mockUrl = String.format("rtsp://localhost:8554/mock_stream_%s", camera.getCode());
            log.debug("Mock RTSP URL for camera {}: {}", camera.getCode(), mockUrl);
            return mockUrl;
        } else {
            // 真实设备模式：构建真实的RTSP地址
            String username = camera.getUsername() != null ? camera.getUsername() : defaultUsername;
            String password = camera.getPassword() != null ? camera.getPassword() : defaultPassword;
            
            // 海康摄像头RTSP格式：
            // rtsp://[username]:[password]@[ip]:[port]/Streaming/Channels/[channel][streamType]
            // channel: 通道号（1开始）
            // streamType: 码流类型（1-主码流，2-子码流）
            // 海康格式：rtsp://admin:password@ip:554/Streaming/Channels/<通道号><两位码流类型>
            // 例如：通道1主码流=101，通道1子码流=102
            String rtspUrl = String.format("rtsp://%s:%s@%s:%d/Streaming/Channels/%d%02d",
                    username,
                    password,
                    camera.getIpAddress(),
                    camera.getRtspPort(),
                    camera.getChannel(),
                    camera.getStreamType()
            );
            
            log.debug("Real RTSP URL for camera {}: {}", camera.getCode(), rtspUrl);
            return rtspUrl;
        }
    }
    
    /**
     * 验证摄像头连接（模拟或真实）
     * @param camera 摄像头信息
     * @return 是否可连接
     */
    public boolean verifyCameraConnection(HikvisionCamera camera) {
        if (mockMode) {
            // 模拟模式：直接返回成功
            log.debug("Mock mode: Camera {} connection verified", camera.getCode());
            return true;
        } else {
            // 真实模式：尝试连接摄像头
            try {
                // TODO: 实现真实的摄像头连接验证逻辑
                // 可以使用HTTP API或RTSP连接测试
                log.info("Verifying real camera connection: {}", camera.getIpAddress());
                return true;
            } catch (Exception e) {
                log.error("Failed to connect to camera: {}", camera.getCode(), e);
                return false;
            }
        }
    }
    
    /**
     * 获取摄像头状态
     * @param camera 摄像头信息
     * @return 是否在线
     */
    public boolean getCameraStatus(HikvisionCamera camera) {
        if (mockMode) {
            // 模拟模式：根据数据库状态返回
            return camera.getStatus() == 1;
        } else {
            // 真实模式：实际检测摄像头状态
            return verifyCameraConnection(camera);
        }
    }
    
    /**
     * 判断是否为模拟模式
     */
    public boolean isMockMode() {
        return mockMode;
    }
}
