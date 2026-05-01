# WebRTC 视频监控系统 🎥

基于 **Spring Boot + Vue 3 + webrtc-streamer** 的低延迟视频监控解决方案。

---

## ✨ 核心特性

- ✅ **超低延迟**：< 500ms（WebRTC 协议）
- ✅ **流复用机制**：多个用户观看同一路摄像头，只占用一个 RTSP 连接
- ✅ **简洁架构**：前端只需调用 `connect(videoId)`，无需处理复杂的 WebRTC 协商
- ✅ **自动资源管理**：引用计数 + 定时清理，防止资源泄漏
- ✅ **响应式设计**：支持从手机到大屏的各种分辨率
- ✅ **本地测试模式**：无需真实摄像头即可开发测试

---

## 🚀 快速开始

### 前置条件

1. **Node.js** >= 16
2. **Java** >= 17
3. **Maven** >= 3.6
4. **webrtc-streamer**（已部署在 http://127.0.0.1:8000）

### 一键启动

```bash
# Windows
start-simplified.bat

# 或手动启动
# 1. 启动后端
mvn spring-boot:run

# 2. 安装前端依赖并启动
cd frontend
npm install
npm run dev
```

访问：http://localhost:3000

---

## 📦 技术栈

| 层级 | 技术 |
|------|------|
| **前端** | Vue 3 + Vite + webrtc-streamer-js |
| **后端** | Spring Boot + JPA + H2 Database |
| **流媒体** | webrtc-streamer（RTSP → WebRTC） |
| **数据库** | H2（内存模式，测试用）/ MySQL（生产用） |

---

## 🔄 工作流程

```
用户点击播放
    ↓
前端请求后端 API: POST /api/cameras/CAM001/play
    ↓
后端返回: { videoId: "CAM001" }
    ↓
前端调用: webRtcServer.connect("CAM001")
    ↓
webrtc-streamer 建立 WebRTC 连接
    ↓
视频显示在 <video> 标签中 ✅
```

**就这么简单！** 无需 WebSocket，无需手动 WebRTC 协商。

---

## ⚙️ 配置说明

### 后端配置（application.yaml）

```yaml
webrtc-streamer:
  base-url: http://127.0.0.1:8000  # webrtc-streamer 地址
  
  hikvision:
    mock-mode: true  # 测试模式（无需真实摄像头）
    # 生产环境改为 false，并配置真实 RTSP 地址
```

### webrtc-streamer 配置

**方式 1：命令行注册**
```bash
webrtc-streamer.exe -H 0.0.0.0:8000
```

**方式 2：配置文件（config.json）**
```json
{
  "urls": {
    "CAM001": "rtsp://admin:password@192.168.1.100:554/Streaming/Channels/101",
    "CAM002": "rtsp://admin:password@192.168.1.100:554/Streaming/Channels/201"
  }
}
```

```bash
webrtc-streamer.exe -C config.json
```

---

## 📊 代码统计

| 文件 | 重构前 | 重构后 | 减少 |
|------|--------|--------|------|
| VideoPlayer.vue | 408 行 | 150 行 | ⬇️ 63% |
| 后端控制器 | 2 个 | 1 个 | ⬇️ 50% |
| 总复杂度 | 高 | 低 | ⬇️ 显著 |

---

## 🐛 常见问题

### 1. 视频无法播放

**检查：**
- ✅ webrtc-streamer 是否运行？访问 http://127.0.0.1:8000
- ✅ 摄像头是否已注册到 webrtc-streamer？
- ✅ 浏览器控制台是否有错误？

### 2. CORS 错误

启动 webrtc-streamer 时添加参数：
```bash
webrtc-streamer.exe -cors-domain=*
```

### 3. 如何添加真实摄像头？

1. 修改 `application.yaml`：`mock-mode: false`
2. 配置真实的 RTSP 地址
3. 在 webrtc-streamer 中注册摄像头

---

## 📝 API 文档

### 获取摄像头列表
```http
GET /api/cameras
```

### 播放视频流
```http
POST /api/cameras/{code}/play
Content-Type: application/json

{
  "userId": "user_xxx"
}

Response:
{
  "success": true,
  "videoId": "CAM001",
  "sessionId": "uuid-xxx",
  "referenceCount": 1
}
```

### 停止播放
```http
POST /api/cameras/{code}/stop
Content-Type: application/json

{
  "userId": "user_xxx"
}
```

---

## 🎯 下一步

- [ ] 实现真实摄像头接入（关闭 mock-mode）
- [ ] 添加用户认证和权限控制
- [ ] 实现视频录制功能
- [ ] 添加云台控制（PTZ）
- [ ] 支持更多摄像头品牌（大华、宇视等）

---

## 📄 许可证

MIT License

---

## 🙏 致谢

- [webrtc-streamer](https://github.com/mpromonet/webrtc-streamer) - 优秀的 RTSP to WebRTC 解决方案
- [Vue.js](https://vuejs.org/) - 渐进式 JavaScript 框架
- [Spring Boot](https://spring.io/projects/spring-boot) - Java 微服务框架
