/**
 * 根据文件名或 MIME 类型判断文件类型。
 * @param {String} fileName
 * @param {String} fileType - MIME 类型
 * @returns {Number} 1-文本, 2-图片, 3-音频, 4-视频, 5-文件
 */
export function getFileType(fileName, fileType) {
	if (fileType) {
		if (fileType.startsWith('image/')) return 2
		if (fileType.startsWith('audio/')) return 3
		if (fileType.startsWith('video/')) return 4
	}
	const name = (fileName || '').toLowerCase()
	if (name) {
		if (name.match(/\.(jpg|jpeg|png|gif|bmp|webp|svg)$/)) return 2
		if (name.match(/\.(mp3|wav|aac|m4a|ogg|flac|wma|opus)$/)) return 3
		if (name.match(/\.(mp4|mov|avi|mkv|flv|wmv|webm|3gp)$/)) return 4
		if (name.match(/\.(txt|md|html|css|js|json|doc|docx|xls|xlsx|pdf|zip|rar|7z|tar|gz|ppt|pptx|csv|xml|py|java|c|cpp|h|go|rs|ts|tsx|jsx|vue|php|sql|rb|swift|kt|dart)$/)) return 5
	}
	return 5
}
