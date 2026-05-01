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

let webRtcServer = null

// 开始播放
const startPlay = async () => {
  isLoading.value = true
  isError.value = false
  
  try {
    // 1. 请求后端获取视频 ID
    const response = await fetch(`/api/cameras/${props.camera.code}/play`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        userId: props.userId
      })
    })
    
    if (!response.ok) {
      throw new Error('Failed to get stream info')
    }
    
    const streamInfo = await response.json()
    const videoId = streamInfo.videoId
    
    // 2. 动态加载 webrtc-streamer-js
    if (!window.WebRtcStreamer) {
      await loadWebRtcStreamerScript()
    }
    
    // 3. 创建 WebRtcStreamer 实例并连接
    webRtcServer = new window.WebRtcStreamer(videoElement.value, 'http://127.0.0.1:8000')
    webRtcServer.connect(videoId)
    
    isPlaying.value = true
    console.log('✅ 视频播放成功:', videoId)
    
  } catch (error) {
    console.error('❌ 播放失败:', error)
    isError.value = true
  } finally {
    isLoading.value = false
  }
}

// 动态加载 webrtc-streamer-js 脚本
const loadWebRtcStreamerScript = () => {
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = 'http://127.0.0.1:8000/webrtcstreamer.js'
    script.onload = resolve
    script.onerror = reject
    document.head.appendChild(script)
  })
}

// 停止播放
const stopPlay = async () => {
  try {
    // 通知后端释放流
    await fetch(`/api/cameras/${props.camera.code}/stop`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        userId: props.userId
      })
    })
    
    // 断开 WebRTC 连接
    if (webRtcServer) {
      webRtcServer.disconnect()
      webRtcServer = null
    }
    
    isPlaying.value = false
    console.log('⏹ 视频已停止')
    
  } catch (error) {
    console.error('停止失败:', error)
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
