<template>
  <div class="video-player" :class="{ 'playing': isPlaying, 'error': isError }">
    <!-- 未播放状态 -->
    <div v-if="!isPlaying && !isLoading" class="placeholder" @click="startPlay">
      <div class="camera-info">
        <h3>{{ camera.name }}</h3>
        <p>{{ camera.code }}</p>
        <button class="play-btn">▶ 播放</button>
      </div>
    </div>
    
    <!-- 加载中 -->
    <div v-if="isLoading" class="loading">
      <div class="spinner"></div>
      <p>连接中...</p>
    </div>
    
    <!-- 视频元素 -->
    <video 
      ref="videoElement"
      autoplay 
      playsinline
      muted
      class="video-element"
    ></video>
    
    <!-- 错误提示 -->
    <div v-if="isError" class="error-message">
      <p>连接失败</p>
      <button @click="retry">重试</button>
    </div>
    
    <!-- 控制按钮 -->
    <div v-if="isPlaying" class="controls">
      <button @click="stopPlay" class="stop-btn">⏹ 停止</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'

const props = defineProps({
  camera: {
    type: Object,
    required: true
  },
  userId: {
    type: String,
    required: true
  }
})

const videoElement = ref(null)
const isPlaying = ref(false)
const isLoading = ref(false)
const isError = ref(false)

let pc = null
let peerId = null
let icePollingInterval = null

// 生成唯一 peerId
const generatePeerId = () => {
  return 'peer_' + Math.random().toString(36).substr(2, 9) + '_' + Date.now()
}

// 开始播放
const startPlay = async () => {
  isLoading.value = true
  isError.value = false

  try {
    // 1. 请求后端获取流信息（包含 RTSP 地址）
    const playResponse = await fetch(`/api/cameras/${props.camera.code}/play`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: props.userId })
    })

    if (!playResponse.ok) {
      throw new Error('Failed to get stream info')
    }

    const streamInfo = await playResponse.json()
    const cameraCode = streamInfo.cameraCode

    // 2. 生成 peerId（每个浏览器客户端唯一）
    peerId = generatePeerId()

    // 3. 创建 RTCPeerConnection
    pc = new RTCPeerConnection({
      iceServers: [{ urls: 'stun:stun.l.google.com:19302' }],
      // 禁用 mDNS，避免 .local 地址解析超时
      iceCandidatePoolSize: 10
    })

    // 4. 监听视频流
    pc.ontrack = (event) => {
      if (videoElement.value && event.streams && event.streams[0]) {
        videoElement.value.srcObject = event.streams[0]
        isPlaying.value = true
        console.log('✅ 视频流已接收:', cameraCode)
      }
    }

    // 5. 收集并发送本地 ICE Candidate 到后端
    pc.onicecandidate = (event) => {
      if (event.candidate) {
        const cand = event.candidate.candidate;
        // 跳过 mDNS .local 地址，避免解析超时
        if (cand.includes('.local')) {
          console.log('跳过 mDNS candidate:', cand);
          return;
        }
        fetch('/api/webrtc/ice', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            peerId: peerId,
            candidate: {
              candidate: event.candidate.candidate,
              sdpMid: event.candidate.sdpMid,
              sdpMLineIndex: event.candidate.sdpMLineIndex
            }
          })
        }).catch(err => console.warn('发送 ICE candidate 失败:', err))
      }
    }

    // 6. 向后端请求 Offer（后端代理与 webrtc-streamer 交互）
    const offerResponse = await fetch('/api/webrtc/offer', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        cameraCode: cameraCode,
        peerId: peerId
      })
    })

    if (!offerResponse.ok) {
      throw new Error('Failed to get WebRTC offer')
    }

    const offer = await offerResponse.json()

    // 7. 设置 Remote Description (Offer)
    await pc.setRemoteDescription(new RTCSessionDescription({
      type: offer.type,
      sdp: offer.sdp
    }))

    // 8. 创建 Answer
    const answer = await pc.createAnswer()
    await pc.setLocalDescription(answer)

    // 9. 发送 Answer 到后端（由后端转发给 webrtc-streamer）
    await fetch('/api/webrtc/answer', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        cameraCode: cameraCode,
        peerId: peerId,
        sdp: answer.sdp,
        type: answer.type
      })
    })

    // 10. 轮询获取 webrtc-streamer 的 ICE Candidates
    icePollingInterval = setInterval(async () => {
      try {
        const iceResponse = await fetch(`/api/webrtc/ice/${peerId}`)
        if (iceResponse.status === 200) {
          const candidates = await iceResponse.json()
          for (const cand of candidates) {
            await pc.addIceCandidate(new RTCIceCandidate({
              candidate: cand.candidate,
              sdpMid: cand.sdpMid,
              sdpMLineIndex: cand.sdpMLineIndex
            }))
          }
          if (candidates.length > 0) {
            clearInterval(icePollingInterval)
            icePollingInterval = null
          }
        }
      } catch (err) {
        // 忽略轮询错误
      }
    }, 1000)

    // 连接状态监控
    pc.onconnectionstatechange = () => {
      console.log('WebRTC connection state:', pc.connectionState)
      if (pc.connectionState === 'failed' || pc.connectionState === 'disconnected') {
        isError.value = true
        isPlaying.value = false
      }
    }

  } catch (error) {
    console.error('❌ 播放失败:', error)
    isError.value = true
    cleanup()
  } finally {
    isLoading.value = false
  }
}

// 停止播放
const stopPlay = async () => {
  try {
    // 通知后端释放流
    await fetch(`/api/cameras/${props.camera.code}/stop`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: props.userId })
    })

    // 通知后端断开 webrtc 连接
    if (peerId) {
      await fetch(`/api/webrtc/hangup/${peerId}`, { method: 'POST' })
    }

    cleanup()
    isPlaying.value = false
    console.log('⏹ 视频已停止')

  } catch (error) {
    console.error('停止失败:', error)
  }
}

// 清理资源
const cleanup = () => {
  if (icePollingInterval) {
    clearInterval(icePollingInterval)
    icePollingInterval = null
  }
  if (pc) {
    pc.close()
    pc = null
  }
  peerId = null
  if (videoElement.value) {
    videoElement.value.srcObject = null
  }
}

// 重试
const retry = () => {
  isError.value = false
  startPlay()
}

// 组件卸载时清理
onUnmounted(() => {
  if (isPlaying.value) {
    stopPlay()
  } else {
    cleanup()
  }
})
</script>

<style scoped>
.video-player {
  position: relative;
  width: 100%;
  height: 100%;
  background: #000;
  border-radius: 4px;
  overflow: hidden;
}

.placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  transition: opacity 0.3s;
}

.placeholder:hover {
  opacity: 0.8;
}

.camera-info {
  text-align: center;
  color: white;
}

.camera-info h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
}

.camera-info p {
  margin: 0 0 20px 0;
  font-size: 12px;
  opacity: 0.8;
}

.play-btn {
  padding: 10px 30px;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid white;
  color: white;
  border-radius: 25px;
  cursor: pointer;
  transition: all 0.3s;
}

.play-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.loading {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.video-element {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.error-message {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: white;
}

.error-message button {
  margin-top: 10px;
  padding: 8px 20px;
  background: #ff4444;
  border: none;
  color: white;
  border-radius: 4px;
  cursor: pointer;
}

.controls {
  position: absolute;
  bottom: 10px;
  right: 10px;
  opacity: 0;
  transition: opacity 0.3s;
}

.video-player:hover .controls {
  opacity: 1;
}

.stop-btn {
  padding: 5px 15px;
  background: rgba(255, 68, 68, 0.8);
  border: none;
  color: white;
  border-radius: 4px;
  cursor: pointer;
}

.playing {
  border: 2px solid #4CAF50;
}

.error {
  border: 2px solid #ff4444;
}
</style>
