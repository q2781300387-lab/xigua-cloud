import config from '@/config/index.js'
import { normalizeMediaUrl } from '@/utils/media.js'

export { getFileType } from '@/core/index.js'

// #ifdef H5
function pickFileName(file, path) {
	if (file && typeof file === 'object' && (file.name || file.fileName)) {
		return file.name || file.fileName
	}
	if (typeof path === 'string' && !path.startsWith('blob:')) {
		const name = path.split('/').pop()
		if (name) return name
	}
	return 'upload.jpg'
}

async function resolveUploadBlob(file) {
	if (typeof Blob !== 'undefined' && file instanceof Blob) {
		return { blob: file, name: pickFileName(file) }
	}
	if (file && typeof file === 'object') {
		if (typeof Blob !== 'undefined' && file._file instanceof Blob) {
			return { blob: file._file, name: pickFileName(file, file.path) }
		}
		if (typeof Blob !== 'undefined' && file.file instanceof Blob) {
			return { blob: file.file, name: pickFileName(file, file.path) }
		}
	}
	const path = typeof file === 'string'
		? file
		: file && (file.path || file.tempFilePath || file.url)
	if (!path) throw new Error('无法读取文件')
	const response = await fetch(path)
	if (!response.ok) throw new Error('无法读取文件')
	const blob = await response.blob()
	return { blob, name: pickFileName(file, path) }
}
// #endif

/**
 * 上传文件
 * POST /file/upload
 * @param {Object|String|File} file - File/Blob、uni 临时文件对象或临时路径
 * @returns {Promise<Object>} 返回格式：{ code: 200, msg: string, data: string(文件URL) }
 */
export function uploadFile(file, options = {}) {
	const onProgress = typeof options.onProgress === 'function' ? options.onProgress : null
	const onTask = typeof options.onTask === 'function' ? options.onTask : null
	// #ifdef H5
	return (async () => {
		const token = uni.getStorageSync(config.tokenKey) || ''
		const { blob, name } = await resolveUploadBlob(file)
		return new Promise((resolve, reject) => {
			const xhr = new XMLHttpRequest()
			const task = {
				abort: () => xhr.abort()
			}
			if (onTask) onTask(task)
			xhr.open('POST', `${config.baseURL}/client/file/upload`)
			if (token) xhr.setRequestHeader(config.tokenKey, token)
			xhr.upload.onprogress = (event) => {
				if (onProgress && event.lengthComputable) onProgress({ loaded: event.loaded, total: event.total })
			}
			xhr.onload = () => {
				let data = null
				try {
					data = JSON.parse(xhr.responseText || '{}')
				} catch (error) {
					reject(new Error('解析响应失败'))
					return
				}
				if (xhr.status >= 200 && xhr.status < 300 && data.code === 200) {
					resolve({ ...data, data: normalizeMediaUrl(data.data) })
				} else {
					reject(new Error(data.msg || '上传失败'))
				}
			}
			xhr.onerror = () => reject(new Error('网络异常，上传失败'))
			xhr.onabort = () => {
				const error = new Error('已取消上传')
				error.name = 'AbortError'
				error.code = 'ERR_CANCELED'
				reject(error)
			}
			const formData = new FormData()
			formData.append('file', blob, name)
			xhr.send(formData)
		})
	})()
	// #endif

	// #ifndef H5
	return new Promise((resolve, reject) => {
		const token = uni.getStorageSync(config.tokenKey) || ''
		const task = uni.uploadFile({
			url: `${config.baseURL}/client/file/upload`,
			filePath: file.path || file,
			name: 'file',
			header: {
				...(token && { [config.tokenKey]: token })
			},
			success: (res) => {
				try {
					const data = JSON.parse(res.data)
					if (data.code === 200) {
						resolve({ ...data, data: normalizeMediaUrl(data.data) })
					} else {
						reject(new Error(data.msg || '上传失败'))
					}
				} catch (e) {
					console.error('上传文件响应解析失败:', e)
					reject(new Error('解析响应失败'))
				}
			},
			fail: (err) => {
				if (String((err && err.errMsg) || '').indexOf('abort') !== -1) {
					const error = new Error('已取消上传')
					error.name = 'AbortError'
					error.code = 'ERR_CANCELED'
					reject(error)
					return
				}
				console.error('上传文件失败:', err)
				reject(err)
			}
		})
		if (task && task.onProgressUpdate) {
			task.onProgressUpdate((event) => {
				if (onProgress) onProgress({ loaded: event.progress, total: 100, percent: event.progress })
			})
		}
		if (onTask) onTask(task)
	})
	// #endif
}