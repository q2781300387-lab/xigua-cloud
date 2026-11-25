import { defineStore } from 'pinia'

export const useVideoCallStore = defineStore('videoCall', {
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
