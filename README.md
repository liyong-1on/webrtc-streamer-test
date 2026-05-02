# WebRTC 视频监控系统 🎥

基于 **Spring Boot + Vue 3 + webrtc-streamer** 的低延迟视频监控解决方案。

采用**后端信令代理架构**：前端不再直连 webrtc-streamer，所有 WebRTC 信令交互（SDP Offer/Answer、ICE Candidate 交换）均由后端代理完成，前端仅需使用浏览器原生 WebRTC API 接收视频流。

---

## ✨ 核心特性

- ✅ **超低延迟**：< 500ms（WebRTC 协议）
- ✅ **流复用机制**：多个用户观看同一路摄像头，只占用一个 RTSP 连接，通过引用计数共享
- ✅ **后端信令代理**：前端不暴露 webrtc-streamer 地址，所有信令交互走后端，安全性更高
- ✅ **原生 WebRTC 播放**：前端使用浏览器原生 `RTCPeerConnection`，无需加载 `webrtcstreamer.js`
- ✅ **自动资源管理**：引用计数 + 空闲超时 + 定时清理，防止资源泄漏
- ✅ **响应式设计**：支持从手机到大屏的各种分辨率
- ✅ **本地测试模式**：无需真实摄像头即可开发测试

---

## 🏗️ 架构设计

```
┌─────────────┐                          ┌──────────────┐
│   浏览器     │ ① POST /api/cameras/xxx/play │              │
│  (Vue3)     │─────────────────────────→│   后端服务    │
│             │                          │  (Spring Boot)│
│ RTCPeerConnection                     │    :8081     │
│             │ ② POST /api/webrtc/offer  │              │
│             │─────────────────────────→│              │
│             │ ←────────────────────────│              │
│             │    返回 SDP Offer          │              │
│             │                          │              │
│             │ ③ POST /api/webrtc/answer │              │
│             │─────────────────────────→│              │
│             │                          │   WebRtcProxy │
│             │ ④ 轮询 /api/webrtc/ice    │    Service   │
│             │─────────────────────────→│              │
│             │                          │      ↓       │
│             │                          │  HTTP 信令    │
│             │                          │      ↓       │
│ ontrack     │ ←────────────────────────│ webrtc-streamer
│ 接收视频流   │      WebRTC 媒体通道      │   :8000      │
└─────────────┘                          └──────────────┘
```

### 播放流程详解

1. **获取流信息**：前端调用 `POST /api/cameras/{code}/play`，后端查询摄像头、构建 RTSP 地址、创建流会话，返回 `{cameraCode, sessionId, rtspUrl}`
2. **获取 Offer**：前端生成唯一 `peerId`，调用 `POST /api/webrtc/offer`，后端代为请求 webrtc-streamer 生成 SDP Offer 并返回
3. **设置 Answer**：前端创建 `RTCPeerConnection`，设置 Remote Description，生成 Answer，调用 `POST /api/webrtc/answer` 由后端转发给 webrtc-streamer
4. **ICE 交换**：前后端通过 `/api/webrtc/ice` 系列接口交换 ICE Candidate
5. **接收视频**：WebRTC 连接建立后，浏览器 `ontrack` 事件接收 `MediaStream`，绑定到 `<video>` 标签播放
6. **停止播放**：前端调用 `POST /api/cameras/{code}/stop` 释放流，并调用 `POST /api/webrtc/hangup/{peerId}` 断开 WebRTC 连接

---

## 📦 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **前端** | Vue 3 + Vite（原生 WebRTC API） | Vue 3.4, Vite 5 |
| **后端** | Spring Boot + MyBatis-Plus | Spring Boot 2.5.0, MyBatis-Plus 3.5.7 |
| **数据库** | MySQL（开发/生产）/ H2（测试）| MySQL 8+ |
| **流媒体** | webrtc-streamer（RTSP → WebRTC）| 独立进程 :8000 |
| **HTTP 客户端** | Spring RestTemplate | 内置 |

---

## 📁 项目结构

```
webrtc-streamer/
├── frontend/                      # Vue 3 前端
│   ├── src/
│   │   ├── App.vue               # 主页面（摄像头网格）
│   │   ├── VideoPlayer.vue       # 视频播放器（原生 WebRTC）
│   │   └── main.js               # 入口
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
├── src/main/java/com/example/webrtctest/
│   ├── config/
│   │   ├── CorsConfig.java        # 跨域配置
│   │   └── DataInitializer.java   # 数据初始化
│   ├── controller/
│   │   ├── CameraController.java  # 摄像头管理 API
│   │   └── WebRtcController.java  # WebRTC 信令代理 API
│   ├── entity/
│   │   └── HikvisionCamera.java   # 摄像头实体
│   ├── mapper/
│   │   └── CameraMapper.java      # MyBatis-Plus 数据访问
│   ├── service/
│   │   ├── HikvisionService.java  # 海康摄像头服务（RTSP 地址构建）
│   │   ├── StreamManager.java     # 流复用管理器（核心）
│   │   ├── StreamSession.java     # 流会话实体
│   │   └── WebRtcProxyService.java # webrtc-streamer 代理服务
│   └── WebrtcTestApplication.java # 启动类
├── src/main/resources/
│   ├── static/
│   └── application.yaml           # 配置文件
├── schema.sql                     # 数据库建表脚本
├── pom.xml                        # Maven 配置
└── README.md                      # 本文档
```

---

## 🚀 快速开始

### 前置条件

1. **Node.js** >= 16
2. **Java** >= 8（项目编译目标 1.8，建议 JDK 17）
3. **Maven** >= 3.6
4. **MySQL**（可选，mock-mode=true 时不需要真实摄像头和数据库）
5. **webrtc-streamer**（独立进程，默认 http://127.0.0.1:8000）

### 1. 启动 webrtc-streamer

```bash
# Windows
webrtc-streamer.exe -H 0.0.0.0:8000

# Linux
./webrtc-streamer -H 0.0.0.0:8000
```

验证：浏览器访问 http://127.0.0.1:8000 应显示 webrtc-streamer 页面。

### 2. 启动后端

```bash
# 进入项目根目录
cd webrtc-streamer

# 编译并运行
mvn spring-boot:run
```

后端服务将启动在 http://localhost:8081

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器将启动在 http://localhost:3000

### 4. 访问系统

打开浏览器访问 http://localhost:3000，点击任意摄像头即可播放。

---

## ⚙️ 配置说明

### 后端配置（`application.yaml`）

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/webrtc_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: root

# WebRTC Streamer 配置
webrtc-streamer:
  base-url: http://127.0.0.1:8000        # webrtc-streamer 服务地址
  stream:
    max-active-streams: 64               # 最大活跃流数量
    idle-timeout: 300000                 # 空闲超时（5分钟）
    cleanup-interval: 60000              # 清理间隔（1分钟）
  hikvision:
    mock-mode: true                      # true=模拟模式（无需真实摄像头）
    rtsp-base-url: rtsp://admin:password@192.168.1.100:554/Streaming/Channels/
    username: admin
    password: password123
```

| 配置项 | 说明 |
|--------|------|
| `webrtc-streamer.base-url` | webrtc-streamer 服务地址，后端通过 HTTP 与其交互 |
| `stream.max-active-streams` | 系统最多同时维护的 RTSP 流数量 |
| `stream.idle-timeout` | 流多久无访问后自动释放（毫秒） |
| `hikvision.mock-mode` | `true` 返回模拟 RTSP 地址；`false` 根据摄像头信息构建真实 RTSP 地址 |

### webrtc-streamer 启动参数

```bash
# 基础启动（允许局域网访问）
webrtc-streamer.exe -H 0.0.0.0:8000

# 如需限制来源（生产环境建议）
webrtc-streamer.exe -H 0.0.0.0:8000 -cors-domain=http://localhost:3000
```

---

## 📝 API 文档

### 摄像头管理接口

#### 获取所有摄像头
```http
GET /api/cameras
```

#### 获取在线摄像头
```http
GET /api/cameras/online
```

#### 根据编码获取摄像头
```http
GET /api/cameras/code/{code}
```

#### 播放视频流（获取流信息）
```http
POST /api/cameras/{code}/play
Content-Type: application/json

{
  "userId": "user_xxx"
}
```

**Response:**
```json
{
  "success": true,
  "sessionId": "uuid-xxx",
  "cameraCode": "CAM001",
  "cameraName": "大门口",
  "rtspUrl": "rtsp://localhost:8554/mock_stream_CAM001",
  "isMockMode": true,
  "referenceCount": 1
}
```

#### 停止播放
```http
POST /api/cameras/{code}/stop
Content-Type: application/json

{
  "userId": "user_xxx"
}
```

#### 流统计信息
```http
GET /api/cameras/stats/streams
```

#### 健康检查
```http
GET /api/cameras/health
```

### WebRTC 信令代理接口

前端通过以下接口与 webrtc-streamer 进行信令交互，**所有请求都走后端**。

#### 创建 Offer
```http
POST /api/webrtc/offer
Content-Type: application/json

{
  "cameraCode": "CAM001",
  "peerId": "peer_abc123_1699123456789"
}
```

**Response:**
```json
{
  "sdp": "v=0\r\no=- ...",
  "type": "offer"
}
```

#### 设置 Answer
```http
POST /api/webrtc/answer
Content-Type: application/json

{
  "cameraCode": "CAM001",
  "peerId": "peer_abc123_1699123456789",
  "sdp": "v=0\r\no=- ...",
  "type": "answer"
}
```

#### 获取 ICE Candidates（webrtc-streamer 侧）
```http
GET /api/webrtc/ice/{peerId}
```

**Response:**
```json
[
  {
    "candidate": "candidate:...",
    "sdpMid": "0",
    "sdpMLineIndex": 0
  }
]
```

#### 添加 ICE Candidate（浏览器侧）
```http
POST /api/webrtc/ice
Content-Type: application/json

{
  "peerId": "peer_abc123_1699123456789",
  "candidate": {
    "candidate": "candidate:...",
    "sdpMid": "0",
    "sdpMLineIndex": 0
  }
}
```

#### 断开连接
```http
POST /api/webrtc/hangup/{peerId}
```

---

## 🔧 后端核心类说明

| 类名 | 职责 |
|------|------|
| `CameraController` | 摄像头 CRUD、播放/停止接口 |
| `WebRtcController` | WebRTC 信令代理接口（offer/answer/ice/hangup） |
| `StreamManager` | **核心**。管理活跃流会话，实现流复用（引用计数）、空闲清理 |
| `StreamSession` | 单个流会话的数据结构（sessionId、rtspUrl、referenceCount、active 等） |
| `HikvisionService` | 构建 RTSP URL（支持 mock 模式和真实海康设备模式） |
| `WebRtcProxyService` | 通过 HTTP 与 webrtc-streamer 进程交互，转发 SDP 和 ICE |

### 流复用机制

`StreamManager` 使用 `ConcurrentHashMap` 维护两个映射：
- `activeStreams`: `cameraCode → StreamSession`（一个摄像头对应一个流）
- `userSessions`: `userId_cameraCode → sessionId`（记录用户正在观看的摄像头）

当新用户请求播放已有活跃流的摄像头时，直接复用现有 `StreamSession`，引用计数 +1；当用户停止播放时，引用计数 -1，计数归零后标记为非活跃，等待定时清理。

---

## 🐛 常见问题

### 1. 视频无法播放

**排查步骤：**
1. 确认 webrtc-streamer 已启动：访问 http://127.0.0.1:8000
2. 确认后端已启动且无报错：访问 http://localhost:8081/api/cameras/health
3. 打开浏览器 F12，检查 Console 和 Network：
   - `/api/cameras/{code}/play` 是否返回 200 和 `rtspUrl`？
   - `/api/webrtc/offer` 是否返回 SDP？
   - WebRTC `connectionState` 是否为 `connected`？

### 2. 后端提示 "Stream not found or inactive"

流会话可能已被清理。原因：
- 长时间无用户观看，超过 `idle-timeout`（默认 5 分钟）
- 所有观看用户都已调用 `stop`

**解决**：重新点击播放即可。

### 3. 如何接入真实海康摄像头？

1. 修改 `application.yaml`：
   ```yaml
   hikvision:
     mock-mode: false
   ```
2. 在数据库中录入摄像头信息（IP、端口、用户名、密码、通道号、码流类型）
3. 确保 webrtc-streamer 能访问摄像头的 RTSP 端口（默认 554）
4. 重新播放即可自动构建真实 RTSP 地址

### 4. webrtc-streamer 与后端不在同一台机器

修改 `application.yaml`：
```yaml
webrtc-streamer:
  base-url: http://192.168.1.xxx:8000
```

### 5. 前端报 "Failed to get WebRTC offer"

- 检查后端日志中 `WebRtcProxyService` 是否能成功访问 webrtc-streamer
- 确认 webrtc-streamer 的 `/api/call` 接口可用
- 检查 RTSP 地址是否有效（可在 VLC 中测试播放）

---

## 🎯 下一步计划

- [ ] 实现用户认证与权限控制（JWT）
- [ ] 添加云台控制（PTZ）接口
- [ ] 支持视频录制与回放
- [ ] 扩展支持大华、宇视等更多品牌摄像头
- [ ] 添加 WebRTC 连接质量监控（码率、丢包率统计）
- [ ] 后端集成 webrtc-streamer（减少独立进程依赖）

---

## 📄 许可证

MIT License

---

## 🙏 致谢

- [webrtc-streamer](https://github.com/mpromonet/webrtc-streamer) - 优秀的 RTSP to WebRTC 开源方案
- [Vue.js](https://vuejs.org/) - 渐进式 JavaScript 框架
- [Spring Boot](https://spring.io/projects/spring-boot) - Java 微服务框架
- [MyBatis-Plus](https://baomidou.com/) - 增强型 MyBatis 工具
