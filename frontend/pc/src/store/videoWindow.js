import { defineStore } from 'pinia'

export const useVideoWindowStore = defineStore('videoWindow', {
    state: () => ({
        showVideoWindow: false,
        callType: null, // outgoing, incoming
        caller: null,
    }),

    actions: {
        openVideoWindow(callType, caller) {
            this.callType = callType
            this.caller = caller
            this.showVideoWindow = true
        },

        closeVideoWindow() {
            this.showVideoWindow = false
            this.caller = null
            this.callType = null
        }
    }
})
