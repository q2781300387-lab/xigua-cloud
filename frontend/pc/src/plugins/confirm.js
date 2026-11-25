/**
 * 全局确认弹框
 *
 * 说明：历史上这里用独立 createApp + Teleport 渲染 ConfirmDialog，
 * 但在玻璃主题下发现该层始终不参与绘制（DOM 存在、命中也正常，就是画不出来），
 * 导致「移出成员 / 退群 / 解散」等所有二次确认都变成一个看不见的空框。
 * 因此改为直接操作 DOM 渲染，层级与配色完全可控。
 *
 * 兼容两种调用方式：
 * 1) Element Plus 风格：this.$confirm('正文', '标题', { confirmButtonText, cancelButtonText })
 *    → 返回 Promise：点「确定」resolve，点「取消」reject(new Error('cancel'))
 * 2) 对象风格：showConfirm({ title, content, onConfirm, onCancel })
 */

let closeCurrent = null

function isDark() {
    const el = document.documentElement
    return !!el && (el.dataset.theme === 'dark' || el.classList.contains('dark'))
}

function mount({ title, content, confirmText, cancelText, onConfirm, onCancel }) {
    // 单例：同一时刻只保留一个确认框
    if (closeCurrent) closeCurrent(false)

    const dark = isDark()
    const mask = document.createElement('div')
    mask.setAttribute('data-xg-confirm', 'mask')
    mask.style.cssText = [
        'position:fixed', 'left:0', 'top:0', 'right:0', 'bottom:0',
        'z-index:2600', 'background:rgba(0,0,0,.45)',
        'display:flex', 'align-items:center', 'justify-content:center',
        'font-family:inherit', 'box-sizing:border-box'
    ].join(';')

    const box = document.createElement('div')
    box.setAttribute('data-xg-confirm', 'box')
    const narrow = window.innerWidth < 768
    box.style.cssText = [
        'box-sizing:border-box',
        narrow ? 'width:100%' : 'width:320px',
        'max-width:100%',
        'background:' + (dark ? '#1c1c1e' : 'rgba(255,255,255,.97)'),
        'color:' + (dark ? '#f2f2f7' : '#1c1c1e'),
        'border:1px solid ' + (dark ? 'rgba(84,84,88,.6)' : 'rgba(60,60,67,.12)'),
        'border-radius:' + (narrow ? '16px 16px 0 0' : '14px'),
        'box-shadow:0 12px 40px rgba(19,45,83,.24)',
        'padding:20px',
        narrow ? 'margin-top:auto' : '',
        'animation:xgConfirmIn .2s cubic-bezier(.32,.72,0,1)'
    ].filter(Boolean).join(';')

    const titleEl = document.createElement('div')
    titleEl.style.cssText = 'font-size:17px;font-weight:600;margin-bottom:8px;'
    titleEl.textContent = title || '提示'

    const contentEl = document.createElement('div')
    contentEl.style.cssText = 'font-size:14px;line-height:1.5;margin-bottom:20px;color:' +
        (dark ? 'rgba(235,235,245,.66)' : 'rgba(60,60,67,.62)')
    contentEl.textContent = content || ''

    const actions = document.createElement('div')
    actions.style.cssText = 'display:flex;justify-content:flex-end;gap:12px;'

    const btnBase = 'border:none;padding:8px 18px;border-radius:8px;font-size:14px;font-weight:500;cursor:pointer;' +
        'transition:filter .2s,background .2s;-webkit-tap-highlight-color:transparent;'

    const cancelBtn = document.createElement('button')
    cancelBtn.type = 'button'
    cancelBtn.textContent = cancelText || '取消'
    cancelBtn.style.cssText = btnBase + 'background:' +
        (dark ? 'rgba(120,120,128,.24)' : 'rgba(120,120,128,.14)') + ';color:inherit;'

    const confirmBtn = document.createElement('button')
    confirmBtn.type = 'button'
    confirmBtn.textContent = confirmText || '确定'
    confirmBtn.style.cssText = btnBase + 'background:#0a84ff;color:#fff;'

    actions.appendChild(cancelBtn)
    actions.appendChild(confirmBtn)

    if (!title) titleEl.style.display = 'none'
    if (!content) contentEl.style.display = 'none'

    box.appendChild(titleEl)
    box.appendChild(contentEl)
    box.appendChild(actions)

    // 移动端贴着底部（iOS ActionSheet 式的观感）
    if (narrow) {
        mask.style.alignItems = 'flex-end'
        box.style.marginTop = '0'
    }

    mask.appendChild(box)

    // 入场动画的关键帧只注册一次
    if (!document.getElementById('xg-confirm-keyframes')) {
        const style = document.createElement('style')
        style.id = 'xg-confirm-keyframes'
        style.textContent = '@keyframes xgConfirmIn{from{transform:translateY(10px)}to{transform:translateY(0)}}'
        document.head.appendChild(style)
    }

    document.body.appendChild(mask)

    let settled = false
    function close(confirmed) {
        if (settled) return
        settled = true
        mask.remove()
        document.removeEventListener('keydown', onKey)
        closeCurrent = null
        if (confirmed) {
            onConfirm && onConfirm()
        } else {
            onCancel && onCancel()
        }
    }

    function onKey(e) {
        if (e.key === 'Escape') close(false)
    }

    cancelBtn.addEventListener('click', () => close(false))
    confirmBtn.addEventListener('click', () => close(true))
    mask.addEventListener('click', (e) => {
        if (e.target === mask) close(false)
    })
    document.addEventListener('keydown', onKey)

    closeCurrent = close

    // 让键盘用户可以直接回车确认
    setTimeout(() => { try { confirmBtn.focus() } catch (e) { /* noop */ } }, 60)

    return close
}

export function showConfirm(...args) {
    const first = args[0]

    // ---- 对象风格 ----
    if (first && typeof first === 'object' && !Array.isArray(first)) {
        mount({
            title: first.title || '提示',
            content: first.content || '',
            confirmText: first.confirmText || '确定',
            cancelText: first.cancelText || '取消',
            onConfirm: first.onConfirm || null,
            onCancel: first.onCancel || null
        })
        return
    }

    // ---- Element Plus 风格 ----
    const message = typeof first === 'string' ? first : ''
    const title = typeof args[1] === 'string' && args[1] ? args[1] : '提示'
    const options = (args[1] && typeof args[1] === 'object') ? args[1] : (args[2] || {})

    return new Promise((resolve, reject) => {
        mount({
            title,
            content: message,
            confirmText: options.confirmButtonText || '确定',
            cancelText: options.cancelButtonText || '取消',
            onConfirm: () => resolve('confirm'),
            onCancel: () => reject(new Error('cancel'))
        })
    })
}
