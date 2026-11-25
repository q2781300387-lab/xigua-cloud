// 媒体与旧域名归一化：保证 HTTPS 页面不会去请求旧的 HTTP 主机
const LEGACY_HOST_RE = /^(?:https?:)?\/\/(?:47\.94\.184\.61|api\.xgchat\.cn)(?::\d+)?(\/.*)?$/i

function isH5Runtime() {
	return typeof window !== 'undefined' && !!window.location
}

export function normalizeMediaUrl(url) {
	if (!url || typeof url !== 'string') return url
	if (!isH5Runtime()) return url

	const match = url.match(LEGACY_HOST_RE)
	if (match) {
		return match[1] || '/'
	}
	return url
}

export function normalizeAssetUrlsDeep(value) {
	if (typeof value === 'string') return normalizeMediaUrl(value)
	if (Array.isArray(value)) return value.map(normalizeAssetUrlsDeep)
	if (value && typeof value === 'object') {
		if (typeof Blob !== 'undefined' && value instanceof Blob) return value
		const result = {}
		Object.keys(value).forEach(key => {
			result[key] = normalizeAssetUrlsDeep(value[key])
		})
		return result
	}
	return value
}
