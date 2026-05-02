-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS webrtc_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE webrtc_db;

-- 创建摄像头表
CREATE TABLE IF NOT EXISTS `hikvision_camera` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '摄像头名称',
    `code` VARCHAR(50) NOT NULL COMMENT '摄像头编码（唯一标识）',
    `ip_address` VARCHAR(50) NOT NULL COMMENT 'IP地址',
    `rtsp_port` INT NOT NULL DEFAULT 554 COMMENT 'RTSP端口',
    `username` VARCHAR(100) DEFAULT NULL COMMENT '用户名',
    `password` VARCHAR(100) DEFAULT NULL COMMENT '密码',
    `channel` INT NOT NULL DEFAULT 1 COMMENT '通道号',
    `stream_type` INT NOT NULL DEFAULT 1 COMMENT '码流类型：1-主码流，2-子码流。RTSP地址格式：Channels/<通道号><两位码流类型>，如101=通道1主码流，102=通道1子码流',
    `location` VARCHAR(200) DEFAULT NULL COMMENT '位置信息',
    `status` INT NOT NULL DEFAULT 1 COMMENT '状态：0-离线，1-在线',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_status` (`status`),
    KEY `idx_ip_address` (`ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='海康摄像头配置表';

-- 插入测试数据（64个摄像头）
INSERT INTO `hikvision_camera` (`name`, `code`, `ip_address`, `rtsp_port`, `username`, `password`, `channel`, `stream_type`, `location`, `status`) VALUES
('测试摄像头-01', 'CAM001', '192.168.1.101', 554, 'admin', 'password123', 1, 1, '测试区域-1', 1),
('测试摄像头-02', 'CAM002', '192.168.1.102', 554, 'admin', 'password123', 1, 1, '测试区域-1', 1),
('测试摄像头-03', 'CAM003', '192.168.1.103', 554, 'admin', 'password123', 1, 1, '测试区域-1', 1),
('测试摄像头-04', 'CAM004', '192.168.1.104', 554, 'admin', 'password123', 1, 1, '测试区域-1', 1),
('测试摄像头-05', 'CAM005', '192.168.1.105', 554, 'admin', 'password123', 1, 1, '测试区域-1', 1),
('测试摄像头-06', 'CAM006', '192.168.1.106', 554, 'admin', 'password123', 1, 1, '测试区域-1', 1),
('测试摄像头-07', 'CAM007', '192.168.1.107', 554, 'admin', 'password123', 1, 1, '测试区域-1', 1),
('测试摄像头-08', 'CAM008', '192.168.1.108', 554, 'admin', 'password123', 1, 1, '测试区域-1', 1),
('测试摄像头-09', 'CAM009', '192.168.1.109', 554, 'admin', 'password123', 1, 1, '测试区域-2', 1),
('测试摄像头-10', 'CAM010', '192.168.1.110', 554, 'admin', 'password123', 1, 1, '测试区域-2', 1),
('测试摄像头-11', 'CAM011', '192.168.1.111', 554, 'admin', 'password123', 1, 1, '测试区域-2', 1),
('测试摄像头-12', 'CAM012', '192.168.1.112', 554, 'admin', 'password123', 1, 1, '测试区域-2', 1),
('测试摄像头-13', 'CAM013', '192.168.1.113', 554, 'admin', 'password123', 1, 1, '测试区域-2', 1),
('测试摄像头-14', 'CAM014', '192.168.1.114', 554, 'admin', 'password123', 1, 1, '测试区域-2', 1),
('测试摄像头-15', 'CAM015', '192.168.1.115', 554, 'admin', 'password123', 1, 1, '测试区域-2', 1),
('测试摄像头-16', 'CAM016', '192.168.1.116', 554, 'admin', 'password123', 1, 1, '测试区域-2', 1),
('测试摄像头-17', 'CAM017', '192.168.1.117', 554, 'admin', 'password123', 1, 1, '测试区域-3', 1),
('测试摄像头-18', 'CAM018', '192.168.1.118', 554, 'admin', 'password123', 1, 1, '测试区域-3', 1),
('测试摄像头-19', 'CAM019', '192.168.1.119', 554, 'admin', 'password123', 1, 1, '测试区域-3', 1),
('测试摄像头-20', 'CAM020', '192.168.1.120', 554, 'admin', 'password123', 1, 1, '测试区域-3', 1),
('测试摄像头-21', 'CAM021', '192.168.1.121', 554, 'admin', 'password123', 1, 1, '测试区域-3', 1),
('测试摄像头-22', 'CAM022', '192.168.1.122', 554, 'admin', 'password123', 1, 1, '测试区域-3', 1),
('测试摄像头-23', 'CAM023', '192.168.1.123', 554, 'admin', 'password123', 1, 1, '测试区域-3', 1),
('测试摄像头-24', 'CAM024', '192.168.1.124', 554, 'admin', 'password123', 1, 1, '测试区域-3', 1),
('测试摄像头-25', 'CAM025', '192.168.1.125', 554, 'admin', 'password123', 1, 1, '测试区域-4', 1),
('测试摄像头-26', 'CAM026', '192.168.1.126', 554, 'admin', 'password123', 1, 1, '测试区域-4', 1),
('测试摄像头-27', 'CAM027', '192.168.1.127', 554, 'admin', 'password123', 1, 1, '测试区域-4', 1),
('测试摄像头-28', 'CAM028', '192.168.1.128', 554, 'admin', 'password123', 1, 1, '测试区域-4', 1),
('测试摄像头-29', 'CAM029', '192.168.1.129', 554, 'admin', 'password123', 1, 1, '测试区域-4', 1),
('测试摄像头-30', 'CAM030', '192.168.1.130', 554, 'admin', 'password123', 1, 1, '测试区域-4', 1),
('测试摄像头-31', 'CAM031', '192.168.1.131', 554, 'admin', 'password123', 1, 1, '测试区域-4', 1),
('测试摄像头-32', 'CAM032', '192.168.1.132', 554, 'admin', 'password123', 1, 1, '测试区域-4', 1),
('测试摄像头-33', 'CAM033', '192.168.1.133', 554, 'admin', 'password123', 1, 1, '测试区域-5', 1),
('测试摄像头-34', 'CAM034', '192.168.1.134', 554, 'admin', 'password123', 1, 1, '测试区域-5', 1),
('测试摄像头-35', 'CAM035', '192.168.1.135', 554, 'admin', 'password123', 1, 1, '测试区域-5', 1),
('测试摄像头-36', 'CAM036', '192.168.1.136', 554, 'admin', 'password123', 1, 1, '测试区域-5', 1),
('测试摄像头-37', 'CAM037', '192.168.1.137', 554, 'admin', 'password123', 1, 1, '测试区域-5', 1),
('测试摄像头-38', 'CAM038', '192.168.1.138', 554, 'admin', 'password123', 1, 1, '测试区域-5', 1),
('测试摄像头-39', 'CAM039', '192.168.1.139', 554, 'admin', 'password123', 1, 1, '测试区域-5', 1),
('测试摄像头-40', 'CAM040', '192.168.1.140', 554, 'admin', 'password123', 1, 1, '测试区域-5', 1),
('测试摄像头-41', 'CAM041', '192.168.1.141', 554, 'admin', 'password123', 1, 1, '测试区域-6', 1),
('测试摄像头-42', 'CAM042', '192.168.1.142', 554, 'admin', 'password123', 1, 1, '测试区域-6', 1),
('测试摄像头-43', 'CAM043', '192.168.1.143', 554, 'admin', 'password123', 1, 1, '测试区域-6', 1),
('测试摄像头-44', 'CAM044', '192.168.1.144', 554, 'admin', 'password123', 1, 1, '测试区域-6', 1),
('测试摄像头-45', 'CAM045', '192.168.1.145', 554, 'admin', 'password123', 1, 1, '测试区域-6', 1),
('测试摄像头-46', 'CAM046', '192.168.1.146', 554, 'admin', 'password123', 1, 1, '测试区域-6', 1),
('测试摄像头-47', 'CAM047', '192.168.1.147', 554, 'admin', 'password123', 1, 1, '测试区域-6', 1),
('测试摄像头-48', 'CAM048', '192.168.1.148', 554, 'admin', 'password123', 1, 1, '测试区域-6', 1),
('测试摄像头-49', 'CAM049', '192.168.1.149', 554, 'admin', 'password123', 1, 1, '测试区域-7', 1),
('测试摄像头-50', 'CAM050', '192.168.1.150', 554, 'admin', 'password123', 1, 1, '测试区域-7', 1),
('测试摄像头-51', 'CAM051', '192.168.1.101', 554, 'admin', 'password123', 1, 1, '测试区域-7', 1),
('测试摄像头-52', 'CAM052', '192.168.1.102', 554, 'admin', 'password123', 1, 1, '测试区域-7', 1),
('测试摄像头-53', 'CAM053', '192.168.1.103', 554, 'admin', 'password123', 1, 1, '测试区域-7', 1),
('测试摄像头-54', 'CAM054', '192.168.1.104', 554, 'admin', 'password123', 1, 1, '测试区域-7', 1),
('测试摄像头-55', 'CAM055', '192.168.1.105', 554, 'admin', 'password123', 1, 1, '测试区域-7', 1),
('测试摄像头-56', 'CAM056', '192.168.1.106', 554, 'admin', 'password123', 1, 1, '测试区域-7', 1),
('测试摄像头-57', 'CAM057', '192.168.1.107', 554, 'admin', 'password123', 1, 1, '测试区域-8', 1),
('测试摄像头-58', 'CAM058', '192.168.1.108', 554, 'admin', 'password123', 1, 1, '测试区域-8', 1),
('测试摄像头-59', 'CAM059', '192.168.1.109', 554, 'admin', 'password123', 1, 1, '测试区域-8', 1),
('测试摄像头-60', 'CAM060', '192.168.1.110', 554, 'admin', 'password123', 1, 1, '测试区域-8', 1),
('测试摄像头-61', 'CAM061', '192.168.1.111', 554, 'admin', 'password123', 1, 1, '测试区域-8', 1),
('测试摄像头-62', 'CAM062', '192.168.1.112', 554, 'admin', 'password123', 1, 1, '测试区域-8', 1),
('测试摄像头-63', 'CAM063', '192.168.1.113', 554, 'admin', 'password123', 1, 1, '测试区域-8', 1),
('测试摄像头-64', 'CAM064', '192.168.1.114', 554, 'admin', 'password123', 1, 1, '测试区域-8', 1);

-- 本地模拟测试摄像头（配合 MediaMTX + FFmpeg 使用）
-- FFmpeg 推流命令：ffmpeg -re -stream_loop -1 -i "test.mp4" -c:v libx264 -g 25 -c:a aac -f rtsp rtsp://localhost:8554/Streaming/Channels/101
UPDATE `webrtc_db`.`hikvision_camera` SET `name` = '模拟摄像头-01', `code` = 'CAM001', `ip_address` = 'localhost', `rtsp_port` = 8554, `username` = 'admin', `password` = 'password', `channel` = 1, `stream_type` = 1, `location` = '测试区域-1', `status` = 1, `create_time` = '2026-05-02 16:57:46', `update_time` = '2026-05-02 16:57:56' WHERE `id` = 1;
UPDATE `webrtc_db`.`hikvision_camera` SET `name` = '模拟摄像头-02', `code` = 'CAM002', `ip_address` = 'localhost', `rtsp_port` = 8554, `username` = 'admin', `password` = 'password', `channel` = 1, `stream_type` = 2, `location` = '测试区域-2', `status` = 1, `create_time` = '2026-05-02 16:57:46', `update_time` = '2026-05-02 16:57:58' WHERE `id` = 2;


