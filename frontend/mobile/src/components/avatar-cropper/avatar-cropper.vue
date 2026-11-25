<template>
	<view v-if="visible" class="crop-mask" @click.self="onCancel">
		<view class="crop-panel" @click.stop>
			<view class="crop-head">
				<text class="crop-head-btn" @click="onCancel">取消</text>
				<text class="crop-head-title">裁剪头像</text>
				<text class="crop-head-btn primary" @click="onConfirm">确定</text>
			</view>
			<view class="crop-stage" id="avatar-crop-stage"></view>
			<text class="crop-tip">拖动图片调整位置，双指缩放取景</text>
		</view>
	</view>
</template>

<script>
// #ifdef H5
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'
// #endif

export default {
	name: 'AvatarCropper',
	props: {
		visible: { type: Boolean, default: false },
		src: { type: String, default: '' }
	},
	data() {
		return {
			cropper: null
		}
	},
	watch: {
		visible(value) {
			if (value) {
				this.$nextTick(() => this.initCropper())
			} else {
				this.destroyCropper()
			}
		},
		src() {
			if (this.visible) this.$nextTick(() => this.initCropper())
		}
	},
	beforeDestroy() {
		this.destroyCropper()
	},
	methods: {
		initCropper() {
			// #ifdef H5
			/* eslint-disable no-undef */
			if (typeof document === 'undefined' || !this.src) return
			const stage = document.getElementById('avatar-crop-stage')
			if (!stage) return
			this.destroyCropper()
			stage.innerHTML = ''
			const image = document.createElement('img')
			image.alt = 'avatar'
			image.src = this.src
			image.style.display = 'block'
			image.style.maxWidth = '100%'
			stage.appendChild(image)
			this.cropper = new Cropper(image, {
				aspectRatio: 1,
				viewMode: 1,
				dragMode: 'move',
				background: false,
				guides: false,
				responsive: true,
				checkOrientation: false,
				autoCropArea: 0.86,
				toggleDragModeOnDblclick: false
			})
			// #endif
		},
		destroyCropper() {
			if (this.cropper && typeof this.cropper.destroy === 'function') {
				this.cropper.destroy()
			}
			this.cropper = null
		},
		onCancel() {
			this.destroyCropper()
			this.$emit('cancel')
		},
		onConfirm() {
			// #ifdef H5
			if (!this.cropper) return
			const canvas = this.cropper.getCroppedCanvas({
				width: 400,
				height: 400,
				imageSmoothingEnabled: true,
				imageSmoothingQuality: 'high',
				fillColor: '#ffffff'
			})
			if (!canvas) return
			const finish = (blob) => {
				this.destroyCropper()
				if (blob) {
					this.$emit('confirm', blob)
				} else {
					this.$emit('error', '裁剪失败，请重试')
				}
			}
			if (canvas.toBlob) {
				canvas.toBlob(finish, 'image/jpeg', 0.9)
			} else {
				const dataUrl = canvas.toDataURL('image/jpeg', 0.9)
				const parts = dataUrl.split(',')
				const bytes = atob(parts[1])
				const buffer = new Uint8Array(bytes.length)
				for (let i = 0; i < bytes.length; i += 1) buffer[i] = bytes.charCodeAt(i)
				finish(new Blob([buffer], { type: 'image/jpeg' }))
			}
			// #endif
		}
	}
}
</script>

<style lang="scss" scoped>
.crop-mask {
	position: fixed;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
	z-index: 999;
	background: #000000;
	display: flex;
	align-items: stretch;
}

.crop-panel {
	width: 100%;
	height: 100%;
	display: flex;
	flex-direction: column;
	background: #000000;
	padding-bottom: env(safe-area-inset-bottom);
	overflow: hidden;
}

.crop-head {
	height: 96rpx;
	flex: none;
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: env(safe-area-inset-top) 28rpx 0;
	background: #1c1c1e;
	box-sizing: content-box;
}

.crop-head-title {
	font-size: 30rpx;
	font-weight: 600;
	color: #ffffff;
}

.crop-head-btn {
	font-size: 26rpx;
	color: rgba(255, 255, 255, 0.72);
	padding: 16rpx 4rpx;
}

.crop-head-btn.primary {
	color: #ffffff;
	font-weight: 600;
}

.crop-stage {
	width: 100%;
	flex: 1;
	min-height: 0;
	background: #000;
	overflow: hidden;
}

.crop-stage :deep(.cropper-container) {
	width: 100% !important;
	height: 100% !important;
}

.crop-tip {
	display: block;
	text-align: center;
	font-size: 22rpx;
	color: rgba(255, 255, 255, 0.5);
	padding: 18rpx 24rpx 0;
}
</style>
