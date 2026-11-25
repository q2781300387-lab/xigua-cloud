package com.xigua.domain.enums;

import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public enum MessageSubType {
    // notify
    FRIEND_ONLINE("friend_online", "好友上线通知"),
    FRIEND_OFFLINE("friend_offline", "好友下线通知"),
    SWITCH_CHAT_WINDOW("switch_chat_window", "切换聊天窗口"),
    REMOVE_FRIEND("close_chat_window", "关闭聊天窗口"),


    // chat
    MES_SEND("mes_send", "消息发送"),
    MES_SEND_ACK("mes_send_ack", "消息发送确认"),
    MES_RECEIVE("mes_receive", "消息接收"),
    SUBMIT_UNREAD("submit_unread", "提交未读消息"),
    MES_READ("mes_read", "消息已读"),
    MES_WITHDRAW("mes_withdraw", "消息撤回"),
    MES_REACTION("mes_reaction", "消息表情回应"),
    TYPING("typing", "对方正在输入"),
    SYSTEM_MES("system_mes", "系统消息"),

    // unread
    FRIEND_UNREAD("friend_unread", "好友未读消息"),

    // heart_beat
    PING("ping", "心跳发送（客户端发送）"),
    PONG("pong", "心跳回复（服务端响应）"),

    // ai
    STOP_BOT_RESPONSE("stop_bot_response", "暂停机器人输出"),
    BOT_RESPONSE_END("bot_response_end", "机器人输出结束"),

    // audio_call
    AUDIO_CALL_REQUEST("audio_call_request", "音频呼叫请求"),
    AUDIO_CALL_INCOMING("audio_call_incoming", "音频呼叫来电"),
    AUDIO_CALL_ACCEPTED("audio_call_accepted", "音频呼叫接受"),
    AUDIO_CALL_REJECTED("audio_call_rejected", "音频呼叫拒绝"),
    AUDIO_CALL_TIMEOUT("audio_call_timeout", "音频呼叫超时未接听"),
    AUDIO_CALL_CANCELED("audio_call_canceled", "音频呼叫已取消"),
    AUDIO_CALL_OFFER("audio_call_offer", "音频呼叫Offer信令"),
    AUDIO_CALL_ANSWER("audio_call_answer", "音频呼叫Answer信令"),
    AUDIO_CALL_ICE_CANDIDATE("audio_call_ice_candidate", "音频呼叫ICE候选"),
    AUDIO_CALL_ENDED("audio_call_ended", "音频通话结束"),
    AUDIO_CALL_MIC_STATUS("audio_call_mic_status", "音频麦克风状态"),
    AUDIO_CALL_SPEAKING("audio_call_speaking", "音频语音活动检测"),
    AUDIO_CALL_STATS("audio_call_stats", "音频通话统计"),
    AUDIO_CALL_PING("audio_call_ping", "音频通话心跳"),

    // video_call
    VIDEO_CALL_REQUEST("video_call_request", "视频呼叫请求"),
    VIDEO_CALL_INCOMING("video_call_incoming", "视频呼叫来电"),
    VIDEO_CALL_ACCEPTED("video_call_accepted", "视频呼叫接受"),
    VIDEO_CALL_REJECTED("video_call_rejected", "视频呼叫拒绝"),
    VIDEO_CALL_TIMEOUT("video_call_timeout", "视频呼叫超时未接听"),
    VIDEO_CALL_CANCELED("video_call_canceled", "视频呼叫已取消"),
    VIDEO_CALL_OFFER("video_call_offer", "视频呼叫Offer信令"),
    VIDEO_CALL_ANSWER("video_call_answer", "视频呼叫Answer信令"),
    VIDEO_CALL_ICE_CANDIDATE("video_call_ice_candidate", "视频呼叫ICE候选"),
    VIDEO_CALL_ENDED("video_call_ended", "视频通话结束"),

    ;

    final String type;

    final String desc;

    public String getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }
}
