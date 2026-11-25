import request from '@/utils/request'

// 上传文件；options 支持 onUploadProgress 与 AbortController.signal
export function uploadFile(file, options = {}) {
    const formData = new FormData()
    formData.append('file', file)
    return request({
        url: 'client/file/upload',
        method: 'post',
        headers: {
            'Content-Type': 'multipart/form-data'
        },
        // 上传失败由调用方汇总提示（多图逐张失败只弹一次）
        silent: true,
        onUploadProgress: options.onUploadProgress,
        signal: options.signal,
        data: formData
    })
}
