import { defineStore } from 'pinia'

export const useVoiceWindowStore = defineStore('voiceWindow', {
    state: () => ({
        showVoiceWindow: false,
        callType: null, // outgoing, incoming
        caller: null,
    }),

    actions: {
        openVoiceWindow(callType, caller) {
            this.callType = callType
            this.caller = caller
            this.showVoiceWindow = true
        },

        closeVoiceWindow() {
            this.showVoiceWindow = false
            this.caller = null
            this.callType = null
        }
    }
})
