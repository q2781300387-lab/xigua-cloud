<template>
	<view class="avatar-container" :style="containerStyle">
		<!-- 有头像URL时显示图片 -->
		<image
			v-if="src"
			class="avatar-image"
			:src="src"
			:mode="mode"
			:style="imageStyle"
			@error="handleError"
		></image>

		<!-- 无头像时显示默认用户图标（仅用于单聊） -->
		<image
			v-else
			class="avatar-default"
			:src="defaultAvatar"
			:mode="mode"
			:style="imageStyle"
		></image>
	</view>
</template>

<script>
/**
 * Avatar 头像组件
 *
 * @description 智能头像组件，支持自定义图片和默认头像
 * - 有URL时：显示图片
 * - 无URL时：显示默认用户头像（仅用于单聊用户，chat_type=1）
 * - 群聊（chat_type=2）必须传入src，不使用默认头像
 *
 * @property {String} src - 头像图片URL（必填，群聊必须有值）
 * @property {Number|String} size - 头像尺寸（rpx），默认80
 * @property {String} shape - 头像形状: 'circle' | 'square'，默认'circle'
 * @property {String} mode - 图片裁剪模式，默认'aspectFill'
 *
 * @example
 * <!-- 单聊用户头像（有URL） -->
 * <avatar :src="userInfo.avatar" :size="100" />
 *
 * <!-- 单聊用户头像（无URL，显示默认） -->
 * <avatar :size="100" />
 *
 * <!-- 群聊头像（必须传入URL） -->
 * <avatar :src="groupInfo.avatar" :size="100" />
 */
export default {
	name: 'Avatar',
	props: {
		// 头像图片URL
		src: {
			type: String,
			default: ''
		},
		// 头像尺寸（rpx）
		size: {
			type: [Number, String],
			default: 80
		},
		// 头像形状: circle-圆形, square-方形
		shape: {
			type: String,
			default: 'circle',
			validator: (value) => ['circle', 'square'].includes(value)
		},
		// 图片裁剪模式
		mode: {
			type: String,
			default: 'aspectFill'
		}
	},
	data() {
		return {
			imageError: false
		}
	},
	computed: {
		// 默认头像路径（仅用于单聊用户）
		defaultAvatar() {
			return '/static/icons/user/default-avatar.svg'
		},
		// 容器样式
		containerStyle() {
			const size = this.resolveSize()
			return {
				width: size,
				height: size,
				borderRadius: this.shape === 'circle' ? '50%' : '16rpx'
			}
		},
		// 图片样式
		imageStyle() {
			const size = this.resolveSize()
			return {
				width: size,
				height: size,
				borderRadius: this.shape === 'circle' ? '50%' : '16rpx'
			}
		}
	},
	methods: {
		/**
		 * 动态内联样式里的 rpx H5 不会自动换算，必须先转成 px，
		 * 否则浏览器会丢弃非法值，图片按原始尺寸铺开。
		 */
		resolveSize() {
			const raw = this.size
			if (typeof raw === 'number') {
				return `${uni.upx2px(raw)}px`
			}
			const value = String(raw || '').trim()
			if (value.endsWith('rpx')) {
				return `${uni.upx2px(parseFloat(value))}px`
			}
			if (/^\d+(\.\d+)?$/.test(value)) {
				return `${uni.upx2px(parseFloat(value))}px`
			}
			return value || '40px'
		},
		// 图片加载失败处理
		handleError() {
			this.imageError = true
			this.$emit('error')
		}
	}
}
</script>

<style scoped>
.avatar-container {
	position: relative;
	overflow: hidden;
	background: #f3f3f4;
	flex-shrink: 0;
}

.avatar-image,
.avatar-default {
	width: 100%;
	height: 100%;
	display: block;
}

.avatar-image {
	object-fit: cover;
}

.avatar-default {
	object-fit: contain;
}
</style>
