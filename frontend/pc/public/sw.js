/* Wexigua Service Worker - 离线可用 + Web Push 接收
 *
 * 缓存策略（v3）：
 * - 导航请求（HTML）永不写入缓存，离线时才回退首页快照，避免旧 index.html 引用已删除的旧 hash 资源导致白屏
 * - 带 hash 的构建产物（/assets/）cache-first，命中即用，未命中再取网络
 * - 其它同源 GET 走 network-first，仅缓存 200 的正常响应
 * - 跳过开发服务器路径（/@vite、/@fs、/src、node_modules）、接口与 WebSocket
 */
const CACHE_NAME = 'wexigua-v5';
const OFFLINE_FALLBACK = '/index.html';

const SKIP_PREFIXES = ['/api', '/ws', '/@vite', '/@fs', '/@id', '/src/', '/node_modules/'];
const STATIC_PREFIXES = ['/assets/', '/icon-', '/vite.svg', '/manifest.webmanifest'];

function shouldSkip(url) {
  return SKIP_PREFIXES.some(function (p) { return url.pathname.indexOf(p) === 0; });
}

function isStaticAsset(url) {
  return STATIC_PREFIXES.some(function (p) { return url.pathname.indexOf(p) === 0; });
}

function putIfValid(request, response) {
  if (!response || !response.ok || response.status !== 200 || response.type !== 'basic') return;
  caches.open(CACHE_NAME).then(function (cache) {
    cache.put(request, response.clone());
  }).catch(function () {});
}

self.addEventListener('install', (event) => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const request = event.request;
  if (request.method !== 'GET') return;

  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;
  if (shouldSkip(url)) return;

  // 1) 页面导航：只走网络，离线才回退快照
  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request).catch(function () {
        return caches.match(OFFLINE_FALLBACK).then(function (cached) {
          return cached || new Response('离线状态，请联网后重试', {
            status: 503,
            headers: { 'Content-Type': 'text/plain; charset=utf-8' }
          });
        });
      })
    );
    return;
  }

  // 2) 带 hash 的静态产物：cache-first
  if (isStaticAsset(url)) {
    event.respondWith(
      caches.match(request).then(function (cached) {
        if (cached) return cached;
        return fetch(request).then(function (response) {
          putIfValid(request, response);
          return response;
        });
      })
    );
    return;
  }

  // 3) 其余同源 GET：network-first
  event.respondWith(
    fetch(request)
      .then(function (response) {
        putIfValid(request, response);
        return response;
      })
      .catch(function () {
        return caches.match(request);
      })
  );
});

/* ============ Web Push ============ */

self.addEventListener('push', (event) => {
  let payload = {};
  try {
    payload = event.data ? event.data.json() : {};
  } catch (e) {
    payload = { title: '西瓜 IM', body: String(event.data ? event.data.text() : '') };
  }
  const title = payload.title || '西瓜 IM';
  const body = payload.body || '你有一条新消息';
  const chatId = payload.chatId || '';
  const chatType = payload.chatType || 1;

  const options = {
    body,
    icon: payload.icon || '/icon-192.png',
    badge: '/icon-192.png',
    tag: payload.tag || ('xg-' + chatId),
    renotify: true,
    data: { chatId, chatType, url: payload.url || '/chat' }
  };

  event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const data = event.notification.data || {};
  const target = data.url || '/chat';

  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((list) => {
      for (const client of list) {
        // 已开着的页面：聚焦并让它自己跳到目标会话
        if (client.url.indexOf(self.location.origin) === 0 && 'focus' in client) {
          client.postMessage({ type: 'push-open-chat', chatId: data.chatId, chatType: data.chatType });
          return client.focus();
        }
      }
      if (self.clients.openWindow) {
        return self.clients.openWindow(target);
      }
    })
  );
});
