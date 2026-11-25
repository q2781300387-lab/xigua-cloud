import { core } from '@/core'

// 发送邮箱验证码；config 可传 { silent: true } 关闭全局提示
export function sendEmail(email, config = {}) {
    return core.email.sendEmailCode(email, config)
}
