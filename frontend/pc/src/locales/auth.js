import { ref } from "vue";

export const AUTH_LOCALE_KEY = "xigua-auth-locale";

export const AUTH_LANGUAGES = [
  { value: "zh-CN", label: "简体中文", shortLabel: "中文", htmlLang: "zh-CN" },
  { value: "en-US", label: "English", shortLabel: "EN", htmlLang: "en" },
];

const SUPPORTED = AUTH_LANGUAGES.map((item) => item.value);

function readStoredLocale() {
  try {
    const saved = localStorage.getItem(AUTH_LOCALE_KEY);
    if (SUPPORTED.includes(saved)) return saved;
  } catch (e) {
    /* 隐私模式下 localStorage 可能不可用，回退到中文 */
  }
  return "zh-CN";
}

export const authLocale = ref(readStoredLocale());

export function setAuthLocale(value) {
  if (!SUPPORTED.includes(value)) return;
  authLocale.value = value;
  try {
    localStorage.setItem(AUTH_LOCALE_KEY, value);
  } catch (e) {
    /* 忽略存储失败，本次切换仍然生效 */
  }
  if (typeof document !== "undefined") {
    document.documentElement.lang = value === "en-US" ? "en" : "zh-CN";
  }
}

export function syncAuthLanguageTag() {
  if (typeof document === "undefined") return;
  document.documentElement.lang = authLocale.value === "en-US" ? "en" : "zh-CN";
}

export const AUTH_MESSAGES = {
  "zh-CN": {
    common: {
      navLabel: "站点导航",
      nav: {
        about: "关于",
        features: "功能",
        contact: "联系我们",
      },
      switchLanguage: "切换语言",
      secure: "SECURE • ENCRYPTED • PRIVATE",
      copyright: "© 2026 西瓜即时通讯. All rights reserved.",
    },
    info: {
      backToLogin: "返回登录",
      createAccount: "创建账号",
      about: {
        pageTitle: "关于 · 西瓜IM",
        eyebrow: "关于西瓜IM",
        title: "让沟通回归简单",
        lead:
          "西瓜IM 是一个基于 Spring Cloud 微服务架构的多端即时通讯系统，覆盖单聊、群聊、AI 助手与朋友圈等日常沟通场景。",
        sections: [
          {
            heading: "我们关注什么",
            body:
              "消息要及时送达，界面要足够克制，常用操作要一眼能找到。登录、聊天、联系人和设置都围绕这三点展开。",
          },
          {
            heading: "技术底座",
            body:
              "后端采用 Spring Cloud Alibaba 微服务拆分，WebSocket 维持长连接，Redis 承担在线状态与缓存，MySQL 保存业务数据，Kafka 处理异步消息，MinIO 存放图片与文件。",
          },
          {
            heading: "设计语言",
            body:
              "浅色 iOS 风格、黑白品牌、低对比边框和实体按键。减少装饰，让内容和操作成为视觉重点。",
          },
        ],
      },
      features: {
        pageTitle: "功能 · 西瓜IM",
        eyebrow: "功能",
        title: "从一条消息开始",
        lead:
          "聊天、社交、AI 和效率工具放在同一个工作空间里，常用能力不需要来回切换应用。",
        cards: [
          {
            title: "即时通讯",
            body: "单聊、群聊、消息撤回、引用回复、@提及和已读回执。",
          },
          {
            title: "语音与通话",
            body: "语音消息、语音转文字，以及一对一视频通话。",
          },
          {
            title: "AI 助手",
            body: "AI 对话、智能回复、群聊摘要与待办提取，以及文生图。",
          },
          {
            title: "社交与搜索",
            body: "朋友圈发布与浏览，全局搜索快速定位联系人和消息。",
          },
          {
            title: "效率工具",
            body: "定时消息、待办与日程提醒，以及桌面通知。",
          },
          {
            title: "群组协作",
            body: "群文件中心、群管理、成员与权限控制。",
          },
        ],
        note: "功能以实际页面为准，后续会继续迭代。",
      },
      contact: {
        pageTitle: "联系 · 西瓜IM",
        eyebrow: "联系我们",
        title: "找到对的入口",
        lead:
          "账号、登录、注册或功能建议，可以先从下面的分类找到对应的处理方式。",
        channels: [
          {
            title: "账号与登录",
            body:
              "忘记密码、账号异常或登录失败：在登录页点击「忘记密码」，按提示联系管理员。",
          },
          {
            title: "注册与验证码",
            body:
              "QQ 邮箱收不到验证码时，先检查垃圾邮件和邮箱地址，再联系管理员核对发信配置。",
          },
          {
            title: "建议与问题反馈",
            body:
              "记录问题发生的时间、页面和操作步骤，交给系统管理员或项目维护者，便于复现。",
          },
        ],
        note:
          "当前项目未配置公开客服热线，请通过部署管理员或项目维护者联系。",
      },
    },
    login: {
      pageTitle: "登录 · 西瓜IM",
      heading: "欢迎回来",
      subtitle: "请输入您的凭据以访问您的工作空间",
      accountLabel: "账号",
      accountPlaceholder: "请输入用户名 / 邮箱",
      passwordLabel: "密码",
      passwordPlaceholder: "请输入密码",
      remember: "记住账号",
      forgot: "忘记密码",
      submit: "登录",
      submitting: "登录中...",
      showPassword: "显示密码",
      hidePassword: "隐藏密码",
      otherMethods: "其他方式登录",
      messageMethod: "消息验证",
      messageMethodLabel: "使用消息验证登录（暂未开放）",
      messageMethodTitle: "消息验证登录暂未开放",
      fingerprintMethod: "指纹",
      fingerprintMethodLabel: "使用指纹登录（暂未开放）",
      fingerprintMethodTitle: "指纹登录暂未开放",
      mobileMethodLabel: "打开手机端",
      mobileMethodTitle: "在新标签页打开手机端",
      noAccount: "还没有账号？",
      join: "申请加入",
      validation: {
        usernameRequired: "请输入用户名/邮箱",
        passwordRequired: "请输入密码",
        passwordLength: "密码长度在 6 到 20 个字符之间",
      },
      toast: {
        contactAdmin: "请联系管理员重置密码",
        altUnavailable: "{method}登录暂未开放，请使用账号密码登录",
        checkInput: "请检查输入的内容",
        loginFailed: "登录失败：",
        userInfoFailed: "获取用户信息失败：",
        tryLater: "请稍后重试",
      },
    },
    register: {
      pageTitle: "注册 · 西瓜IM",
      heading: "创建账户",
      subtitle: "填写以下信息创建您的工作空间账户",
      usernameLabel: "用户名",
      usernamePlaceholder: "请输入用户名",
      emailLabel: "QQ邮箱",
      emailPlaceholder: "example@qq.com",
      codeLabel: "验证码",
      codePlaceholder: "6位数字验证码",
      getCode: "获取验证码",
      sendingCode: "发送中...",
      resendCode: "重新获取({seconds}s)",
      passwordLabel: "密码",
      passwordPlaceholder: "••••••••",
      passwordHint: "8-20 位，需同时包含字母与数字",
      confirmLabel: "确认密码",
      confirmPlaceholder: "请再次输入密码",
      showConfirm: "显示确认密码",
      hideConfirm: "隐藏确认密码",
      submit: "创建账户",
      submitting: "注册中...",
      haveAccount: "已有账户？",
      signIn: "立即登录",
      validation: {
        usernameRequired: "请输入用户名",
        usernameLength: "用户名长度在2到20个字符之间",
        emailRequired: "请输入QQ邮箱",
        emailInvalid: "请输入正确的QQ邮箱格式",
        codeRequired: "请输入验证码",
        codeLength: "验证码为6位数字",
        passwordRequired: "请输入密码",
        passwordLength: "密码长度在8到20个字符之间",
        passwordNoSpaces: "密码不能包含空格",
        passwordLetter: "密码必须包含字母",
        passwordDigit: "密码必须包含数字",
        passwordSame: "密码不能与用户名相同",
        passwordRepeats: "密码不能包含3个及以上连续重复的字符",
        confirmRequired: "请再次输入密码",
        confirmMismatch: "两次输入的密码不一致",
      },
      toast: {
        checkInput: "请检查输入的内容",
        success: "注册成功，请登录",
        registerFailed: "注册失败，请稍后重试",
        codeSent: "验证码已发送至您的邮箱",
        sendFailed: "发送验证码失败，请稍后重试",
      },
    },
  },
  "en-US": {
    common: {
      navLabel: "Site navigation",
      nav: {
        about: "About",
        features: "Features",
        contact: "Contact",
      },
      switchLanguage: "Switch language",
      secure: "SECURE • ENCRYPTED • PRIVATE",
      copyright: "© 2026 Xigua IM. All rights reserved.",
    },
    info: {
      backToLogin: "Back to sign in",
      createAccount: "Create account",
      about: {
        pageTitle: "About · Xigua IM",
        eyebrow: "About Xigua IM",
        title: "Make conversations feel simple",
        lead:
          "Xigua IM is a multi-platform messaging system built on Spring Cloud microservices, covering direct messages, group chats, an AI assistant, and Moments.",
        sections: [
          {
            heading: "What we focus on",
            body:
              "Messages should arrive quickly, the interface should stay restrained, and common actions should be easy to find. Sign-in, chat, contacts, and settings are all built around those three goals.",
          },
          {
            heading: "Technical foundation",
            body:
              "The backend is split into Spring Cloud Alibaba microservices. WebSocket keeps connections open, Redis handles presence and caching, MySQL stores business data, Kafka carries asynchronous events, and MinIO stores images and files.",
          },
          {
            heading: "Design language",
            body:
              "A light iOS look, a black-and-white brand, low-contrast borders, and physical buttons. Less decoration, more focus on content and actions.",
          },
        ],
      },
      features: {
        pageTitle: "Features · Xigua IM",
        eyebrow: "Features",
        title: "It starts with one message",
        lead:
          "Chat, social, AI, and productivity tools live in one workspace, so common tasks don't require switching between apps.",
        cards: [
          {
            title: "Messaging",
            body:
              "Direct and group chats, message recall, quote replies, mentions, and read receipts.",
          },
          {
            title: "Voice and calls",
            body: "Voice messages, speech-to-text, and one-to-one video calls.",
          },
          {
            title: "AI assistant",
            body:
              "AI chat, smart replies, group summaries and action-item extraction, plus text-to-image.",
          },
          {
            title: "Social and search",
            body:
              "Publish and browse Moments, and find contacts or messages with global search.",
          },
          {
            title: "Productivity",
            body:
              "Scheduled messages, to-dos and reminders, and desktop notifications.",
          },
          {
            title: "Group collaboration",
            body: "A group file center, group management, and member permissions.",
          },
        ],
        note: "Features follow the live product and will keep evolving.",
      },
      contact: {
        pageTitle: "Contact · Xigua IM",
        eyebrow: "Contact",
        title: "Find the right channel",
        lead:
          "For account, sign-in, registration, or product feedback, start with the category below.",
        channels: [
          {
            title: "Account and sign-in",
            body:
              "Forgot your password, see an account issue, or can't sign in? Use “Forgot password?” on the sign-in page and contact your administrator.",
          },
          {
            title: "Registration and codes",
            body:
              "If the QQ email code doesn't arrive, check spam and the address first, then ask your administrator to verify the mail configuration.",
          },
          {
            title: "Feedback and issues",
            body:
              "Note when the issue happened, which page you were on, and what you did, then send it to your administrator or project maintainer so it can be reproduced.",
          },
        ],
        note:
          "This deployment has no public support hotline. Contact your deployment administrator or project maintainer.",
      },
    },
    login: {
      pageTitle: "Sign in · Xigua IM",
      heading: "Welcome back",
      subtitle: "Enter your credentials to access your workspace.",
      accountLabel: "Account",
      accountPlaceholder: "Username or email",
      passwordLabel: "Password",
      passwordPlaceholder: "Enter your password",
      remember: "Remember account",
      forgot: "Forgot password?",
      submit: "Sign in",
      submitting: "Signing in...",
      showPassword: "Show password",
      hidePassword: "Hide password",
      otherMethods: "Other sign-in options",
      messageMethod: "Message verification",
      messageMethodLabel: "Sign in with message verification (coming soon)",
      messageMethodTitle: "Message verification is not available yet",
      fingerprintMethod: "Fingerprint",
      fingerprintMethodLabel: "Sign in with fingerprint (coming soon)",
      fingerprintMethodTitle: "Fingerprint sign-in is not available yet",
      mobileMethodLabel: "Open mobile site",
      mobileMethodTitle: "Open the mobile site in a new tab",
      noAccount: "New to Xigua IM?",
      join: "Join now",
      validation: {
        usernameRequired: "Enter your username or email.",
        passwordRequired: "Enter your password.",
        passwordLength: "Password must be 6–20 characters.",
      },
      toast: {
        contactAdmin: "Contact your administrator to reset your password.",
        altUnavailable:
          "{method} sign-in is not available yet. Use your username and password.",
        checkInput: "Check the information you entered.",
        loginFailed: "Sign-in failed: ",
        userInfoFailed: "Couldn't load your account: ",
        tryLater: "Please try again later.",
      },
    },
    register: {
      pageTitle: "Create account · Xigua IM",
      heading: "Create account",
      subtitle: "Fill in your details to create your workspace account.",
      usernameLabel: "Username",
      usernamePlaceholder: "Enter a username",
      emailLabel: "QQ email",
      emailPlaceholder: "example@qq.com",
      codeLabel: "Verification code",
      codePlaceholder: "6-digit code",
      getCode: "Get code",
      sendingCode: "Sending...",
      resendCode: "Resend ({seconds}s)",
      passwordLabel: "Password",
      passwordPlaceholder: "••••••••",
      passwordHint: "8–20 characters with letters and numbers",
      confirmLabel: "Confirm password",
      confirmPlaceholder: "Enter your password again",
      showConfirm: "Show confirm password",
      hideConfirm: "Hide confirm password",
      submit: "Create account",
      submitting: "Creating account...",
      haveAccount: "Already have an account?",
      signIn: "Sign in",
      validation: {
        usernameRequired: "Enter a username.",
        usernameLength: "Username must be 2–20 characters.",
        emailRequired: "Enter your QQ email.",
        emailInvalid: "Enter a valid QQ email address.",
        codeRequired: "Enter the verification code.",
        codeLength: "The verification code must be 6 digits.",
        passwordRequired: "Enter a password.",
        passwordLength: "Password must be 8–20 characters.",
        passwordNoSpaces: "Password cannot contain spaces.",
        passwordLetter: "Password must include a letter.",
        passwordDigit: "Password must include a number.",
        passwordSame: "Password cannot match your username.",
        passwordRepeats: "Password cannot contain 3 or more repeated characters.",
        confirmRequired: "Enter your password again.",
        confirmMismatch: "The passwords do not match.",
      },
      toast: {
        checkInput: "Check the information you entered.",
        success: "Account created. Please sign in.",
        registerFailed: "Registration failed. Please try again.",
        codeSent: "The verification code has been sent to your email.",
        sendFailed: "Couldn't send the code. Please try again.",
      },
    },
  },
};

export function getAuthCopy(locale) {
  return AUTH_MESSAGES[locale] || AUTH_MESSAGES["zh-CN"];
}

// 后端目前返回中文提示；英文界面下把常见提示映射为英文，
// 未命中的中文提示改用各页面自带的英文兜底，避免中英混排。
const SERVER_MESSAGE_TRANSLATIONS = [
  ["密码不能包含 3 个及以上连续重复的字符", "Password cannot contain 3 or more repeated characters."],
  ["验证码错误或已过期", "The verification code is incorrect or expired."],
  ["密码不能与用户名相同", "Password cannot match your username."],
  ["验证码已过期", "The verification code has expired. Request a new one."],
  ["验证码错误", "Incorrect verification code."],
  ["用户名已存在", "That username is already taken."],
  ["邮箱已存在", "That email is already registered."],
  ["邮箱格式错误", "Enter a valid QQ email address."],
  ["该邮箱未注册", "That email is not registered."],
  ["密码不可以为空", "Enter a password."],
  ["密码至少", "Password must be at least 8 characters."],
  ["密码最多", "Password must be at most 20 characters."],
  ["密码不能包含空格", "Password cannot contain spaces."],
  ["密码必须包含字母", "Password must include a letter."],
  ["密码必须包含数字", "Password must include a number."],
  ["密码错误", "Incorrect password."],
  ["用户不存在", "Account not found."],
];

export function localizeServerMessage(message, fallback) {
  const raw = typeof message === "string" ? message.trim() : "";
  if (authLocale.value !== "en-US") return raw || fallback;
  if (!raw) return fallback;
  for (const [zh, en] of SERVER_MESSAGE_TRANSLATIONS) {
    if (raw.includes(zh)) return en;
  }
  // 后端已经是英文（或英文与其他语言混排）时直接展示，不再二次处理
  return /[\u4e00-\u9fff]/.test(raw) ? fallback : raw;
}
