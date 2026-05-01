package com.example.webrtctest.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebRTC流会话信息
 */
@Data
@Slf4j
public class StreamSession {
    
    /**
     * 会话ID（唯一标识）
     */
    private String sessionId;
    
    /**
     * 摄像头编码
     */
    private String cameraCode;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * RTSP流地址
     */
    private String rtspUrl;
    
    /**
     * WebRTC流地址
     */
    private String webrtcUrl;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessTime;
    
    /**
     * 引用计数（多少个用户在使用这个流）
     */
    private AtomicInteger referenceCount;
    
    /**
     * 是否活跃
     */
    private volatile boolean active;
    
    public StreamSession(String sessionId, String cameraCode, String userId, String rtspUrl) {
        this.sessionId = sessionId;
        this.cameraCode = cameraCode;
        this.userId = userId;
        this.rtspUrl = rtspUrl;
        this.createTime = LocalDateTime.now();
        this.lastAccessTime = LocalDateTime.now();
        this.referenceCount = new AtomicInteger(1);
        this.active = true;
    }
    
    /**
     * 增加引用计数
     */
    public int incrementReference() {
        int count = referenceCount.incrementAndGet();
        this.lastAccessTime = LocalDateTime.now();
        log.debug("Stream {} reference count increased to {}", sessionId, count);
        return count;
    }
    
    /**
     * 减少引用计数
     */
    public int decrementReference() {
        int count = referenceCount.decrementAndGet();
        this.lastAccessTime = LocalDateTime.now();
        log.debug("Stream {} reference count decreased to {}", sessionId, count);
        return count;
    }
    
    /**
     * 获取引用计数
     */
    public int getReferenceCount() {
        return referenceCount.get();
    }
    
    /**
     * 更新最后访问时间
     */
    public void touch() {
        this.lastAccessTime = LocalDateTime.now();
    }
    
    /**
     * 检查是否超时
     */
    public boolean isIdle(long timeoutMs) {
        long idleTime = java.time.Duration.between(lastAccessTime, LocalDateTime.now()).toMillis();
        return idleTime > timeoutMs;
    }
}
