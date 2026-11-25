/**
 * 左滑返回上一页混入
 * 使用方法：在页面中引入并混入
 * import swipeBack from '@/mixins/swipeBack.js'
 * export default {
 *   mixins: [swipeBack]
 * }
 */
export default {
	data() {
		return {
			// 触摸开始位置
			startX: 0,
			startY: 0,
			// 触摸移动位置
			moveX: 0,
			moveY: 0,
			// 是否正在滑动
			isSwiping: false,
			// 左滑返回阈值（px）
			swipeThreshold: 80
		};
	},
	onLoad() {
		// 检查是否可以返回
		const pages = getCurrentPages();
		this.canGoBack = pages.length > 1;
	},
	methods: {
		/**
		 * 触摸开始
		 */
		handleTouchStart(e) {
			if (!this.canGoBack) return;

			const touch = e.touches[0] || e.changedTouches[0];
			this.startX = touch.pageX;
			this.startY = touch.pageY;
			this.isSwiping = false;

			// 只在屏幕左边缘触发（左边 50px 范围内）
			if (this.startX > 50) {
				this.startX = 0;
				this.startY = 0;
			}
		},

		/**
		 * 触摸移动
		 */
		handleTouchMove(e) {
			if (!this.canGoBack || this.startX === 0) return;

			const touch = e.touches[0] || e.changedTouches[0];
			this.moveX = touch.pageX;
			this.moveY = touch.pageY;

			const deltaX = this.moveX - this.startX;
			const deltaY = this.moveY - this.startY;

			// 判断是否为横向滑动（横向滑动距离大于纵向）
			if (Math.abs(deltaX) > Math.abs(deltaY)) {
				this.isSwiping = true;
			}
		},

		/**
		 * 触摸结束
		 */
		handleTouchEnd(e) {
			if (!this.canGoBack || this.startX === 0) return;

			const touch = e.changedTouches[0];
			const endX = touch.pageX;
			const deltaX = endX - this.startX;

			// 判断是否为右滑且超过阈值
			if (this.isSwiping && deltaX > this.swipeThreshold) {
				this.handleGoBack();
			}

			// 重置状态
			this.startX = 0;
			this.startY = 0;
			this.moveX = 0;
			this.moveY = 0;
			this.isSwiping = false;
		},

		/**
		 * 返回上一页
		 */
		handleGoBack() {
			uni.navigateBack({
				delta: 1,
				fail: () => {
					console.log('返回失败，可能是首页');
				}
			});
		}
	}
};
