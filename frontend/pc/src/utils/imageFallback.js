/**
 * 全站图片兜底 + 历史地址归一化
 * ------------------------------------------------------------
 * 后端头像大多指向 http://127.0.0.1:9000（MinIO），本地没起 MinIO 时
 * 每个 <img> 都是破图。这里用一次全局捕获（capture 阶段）统一替换成
 * 本地 SVG 占位，不去逐页改模板，也不会因为某个页面漏改而继续破图。
 *
 * 另一类破图来自「前端端口变了但数据里的绝对地址没变」：早期在 5173 端口
 * 运行时，机器人默认头像被写成 http://localhost:5173/bot-default-avatar.png，
 * 落进了缓存与历史数据；站点换到 5102 后这些地址全部 ERR_CONNECTION_REFUSED。
 * 因此先尝试把这类地址归一成同源相对路径再加载一次，确实取不到才退到占位图。
 *
 * 注意：
 * - error 事件不冒泡，必须 capture=true 才能拿到；
 * - data-fallback 标记防止占位图自身再触发一次死循环；
 * - data-retried 标记保证每条图片最多只重试一次；
 * - 只归一「本地主机 + 前端 dev 端口」，MinIO(9000) 等真实服务地址保持原样；
 * - 只处理 <img>，不影响其它元素。
 */

const FALLBACK_IMG =
  'data:image/svg+xml;charset=utf-8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 96 96">' +
      '<rect width="96" height="96" rx="18" fill="#e8eaf0"/>' +
      '<circle cx="48" cy="38" r="15" fill="#c3c8d4"/>' +
      '<path d="M18 88c5-17 16-26 30-26s25 9 30 26" fill="#c3c8d4"/>' +
      '</svg>'
  );

// 只命中本地主机的「前端 dev 端口」；9000(MinIO)、8083-8090(后端) 都不在内，避免误伤真实资源地址
const DEV_HOST_RE =
    /^https?:\/\/(?:localhost|127\.0\.0\.1)(?::(5173|5100|5102|4173))?(\/[^\s]*)$/i;

// 部分历史数据把 MinIO 地址直接落成了 localhost:9000。线上页面访问不到用户本机，
// 统一改走站点已有的 /media 同源代理；本地开发仍保留 9000，避免影响真实 MinIO。
const LOCAL_MINIO_RE = /^https?:\/\/(?:localhost|127\.0\.0\.1):9000\/([^\s]+)$/i;

function isLocalRuntime() {
  if (typeof location === 'undefined') return false;
  return ['localhost', '127.0.0.1', '0.0.0.0'].indexOf(location.hostname) !== -1;
}

// 线上旧数据里可能仍保存 IP 绝对地址；HTTPS 页面下这些资源会被 Mixed Content 拦截，
// 统一归一成同源相对路径，让代理按当前访问域名转发。
const PUBLIC_HOST_RE = /^https?:\/\/47\.94\.184\.61(?::\d+)?(\/[^\s]*)$/i;

/**
 * 把早期遗留的本地绝对地址归一成同源相对路径。
 * http://localhost:5173/bot-default-avatar.png -> /bot-default-avatar.png
 * 线上: http://localhost:9000/xigua/a.jpg -> /media/xigua/a.jpg
 * 本地开发: http://localhost:9000/xigua/a.jpg -> 原样返回（真实 MinIO）
 */
export function normalizeAssetUrl(url) {
    if (typeof url !== 'string') return url;
    const dev = url.match(DEV_HOST_RE);
    if (dev) return dev[2] || '/';
    const minio = url.match(LOCAL_MINIO_RE);
    if (minio && !isLocalRuntime()) return '/media/' + minio[1];
    const pub = url.match(PUBLIC_HOST_RE);
    if (pub) return pub[1] || '/';
    return url;
}

/**
 * 去掉 WebSocket 帧里可能出现的旧 HTTP 主机前缀，保留路径部分。
 * 这样 HTTPS 页面收到 /media/... 时会走当前域名的同源代理。
 */
export function normalizeSameOriginText(text) {
    if (typeof text !== 'string') return text;
    return text.replace(/https?:\/\/47\.94\.184\.61(?::\d+)?/g, '');
}

/**
 * 递归归一化一个对象/数组里所有字符串字段（用于清理 localStorage 里读出的历史数据）。
 * 深度上限 8 层，超出即原样返回，避免环形引用或超深结构拖慢启动。
 */
export function normalizeAssetUrlsDeep(node, depth = 0) {
  if (node == null || depth > 8) return node;
  if (typeof Blob !== 'undefined' && node instanceof Blob) return node;
  if (typeof node === 'string') return normalizeAssetUrl(node);
  if (Array.isArray(node)) {
    for (let i = 0; i < node.length; i++) {
      node[i] = normalizeAssetUrlsDeep(node[i], depth + 1);
    }
    return node;
  }
  if (typeof node === 'object') {
    for (const key of Object.keys(node)) {
      node[key] = normalizeAssetUrlsDeep(node[key], depth + 1);
    }
    return node;
  }
  return node;
}

export function installImageFallback() {
  if (typeof document === 'undefined') return;
  if (window.__XG_IMG_FALLBACK__) return;
  window.__XG_IMG_FALLBACK__ = true;

  document.addEventListener(
    'error',
    (e) => {
      const el = e.target;
      if (!el || el.tagName !== 'IMG') return;
      if (el.dataset.fallback === '1') return;

      const raw = el.getAttribute('src') || '';
      // 第一跳：地址看起来是历史遗留的本地 dev 端口，先换成同源相对路径重试
      const fixed = normalizeAssetUrl(raw);
      if (fixed !== raw && el.dataset.retried !== '1') {
        el.dataset.retried = '1';
        el.setAttribute('src', fixed);
        return;
      }

      // 第二跳（或本来就是正常地址）：换成内联占位图，避免页面继续破图
      el.dataset.fallback = '1';
      if (el.dataset.originSrc === undefined) {
        el.dataset.originSrc = raw;
      }
      el.classList.add('img-fallback');
      el.setAttribute('src', FALLBACK_IMG);
    },
    true
  );
}

export default installImageFallback;
