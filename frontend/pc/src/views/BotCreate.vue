<template>
  <div class="bot-create-container">
    <!-- 顶部标题 -->
    <div class="create-header">
      <button class="back-btn" @click="goBack">
        <i class="icon-back">←</i> 返回
      </button>
      <h2 class="create-title">新建机器人</h2>
    </div>

    <!-- 创建表单 -->
    <div class="create-form">
      <!-- 头像上传 -->
      <div class="form-section">
        <h3 class="section-title">基本信息</h3>
        <div class="avatar-upload">
          <div class="avatar-preview">
            <img :src="formData.avatar || defaultAvatar" alt="机器人头像" class="avatar-img" @error="handleAvatarError">
            <div class="avatar-upload-overlay" @click="triggerFileInput">
              <i class="icon-upload">📷</i>
              <span>更换头像</span>
            </div>
          </div>
          <input 
            type="file" 
            ref="fileInput" 
            class="file-input" 
            accept="image/*" 
            @change="handleFileChange"
          >
        </div>
      </div>

      <!-- 基本信息 -->
      <div class="form-section">
        <div class="form-item">
          <label class="form-label">机器人名称 *</label>
          <input 
            type="text" 
            v-model="formData.name" 
            class="form-input" 
            placeholder="请输入机器人名称"
            maxlength="50"
            @input="validateName"
          >
          <div class="form-error" v-if="errors.name">{{ errors.name }}</div>
        </div>

        <div class="form-item">
          <label class="form-label">机器人描述</label>
          <textarea 
            v-model="formData.description" 
            class="form-textarea" 
            placeholder="请输入机器人描述，介绍机器人的功能和特点"
            maxlength="300"
            rows="3"
          ></textarea>
          <div class="form-hint">{{ formData.description.length }}/300</div>
        </div>
      </div>

      <!-- 提示词配置 -->
      <div class="form-section">
        <h3 class="section-title">提示词配置</h3>
        <div class="form-item">
          <label class="form-label">系统提示词 *</label>
          <textarea 
            v-model="formData.prompt" 
            class="form-textarea" 
            placeholder="请输入系统提示词，这将决定机器人的行为和回复风格"
            maxlength="2000"
            rows="6"
            @input="validatePrompt"
          ></textarea>
          <div class="form-error" v-if="errors.prompt">{{ errors.prompt }}</div>
          <div class="form-hint">{{ formData.prompt.length }}/2000</div>
          <div class="form-tip">
            <i class="icon-info">ℹ️</i>
            提示词质量直接影响机器人回复效果，建议详细描述角色、能力和回复风格
          </div>
        </div>
      </div>

      <!-- 底部按钮 -->
      <div class="form-actions">
        <button class="action-btn cancel-btn" @click="goBack">取消</button>
        <button class="action-btn create-btn" @click="submitForm" :disabled="isSubmitting">
          {{ isSubmitting ? '创建中...' : '创建机器人' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { createBot } from '@/api/bot.js'
import { uploadFile } from '@/api/file.js'
import defaultAvatar from '@/static/images/user_default.png'

export default {
  name: 'BotCreate',
  data() {
    return {
      formData: {
        name: '',
        description: '',
        prompt: '',
        avatar: ''
      },
      errors: {
        name: '',
        prompt: ''
      },
      isSubmitting: false,
      defaultAvatar
    }
  },
  methods: {
    // 返回上一页
    goBack() {
      this.$router.back()
    },

    // 头像错误处理
    handleAvatarError(event) {
      event.target.src = this.defaultAvatar
    },

    // 触发文件输入
    triggerFileInput() {
      this.$refs.fileInput.click()
    },

    // 处理文件上传
    handleFileChange(event) {
      const file = event.target.files[0]
      if (file) {
        const isImage = file.type.startsWith('image/')
        const isLt2M = file.size / 1024 / 1024 < 2

        if (!isImage) {
          this.$message.error('请选择图片文件')
          return
        }

        if (!isLt2M) {
          this.$message.error('图片大小不能超过2MB')
          return
        }

        // 先预览头像
        const reader = new FileReader()
        reader.onload = (e) => {
          this.formData.avatar = e.target.result
        }
        reader.readAsDataURL(file)

        // 上传文件（直接传递 file 对象，uploadFile 内部会创建 FormData）
        uploadFile(file).then(response => {
          console.log('上传成功响应:', response)
          if (response.code === 200) {
            // 上传成功，使用返回的文件URL
            // 注意：服务器返回的结构是 {code: 200, msg: '操作成功', data: '图片URL'}
            console.log('文件URL:', response.data)
            this.formData.avatar = response.data
            console.log('更新后的formData.avatar:', this.formData.avatar)
          } else {
            this.$message.error('上传失败：' + response.message)
            // 上传失败，保持本地预览
          }
        }).catch(error => {
          console.error('上传失败:', error)
          this.$message.error('上传失败，请稍后重试')
          // 上传失败，保持本地预览
        })
      }
    },

    // 验证名称
    validateName() {
      if (!this.formData.name.trim()) {
        this.errors.name = '请输入机器人名称'
      } else if (this.formData.name.length > 50) {
        this.errors.name = '名称长度不能超过50个字符'
      } else {
        this.errors.name = ''
      }
    },

    // 验证提示词
    validatePrompt() {
      if (!this.formData.prompt.trim()) {
        this.errors.prompt = '请输入系统提示词'
      } else if (this.formData.prompt.length > 2000) {
        this.errors.prompt = '提示词长度不能超过2000个字符'
      } else {
        this.errors.prompt = ''
      }
    },

    // 表单验证
    validateForm() {
      this.validateName()
      this.validatePrompt()
      return !this.errors.name && !this.errors.prompt
    },

    // 提交表单
    submitForm() {
      if (!this.validateForm()) {
        this.$message.error('请完善表单信息')
        return
      }

      this.isSubmitting = true
      createBot(this.formData).then(response => {
        if (response.code === 200) {
          this.$message.success('机器人创建成功')
          // 跳转到机器人详情页或机器人列表页
          this.$router.push('/bot')
        } else {
          this.$message.error('创建失败：' + response.message)
        }
      }).catch(error => {
        console.error('创建机器人失败:', error)
        this.$message.error('创建失败，请稍后重试')
      }).finally(() => {
        this.isSubmitting = false
      })
    }
  }
}
</script>

<style scoped>
/* ===== 新建机器人 · iOS 设计语言 ===== */
.bot-create-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--bg);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* 顶部标题 */
.create-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: var(--bg-blur);
  -webkit-backdrop-filter: saturate(180%) blur(28px);
  backdrop-filter: saturate(180%) blur(28px);
  border-bottom: 1px solid var(--separator);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background-color: transparent;
  color: #007aff;
  border: none;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}

.back-btn:hover {
  background: rgba(0, 122, 255, 0.1);
}

.create-title {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.3px;
  color: var(--label);
  margin: 0;
}

/* 创建表单 */
.create-form {
  flex: 1;
  padding: 20px;
  max-width: 600px;
  margin: 0 auto;
  width: 100%;
  overflow-y: auto;
  max-height: calc(100vh - 160px);
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.create-form::-webkit-scrollbar {
  display: none;
}

.form-section {
  margin-bottom: 20px;
  padding: 20px;
  background-color: var(--card);
  border-radius: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06), 0 8px 24px rgba(0, 0, 0, 0.03);
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--label);
  margin: 0 0 16px 0;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--separator);
}

/* 头像上传 */
.avatar-upload {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.avatar-preview {
  position: relative;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 2px dashed rgba(0, 122, 255, 0.35);
  background-color: var(--card);
  box-shadow: 0 2px 8px var(--separator);
  transition: all 0.3s cubic-bezier(0.32, 0.72, 0, 1);
}

.avatar-preview:hover {
  border-color: #007aff;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: var(--label-secondary);
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.avatar-preview:hover .avatar-upload-overlay {
  opacity: 1;
}

.avatar-upload-overlay i {
  font-size: 18px;
  margin-bottom: 4px;
  font-style: normal;
}

.avatar-upload-overlay span {
  font-size: 10px;
}

.file-input {
  display: none;
}

/* 表单项 */
.form-item {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--label);
  margin-bottom: 6px;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 10px 14px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
  box-sizing: border-box;
  background: var(--card);
  box-shadow: 0 0 0 1px var(--separator-strong);
  color: var(--label);
  font-family: inherit;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  box-shadow: 0 0 0 2px rgba(0, 122, 255, 0.3);
}

.form-textarea {
  resize: none;
}

.form-error {
  font-size: 11px;
  color: #ff3b30;
  margin-top: 4px;
}

.form-hint {
  font-size: 11px;
  color: var(--label-secondary);
  margin-top: 4px;
  text-align: right;
}

.form-tip {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 10px 12px;
  background-color: rgba(0, 122, 255, 0.08);
  border-radius: 12px;
  font-size: 11px;
  color: #007aff;
  margin-top: 6px;
}

.form-tip i {
  font-size: 12px;
  margin-top: 1px;
  font-style: normal;
}

/* 底部按钮 */
.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding: 16px 20px;
  background: rgba(249, 249, 252, 0.88);
  -webkit-backdrop-filter: saturate(180%) blur(24px);
  backdrop-filter: saturate(180%) blur(24px);
  border-top: 1px solid var(--separator);
  margin-top: 20px;
}

.action-btn {
  padding: 11px 24px;
  border: none;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
  font-family: inherit;
}

.cancel-btn {
  background-color: var(--card);
  color: var(--label-secondary);
  border: 1px solid rgba(60, 60, 67, 0.15);
}

.cancel-btn:hover {
  background-color: var(--fill);
}

.create-btn {
  background-color: #007aff;
  color: white;
  box-shadow: 0 4px 14px rgba(0, 122, 255, 0.35);
}

.create-btn:hover {
  background-color: #3395ff;
}

.create-btn:disabled {
  background-color: var(--fill-strong);
  box-shadow: none;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .create-header {
    padding: 16px;
  }

  .create-form {
    padding: 16px;
  }

  .form-section {
    padding: 16px;
  }

  .form-actions {
    flex-direction: column;
    padding: 16px;
  }

  .action-btn {
    width: 100%;
  }
}
</style>