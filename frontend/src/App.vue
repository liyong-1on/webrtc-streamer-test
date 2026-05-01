<template>
  <div class="app">
    <header class="header">
      <h1>WebRTC视频监控系统</h1>
      <div class="stats">
        <span>活跃流: {{ activeStreamCount }}</span>
        <span>总摄像头: {{ cameras.length }}</span>
        <button @click="loadAllCameras" :disabled="loading">
          {{ loading ? '加载中...' : '刷新' }}
        </button>
      </div>
    </header>
    
    <main class="main-content">
      <!-- 加载状态 -->
      <div v-if="loading && cameras.length === 0" class="loading-state">
        <p>加载摄像头列表...</p>
      </div>
      
      <!-- 空状态 -->
      <div v-else-if="cameras.length === 0" class="empty-state">
        <p>暂无摄像头数据</p>
        <p style="font-size: 14px; color: #999; margin-top: 10px;">
          请确保后端服务已启动：http://localhost:8081
        </p>
        <button @click="loadAllCameras" style="margin-top: 20px; padding: 10px 20px;">
          重试
        </button>
      </div>
      
      <!-- 摄像头网格 -->
      <div v-else class="camera-grid">
        <VideoPlayer
          v-for="camera in cameras"
          :key="camera.code"
          :camera="camera"
          :userId="userId"
          class="grid-item"
        />
      </div>
      
      <!-- 错误提示 -->
      <div v-if="error" class="error-state">
        <p style="color: red;">{{ error }}</p>
        <button @click="loadAllCameras">重试</button>
      </div>
    </main>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import VideoPlayer from './VideoPlayer.vue'

export default {
  name: 'App',
  components: {
    VideoPlayer
  },
  setup() {
    const cameras = ref([])
    const loading = ref(false)
    const activeStreamCount = ref(0)
    const error = ref('')
    const userId = ref('user_' + Math.random().toString(36).substr(2, 9))
    
    // 加载摄像头列表
    const loadAllCameras = async () => {
      loading.value = true
      error.value = ''
      
      try {
        console.log('Fetching cameras...')
        const response = await fetch('/api/cameras')
        
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }
        
        const data = await response.json()
        console.log('Cameras loaded:', data.length)
        
        cameras.value = data
        
        // 获取活跃流统计
        await loadStreamStats()
        
      } catch (err) {
        console.error('Failed to load cameras:', err)
        error.value = `加载失败: ${err.message}。请检查后端服务是否启动（http://localhost:8081）`
      } finally {
        loading.value = false
      }
    }
    
    // 加载流统计信息
    const loadStreamStats = async () => {
      try {
        const response = await fetch('/api/cameras/stats/streams')
        if (response.ok) {
          const stats = await response.json()
          activeStreamCount.value = stats.activeStreamCount || 0
        }
      } catch (err) {
        console.error('Failed to load stream stats:', err)
      }
    }
    
    // 定时更新统计信息
    const startStatsUpdate = () => {
      setInterval(loadStreamStats, 5000) // 每5秒更新一次
    }
    
    onMounted(() => {
      console.log('App mounted')
      loadAllCameras()
      startStatsUpdate()
    })
    
    return {
      cameras,
      loading,
      activeStreamCount,
      error,
      userId,
      loadAllCameras
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: #f5f5f5;
}

.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 20px 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header h1 {
  font-size: 24px;
  margin-bottom: 10px;
}

.stats {
  display: flex;
  gap: 20px;
  align-items: center;
  font-size: 14px;
}

.stats button {
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid white;
  color: white;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.stats button:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.3);
}

.stats button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.main-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.loading-state,
.empty-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  font-size: 18px;
  color: #666;
}

.camera-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 15px;
}

.grid-item {
  aspect-ratio: 16 / 9;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s, box-shadow 0.3s;
}

.grid-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 响应式设计 */
@media (max-width: 1920px) {
  .camera-grid {
    grid-template-columns: repeat(8, 1fr);
  }
}

@media (max-width: 1440px) {
  .camera-grid {
    grid-template-columns: repeat(6, 1fr);
  }
}

@media (max-width: 1024px) {
  .camera-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 768px) {
  .camera-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .header h1 {
    font-size: 20px;
  }
  
  .stats {
    flex-wrap: wrap;
    gap: 10px;
  }
}

@media (max-width: 480px) {
  .camera-grid {
    grid-template-columns: 1fr;
  }
}
</style>
