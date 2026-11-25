import { core } from '@/core/index.js'

export const getPrivacySettings = () => core.privacy.getSettings()
export const savePrivacySettings = (data) => core.privacy.saveSettings(data)
export const getBlockList = () => core.privacy.getBlocks()
export const blockUser = (targetId) => core.privacy.block(targetId)
export const unblockUser = (targetId) => core.privacy.unblock(targetId)
export const isUserBlocked = (targetId) => core.privacy.isBlocked(targetId)
export const getLoginDevices = () => core.privacy.getDevices()
export const kickOtherDevices = () => core.privacy.kickOthers()
export const submitReport = (data) => core.privacy.report(data)
export const exportPersonalData = () => core.privacy.exportData()
export const deactivateAccount = (data) => core.privacy.deactivate(data)
