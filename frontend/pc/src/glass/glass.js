/* ============================================================
 * 玻璃主题控制器 · Aqua 风格移植（轻量版）
 * - 设置持久化：localStorage 前缀 xigua.glass.*
 * - 一键开关：挂 / 卸 html[data-glass] 门控属性
 * - 流体配色：HSL 连续调色（移植自 Aqua fluid-tones 思路）
 * - 背景层 / 粒子 / 边缘渐隐 均由本模块注入与回收
 * 两个前端共用同一份实现（深色检测自适应 html.dark / data-theme）
 * ============================================================ */

(function () {
  const PREFIX = 'xigua.glass.'
  const HUE_BASE = 217

  const DEFAULTS = {
    enabled: true,
    blur: 18, // 0-40
    frost: 7, // 0-100
    hue: 320, // 0-360
    depth: 25, // 0-100
    brightness: 50, // 0-100（50=不偏）
    background: 'fluid', // fluid | wallpaper
    wallpaper: '', // dataURL
    wallpaperBlur: 0, // 0-40
    wallpaperFrost: 0, // 0-100
    particles: true,
    edges: true,
  }

  let settings = {}
  let observer = null
  let particleRaf = 0
  let disposed = false

  /* ---------- 工具 ---------- */
  function clamp(v, min, max) {
    return Math.max(min, Math.min(max, v))
  }

  function readAll() {
    const out = {}
    try {
      const raw = localStorage.getItem(PREFIX + 'settings')
      if (raw) Object.assign(out, JSON.parse(raw))
    } catch (e) {
      /* 忽略损坏数据 */
    }
    Object.keys(DEFAULTS).forEach((k) => {
      if (!(k in out)) out[k] = DEFAULTS[k]
    })
    return out
  }

  function save() {
    try {
      localStorage.setItem(PREFIX + 'settings', JSON.stringify(settings))
    } catch (e) {
      /* localStorage 满（大壁纸）时忽略 */
    }
  }

  function isDark() {
    const html = document.documentElement
    return html.classList.contains('dark') || html.getAttribute('data-theme') === 'dark'
  }

  /* ---------- 流体配色（HSL） ---------- */
  function toneHue(hue) {
    return (hue + HUE_BASE) % 360
  }

  function hslStr(h, s, l) {
    return 'hsl(' + h + ',' + s + '%,' + l + '%)'
  }

  // depth: 0=纯深 / 100=极浅；暗色模式整体压暗
  function fluidColors(dark, hue, depth) {
    const base = toneHue(hue)
    const d = depth / 100
    let l1, l2, l3
    if (dark) {
      l1 = 16 + d * 14
      l2 = 14 + d * 12
      l3 = 20 + d * 16
    } else {
      l1 = 62 + d * 18
      l2 = 72 + d * 16
      l3 = 82 + d * 14
    }
    return {
      c1: hslStr((base + 360) % 360, 66, l1),
      c2: hslStr((base + 36) % 360, 58, l2),
      c3: hslStr((base + 72) % 360, 52, l3),
    }
  }

  /* ---------- 背景层 ---------- */
  function backdropEl() {
    return document.querySelector('.xg-glass-backdrop')
  }

  function buildBackdrop() {
    const wrap = document.createElement('div')
    wrap.className = 'xg-glass-backdrop'

    const ambient = document.createElement('div')
    ambient.className = 'xg-glass-ambient'
    wrap.appendChild(ambient)

    if (settings.background === 'fluid') {
      for (let i = 1; i <= 3; i++) {
        const b = document.createElement('i')
        b.className = 'xg-glass-fluid f' + i
        wrap.appendChild(b)
      }
    } else if (settings.wallpaper) {
      const w = document.createElement('div')
      w.className = 'xg-glass-wallpaper'
      const img = document.createElement('img')
      img.src = settings.wallpaper
      img.alt = ''
      w.appendChild(img)
      wrap.appendChild(w)
    }

    const veil = document.createElement('div')
    veil.className = 'xg-glass-veil'
    wrap.appendChild(veil)

    return wrap
  }

  function mountParticles() {
    if (!settings.particles || disposed) return
    if (document.querySelector('.xg-glass-particles')) return
    const canvas = document.createElement('canvas')
    canvas.className = 'xg-glass-particles'
    document.body.appendChild(canvas)
    const ctx = canvas.getContext('2d')
    const rect = () => {
      canvas.width = window.innerWidth
      canvas.height = window.innerHeight
    }
    rect()
    window.addEventListener('resize', rect)

    const N = 64
    const pts = []
    // 青 / 蓝 / 紫 三色带（浅色模式）
    const HUES = ['rgba(126,203,255,', 'rgba(90,155,255,', 'rgba(154,123,255,']
    for (let i = 0; i < N; i++) {
      const tier = i % 3 // 0 大光斑 / 1 中亮点 / 2 小细点
      pts.push({
        x: Math.random(),
        y: Math.random(),
        r:
          tier === 0
            ? 2.6 + Math.random() * 2.2
            : tier === 1
              ? 1.4 + Math.random() * 1.1
              : 0.5 + Math.random() * 0.7,
        vx: (Math.random() - 0.5) * 0.0002,
        vy: -0.00006 - Math.random() * 0.0001, // 轻微上浮
        ph: Math.random() * Math.PI * 2,
        a:
          tier === 0
            ? 0.1 + Math.random() * 0.16
            : tier === 1
              ? 0.22 + Math.random() * 0.24
              : 0.35 + Math.random() * 0.35,
        tier,
        hue: HUES[i % 3],
      })
    }

    let t = 0
    const loop = () => {
      if (disposed) return
      const w = canvas.width
      const h = canvas.height
      ctx.clearRect(0, 0, w, h)
      const dark = isDark()
      t += 0.004
      for (const p of pts) {
        p.x += p.vx
        p.y += p.vy + Math.sin(t * 6 + p.ph) * 0.00004
        if (p.x < -0.05) p.x = 1.05
        if (p.x > 1.05) p.x = -0.05
        if (p.y < -0.05) p.y = 1.05
        if (p.y > 1.05) p.y = -0.05
        const tw = 0.55 + 0.45 * Math.sin(t * (2 + p.tier) + p.ph)
        const alpha = p.a * tw
        const px = p.x * w
        const py = p.y * h
        if (p.tier === 0) {
          // 大光斑：径向渐变光晕
          const glow = p.r * 3
          const g = ctx.createRadialGradient(px, py, 0, px, py, glow)
          g.addColorStop(0, p.hue + (alpha * 0.85).toFixed(3) + ')')
          g.addColorStop(1, p.hue + '0)')
          ctx.fillStyle = g
          ctx.beginPath()
          ctx.arc(px, py, glow, 0, Math.PI * 2)
        } else {
          ctx.beginPath()
          if (dark) {
            ctx.fillStyle = 'rgba(180, 205, 255, ' + alpha.toFixed(3) + ')'
          } else {
            ctx.fillStyle = p.hue + alpha.toFixed(3) + ')'
          }
          ctx.arc(px, py, p.r, 0, Math.PI * 2)
        }
        ctx.fill()
      }
      particleRaf = requestAnimationFrame(loop)
    }
    particleRaf = requestAnimationFrame(loop)
  }

  function mountEdges() {
    if (!settings.edges) return
    if (document.querySelector('.xg-glass-edge')) return
    const t = document.createElement('div')
    t.className = 'xg-glass-edge top'
    const b = document.createElement('div')
    b.className = 'xg-glass-edge bottom'
    document.body.appendChild(t)
    document.body.appendChild(b)
  }

  function teardown() {
    document.querySelectorAll('.xg-glass-backdrop').forEach((n) => n.remove())
    const c = document.querySelector('.xg-glass-particles')
    if (c) {
      cancelAnimationFrame(particleRaf)
      c.remove()
    }
    document.querySelectorAll('.xg-glass-edge').forEach((n) => n.remove())
  }

  /* ---------- 应用 ---------- */
  function apply() {
    if (disposed) return
    const root = document.documentElement
    if (!settings.enabled) {
      root.removeAttribute('data-glass')
      teardown()
      return
    }
    root.setAttribute('data-glass', '')
    const dark = isDark()
    const colors = fluidColors(dark, settings.hue, settings.depth)
    const st = root.style
    st.setProperty('--glass-c1', colors.c1)
    st.setProperty('--glass-c2', colors.c2)
    st.setProperty('--glass-c3', colors.c3)
    st.setProperty('--glass-blur', clamp(settings.blur, 0, 40) + 'px')
    st.setProperty('--glass-frost', clamp(settings.frost, 0, 100) / 100)
    st.setProperty('--glass-wallpaper-blur', clamp(settings.wallpaperBlur, 0, 40) + 'px')
    st.setProperty('--glass-wallpaper-frost', clamp(settings.wallpaperFrost, 0, 100) / 100)
    // 亮度 veil：50=无；>50 白 overlay，<50 黑 overlay，各 0-0.35
    const b = clamp(settings.brightness, 0, 100)
    const wv = b > 50 ? (b - 50) / 50 * 0.35 : 0
    const bv = b < 50 ? (50 - b) / 50 * 0.35 : 0
    st.setProperty('--glass-brightness-w', wv.toFixed(3))
    st.setProperty('--glass-brightness-b', bv.toFixed(3))

    // 背景层
    const old = backdropEl()
    if (old) old.remove()
    document.body.appendChild(buildBackdrop())
    mountParticles()
    mountEdges()
  }

  /* ---------- 深色联动 ---------- */
  function watchDark() {
    if (observer) observer.disconnect()
    observer = new MutationObserver(() => {
      if (!settings.enabled) return
      apply()
    })
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class', 'data-theme'] })
  }

  /* ---------- 公开 API ---------- */
  window.XG_GLASS = {
    init() {
      disposed = false
      settings = readAll()
      apply()
      watchDark()
    },
    dispose() {
      disposed = true
      if (observer) observer.disconnect()
      teardown()
      document.documentElement.removeAttribute('data-glass')
    },
    get() {
      return Object.assign({}, settings)
    },
    set(key, value) {
      if (!(key in DEFAULTS)) return
      settings[key] = value
      save()
      apply()
    },
    toggle() {
      settings.enabled = !settings.enabled
      save()
      apply()
      return settings.enabled
    },
    reset() {
      settings = Object.assign({}, DEFAULTS)
      save()
      apply()
    },
  }
})()
