import { defineStore } from 'pinia'

export const useVoiceCallStore = defineStore('voiceCall', {
    state: () => ({
        visible: false,
        caller: null, // { userId, name, avatar }
    }),

    actions: {
        showIncomingCall(caller) {
            this.caller = caller
            this.visible = true
        },
        hide() {
            this.visible = false
            this.caller = null
        },
    },
})
