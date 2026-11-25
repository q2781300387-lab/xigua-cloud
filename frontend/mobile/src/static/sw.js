/* 西瓜 IM H5 本地缓存：首屏秒开 + 静态资源离线可用 */
const VERSION = 'xigua-shell-v4';
const SHELL_CACHE = VERSION + '-shell';
const STATIC_CACHE = VERSION + '-static';
const API_PREFIXES = ['/client/', '/center/', '/ai/', '/admin/', '/search/', '/media/'];

self.addEventListener('install', (event) => {
  event.waitUntil((async () => {
    const cache = await caches.open(SHELL_CACHE);
    await Promise.all(['/', '/index.html'].map(async (url) => {
      try {
        const response = await fetch(url, { cache: 'reload' });
        if (response && response.ok) await cache.put(url, response.clone());
      } catch (e) {
        /* 首次安装网络失败时，等运行时再缓存 */
      }
    }));
    await self.skipWaiting();
  })());
});

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(keys.filter((key) => key.indexOf(VERSION) !== 0).map((key) => caches.delete(key)));
    await self.clients.claim();
  })());
});

function isApi(url) {
  return API_PREFIXES.some((prefix) => url.pathname.indexOf(prefix) === 0);
}

async function cacheNavigation(request) {
  const cache = await caches.open(SHELL_CACHE);
  const cached = (await cache.match('/index.html')) || (await cache.match('/'));
  const network = fetch(request, { cache: 'no-cache' })
    .then((response) => {
      if (response && response.ok) cache.put('/index.html', response.clone());
      return response;
    })
    .catch(() => null);

  if (cached) {
    /* 先用本地入口立即渲染，新版本在后台更新，下次打开生效 */
    network.catch(() => {});
    return cached;
  }
  return (await network) || fetch(request);
}

async function cacheStatic(request) {
  const cache = await caches.open(STATIC_CACHE);
  const cached = await cache.match(request);
  if (cached) return cached;
  const response = await fetch(request);
  if (response && response.ok) cache.put(request, response.clone());
  return response;
}

self.addEventListener('fetch', (event) => {
  const request = event.request;
  if (request.method !== 'GET') return;

  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;
  if (isApi(url)) return;

  if (request.mode === 'navigate') {
    event.respondWith(cacheNavigation(request));
    return;
  }

  if (url.pathname.indexOf('/static/') === 0) {
    event.respondWith(cacheStatic(request));
  }
});

/* Web Push：页面关闭/后台时由系统通知唤起 */
self.addEventListener('push', (event) => {
  let payload = {};
  try {
    payload = event.data ? event.data.json() : {};
  } catch (e) {
    payload = { body: event.data ? event.data.text() : '' };
  }
  const title = payload.title || '西瓜 IM';
  const options = {
    body: payload.body || '你有一条新消息',
    icon: '/static/brand-lockup.png',
    badge: '/static/brand-lockup.png',
    tag: payload.tag || 'xigua-message',
    renotify: true,
    data: {
      url: payload.url || '/chat',
      chatId: payload.chatId || '',
      chatType: payload.chatType || 1
    }
  };
  event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const target = (event.notification.data && event.notification.data.url) || '/chat';
  event.waitUntil((async () => {
    const clientsList = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
    for (const client of clientsList) {
      if ('focus' in client) {
        if ('navigate' in client && target.indexOf('/chat') === 0) {
          try { await client.navigate(target); } catch (e) { /* 跨路由导航失败时仅聚焦 */ }
        }
        return client.focus();
      }
    }
    if (self.clients.openWindow) return self.clients.openWindow(target);
  })());
});
