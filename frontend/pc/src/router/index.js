import { createRouter, createWebHistory } from 'vue-router';
import { getCookie } from '@/utils/cookie.js';
import { getTokenUserId, removeToken } from '@/utils/auth.js';
import { getObject } from '@/utils/localStorage.js';

// 路由级懒加载：首屏只加载核心包，切页按需取 chunk
const Login = () => import('@/views/Login.vue');
const Register = () => import('@/views/Register.vue');
const AuthInfo = () => import('@/views/AuthInfo.vue');
const Index = () => import('@/views/Index.vue');
const Chat = () => import('@/views/Chat.vue');
const Profile = () => import('@/views/Profile.vue');
const Home = () => import('@/views/Home.vue');
const Friend = () => import('@/views/Friend.vue');
const Moments = () => import('@/views/Moments.vue');
const MomentDetail = () => import('@/views/MomentDetail.vue');
const TreeHole = () => import('@/views/TreeHole.vue');
const Todo = () => import('@/views/Todo.vue');
const CallRecords = () => import('@/views/CallRecords.vue');
const Favorites = () => import('@/views/Favorites.vue');
const Bot = () => import('@/views/Bot.vue');
const BotCreate = () => import('@/views/BotCreate.vue');
const AiChat = () => import('@/views/AiChat.vue');
const SettingsPrivacy = () => import('@/views/SettingsPrivacy.vue');

const routes = [
    {
        path: '/',
        redirect: '/login'
    },
    {
        path: '/login',
        name: 'Login',
        component: Login,
        meta: { noAuth: true } // 标记为不需要认证
    },
    {
        path: '/register',
        name: 'Register',
        component: Register,
        meta: { noAuth: true } // 标记为不需要认证
    },
    {
        path: '/about',
        name: 'About',
        component: AuthInfo,
        meta: { noAuth: true, infoPage: 'about' }
    },
    {
        path: '/features',
        name: 'Features',
        component: AuthInfo,
        meta: { noAuth: true, infoPage: 'features' }
    },
    {
        path: '/contact',
        name: 'Contact',
        component: AuthInfo,
        meta: { noAuth: true, infoPage: 'contact' }
    },
    {
        path: '/',
        name: 'Index',
        component: Index,
        children: [
            {
                path: '', // 默认子路由
                name: 'Root',
                redirect: 'home'
            },
            {
                path: 'home',
                name: 'Home',
                component: Home
            },
            {
                path: 'chat',
                name: 'Chat',
                component: Chat
            },
            {
                path: 'bot',
                name: 'Bot',
                component: Bot
            },
            {
                path: 'bot/create',
                name: 'BotCreate',
                component: BotCreate
            },
            {
                // /ai 没有独立页面，直接落到机器人列表，避免手输地址出现空白页
                path: 'ai',
                redirect: '/bot'
            },
            {
                path: 'profile',
                name: 'Profile',
                component: Profile
            },
            {
                path: 'settings/privacy',
                name: 'SettingsPrivacy',
                component: SettingsPrivacy
            },
            {
                path: 'friend',
                name: 'Friend',
                component: Friend
            },
            {
                path: 'moments',
                name: 'Moments',
                component: Moments
            },
            {
                path: 'moments/:id',
                name: 'MomentDetail',
                component: MomentDetail
            },
            {
                path: 'treehole',
                name: 'TreeHole',
                component: TreeHole
            },
            {
                path: 'todo',
                name: 'Todo',
                component: Todo
            },
            {
                path: 'calls',
                name: 'CallRecords',
                component: CallRecords
            },
            {
                path: 'favorites',
                name: 'Favorites',
                component: Favorites
            },
            {
                path: 'ai-chat/:botId',
                name: 'AiChat',
                component: AiChat
            },
            {
                path: 'ai-chat/:botId/session/:sessionId',
                name: 'AiChatSession',
                component: AiChat
            },
            {
                path: 'ai-chat/:botId/:pathMatch(.*)*',
                redirect: to => `/ai-chat/${to.params.botId}`
            },
        ]
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

// 全局前置守卫
router.beforeEach(async (to, from, next) => {
    // 重复导航（连点同一菜单）直接跳过，避免闪烁与多余请求
    if (from.fullPath === to.fullPath && from.name === to.name) {
        return false;
    }

    const toPath = to.path;
    const fullPath = to.fullPath;
    // 不需要认证的路由直接放行（如注册页等）
    if (to.meta.noAuth) return next()

    // 检查本地是否存在登录凭证
    const localToken = getCookie('xigua-token')

    // 界面身份和登录态必须属于同一个账号。历史上出现过 cookie 仍是上一个账号、
    // 但 userInfo 已换成新账号的情况，页面会以新用户名展示、接口却按旧账号返回数据，
    // 属于跨账号信息暴露，必须在进入页面前强制重新登录。
    if (localToken) {
        const uiUser = getObject('userInfo') || {}
        const tokenUserId = getTokenUserId(localToken)
        if (uiUser.id && tokenUserId && String(uiUser.id) !== tokenUserId) {
            removeToken('xigua-token')
            localStorage.removeItem('userInfo')
            localStorage.removeItem('currentChatWindow')
            return next('/login')
        }
    }

    // 如果本地已登录, 直接放行
    if (localToken) {
        return next();
    }

    // 本地未登录时跳转到登录页
    if (!localToken) {
        return next('/login');
    }

    // 默认放行
    next()
})
export default router;
