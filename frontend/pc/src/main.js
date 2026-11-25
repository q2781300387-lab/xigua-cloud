import { createApp } from 'vue';
import './style.css';
import './glass/glass.css';
import './glass/glass.js'; // 玻璃主题控制器（挂 window.XG_GLASS）
import App from './App.vue';
import router from './router';
import { createPinia } from 'pinia'
import { showConfirm } from '@/plugins/confirm'
import { ElMessage } from '@/plugins/message'
import { installImageFallback } from '@/utils/imageFallback'
import { getObject, setObject } from '@/utils/localStorage'

// 清理旧版本缓存的密码哈希（登录信息只需要 id / 昵称 / 头像等公开字段）
const cachedUser = getObject('userInfo')
if (cachedUser && cachedUser.password) {
  setObject('userInfo', cachedUser)
}

// 创建 Vue 应用实例
const app = createApp(App);

// 注册路由（element-plus 组件/API 由 unplugin 按需自动引入）
app.use(router);

const pinia = createPinia()
app.use(pinia)

// 挂载 Vue 应用实例
app.mount('#app');

// 玻璃主题初始化（DOM 就绪后应用）
window.XG_GLASS && window.XG_GLASS.init();

// 图片兜底：MinIO / 外链头像加载失败时统一换成占位图，避免全站破图
installImageFallback();

app.config.globalProperties.$confirm = showConfirm
// element-plus 已移除，但页面里仍大量使用 this.$message.success/error/...
// 这里统一挂到自绘 toast 上，避免调用时因 undefined 抛 TypeError 导致提示丢失
app.config.globalProperties.$message = ElMessage
