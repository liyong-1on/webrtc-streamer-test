package com.example.webrtctest.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebRTC流管理器 - 核心服务
 * 实现流复用和资源自动释放
 */
@Service
@Slf4j
public class StreamManager {

    /**
     * 活跃流会话映射：cameraCode -> StreamSession
     * 同一个摄像头的流只创建一个实例，多个用户共享
     */
    private final Map<String, StreamSession> activeStreams = new ConcurrentHashMap<>();

    /**
     * 用户会话映射：userId_cameraCode -> sessionId
     * 记录每个用户正在观看的摄像头
     */
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();

    @Value("${webrtc-streamer.stream.max-active-streams:64}")
    private int maxActiveStreams;

    @Value("${webrtc-streamer.stream.idle-timeout:300000}")
    private long idleTimeout;

    @PostConstruct
    public void init() {
        log.info("StreamManager initialized with maxActiveStreams={}, idleTimeout={}ms",
                maxActiveStreams, idleTimeout);
    }

    /**
     * 请求获取流（支持流复用）
     *
     * @param cameraCode 摄像头编码
     * @param userId     用户ID
     * @param rtspUrl    RTSP流地址
     * @return StreamSession
     */
    public synchronized StreamSession acquireStream(String cameraCode, String userId, String rtspUrl) {
        String userKey = userId + "_" + cameraCode;

        // 检查用户是否已经有这个摄像头的会话
        if (userSessions.containsKey(userKey)) {
            String existingSessionId = userSessions.get(userKey);
            StreamSession existingSession = activeStreams.get(cameraCode);
            if (existingSession != null && existingSession.isActive()) {
                existingSession.incrementReference();
                existingSession.touch();
                log.info("\n========== 🔄 流复用 ==========\n"
                                + "摄像头: {}\n"
                                + "用户: {}\n"
                                + "会话ID: {}\n"
                                + "引用计数: {} → {}\n"
                                + "活跃流总数: {}\n"
                                + "==============================\n",
                        cameraCode, userId, existingSessionId,
                        existingSession.getReferenceCount() - 1, existingSession.getReferenceCount(),
                        activeStreams.size());
                return existingSession;
            }
        }

        // 检查是否已有该摄像头的活跃流
        StreamSession session = activeStreams.get(cameraCode);
        if (session != null && session.isActive()) {
            // 流复用：增加引用计数
            int oldRefCount = session.getReferenceCount();
            session.incrementReference();
            session.touch();
            userSessions.put(userKey, session.getSessionId());
            log.info("\n========== 🔄 流复用 ==========\n"
                            + "摄像头: {}\n"
                            + "用户: {}\n"
                            + "会话ID: {}\n"
                            + "引用计数: {} → {}\n"
                            + "活跃流总数: {}\n"
                            + "==============================\n",
                    cameraCode, userId, session.getSessionId(),
                    oldRefCount, session.getReferenceCount(),
                    activeStreams.size());
            return session;
        }

        // 检查是否超过最大流数量
        if (activeStreams.size() >= maxActiveStreams) {
            log.warn("\n⚠️  警告: 已达到最大活跃流数量 ({})，正在清理空闲流...\n", maxActiveStreams);
            // 可以选择清理最久未使用的流
            cleanupIdleStreams();
        }

        // 创建新的流会话
        String sessionId = UUID.randomUUID().toString();
        StreamSession newSession = new StreamSession(sessionId, cameraCode, userId, rtspUrl);

        activeStreams.put(cameraCode, newSession);
        userSessions.put(userKey, sessionId);

        log.info("\n========== ✨ 新建流 ==========\n"
                        + "摄像头: {}\n"
                        + "用户: {}\n"
                        + "会话ID: {}\n"
                        + "RTSP地址: {}\n"
                        + "引用计数: 1\n"
                        + "活跃流总数: {} → {}\n"
                        + "==============================\n",
                cameraCode, userId, sessionId, rtspUrl,
                activeStreams.size() - 1, activeStreams.size());

        return newSession;
    }

    /**
     * 释放流
     *
     * @param cameraCode 摄像头编码
     * @param userId     用户ID
     */
    public synchronized void releaseStream(String cameraCode, String userId) {
        String userKey = userId + "_" + cameraCode;
        StreamSession session = activeStreams.get(cameraCode);

        if (session == null) {
            log.warn("⚠️  警告: 未找到摄像头 {} 的活跃流", cameraCode);
            return;
        }

        // 减少引用计数
        int oldRefCount = session.getReferenceCount();
        int refCount = session.decrementReference();

        // 移除用户会话记录
        userSessions.remove(userKey);

        // 如果引用计数为0，标记为非活跃
        if (refCount <= 0) {
            session.setActive(false);
            log.info("\n========== 🛑 流已停止 ==========\n"
                            + "摄像头: {}\n"
                            + "用户: {}\n"
                            + "引用计数: {} → {}\n"
                            + "状态: 非活跃（等待自动清理）\n"
                            + "==============================\n",
                    cameraCode, userId, oldRefCount, refCount);
        } else {
            log.info("\n========== ➖ 引用减少 ==========\n"
                            + "摄像头: {}\n"
                            + "用户: {}\n"
                            + "引用计数: {} → {}\n"
                            + "状态: 仍活跃（{} 个用户在观看）\n"
                            + "==============================\n",
                    cameraCode, userId, oldRefCount, refCount, refCount);
        }
    }

    /**
     * 定时清理空闲流（每分钟执行一次）
     */
    @Scheduled(fixedDelayString = "${webrtc-streamer.stream.cleanup-interval:60000}")
    public void cleanupIdleStreams() {
        log.debug("\n🧹 开始清理空闲流...");

        int cleanedCount = 0;
        for (Map.Entry<String, StreamSession> entry : activeStreams.entrySet()) {
            StreamSession session = entry.getValue();

            // 清理非活跃且超时的流
            if (!session.isActive() || session.isIdle(idleTimeout)) {
                log.info("\n========== 🗑️  清理流 ==========\n"
                                + "摄像头: {}\n"
                                + "最后访问: {}\n"
                                + "引用计数: {}\n"
                                + "原因: {}\n"
                                + "==============================\n",
                        entry.getKey(),
                        session.getLastAccessTime(),
                        session.getReferenceCount(),
                        !session.isActive() ? "引用计数为0" : "空闲超时");
                cleanedCount++;
            }
        }

        activeStreams.entrySet().removeIf(entry -> {
            StreamSession session = entry.getValue();
            return !session.isActive() || session.isIdle(idleTimeout);
        });

        if (cleanedCount > 0) {
            log.info("\n✅ 清理完成: 移除了 {} 个空闲流，当前活跃流: {}\n",
                    cleanedCount, activeStreams.size());
        } else {
            log.debug("✅ 清理完成: 没有需要清理的流，当前活跃流: {}", activeStreams.size());
        }
    }

    /**
     * 获取活跃流数量
     */
    public int getActiveStreamCount() {
        return activeStreams.size();
    }

    /**
     * 获取所有活跃流
     */
    public Map<String, StreamSession> getActiveStreams() {
        return new ConcurrentHashMap<>(activeStreams);
    }

    /**
     * 根据摄像头编码获取流
     */
    public StreamSession getStreamByCamera(String cameraCode) {
        return activeStreams.get(cameraCode);
    }

    /**
     * 强制关闭指定摄像头的流
     */
    public synchronized void forceCloseStream(String cameraCode) {
        StreamSession session = activeStreams.remove(cameraCode);
        if (session != null) {
            session.setActive(false);
            log.warn("Force closed stream for camera={}", cameraCode);
        }
    }
}
