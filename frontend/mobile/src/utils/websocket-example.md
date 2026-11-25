# WebSocket 使用示例

## 基本概念

WebSocket 采用单例模式，全局只有一个连接实例，多个页面可以同时订阅消息。

## 在页面中使用

### 1. 聊天页面示例 (chat-list.vue)

```vue
<script>
import { subscribeMessage, unsubscribeMessage } from '@/utils/websocket.js'

export default {
  data() {
    return {
      chatList: []
    }
  },

  onShow() {
    // 订阅 WebSocket 消息
    subscribeMessage(this.handleWebSocketMessage)
  },

  onHide() {
    // 取消订阅（可选，如果页面隐藏时不需要处理消息）
    unsubscribeMessage(this.handleWebSocketMessage)
  },

  onUnload() {
    // 页面卸载时取消订阅
    unsubscribeMessage(this.handleWebSocketMessage)
  },

  methods: {
    // WebSocket 消息处理函数
    handleWebSocketMessage(data) {
      console.log('收到 WebSocket 消息:', data)

      try {
        const message = JSON.parse(data)

        // 根据消息类型处理
        switch (message.messageType) {
          case 'chat':
            this.handleNewChatMessage(message)
            break
          case 'heart_beat':
            console.log('收到心跳响应')
            break
          default:
            console.log('未知消息类型:', message.messageType)
        }
      } catch (error) {
        console.error('解析 WebSocket 消息失败:', error)
      }
    },

    // 处理新的聊天消息
    handleNewChatMessage(message) {
      // 更新聊天列表
      const chatIndex = this.chatList.findIndex(chat => chat.id === message.chatId)

      if (chatIndex !== -1) {
        // 更新已存在的聊天
        this.chatList[chatIndex].lastMessage = message.content
        this.chatList[chatIndex].time = this.formatTime(message.createTime)
        this.chatList[chatIndex].unreadCount++

        // 移到列表顶部
        const chat = this.chatList.splice(chatIndex, 1)[0]
        this.chatList.unshift(chat)
      } else {
        // 添加新的聊天
        this.chatList.unshift({
          id: message.chatId,
          name: message.senderName,
          avatar: message.senderAvatar,
          lastMessage: message.content,
          time: this.formatTime(message.createTime),
          unreadCount: 1,
          online: true
        })
      }

      // 播放提示音（可选）
      this.playNotificationSound()

      // 显示通知
      uni.showToast({
        title: `${message.senderName}: ${message.content}`,
        icon: 'none',
        duration: 2000
      })
    },

    playNotificationSound() {
      // 播放消息提示音
      const innerAudioContext = uni.createInnerAudioContext()
      innerAudioContext.src = '/static/sounds/message.mp3'
      innerAudioContext.play()
    }
  }
}
</script>
```

### 2. 聊天详情页示例 (chat-detail.vue)

```vue
<script>
import { subscribeMessage, unsubscribeMessage, sendWebSocketMessage } from '@/utils/websocket.js'

export default {
  data() {
    return {
      chatId: '',
      messages: [],
      inputText: ''
    }
  },

  onLoad(options) {
    this.chatId = options.id
    this.loadMessages()
  },

  onShow() {
    // 订阅消息
    subscribeMessage(this.handleWebSocketMessage)
  },

  onUnload() {
    // 取消订阅
    unsubscribeMessage(this.handleWebSocketMessage)
  },

  methods: {
    // 处理 WebSocket 消息
    handleWebSocketMessage(data) {
      try {
        const message = JSON.parse(data)

        // 只处理当前聊天的消息
        if (message.chatId === this.chatId && message.messageType === 'chat') {
          this.messages.push({
            id: message.id,
            content: message.content,
            senderId: message.senderId,
            time: message.createTime,
            isSelf: false
          })

          // 滚动到底部
          this.$nextTick(() => {
            this.scrollToBottom()
          })
        }
      } catch (error) {
        console.error('处理消息失败:', error)
      }
    },

    // 发送消息
    sendMessage() {
      if (!this.inputText.trim()) return

      const userInfo = uni.getStorageSync('userInfo')
      if (!userInfo) return

      const userInfoObj = JSON.parse(userInfo)

      const message = {
        chatId: this.chatId,
        senderId: userInfoObj.id,
        content: this.inputText,
        messageType: 'chat',
        createTime: Date.now()
      }

      // 通过 WebSocket 发送消息
      const success = sendWebSocketMessage(message)

      if (success) {
        // 添加到本地消息列表
        this.messages.push({
          id: Date.now(),
          content: this.inputText,
          senderId: userInfoObj.id,
          time: Date.now(),
          isSelf: true
        })

        // 清空输入框
        this.inputText = ''

        // 滚动到底部
        this.$nextTick(() => {
          this.scrollToBottom()
        })
      } else {
        uni.showToast({
          title: 'WebSocket 未连接',
          icon: 'none'
        })
      }
    },

    scrollToBottom() {
      // 实现滚动到底部的逻辑
    }
  }
}
</script>
```

## 消息格式示例

### 聊天消息
```json
{
  "chatId": "1922827743412375554",
  "senderId": "1922832372359516162",
  "senderName": "六边形战士",
  "senderAvatar": "http://localhost:9000/xigua/avatar.jpg",
  "content": "你好",
  "messageType": "chat",
  "subType": "text",
  "createTime": 1755219670525
}
```

### 心跳消息
```json
{
  "senderId": "1922832372359516162",
  "message": "ping",
  "messageType": "heart_beat",
  "subType": "ping",
  "createTime": 1755219670525
}
```

## API 说明

### connectWebSocket()
连接 WebSocket 服务器（登录后自动调用）

### closeWebSocket()
关闭 WebSocket 连接（退出登录时自动调用）

### subscribeMessage(listener)
订阅 WebSocket 消息
- `listener`: 消息处理函数，接收一个参数 `data`（字符串格式）

### unsubscribeMessage(listener)
取消订阅 WebSocket 消息
- `listener`: 之前订阅时传入的消息处理函数

### sendWebSocketMessage(message)
发送 WebSocket 消息
- `message`: 消息对象或字符串
- 返回: `boolean` 表示是否发送成功

### getWebSocketStatus()
获取 WebSocket 连接状态
- 返回对象:
  ```javascript
  {
    isConnected: boolean,      // 是否已连接
    reconnectAttempts: number,  // 重连次数
    isReconnecting: boolean     // 是否正在重连
  }
  ```

## 注意事项

1. **单例模式**: WebSocket 连接是全局唯一的，多个页面共享同一个连接
2. **订阅管理**: 每个页面需要在合适的生命周期订阅和取消订阅
3. **消息过滤**: 在消息处理函数中根据业务需求过滤消息
4. **错误处理**: 始终使用 try-catch 处理 JSON 解析
5. **性能优化**: 不需要接收消息的页面应及时取消订阅
6. **自动重连**: 连接断开后会自动重连，最多重试 5 次
7. **心跳机制**: 每 60 秒自动发送一次心跳，保持连接活跃
