<template>
  <div class="avatar-editor-container">
    <!-- 编辑器头部 -->
    <div class="editor-header">
      <h2 class="editor-title">编辑头像</h2>
      <button class="close-button" @click="$emit('back')">
        <img src="@/static/icons/close.png" alt="关闭" />
      </button>
    </div>

    <!-- 图片预览和编辑区域 -->
    <div class="editor-content">
      <!-- 当前头像预览 -->
      <div class="avatar-preview">
        <img :src="currentImage" class="preview-image" alt="头像预览">
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <button @click="selectImage" class="btn select-btn">选择图片</button>
        <button
            @click="saveAvatar"
            class="btn save-btn"
            :disabled="!hasChanges"
        >
          保存
        </button>
      </div>

      <!-- 隐藏的文件输入 -->
      <input
          type="file"
          ref="fileInput"
          accept="image/*"
          @change="handleFileSelect"
          style="display: none;"
      >
    </div>
  </div>
</template>

<script>
import { uploadFile } from '@/api/file'
import { uploadAvatar, getUserInfo } from '@/api/user'

export default {
  props: {
    initialImage: {
      type: String,
      default: '' // 默认值
    }
  },
  data() {
    return {
      currentImage: this.initialImage,
      selectedFile: null,
      hasChanges: false
    }
  },
  methods: {
    selectImage() {
      this.$refs.fileInput.click();
    },
    handleFileSelect(e) {
      const file = e.target.files[0];
      if (file) {
        // 直接预览选择的图片
        const reader = new FileReader();
        reader.onload = (event) => {
          this.currentImage = event.target.result;
          this.selectedFile = file;
          this.hasChanges = true;
        };
        reader.readAsDataURL(file);
      }
    },
    async saveAvatar() {
      if (!this.selectedFile) {
        this.$emit('confirm', null);
        return;
      }

      // 通过文件后缀名验证图片类型（更简单的方式）
      const validExtensions = ['.jpg', '.jpeg', '.png'];
      const fileExtension = this.selectedFile.name.toLowerCase()
          .slice(this.selectedFile.name.lastIndexOf('.'));

      if (!validExtensions.includes(fileExtension)) {
        this.$message.error('请上传常规图片格式（JPG/PNG）');
        return;
      }

      try {
        const uploadResponse = await uploadFile(this.selectedFile);
        if (uploadResponse.code !== 200) {
          this.$message.error(uploadResponse.msg || (uploadResponse.data && uploadResponse.data.msg) || '图片上传失败');
          return;
        }

        const avatarUrl = typeof uploadResponse.data === 'string'
          ? uploadResponse.data
          : (uploadResponse.data && uploadResponse.data.url);

        if (!avatarUrl) {
          this.$message.error('图片地址获取失败，请重试');
          return;
        }

        const avatarResponse = await uploadAvatar(avatarUrl);
        if (avatarResponse.code !== 200) {
          this.$message.error(avatarResponse.msg || (avatarResponse.data && avatarResponse.data.msg) || '头像更新失败');
          return;
        }

        this.$message.success('头像更新成功');
        this.$emit('confirm', avatarUrl);
      } catch (error) {
        console.error('上传出错:', error);
        this.$message.error('上传服务异常，请稍后重试');
      }
    }
  }
}
</script>

<style scoped>
/* ===== 头像编辑器 · iOS 设计语言 ===== */
.avatar-editor-container {
  width: 100%;
  height: 100%;
  background: var(--bg);
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: var(--bg-blur);
  -webkit-backdrop-filter: saturate(180%) blur(28px);
  backdrop-filter: saturate(180%) blur(28px);
  border-bottom: 1px solid var(--separator);
  position: relative;
}

.editor-title {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.3px;
  margin: 0;
  text-align: left;
  flex: 1;
  color: var(--label);
}

.close-button {
  background: none;
  border: none;
  cursor: pointer;
  padding: 5px;
  position: absolute;
  right: 15px;
  top: 50%;
  transform: translateY(-50%);
  outline: none;
  -webkit-tap-highlight-color: transparent;
}

.close-button:hover::after {
  content: "关闭";
  position: absolute;
  left: 50%;
  bottom: -25px;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.75);
  color: white;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 12px;
  white-space: nowrap;
}

.close-button img {
  width: 20px;
  height: 20px;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.close-button:hover img {
  opacity: 1;
}

.editor-content {
  flex: 1;
  padding: 30px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.avatar-preview {
  width: 200px;
  height: 200px;
  border-radius: 50%;
  overflow: hidden;
  border: 4px solid #ffffff;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18);
  margin-bottom: 30px;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.action-buttons {
  display: flex;
  gap: 15px;
  width: 100%;
  max-width: 320px;
}

.btn {
  flex: 1;
  padding: 13px 0;
  border-radius: 999px;
  border: none;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
  outline: none;
  font-family: inherit;
}

.select-btn {
  background-color: var(--card);
  color: #007aff;
  border: 1px solid rgba(0, 122, 255, 0.35);
}

.select-btn:hover {
  background: rgba(0, 122, 255, 0.08);
}

.save-btn {
  background-color: #007aff;
  color: white;
  box-shadow: 0 4px 14px rgba(0, 122, 255, 0.35);
}

.save-btn:hover {
  background-color: #3395ff;
}

.save-btn:disabled {
  background-color: var(--fill-strong);
  box-shadow: none;
  cursor: not-allowed;
}
</style>