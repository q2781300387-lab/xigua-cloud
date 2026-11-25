<template>
  <!-- 右侧聊天窗口 -->
  <div class="chat-window">
    <!-- 搜索历史聊天记录（覆盖聊天窗口；固定定位避免玻璃主题的合成层把面板压在消息下面） -->
    <ChatHistorySearch
        v-if="showHistorySearch"
        :style="historyPanelStyle"
        :currentUser="currentUser"
        :currentChatWindow="currentChatWindow"
        @close="closeHistorySearch"
        @locate-message="locateMessage"
    />

    <!-- 聊天标题栏 -->
    <div class="chat-header">
      <!-- 窄屏返回会话列表：桌面端隐藏 -->
      <button class="mobile-back" type="button" aria-label="返回会话列表" @click="$emit('back')">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M15 5l-7 7 7 7"/>
        </svg>
      </button>
      <div
          class="chat-title-wrap"
          :class="{ manageable: currentChatWindow && currentChatWindow.chatType === 2 }"
          :role="currentChatWindow && currentChatWindow.chatType === 2 ? 'button' : null"
          :tabindex="currentChatWindow && currentChatWindow.chatType === 2 ? 0 : -1"
          :aria-label="currentChatWindow && currentChatWindow.chatType === 2 ? '打开群管理' : null"
          @click="openGroupManage"
          @keydown.enter.prevent="openGroupManage"
          @keydown.space.prevent="openGroupManage"
      >
        <div class="chat-title-row">
          <div class="chat-title">{{ displayChatName(currentChatWindow) }}</div>
          <svg v-if="currentChatWindow && currentChatWindow.chatType === 2" class="title-chevron" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M9 5l7 7-7 7"/>
          </svg>
          <span v-if="currentChatWindow && currentChatWindow.chatType === 2 && groupMuteAll === 1" class="mute-chip">全员禁言</span>
        </div>
        <div v-if="typingText" class="typing-line">
          <span class="typing-dots"><i></i><i></i><i></i></span><span class="typing-line-text">{{ typingText }}</span>
        </div>
      </div>
      <div class="chat-actions">
        <button v-if="currentChatWindow.chatType === 1 || currentChatWindow.chatType === 2" type="button" class="search-history-btn" title="搜索聊天记录" aria-label="搜索聊天记录" @click="openHistorySearch">
          <svg class="search-icon" viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg">
            <path fill="currentColor" d="M446.112323 177.545051c137.567677 0.219798 252.612525 104.59798 266.162424 241.493333 13.562828 136.895354-78.778182 261.818182-213.617777 289.008485-134.852525 27.203232-268.386263-52.156768-308.945455-183.608889s25.018182-272.252121 151.738182-325.779394A267.235556 267.235556 0 0 1 446.112323 177.545051m0-62.060607c-182.794343 0-330.989899 148.195556-330.989899 330.989899s148.195556 330.989899 330.989899 330.989899 330.989899-148.195556 330.989899-330.989899-148.195556-330.989899-330.989899-330.989899z m431.321212 793.341415a30.849293 30.849293 0 0 1-21.94101-9.102223l-157.220202-157.220202c-11.752727-12.179394-11.584646-31.534545 0.37495-43.50707 11.972525-11.972525 31.327677-12.140606 43.494141-0.37495l157.220202 157.220202a31.036768 31.036768 0 0 1 6.723232 33.810101 31.004444 31.004444 0 0 1-28.651313 19.174142z m0 0"></path>
          </svg>
        </button>
        <button type="button" class="close-chat" aria-label="关闭聊天窗口" @click="closeChatWindow">×</button>
      </div>
    </div>

    <!-- 群公告横幅：贴在标题栏下方，两行折叠，点击展开；不挤压消息区滚动 -->
    <div
        v-if="currentChatWindow && currentChatWindow.chatType === 2 && groupAnnouncement"
        class="group-announce-banner"
        :class="{ expanded: groupAnnouncementExpanded, clickable: groupAnnouncementLong }"
        :role="groupAnnouncementLong ? 'button' : null"
        :tabindex="groupAnnouncementLong ? 0 : -1"
        :aria-expanded="groupAnnouncementLong ? String(groupAnnouncementExpanded) : null"
        @click="toggleGroupAnnouncement"
        @keydown.enter.prevent="toggleGroupAnnouncement"
        @keydown.space.prevent="toggleGroupAnnouncement"
    >
      <span class="gab-label">群公告</span>
      <span class="gab-text">{{ groupAnnouncement }}</span>
      <span v-if="groupAnnouncementLong" class="gab-toggle">{{ groupAnnouncementExpanded ? '收起' : '展开' }}</span>
    </div>

    <!-- 聊天内容区域 -->
    <div class="messages" ref="messagesContainer" @scroll="handleMessagesScroll">
      <!-- 已不在群聊：不是网络故障，不能给「重试」误导用户 -->
      <div v-if="historyAccessDenied" class="chat-access-blocked">
        <div class="chat-access-icon">
          <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="9" cy="8" r="3.4"></circle>
            <path d="M3.4 20c.8-4 3.2-6 5.6-6 1.3 0 2.5.5 3.5 1.4"></path>
            <path d="M15 15l6 6M21 15l-6 6"></path>
          </svg>
        </div>
        <div class="chat-access-title">你已不在该群聊</div>
        <div class="chat-access-desc">群成员才能查看聊天记录。这个会话已经没有可访问的消息了。</div>
        <button type="button" class="chat-access-btn" @click="removeUnavailableChat">从会话列表移除</button>
      </div>
      <!-- 加载更多提示 -->
      <div v-if="loadingHistory" class="loading-history">
        加载历史消息中...
      </div>
      <!-- 失败态：给出可见的重试入口，避免停在「加载中」没有任何出口 -->
      <div v-else-if="historyLoadFailed" class="loading-history loading-history--failed">
        <span>历史消息加载失败</span>
        <span class="history-retry" @click="getHistoryMes">重试</span>
      </div>

      <!-- 机器人会话空态：历史消息未落库，指到真正的 AI 对话框 -->
      <div
          v-if="currentChatWindow && currentChatWindow.chatType === 3 && !loadingHistory && chatMessages.length === 0"
          class="bot-empty"
      >
        <div class="bot-empty-icon">
          <svg viewBox="0 0 24 24" width="34" height="34" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="6" width="18" height="13" rx="4"/>
            <circle cx="9" cy="12" r="1.4" fill="currentColor"/>
            <circle cx="15" cy="12" r="1.4" fill="currentColor"/>
            <path d="M12 3v3"/>
          </svg>
        </div>
        <div class="bot-empty-title">这里还没有对话记录</div>
        <div class="bot-empty-desc">
          机器人对话在 AI 聊天页进行，那里能看到完整历史，也支持切换模型继续聊。
        </div>
        <button class="bot-empty-btn" @click="goBotAiChat">打开 AI 聊天</button>
      </div>

      <!-- 历史消息 -->
      <div
          v-for="(message, index) in chatMessages"
          :key="`${message.senderId}_${message.createTime}`"
          class="message"
          :data-message="JSON.stringify(message)"
          :data-message-id="message.chatMessageId"
          @contextmenu.prevent="openSmartReplyMenu(message, $event)"
          @click="onMessageRowClick(message)"
          :class="{
              'sender': message.senderId === currentUser.id,
              'receiver': message.senderId !== currentUser.id,
              'system-message': isSystemMessage(message),
              'bubble-pop': index === chatMessages.length - 1 && message.sendStatus !== 'sending',
              'select-mode': selectionMode && !isSystemMessage(message),
              'select-on': selectionMode && isMessageSelected(message)
            }"
      >

        <!-- 多选态勾选框 -->
        <div
            v-if="selectionMode && !isSystemMessage(message)"
            class="msg-select-box"
            :class="{ checked: isMessageSelected(message) }"
        >✓</div>

        <!-- AI 智能回复悬浮按钮（hover 显示） -->
        <button
            v-if="isTextMessage(message)"
            class="ai-reply-float"
            title="AI 智能回复"
            @click.stop="openSmartReplyMenu(message, $event)"
        >
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 3l1.6 4.6L18 9l-4.4 1.4L12 15l-1.6-4.6L6 9l4.4-1.4z"/>
            <path d="M19 14l.8 2.2L22 17l-2.2.8L19 20l-.8-2.2L16 17l2.2-.8z"/>
          </svg>
          AI
        </button>

        <!-- 接收方头像（系统消息不显示头像） -->
        <div v-if="message.senderId !== currentUser.id && !isSystemMessage(message)" class="avatar-container">
          <div class="message-avatar">
            <img v-if="message.chatType == 1 && currentChatWindow.avatar" :src="currentChatWindow.avatar" :alt="currentChatWindow.chatName">
            <img v-if="message.chatType == 3 && currentChatWindow.avatar" :src="currentChatWindow.avatar" :alt="currentChatWindow.chatName">
            <img v-else-if="message.chatType == 2" :src="message.sender.avatar" :alt="message.chatName">
            <span v-else class="avatar-text">{{ currentChatWindow && currentChatWindow.chatName ? currentChatWindow.chatName[0] : '' }}</span>
          </div>
        </div>

        <!-- 消息内容 -->
        <div class="message-content">
          <div
              v-if="!isSystemMessage(message)"
              class="message-header"
              :class="{
                    'sender': message.senderId === currentUser.id,
                    'receiver': message.senderId !== currentUser.id
                  }">
            <span class="message-sender-username" v-if="message.chatType == 2 && message.senderId !== currentUser.id">
            {{ senderLabel(message) }}
          </span>
            <span v-if="isMentionMe(message)" class="mention-me-badge">@我</span>
            <span class="message-time">{{ message.createTime }}</span>
            <span v-if="Number(message.edited) === 1" class="message-edited-tag" :title="message.editTime ? ('编辑于 ' + message.editTime) : '已编辑'">已编辑</span>
          </div>
          <div class="content" :class="{ 'is-card': message.contentType === 9 || message.messageType === 9 || message.contentType === 7 || message.messageType === 7 || message.contentType === 8 || message.messageType === 8 || isLegacyCallRecord(message) }">
            <!-- 系统消息（成员进群 / 移出 / 角色变更）：居中灰条，不进气泡 -->
            <div v-if="isSystemMessage(message)" class="system-message-body">
              <span class="system-message-text">{{ message.message }}</span>
            </div>
            <template v-else>
            <!-- 撤回消息提示 -->
            <div v-if="message.withdrawn === 1" class="withdraw-tip">
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 12a9 9 0 1 0 3-6.7L3 8"/>
                <path d="M3 3v5h5"/>
              </svg>
              <span>{{ message.senderId === currentUser.id ? '你' : senderLabel(message) }}撤回了一条消息</span>
            </div>
            <!-- 正常消息内容 -->
            <template v-else>
              <!-- 引用回复块（点击可跳回被引用的原消息） -->
              <div
                  v-if="message.replyToId && message.replyContent"
                  class="reply-quote"
                  title="点击跳转到原消息"
                  @click.stop="scrollToQuotedMessage(message.replyToId)"
              >
                <div class="reply-quote-line"></div>
                <div class="reply-quote-text">{{ message.replyContent }}</div>
              </div>
              <!-- 机器人加载中状态 -->
              <div v-if="message.isLoading && message.chatType === 3" class="bot-loading">
                <span class="loading-spinner"></span>
                <span class="loading-text">思考中...</span>
              </div>
              <!-- 正常消息内容 -->
              <template v-else>
                <!-- 文本消息（默认） -->
                <span v-if="(!message.contentType && !message.messageType) || message.contentType === 1 || message.messageType === 1" v-html="formatMessageText(message)"></span>
                <!-- 图片消息 -->
                <img v-else-if="message.contentType === 2 || message.messageType === 2" :src="message.message" :alt="message.fileName" class="message-image" @click="openImageViewer(message, $event)" />
                <!-- 语音消息 -->
                <div v-else-if="message.contentType === 3 || message.messageType === 3" class="message-audio">
                  <template v-if="parseVoiceMessage(message)">
                    <button class="voice-play-btn" :class="{ playing: isVoicePlaying(message) }" @click="toggleVoicePlay(message)">
                      <svg v-if="!isVoicePlaying(message)" viewBox="0 0 24 24" width="17" height="17" fill="currentColor">
                        <path d="M8 5.5v13l11-6.5z" />
                      </svg>
                      <svg v-else viewBox="0 0 24 24" width="17" height="17" fill="currentColor">
                        <rect x="6.5" y="5" width="3.6" height="14" rx="1.2" />
                        <rect x="13.9" y="5" width="3.6" height="14" rx="1.2" />
                      </svg>
                    </button>
                    <div class="voice-body">
                      <div class="voice-wave" :class="{ playing: isVoicePlaying(message) }">
                        <span v-for="n in 22" :key="n" :style="{ animationDelay: (n * 0.09) + 's' }"></span>
                      </div>
                      <div class="voice-meta">
                        <span class="voice-duration">{{ parseVoiceMessage(message).duration }}″</span>
                        <span v-if="parseVoiceMessage(message).transcript" class="voice-transcript">{{ parseVoiceMessage(message).transcript }}</span>
                      </div>
                    </div>
                  </template>
                  <audio v-else :src="message.message" controls></audio>
                </div>
                <!-- 视频消息 -->
                <div v-else-if="message.contentType === 4 || message.messageType === 4" class="message-video">
                  <video :src="message.message" controls></video>
                  <span class="file-name">{{ fileDisplayName(message) }}</span>
                </div>
                <!-- 文件消息 -->
                <div v-else-if="message.contentType === 5 || message.messageType === 5" class="message-file">
                  <a :href="message.message" :download="fileDisplayName(message)" class="file-link">
                    <svg class="file-icon" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                      <path d="M14 2v6h6" />
                      <path d="M8 13h8" />
                      <path d="M8 17h5" />
                    </svg>
                    <span class="file-name">{{ fileDisplayName(message) }}</span>
                  </a>
                </div>
                <!-- 视频通话记录 -->
                <div v-else-if="message.contentType === 9 || message.messageType === 9 || isLegacyCallRecord(message)" class="call-card">
                  <div class="call-card-icon">
                    <svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="2.5" y="6" width="13" height="12" rx="3" />
                      <path d="M15.5 10.2l5-2.7v9l-5-2.7z" />
                    </svg>
                  </div>
                  <div class="call-card-main">
                    <div class="call-card-title">视频通话</div>
                    <div class="call-card-sub">{{ callRecordText(message) }}</div>
                  </div>
                  <span v-if="callRecordDuration(message)" class="call-card-duration">{{ callRecordDuration(message) }}</span>
                </div>
                <!-- 合并转发的聊天记录 -->
                <div v-else-if="message.contentType === 7 || message.messageType === 7" class="merge-card" @click.stop="openMergeViewer(message)">
                  <div class="merge-head">
                    <span class="merge-icon">≡</span>
                    <span class="merge-title">聊天记录</span>
                  </div>
                  <div class="merge-line" v-for="(it, i) in mergePreview(message)" :key="i">
                    <span class="merge-who">{{ it.s }}</span>
                    <span class="merge-text">{{ it.c }}</span>
                  </div>
                  <div class="merge-foot">
                    <span>共 {{ mergeCount(message) }} 条</span>
                    <span class="merge-open">查看 ›</span>
                  </div>
                </div>
                <!-- 群投票卡片 -->
                <div v-else-if="message.contentType === 8 || message.messageType === 8" class="vote-card" @click.stop="ensureVote(votePayload(message).voteId)">
                  <div class="vote-head">
                    <span class="vote-badge">投票</span>
                    <span class="vote-title">{{ votePayload(message).title }}</span>
                    <span
                        v-if="voteDetails[votePayload(message).voteId] && voteDetails[votePayload(message).voteId].status === 1"
                        class="vote-status"
                    >已结束</span>
                  </div>
                  <div v-if="voteLoading[votePayload(message).voteId]" class="vote-loading">加载中…</div>
                  <div v-else-if="!voteDetails[votePayload(message).voteId]" class="vote-loading">点击查看结果并投票 ›</div>
                  <div v-else-if="voteDetails[votePayload(message).voteId]" class="vote-options">
                    <div
                        v-for="opt in voteDetails[votePayload(message).voteId].options"
                        :key="opt.index"
                        class="vote-option"
                        :class="{
                          mine: isMyVote(voteDetails[votePayload(message).voteId], opt.index),
                          disabled: voteDetails[votePayload(message).voteId].status === 1,
                          pending: !!votePending[votePayload(message).voteId]
                        }"
                        @click.stop="doVote(votePayload(message).voteId, opt.index)"
                    >
                      <div class="vote-bar" :style="{ width: opt.ratio + '%' }"></div>
                      <div class="vote-line">
                        <span class="vote-text">
                          {{ opt.text }}
                          <span v-if="isMyVote(voteDetails[votePayload(message).voteId], opt.index)" class="vote-mine">已投</span>
                        </span>
                        <span class="vote-count">{{ opt.count }} 票 · {{ opt.ratio }}%</span>
                      </div>
                    </div>
                  </div>
                  <div class="vote-foot" v-if="voteDetails[votePayload(message).voteId]">
                    <span>{{ voteDetails[votePayload(message).voteId].total }} 人参与{{ voteDetails[votePayload(message).voteId].multi ? ' · 多选' : '' }}</span>
                    <span class="vote-actions">
                      <span v-if="voteDetails[votePayload(message).voteId].status === 0" class="vote-tip">
                        {{ myVoteCount(voteDetails[votePayload(message).voteId]) ? '' : '点击选项投票' }}
                      </span>
                      <span v-else class="vote-tip ended">投票已结束</span>
                      <button
                          v-if="voteDetails[votePayload(message).voteId].status === 0 && myVoteCount(voteDetails[votePayload(message).voteId])"
                          class="vote-cancel"
                          @click.stop="cancelVote(votePayload(message).voteId)"
                      >{{ voteDetails[votePayload(message).voteId].multi ? '清空选择' : '取消投票' }}</button>
                      <button
                          v-if="voteDetails[votePayload(message).voteId].status === 0 && voteDetails[votePayload(message).voteId].creatorId == currentUser.id"
                          class="vote-close"
                          @click.stop="confirmCloseVote(votePayload(message).voteId)"
                      >{{ voteConfirmId === votePayload(message).voteId ? '确认结束？' : '结束投票' }}</button>
                    </span>
                  </div>
                </div>
              </template>
            </template>
            </template>
              <!-- 消息翻译结果：可复制、可收起、失败可重试 -->
              <div
                  v-if="translationOf(message)"
                  class="msg-translation"
                  :class="{ 'is-error': translationOf(message).error }"
              >
                <div class="mt-head">
                  <span class="mt-lang">{{ translationOf(message).label }}</span>
                  <span class="mt-tools">
                    <button
                        v-if="translationOf(message).error"
                        class="mt-btn"
                        @click.stop="retryTranslate(message)"
                    >重试</button>
                    <button
                        v-else
                        class="mt-btn"
                        title="复制译文"
                        @click.stop="copyTranslation(translationOf(message).text)"
                    >复制</button>
                    <button class="mt-btn mt-close" title="收起译文" @click.stop="hideTranslation(message)">×</button>
                  </span>
                </div>
                <span class="mt-text">{{ translationOf(message).text }}</span>
              </div>

              <!-- 表情回应：气泡下方聚合展示，点已有表情可取消/切换 -->
              <div
                  v-if="isReactionable(message) && message.reactions && message.reactions.length"
                  class="reaction-row"
                  :class="{ sender: message.senderId === currentUser.id }"
              >
                <button
                    v-for="item in message.reactions"
                    :key="item.emoji"
                    class="reaction-pill"
                    :class="{ mine: item.mine, pending: message.reactionPending }"
                    :title="item.mine ? '再点一次取消回应' : '回应 ' + item.emoji"
                    @click.stop="toggleReaction(message, item.emoji)"
                >
                  <span class="reaction-emoji">{{ item.emoji }}</span>
                  <span class="reaction-count">{{ item.count }}</span>
                </button>
              </div>

              <!-- 悬浮回应入口：hover 气泡右上角出现，点开 8 个表情 -->
              <button
                  v-if="isReactionable(message)"
                  class="reaction-trigger"
                  title="表情回应"
                  @click.stop="openReactionPicker(message, $event)"
              >
                <span>☺</span>
              </button>
          </div>
          <div class="message-bottom" v-if="message.senderId === currentUser.id && !isSystemMessage(message)">
            <!-- 发送中状态 -->
            <span class="send-status" v-if="message.sendStatus === 'sending'">
              <span class="loading-spinner"></span>
              <span class="sending-text">发送中...</span>
            </span>

            <!-- 发送失败 -->
            <span class="send-status" v-else-if="message.sendStatus === 'failed'">
              <span class="failed-icon" @click="resendMessage(message)">❗</span>
              <span class="failed-text">发送失败，点击重试</span>
            </span>

            <!-- 单聊：发送成功（已读/未读） -->
            <span v-else-if="message.chatType == 1 && message.senderId === currentUser.id" class="read-status" :class="{ 'read-tick': message.isRead }">
              <span class="tick">{{ message.isRead ? '✓✓' : '✓' }}</span>
            </span>

            <!-- 群聊：自己发的消息显示实时已读人数，点开看名单 -->
            <span
              v-else-if="message.chatType == 2 && message.senderId === currentUser.id && groupReadNames(message.chatMessageId).length > 0"
              class="read-status group-read-status"
              @click.stop="toggleGroupReaders(message.chatMessageId)"
            >
              {{ groupReadNames(message.chatMessageId).length }} 人已读
              <div v-if="activeGroupReadId === message.chatMessageId" class="group-read-pop" @click.stop>
                <div class="grp-title">
                  已读 {{ groupReadNames(message.chatMessageId).length }}
                  <span v-if="groupReadTotal(message.chatMessageId)"> / {{ groupReadTotal(message.chatMessageId) }}</span>
                </div>
                <div class="grp-row" v-for="(name, idx) in groupReadNames(message.chatMessageId)" :key="idx">{{ name }}</div>
              </div>
            </span>
          </div>
        </div>

        <!-- 发送方头像 -->
        <div v-if="message.senderId === currentUser.id && !isSystemMessage(message)" class="avatar-container">
          <div class="message-avatar">
            <img v-if="currentUser.avatar" :src="currentUser.avatar" :alt="currentUser.username">
            <span v-else class="avatar-text">{{ currentUser.username.charAt(0) }}</span>
          </div>
        </div>
      </div>

      <!-- 系统消息 -->
      <div
          v-for="systemMsg in systemMessages"
          :key="`${systemMsg.senderId}_${systemMsg.createTime}`"
          class="system-message"
      >
        <div class="system-message-content">
          {{ systemMsg.message }}
        </div>
      </div>
    </div>

    <!-- 图片预览区域 -->
      <div v-if="pastedImages.length > 0" class="image-preview-container">
        <div 
          v-for="(image, index) in pastedImages" 
          :key="image.id"
          class="image-preview-item"
        >
          <img 
            :src="image.url" 
            :alt="image.fileName" 
            class="preview-image" 
            @click="openPastedImageViewer(index)"
          />
          <span 
            class="image-remove-btn"
            @click="removePastedImage(index)"
          >×</span>
        </div>
      </div>

    <!-- 待发送的定时消息列表 -->
    <div v-if="!historyAccessDenied && timedMessages.length > 0" class="timed-message-list">
      <div v-for="tm in timedMessages" :key="tm.id" class="timed-message-item">
        <svg class="timed-icon" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="9.5" />
          <path d="M12 6.5V12l3.5 2" />
        </svg>
        <span class="timed-text">{{ tm.content }}</span>
        <span class="timed-time">{{ formatTimedTime(tm.sendAt) }}</span>
        <button class="timed-cancel" @click="cancelTimedMessage(tm.id)">取消</button>
      </div>
    </div>

    <div v-if="!historyAccessDenied" class="input-container">
      <div class="input-tools">
        <div class="left-tools">
          <!-- 模板部分 -->
          <div class="emoji-trigger-container">
            <i class="icon-tool emoji-trigger" @click.stop="toggleEmojiPicker" title="表情">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="9.5" />
                <path d="M8.2 14.5c.9 1.6 2.2 2.4 3.8 2.4s2.9-.8 3.8-2.4" />
                <circle cx="9" cy="10" r="1" fill="currentColor" stroke="none" />
                <circle cx="15" cy="10" r="1" fill="currentColor" stroke="none" />
              </svg>
            </i>

            <div v-if="showEmojiPicker" class="emoji-picker-floating">
              <emoji-picker
                  :native="true"
                  :disable-skin-tones="true"
                  :display-recent="true"
                  :hide-search="true"
                  :show-categories="true"
                  @select="addEmoji"
              />
            </div>
          </div>
        </div>
        <div class="right-tools">
          <i v-if="isGroupChat()" class="icon-tool" @click="openVoteDialog" title="发起群投票">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="4" width="18" height="7.5" rx="2" />
              <rect x="3" y="14.5" width="18" height="5.5" rx="2" />
              <path d="M6.5 7.7h6M6.5 17.2h9" />
            </svg>
          </i>
          <i class="icon-tool" @click="openSummaryDialog" title="AI 智能摘要">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 3l1.9 4.6L18.5 9.5l-4.6 1.9L12 16l-1.9-4.6L5.5 9.5l4.6-1.9z" />
              <path d="M18.5 15.5l.8 2 2 .8-2 .8-.8 2-.8-2-2-.8 2-.8z" />
            </svg>
          </i>
          <i class="icon-tool" @click="openTimedDialog" title="定时发送">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="9.5" />
              <path d="M12 6.5V12l3.5 2" />
            </svg>
          </i>
          <i class="icon-tool" @click="$refs.imageInput.click()" title="发送图片">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3.5" width="18" height="17" rx="3" />
              <circle cx="8.5" cy="9" r="1.6" />
              <path d="M4 18l5-5 3.6 3.6 2.8-2.8L21 19" />
            </svg>
            <input type="file" ref="imageInput" accept="image/*" multiple style="display: none;" @change="handleImageSelect" />
          </i>
          <i class="icon-tool" @click="openFileUpload" title="发送文件">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <path d="M14 2v6h6" />
              <path d="M8 13h8" />
              <path d="M8 17h5" />
            </svg>
            <input type="file" ref="fileInput" style="display: none;" @change="handleFileSelect" />
          </i>
          <i v-if="currentChatWindow && currentChatWindow.chatType === 2" class="icon-tool" @click="openGroupCall(false)" title="群语音通话">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="9" y="2.5" width="6" height="12" rx="3" />
              <path d="M5.5 11.5a6.5 6.5 0 0 0 13 0" />
              <path d="M12 18v3.5" />
            </svg>
          </i>
          <i v-if="currentChatWindow && currentChatWindow.chatType === 2" class="icon-tool" @click="openGroupCall(true)" title="群视频通话" role="button" tabindex="0" aria-label="群视频通话" @keydown.enter.prevent="openGroupCall(true)" @keydown.space.prevent="openGroupCall(true)">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="2" y="6" width="13" height="12" rx="2.5" />
              <path d="M22 8.5l-6 3.5 6 3.5z" />
            </svg>
          </i>
          <i v-if="currentChatWindow && currentChatWindow.chatType !== 2" class="icon-tool" @click="openVoiceChat" title="语音通话">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="9" y="2.5" width="6" height="12" rx="3" />
              <path d="M5.5 11.5a6.5 6.5 0 0 0 13 0" />
              <path d="M12 18v3.5" />
            </svg>
          </i>
          <i v-if="currentChatWindow && currentChatWindow.chatType !== 2" class="icon-tool" @click="openVideoChat" title="视频通话" role="button" tabindex="0" aria-label="视频通话" @keydown.enter.prevent="openVideoChat" @keydown.space.prevent="openVideoChat">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="2" y="6" width="13" height="12" rx="2.5" />
              <path d="M22 8.5l-6 3.5 6 3.5z" />
            </svg>
          </i>
        </div>
      </div>

      <!-- 输入和发送行 -->
      <div class="input-row">
        <!-- 引用回复预览条 -->
        <div v-if="replyTarget" class="reply-preview-bar">
          <div class="reply-preview-info">
            <span class="reply-preview-label">回复：</span>
            <span class="reply-preview-text">{{ replyTarget.message }}</span>
          </div>
          <button class="reply-preview-close" @click="cancelReply">×</button>
        </div>
        <!-- @提及面板：Teleport 到 body，避免被玻璃/transform 合成层改变 fixed 定位基准 -->
        <Teleport to="body">
        <div
            v-if="atPanelShow && currentChatWindow.chatType === 2"
            ref="atPanel"
            class="at-member-panel"
            :style="atPanelStyle"
            role="dialog"
            aria-label="选择要@的成员"
        >
          <div class="at-member-title">选择要@的成员</div>
          <div class="at-member-search-wrap">
            <input
                v-model="atSearch"
                class="at-member-search"
                type="text"
                placeholder="搜索成员昵称"
                aria-label="搜索成员"
                @keydown.stop
            />
          </div>
          <div v-if="atMemberLoading" class="at-member-loading">
            <span class="loading-spinner"></span>
            <span>加载成员中...</span>
          </div>
          <div v-else class="at-member-list">
            <div
                v-for="member in filteredAtMembers"
                :key="member.id"
                class="at-member-item"
                @click="selectAtMember(member)"
            >
              <img v-if="member.avatar" :src="member.avatar" class="at-member-avatar" alt="">
              <span v-else class="at-member-avatar avatar-text">{{ (member.username || '?')[0] }}</span>
              <span class="at-member-name">
                <template v-for="(seg, si) in splitAtName(member.username)">
                  <mark v-if="seg.hit" :key="'hit' + si" class="at-member-hit">{{ seg.text }}</mark>
                  <span v-else :key="'txt' + si">{{ seg.text }}</span>
                </template>
              </span>
            </div>
            <div v-if="filteredAtMembers.length === 0" class="at-member-empty">
              {{ atMembers.length === 0 ? '暂无可@的成员' : '没有匹配的成员' }}
            </div>
          </div>
        </div>
        </Teleport>
        <div class="input-toolbar">
          <button
              v-if="currentChatWindow.chatType === 2"
              class="at-btn"
              title="@ 提及成员"
              aria-label="@ 提及成员"
              :aria-expanded="atPanelShow ? 'true' : 'false'"
              @click="toggleAtPanel"
          >
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="4"/>
              <path d="M16 8v5a3 3 0 0 0 6 0v-1a10 10 0 1 0-4 8"/>
            </svg>
          </button>
          <div v-if="atUsers.length > 0" class="at-tags">
            <span v-for="at in atUsers" :key="at.id" class="at-tag">
              @{{ at.username }}
              <button class="at-tag-remove" title="取消提及" @click.stop="removeAtUser(at)">×</button>
            </span>
          </div>
        </div>
        <AIInput 
          ref="chatInput"
          v-model="newMessage"
          :showEmptyWarn="showEmptyWarn"
          :chatType="currentChatWindow.chatType"
          :isBotTyping="isBotTyping"
          :hasImages="pastedImages.length > 0"
          :muted="groupMutedForMe"
          :voiceMode="voiceMode"
          :voiceRecording="isRecordingVoice"
          :voiceDuration="voiceDuration"
          @atTrigger="handleAtTrigger"
          @typing="notifyTyping"
          @send="sendMessage"
          @keyDown="handleKeyDown"
          @stop="stopBotResponse"
          @pasteImage="handlePasteImage"
          @backspace="handleBackspace"
          @toggleVoice="toggleVoiceMode"
          @voiceStart="startVoiceRecord"
          @voiceStop="stopVoiceRecord(false)"
          @voiceCancel="stopVoiceRecord(true)"
        />
      </div>
    </div>

    <!-- 图片查看器 -->
    <div v-if="showImageViewer" class="image-viewer-overlay" @click="closeImageViewer">
      <div class="image-viewer-container" @click.stop>
        <div class="image-viewer-header">
          <span></span>
          <button class="close-btn" @click="closeImageViewer">×</button>
        </div>
        <div class="image-viewer-content">
          <div class="image-container">
            <img 
              :src="currentImage?.url" 
              :alt="currentImage?.fileName" 
              class="viewer-image"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 粘贴图片查看器（支持左右切换） -->
    <div v-if="showPastedImageViewer" class="image-viewer-overlay" @click="closePastedImageViewer">
      <div class="image-viewer-container" @click.stop>
        <div class="image-viewer-header">
          <span class="image-counter">{{ pastedImageIndex + 1 }} / {{ pastedImages.length }}</span>
          <button class="close-btn" @click="closePastedImageViewer">×</button>
        </div>
        <div class="image-viewer-content">
          <button 
            class="nav-btn prev-btn" 
            @click="prevPastedImage" 
            :disabled="pastedImageIndex === 0"
          >
            ←
          </button>
          <div class="image-container">
            <img 
              :src="pastedImages[pastedImageIndex]?.url" 
              :alt="pastedImages[pastedImageIndex]?.fileName" 
              class="viewer-image"
            />
          </div>
          <button 
            class="nav-btn next-btn" 
            @click="nextPastedImage" 
            :disabled="pastedImageIndex === pastedImages.length - 1"
          >
            →
          </button>
        </div>
      </div>
    </div>

    <!-- 定时发送对话框 -->
    <div v-if="showTimedDialog" class="timed-dialog-overlay" @click.self="showTimedDialog = false">
      <div class="timed-dialog">
        <div class="timed-dialog-title">定时发送</div>
        <textarea
            v-model="timedContent"
            class="timed-dialog-textarea"
            placeholder="输入要定时发送的内容（默认取当前输入框内容）"
            rows="3"
        ></textarea>
        <input v-model="timedSendAt" type="datetime-local" class="timed-dialog-input" />
        <div class="timed-dialog-quick">
          <span class="timed-quick-btn" @click="setQuickTimed(1)">1分钟后</span>
          <span class="timed-quick-btn" @click="setQuickTimed(30)">30分钟后</span>
          <span class="timed-quick-btn" @click="setQuickTimed(60)">1小时后</span>
        </div>
        <div class="timed-dialog-actions">
          <button class="timed-dialog-cancel" @click="showTimedDialog = false">取消</button>
          <button class="timed-dialog-ok" @click="createTimedMessage">确认定时</button>
        </div>
      </div>
    </div>

    <!-- 消息转发：选择目标会话 -->
    <div v-if="forwardDialog.show" class="timed-dialog-overlay" @click.self="closeForwardDialog">
      <div class="timed-dialog forward-dialog">
        <div class="timed-dialog-title">转发到</div>
        <div class="forward-preview">{{ forwardDialog.text }}</div>
        <div v-if="forwardDialog.loading" class="forward-hint">加载会话中…</div>
        <div v-else-if="!forwardDialog.list.length" class="forward-hint">暂无可用会话</div>
        <div v-else class="forward-list">
          <div v-for="(item, i) in forwardDialog.list" :key="i" class="forward-item" @click="doForward(item)">
            <img v-if="item.avatar" :src="item.avatar" class="forward-avatar" />
            <span v-else class="forward-avatar forward-avatar--text">{{ item.name.slice(0, 1) }}</span>
            <span class="forward-name">{{ item.name }}</span>
          </div>
        </div>
        <div class="forward-actions">
          <button class="forward-cancel" @click="closeForwardDialog">取消</button>
        </div>
      </div>
    </div>

    <!-- 多选操作条 -->
    <div v-if="selectionMode" class="merge-bar">
      <span class="merge-bar-count">已选 {{ selectedMessages.length }} 条</span>
      <button class="merge-bar-cancel" @click="exitSelectionMode">取消</button>
      <button class="merge-bar-ok" :disabled="!selectedMessages.length" @click="forwardSelected">合并转发</button>
    </div>

    <!-- 合并转发查看器 -->
    <div v-if="mergeViewer.show" class="timed-dialog-overlay" @click.self="closeMergeViewer">
      <div class="timed-dialog merge-viewer">
        <div class="timed-dialog-title">聊天记录<span class="mv-count">共 {{ mergeViewer.items.length }} 条</span></div>
        <div class="merge-viewer-list">
          <div v-for="(it, i) in mergeViewer.items" :key="i" class="mv-item">
            <div class="mv-head">
              <span class="mv-sender">{{ it.s }}</span>
              <span class="mv-time">{{ it.t }}</span>
            </div>
            <div class="mv-text">{{ it.c }}</div>
          </div>
        </div>
        <div class="timed-dialog-actions">
          <button class="timed-dialog-cancel" @click="closeMergeViewer">关闭</button>
        </div>
      </div>
    </div>

    <!-- 发起群投票 -->
    <div v-if="voteDialog.show" class="timed-dialog-overlay" @click.self="closeVoteDialog">
      <div class="timed-dialog vote-dialog">
        <div class="timed-dialog-title">发起群投票</div>
        <input class="vote-input" v-model="voteDialog.title" maxlength="60" placeholder="投票标题，例如：周末团建去哪儿" />
        <div class="vote-opt-row" v-for="(opt, i) in voteDialog.options" :key="i">
          <input class="vote-input" v-model="voteDialog.options[i]" maxlength="30" :placeholder="'选项 ' + (i + 1)" />
          <button class="vote-opt-del" v-if="voteDialog.options.length > 2" @click="removeVoteOption(i)">×</button>
        </div>
        <button class="vote-opt-add" v-if="voteDialog.options.length < 10" @click="addVoteOption">+ 添加选项</button>
        <label class="vote-multi"><input type="checkbox" v-model="voteDialog.multi" /> 允许多选</label>
        <div class="timed-dialog-actions">
          <button class="timed-dialog-cancel" @click="closeVoteDialog">取消</button>
          <button class="timed-dialog-ok" :disabled="voteDialog.sending" @click="submitCreateVote">{{ voteDialog.sending ? '发起中…' : '发起投票' }}</button>
        </div>
      </div>
    </div>

    <!-- AI 智能摘要弹窗 -->
    <div v-if="summaryDialog.show" class="timed-dialog-overlay" @click.self="closeSummaryDialog">
      <div class="timed-dialog summary-dialog">
        <div class="timed-dialog-title">AI 摘要与待办</div>
        <div v-if="summaryLoading" class="summary-loading">
          <div class="summary-ring"></div>
          <p>AI 正在阅读最近的群消息…</p>
        </div>
        <template v-else-if="summaryDialog.text">
          <div class="summary-result">{{ summaryDialog.text }}</div>
          <div v-if="summaryDialog.todos && summaryDialog.todos.length" class="summary-todos">
            <div class="summary-todos-title">待办事项</div>
            <div v-for="(todo, i) in summaryDialog.todos" :key="i" class="summary-todo">
              <span class="summary-todo-dot">{{ i + 1 }}</span>
              <span class="summary-todo-text">{{ todo.content }}</span>
              <span class="summary-todo-meta">{{ todo.owner }} · {{ todo.due }}</span>
            </div>
          </div>
          <div v-if="summaryDialog.messageCount" class="summary-foot">
            基于最近 {{ summaryDialog.messageCount }} 条消息<template v-if="summaryDialog.generatedAt"> · {{ summaryDialog.generatedAt }}</template>
          </div>
        </template>
        <div v-else class="summary-empty">当前会话还没有可摘要的消息</div>
        <div class="timed-dialog-actions">
          <button class="timed-dialog-cancel" @click="closeSummaryDialog">关闭</button>
          <button class="timed-dialog-ok" :disabled="summaryLoading" @click="openSummaryDialog">
            {{ summaryLoading ? '生成中…' : '重新生成' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 消息操作菜单（引用/撤回/复制/AI 智能回复） -->
    <!-- 挂到 body：祖先 .content-area 有 backdrop-filter，会让 fixed 相对它定位并被 overflow 裁掉 -->
    <Teleport to="body">
    <div
        v-if="smartReplyMenu.show"
        ref="actionMenu"
        class="smart-reply-menu message-action-menu"
        :style="{ left: smartReplyMenu.x + 'px', top: smartReplyMenu.y + 'px' }"
    >
      <div class="message-action-list">
        <!-- 表情回应：右键菜单顶部直接选，降低发现成本 -->
        <div v-if="isReactionable(smartReplyMenu.message)" class="reaction-strip">
          <button
              v-for="emoji in reactionEmojis"
              :key="emoji"
              class="reaction-strip-btn"
              :class="{ active: hasMyReaction(smartReplyMenu.message, emoji) }"
              :title="hasMyReaction(smartReplyMenu.message, emoji) ? '取消 ' + emoji : '回应 ' + emoji"
              @click.stop="toggleReaction(smartReplyMenu.message, emoji)"
          >{{ emoji }}</button>
        </div>
        <div
            v-if="canQuoteMessage(smartReplyMenu.message)"
            class="message-action-item"
            @click="quoteMessage(smartReplyMenu.message)"
        >
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
          <span>引用回复</span>
        </div>
        <div
            v-if="canEditMessage(smartReplyMenu.message)"
            class="message-action-item"
            @click="openEditDialog(smartReplyMenu.message)"
        >
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 20h9"/>
            <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/>
          </svg>
          <span>编辑</span>
        </div>
        <div
            v-if="canWithdrawMessage(smartReplyMenu.message)"
            class="message-action-item"
            @click="withdrawMessage(smartReplyMenu.message)"
        >
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 12a9 9 0 1 0 3-6.7L3 8"/>
            <path d="M3 3v5h5"/>
          </svg>
          <span>撤回</span>
        </div>
        <div
            v-if="canCopyMessage(smartReplyMenu.message)"
            class="message-action-item"
            @click="copyMessage(smartReplyMenu.message)"
        >
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="9" y="9" width="13" height="13" rx="2"/>
            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
          </svg>
          <span>复制</span>
        </div>
        <div
            v-if="canForwardMessage(smartReplyMenu.message)"
            class="message-action-item"
            @click="openForwardDialog(smartReplyMenu.message)"
        >
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 12v7a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-7"/>
            <polyline points="16 7 12 3 8 7"/>
            <line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
          <span>转发</span>
        </div>
        <!-- 收藏 / 取消收藏 -->
        <div
            v-if="isReactionable(smartReplyMenu.message)"
            class="message-action-item"
            @click.stop="toggleFavorite(smartReplyMenu.message)"
        >
          <svg viewBox="0 0 24 24" width="15" height="15" :fill="smartReplyMenu.message.favorited ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 3.4l2.6 5.3 5.8.8-4.2 4.1 1 5.8-5.2-2.7-5.2 2.7 1-5.8-4.2-4.1 5.8-.8z"/>
          </svg>
          <span>{{ smartReplyMenu.message.favorited ? '取消收藏' : '收藏' }}</span>
        </div>
        <!-- 消息翻译（仅文本消息） -->
        <div v-if="isTextMessage(smartReplyMenu.message)" class="message-action-item" @click.stop="toggleTranslateLangs">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 5h9" />
            <path d="M8.5 3v2" />
            <path d="M12 5c-.6 4-2.6 7-6.5 9.5" />
            <path d="M5.5 9.5c1.6 2.4 3.8 4 6 5" />
            <path d="M14 19l3.6-9 3.6 9" />
            <path d="M15.4 16h8.2" />
          </svg>
          <span>翻译</span>
          <span class="translate-langs" v-if="showTranslateLangs">
            <button @click.stop="doTranslate('zh')">中文</button>
            <button @click.stop="doTranslate('en')">EN</button>
            <button @click.stop="doTranslate('ja')">日本語</button>
          </span>
        </div>
        <!-- 多选消息 -->
        <div class="message-action-item" @click.stop="enterSelectionMode">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="4" width="18" height="6.5" rx="2" />
            <rect x="3" y="13.5" width="18" height="6.5" rx="2" />
            <path d="M6.5 7.2h.01M6.5 16.7h.01" />
          </svg>
          <span>多选</span>
        </div>
      </div>
      <div v-if="canSmartReplyMessage(smartReplyMenu.message)" class="smart-reply-panel">
        <div class="smart-reply-divider"></div>
        <div class="smart-reply-header">
          <span>AI 智能回复</span>
          <button class="smart-reply-close" @click="closeSmartReplyMenu">×</button>
        </div>
        <div v-if="smartReplyLoading" class="smart-reply-loading">
          <span class="loading-spinner"></span>
          <span>AI 正在思考回复...</span>
        </div>
        <div v-else-if="smartReplyOptions.length > 0" class="smart-reply-options">
          <div v-for="(opt, i) in smartReplyOptions" :key="i" class="smart-reply-option" @click="sendSmartReply(opt)">
            <span class="smart-reply-option-text">{{ opt }}</span>
          </div>
        </div>
        <div v-else class="smart-reply-error">
          <span>生成失败，请重试</span>
          <button class="smart-reply-retry" @click="fetchSmartReply">重试</button>
        </div>
      </div>
    </div>
    </Teleport>

    <!-- 编辑消息弹窗 -->
    <Teleport to="body">
      <div v-if="editDialog.show" class="edit-message-mask" @click.self="closeEditDialog">
        <div class="edit-message-dialog" role="dialog" aria-modal="true">
          <div class="edit-message-head">
            <span>编辑消息</span>
            <button type="button" aria-label="关闭" @click="closeEditDialog">×</button>
          </div>
          <textarea
              v-model="editDialog.draft"
              class="edit-message-input"
              maxlength="20000"
              rows="4"
              @keydown.ctrl.enter.prevent="submitEditMessage"
              @keydown.meta.enter.prevent="submitEditMessage"
          ></textarea>
          <div class="edit-message-tip">5 分钟内可编辑 · Ctrl / ⌘ + Enter 保存</div>
          <div class="edit-message-actions">
            <button type="button" class="edit-message-btn" @click="closeEditDialog">取消</button>
            <button type="button" class="edit-message-btn primary" :disabled="editDialog.saving" @click="submitEditMessage">
              {{ editDialog.saving ? '保存中…' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>


    <Teleport to="body">
      <div
          v-if="reactionPicker.show"
          class="reaction-picker"
          :style="{ left: reactionPicker.x + 'px', top: reactionPicker.y + 'px' }"
          @click.stop
      >
        <div class="reaction-picker-head">
          <span>回应</span>
          <button class="reaction-picker-close" @click="closeReactionPicker">×</button>
        </div>
        <div class="reaction-picker-grid">
          <button
              v-for="emoji in reactionEmojis"
              :key="emoji"
              class="reaction-picker-btn"
              :class="{ active: hasMyReaction(reactionPicker.message, emoji) }"
              :title="hasMyReaction(reactionPicker.message, emoji) ? '取消 ' + emoji : '回应 ' + emoji"
              @click.stop="toggleReaction(reactionPicker.message, emoji)"
          >{{ emoji }}</button>
        </div>
      </div>
    </Teleport>

    <GroupCallWindow
      v-if="groupCall.open"
      :group-id="groupCall.groupId"
      :group-name="groupCall.groupName"
      :video="groupCall.video"
      @close="closeGroupCall"
    />
  </div>
</template>

<script>
import {getLastMes, getHistoryMes, getMessageInteractions, toggleMessageReaction, toggleMessageFavorite, editMessage, saveConversationSetting, getConversationSetting} from "@/api/chatMessage.js";
import {smartReply, translateMessage} from "@/api/ai_session.js";
import {chatSummary} from "@/api/treehole.js";
import {getObject, remove, setObject} from '@/utils/localStorage.js'
import {closeChat, hideChat} from "@/api/chat.js";
import {getGroupMembers, getGroupDetail, createVote, getVoteDetail, submitVote, closeVote, cancelVote as cancelVoteApi, getGroupAiSummary} from "@/api/group.js";
import {uploadFile} from "@/api/file.js";
import {createTimedMessageApi, listTimedMessagesApi, cancelTimedMessageApi} from "@/api/timedMessage.js";
import { ElMessage } from "@/plugins/message";
import EmojiPicker from "vue3-emoji-picker";
import "vue3-emoji-picker/css";
import {getSocketInstance, getWebSocketClient} from "@/ws/WebSocketClient.js";
import { useVoiceWindowStore } from '@/store/voiceWindow.js'
import { useVideoWindowStore } from '@/store/videoWindow.js'
import AIInput from "@/components/AIInput.vue";
import ChatHistorySearch from "@/components/ChatHistorySearch.vue";
import GroupCallWindow from "@/views/GroupCallWindow.vue";

export default {
  components: {
    EmojiPicker,
    AIInput,
    ChatHistorySearch,
    GroupCallWindow,
  },
  emits: ['open-group-manage', 'back'],
  mounted() {
    // 确保初始加载时路由到chat
    if (this.$route.path === '/') {
      this.$router.replace('/chat')
    }

    // 搜索面板跟随聊天窗口尺寸，窗口 resize 时重算固定定位边界
    window.addEventListener('resize', this.syncHistoryPanelBounds);
    window.addEventListener('resize', this.syncAtPanelBounds);

    this.wsListener = (message) => {
      const data = JSON.parse(message);
      // 处理收到的消息
      this.handleReceiveMessage(data);
    };

    // 订阅websocket消息
    getWebSocketClient().subscribe(this.wsListener);

    // 加载服务端的定时消息列表（发送由服务端调度，页面关闭也生效）
    this.loadTimedMessages();
    this.timedCheckTimer = setInterval(() => this.loadTimedMessages(), 60000);

    // 群聊已读名单：点击气泡外的任意位置收起
    document.addEventListener('click', this.closeGroupReaders);
    document.addEventListener('click', this.closeReactionPicker);
    document.addEventListener('keydown', this.handleGlobalKeydown);

    // 视频通话窗关闭后重拉当前会话，让主叫写入的通话记录立刻落到聊天窗
    const videoStore = useVideoWindowStore();
    this.unwatchVideoWindow = this.$watch(
      () => videoStore.showVideoWindow,
      (open, wasOpen) => {
        if (wasOpen && !open) {
          this.refreshCurrentChatAfterCall();
        }
      }
    );
  },

  watch: {
    // 输入框内容变化：正文里的 @成员 被删掉后，同步移除对应的提及记录
    newMessage(val) {
      const text = String(val || '');
      this.scheduleDraftSave(text);
      // 手动 @ 弹出的面板：@ 已被删掉就收起
      if (this.atPanelShow && text.indexOf('@') === -1) {
        this.atPanelShow = false;
        this.atInsertPos = null;
      }
      if (!this.atUsers || !this.atUsers.length) return;
      const kept = this.atUsers.filter(u => text.indexOf('@' + (u.username || '')) !== -1);
      if (kept.length !== this.atUsers.length) {
        this.atUsers = kept;
      }
    },
    // 监听路由变化，当切换好友窗口时重新加载消息
    '$route.query.friendId': {
      async handler(newVal, oldVal) {
        if (newVal && newVal !== oldVal) {
          this.flushDraft(true);
          // 更新当前聊天窗口
          this.currentChatWindow = getObject('currentChatWindow') || {};
          if (this.currentChatWindow.chatId != null) {
            // 告诉服务端当前打开的好友聊天框
            this.notifySwitchChatWindow();
          }
          // 重新初始化聊天窗口
          await this.initChatWindow();
          this.restoreDraft();
        }
      },
      immediate: false
    }
  },

  beforeDestroy() {
    if (this.unwatchVideoWindow) {
      this.unwatchVideoWindow();
      this.unwatchVideoWindow = null;
    }
    if(this.currentChatWindow){
      this.clearHistoryMes();
    }

    if (this.timedCheckTimer) {
      clearInterval(this.timedCheckTimer);
      this.timedCheckTimer = null;
    }

    if (this.typingSweepTimer) {
      clearInterval(this.typingSweepTimer);
      this.typingSweepTimer = null;
    }

    document.removeEventListener('click', this.closeGroupReaders);
    document.removeEventListener('click', this.closeReactionPicker);
    document.removeEventListener('keydown', this.handleGlobalKeydown);
    window.removeEventListener('resize', this.syncHistoryPanelBounds);
    window.removeEventListener('resize', this.syncAtPanelBounds);

    if (this.wsListener) {
      getWebSocketClient().unsubscribe(this.wsListener);
      this.wsListener = null;
    }
  },
  data() {
    return {
      showEmojiPicker: false,
      messages: [],
      // 当前选中的聊天
      currentChatWindow: {},
      showEmptyWarn: false, // 空白消息提示
      // 聊天窗口状态
      showChatWindow: false,
      newMessage: '', // 输入框内容
      webSocket: null, // WebSocket实例,
      wsListener: null,
      chatMessages: [], // 聊天消息
      // 表情回应：固定白名单，和手机端 / 后端保持一致
      reactionEmojis: ['👍', '❤️', '😂', '😮', '😢', '👏', '🔥', '🎉'],
      reactionPicker: { show: false, x: 0, y: 0, message: null },
      pendingForwardAcks: [], // 转发消息等待服务端 ack，避免未落库也提示成功
      systemMessages: [], // 系统消息
      currentUser: {},
      groupCall: { open: false, groupId: '', groupName: '', video: false },
      historyPageNum: 1, // 当前页码（从1开始）
      historyPageSize: 20, // 每页条数
      historyRequestSeq: 0, // 历史消息请求序号：快速切会话时丢弃旧响应，避免串消息
      hasMoreHistory: false, // 是否还有更多历史消息
      loadingHistory: false, // 是否正在加载
      historyLoadFailed: false, // 历史消息是否加载失败（失败要给出可见的重试入口）
      historyAccessDenied: false, // 已不在群聊：与网络故障分开处理，避免给无效的「重试」
      isScrollingUp: false, // 是否正在向上滚动
      lastScrollTop: 0, // 记录滚动位置
      chatContainerRect: 0,
      chatContainerTop: 0,
      chatContainerBottom: 0,
      nextLoadMesIndex: 4, // 下一次加载消息的索引
      loadedPages: new Set(), // 已加载的页面
      visibleUnReadMessages: new Set(), // 可视区域未读消息
      lastScrollPosition: 0,
      lastScrollTime: 0,
      scrollSpeed: 0, // px/ms
      scrollUnReadTimer: null,
      fastScrollThreshold: 2, // px/ms 超过这个值算快速滑动
      slowSubmitDelay: 300, // 慢速滑动提交延迟
      fastSubmitDelay: 1000, // 快速滑动批量提交延迟
      showVoiceChat: false,
      isBotTyping: false, // 机器人是否正在输出
      isBotPaused: false, // 机器人是否已被暂停
      pastedImages: [], // 已上传成功的图片
      uploadingImages: [], // 正在上传的图片
      maxUploadTime: 30000, // 最大上传时间（30秒）
      maxImages: 5, // 最大图片数量限制
      // 图片查看器
      showImageViewer: false, // 是否显示图片查看器
      currentImage: null, // 当前查看的图片
      // 粘贴图片查看器（支持左右切换）
      showPastedImageViewer: false, // 是否显示粘贴图片查看器
      pastedImageIndex: 0, // 当前查看的粘贴图片索引
      showHistorySearch: false, // 是否显示搜索历史聊天记录面板
      historySearchReturnFocus: null, // 打开搜索前的焦点，关闭时归还
      historyPanelStyle: {}, // 固定定位面板的边界（跟随聊天窗口）
      // ===== 群公告横幅 / 全员禁言 =====
      groupAnnouncement: '', // 群公告正文
      groupAnnouncementTime: '',
      groupAnnouncementExpanded: false, // 横幅是否展开
      groupMuteAll: 0, // 全员禁言：0 关闭 / 1 开启
      groupRole: 0, // 我在当前群的角色：1 群主 / 2 管理员 / 3 成员
      groupDetailToken: '', // 切群竞态守卫：只接受当前群的返回
      // ===== 定时消息 =====
      showTimedDialog: false, // 是否显示定时发送对话框
      timedContent: '', // 定时发送内容
      timedSendAt: '', // 定时发送时间
      timedMessages: [], // 待发送的定时消息列表
      timedCheckTimer: null, // 定时消息轮询器
      summaryDialog: { show: false, text: '', todos: [], messageCount: 0, generatedAt: '' }, // AI 摘要与待办弹窗
      summaryLoading: false, // 摘要生成中
      // ===== 消息转发 =====
      forwardDialog: { show: false, text: '', list: [], loading: false }, // 转发选择会话弹层
      forwardMode: 'single', // single=单条转发 merge=合并转发
      forwardPayload: null, // 合并转发时携带的消息数组
      selectionMode: false, // 多选态
      selectedMessages: [], // 已选消息
      selectionLimit: 100, // 合并转发单次上限
      translations: {}, // 消息翻译结果，key=消息标识
      showTranslateLangs: false, // 翻译语言选择是否展开
      mergeViewer: { show: false, items: [] }, // 合并转发查看器
      voteDialog: { show: false, title: '', options: ['', ''], multi: false, sending: false },
      voteDetails: {}, // 投票详情缓存，key=voteId
      votePending: {}, // 投票请求锁：key=voteId，防止快速连点造成重复提交
      voteLoading: {},
      voteConfirmId: null, // 待二次确认结束的投票 id
      // ===== AI 智能回复 =====
      smartReplyMenu: { show: false, message: null, x: 0, y: 0 }, // 智能回复菜单状态
      editDialog: { show: false, saving: false, message: null, draft: '' }, // 编辑消息弹窗
      menuAnchor: null, // 右键触发时的光标坐标，用于菜单翻转定位
      smartReplyOptions: [], // 生成的回复建议
      smartReplyLoading: false, // 是否正在生成回复
      // ===== 消息撤回 / 引用回复 / @提及 =====
      replyTarget: null, // 引用回复目标 {chatMessageId, message}
      atUsers: [], // @提及的用户 [{id, username}]
      atPanelShow: false, // @成员面板是否显示
      atPanelStyle: {}, // 玻璃主题下用固定定位，避免被聊天内容盖住
      atSearch: '', // @成员面板搜索关键字
      atInsertPos: null, // 手动敲入 @ 时的光标位置，选中成员后按此位置插入
      atMembers: [], // 群成员列表（UserSearchVO）
      atMemberLoading: false, // 群成员加载中
      // ===== 语音消息 =====
      voiceMode: false, // 语音输入模式
      isRecordingVoice: false, // 正在录音
      voiceDuration: 0, // 录音时长（秒）
      voiceChunks: [], // 录音数据块
      voiceStream: null, // 麦克风流
      voiceRecorder: null, // MediaRecorder
      voiceRecognition: null, // 语音识别（转文字）
      voiceTranscript: '', // 识别出的文字
      voiceStartTime: 0, // 录音开始时间
      voiceTimer: null, // 时长计时器
      voicePlayingId: null, // 当前播放的语音消息id
      voiceAudio: null, // 共享播放实例
      // ===== 对方正在输入 =====
      typingUsers: {}, // { senderId: { text, expireAt } } 当前窗口内的输入态
      typingLastSentAt: 0, // 本端上报节流时间戳
      typingSweepTimer: null, // 每秒清扫一次过期的输入态
      // ===== 群聊已读 =====
      groupReaders: {}, // { [chatMessageId]: { readers: { [readerId]: name }, total } }
      activeGroupReadId: '', // 当前展开名单的消息 id
      draftTimer: null,
      draftRestoring: false
    }
  },
  async created() {
    this.currentChatWindow = getObject('currentChatWindow') || {};
    this.currentUser = getObject('userInfo') || {};
    if(this.currentChatWindow.chatId != null){
      // 告诉服务端当前打开的好友聊天框
      this.notifySwitchChatWindow();
    }

    // 初始化聊天窗口
    await this.initChatWindow();
    this.restoreDraft();
  },
  computed: {
    // @ 面板里按昵称过滤后的成员
    filteredAtMembers() {
      const kw = String(this.atSearch || '').trim().toLowerCase();
      const list = this.atMembers || [];
      if (!kw) return list;

      // 命中的排前面；同为命中时，昵称以关键词开头的优先，其次命中位置越靠前越前
      const score = (m) => {
        const name = String((m && m.username) || '').toLowerCase();
        const idx = name.indexOf(kw);
        if (idx === -1) return 9999;
        return (idx === 0 ? 0 : 100) + idx;
      };
      return list
        .filter(m => score(m) < 9999)
        .sort((a, b) => score(a) - score(b) || String(a.username || '').length - String(b.username || '').length);
    },
    // 标题栏下方的输入态文案：单人显示具体是谁，多人显示人数
    typingText() {
      const now = Date.now();
      const list = Object.keys(this.typingUsers || {})
        .map(k => this.typingUsers[k])
        .filter(t => t && t.expireAt > now);
      if (!list.length) return '';
      if (list.length === 1) return list[0].text;
      return `${list.length} 人正在输入…`;
    },
    // 群公告是否超过一行，决定是否显示「展开/收起」
    groupAnnouncementLong() {
      return String(this.groupAnnouncement || '').trim().length > 60;
    },
    // 当前用户是否被全员禁言限制发言（群主、管理员豁免）
    groupMutedForMe() {
      return this.currentChatWindow &&
        this.currentChatWindow.chatType === 2 &&
        Number(this.groupMuteAll) === 1 &&
        Number(this.groupRole) !== 1 &&
        Number(this.groupRole) !== 2;
    }
  },
  methods: {
    // ===== 对方正在输入 =====
    // 本端输入时上报：2.5 秒内只发一帧，避免每次按键都产生一次网络往返
    notifyTyping() {
      if (!this.currentUser || !this.currentUser.id) return;
      if (!this.currentChatWindow || this.currentChatWindow.chatId == null) return;
      // 机器人会话不显示输入态（机器人没有「正在输入」语义）
      if (this.currentChatWindow.chatType === 3) return;
      const now = Date.now();
      if (now - this.typingLastSentAt < 2500) return;
      this.typingLastSentAt = now;
      const frame = {
        senderId: String(this.currentUser.id),
        receiverId: String(this.currentChatWindow.chatId),
        message: '',
        messageType: 'chat',
        subType: 'typing',
        chatType: this.currentChatWindow.chatType,
        contentType: 1
      };
      getWebSocketClient().sendMessage(JSON.stringify(frame));
    },

    // 收到对方的输入态：只在当前打开的会话里展示，5 秒无新帧自动消失
    receiveTyping(data) {
      if (!data || !this.currentUser) return;
      let payload = data;
      const raw = data.message;
      if (typeof raw === 'string' && raw.charAt(0) === '{') {
        try {
          payload = JSON.parse(raw);
        } catch (e) {
          return;
        }
      }
      const senderId = String(payload.senderId || data.senderId || '');
      if (!senderId || senderId === String(this.currentUser.id)) return;
      const chatType = payload.chatType != null ? payload.chatType : data.chatType;
      if (chatType === 3) return;
      // 单聊会话 id 是对方 id；群聊会话 id 是群 id
      const chatId = chatType === 2 ? String(payload.receiverId || '') : senderId;
      if (!this.currentChatWindow || String(this.currentChatWindow.chatId) !== chatId) return;
      const text = chatType === 2
        ? `${payload.senderName || this.resolveSenderName(senderId)} 正在输入…`
        : '对方正在输入…';
      this.typingUsers = {
        ...this.typingUsers,
        [senderId]: { text: text, name: payload.senderName || '', expireAt: Date.now() + 5000 }
      };
      this.startTypingSweep();
    },

    // 群聊里解析发送者昵称：优先用已加载的群成员，其次用消息里带的名字，兜底「有人」
    resolveSenderName(senderId) {
      const id = String(senderId);
      const member = (this.atMembers || []).find(m => String(m.id) === id);
      if (member && member.username) return member.username;
      const msg = (this.chatMessages || []).find(m => String(m.senderId) === id && m.sender && m.sender.username);
      if (msg) return msg.sender.username;
      return '有人';
    },

    // 每秒清扫过期输入态，保证 5 秒内没有任何新帧就自己消失
    startTypingSweep() {
      if (this.typingSweepTimer) return;
      this.typingSweepTimer = setInterval(() => {
        const now = Date.now();
        const next = {};
        let changed = false;
        Object.keys(this.typingUsers || {}).forEach(k => {
          if (this.typingUsers[k] && this.typingUsers[k].expireAt > now) {
            next[k] = this.typingUsers[k];
          } else {
            changed = true;
          }
        });
        if (changed) this.typingUsers = next;
      }, 1000);
    },

    // 对方消息到达时立刻收掉他的输入态，避免「正在输入」和新消息同时出现。
    // 群聊推送帧的 senderId 是群 id（真实发送人只在 sender.username 里），所以同时按昵称兜底匹配。
    clearTypingOf(senderId, senderName) {
      const id = String(senderId || '');
      const name = String(senderName || '');
      const keys = Object.keys(this.typingUsers || {}).filter(k => {
        if (id && k === id) return true;
        return name && this.typingUsers[k] && this.typingUsers[k].name === name;
      });
      if (!keys.length) return;
      const next = { ...this.typingUsers };
      keys.forEach(k => delete next[k]);
      this.typingUsers = next;
    },

    // 把昵称按搜索词切成「命中 / 未命中」片段，模板据此渲染高亮（避免 v-html 带来的注入风险）
    splitAtName(name) {
      const text = String(name || '');
      const kw = String(this.atSearch || '').trim();
      if (!kw) return [{ text: text, hit: false }];
      const lower = text.toLowerCase();
      const kl = kw.toLowerCase();
      const segs = [];
      let from = 0;
      let idx = lower.indexOf(kl, from);
      while (idx !== -1) {
        if (idx > from) segs.push({ text: text.slice(from, idx), hit: false });
        segs.push({ text: text.slice(idx, idx + kw.length), hit: true });
        from = idx + kw.length;
        idx = lower.indexOf(kl, from);
      }
      if (from < text.length) segs.push({ text: text.slice(from), hit: false });
      return segs.length ? segs : [{ text: text, hit: false }];
    },

    async initChatWindow(){
      if(this.currentChatWindow != null){
        this.currentChatWindow = getObject('currentChatWindow') || {};
      }
      if(this.currentUser != null){
        this.currentUser = getObject('userInfo') || {};
      }

      // 切会话先收起所有消息级浮层，避免上一个会话的引用/AI 菜单留在新会话上
      this.closeSmartReplyMenu();
      this.atPanelShow = false;
      this.atInsertPos = null;

      // 清空历史消息所需参数
      this.clearHistoryMes();
      // 群公告横幅 / 全员禁言态：只对群聊拉取，私聊立即清空
      await this.loadGroupDetail();
      const currentFriendId = this.$route.query.friendId || this.currentChatWindow.chatId;

      if(currentFriendId != null){
        // 加载历史消息
        await this.loadHistoryMessages();

        // 确保DOM完全更新后再处理已读消息
        this.$nextTick(() => {
          // 等待非文本消息加载完成
          setTimeout(() => {
            this.getVisibleMessages();
          }, 200);
        });
      }
    },

    // 群资料在群管理页保存后，会话列表已刷新；这里同步当前窗口标题/头像，
    // 不必重载历史消息，输入框草稿和滚动位置都保留。
    syncChatMeta(row) {
      if (!row || !this.currentChatWindow) {
        return;
      }
      if (String(row.chatId) !== String(this.currentChatWindow.chatId)) {
        return;
      }
      if (row.chatName) {
        this.currentChatWindow.chatName = row.chatName;
      }
      if (row.avatar !== undefined) {
        this.currentChatWindow.avatar = row.avatar;
      }
      // 群名/群资料变化时，公告和禁言状态可能也变了，顺手刷新横幅
      this.loadGroupDetail();
    },

    // 拉取群公告与全员禁言状态；带令牌防止快速切群时旧请求覆盖新会话
    async loadGroupDetail() {
      const chat = this.currentChatWindow || {};
      const groupId = chat.chatType === 2 && chat.chatId != null ? String(chat.chatId) : '';
      const token = groupId + '#' + Date.now() + '#' + Math.random();
      this.groupDetailToken = token;
      this.groupAnnouncementExpanded = false;

      if (!groupId) {
        this.groupAnnouncement = '';
        this.groupAnnouncementTime = '';
        this.groupMuteAll = 0;
        this.groupRole = 0;
        return;
      }

      try {
        // 群公告/禁言属后台刷新，失败由聊天页静默降级，不弹全局 500
        const res = await getGroupDetail(groupId, { silent: true });
        if (this.groupDetailToken !== token) return; // 已切到别的群，丢弃旧结果
        if (!res || res.code !== 200 || !res.data) return;
        const detail = res.data;
        this.groupAnnouncement = String(detail.announcement || '').trim();
        this.groupAnnouncementTime = detail.announcementTime || '';
        this.groupMuteAll = Number(detail.muteAll) === 1 ? 1 : 0;
        this.groupRole = Number(detail.role) || 0;
      } catch (e) {
        if (this.groupDetailToken !== token) return;
        // 群公告拉取失败不影响聊天本身，静默降级为无横幅
        this.groupAnnouncement = '';
        this.groupMuteAll = 0;
        this.groupRole = 0;
      }
    },

    toggleGroupAnnouncement() {
      if (!this.groupAnnouncementLong) return;
      this.groupAnnouncementExpanded = !this.groupAnnouncementExpanded;
    },

    // 告诉服务端当前打开的好友聊天框
    notifySwitchChatWindow(){
      const message = {
        senderId: this.currentUser.id,
        receiverId: this.currentChatWindow.chatId,
        message: this.newMessage.trim(),
        messageType: 'notify',
        subType: 'switch_chat_window',
        createTime : new Date()
      };
      getWebSocketClient().sendMessage(JSON.stringify(message));
    },

    // 机器人会话没有历史落库，给出明确入口而不是留白
    goBotAiChat() {
      const botId = this.currentChatWindow && this.currentChatWindow.chatId;
      if (botId) {
        this.$router.push('/ai-chat/' + botId);
      }
    },

    // 加载历史消息方法
    async loadHistoryMessages() {
      // 查询历史消息
      await this.getHistoryMes();
      // 确保DOM更新后再滚动到底部
      this.$nextTick(() => {
        this.scrollToBottom();
      });
    },

    // 通话结束回到聊天窗：整页重拉，确保刚写入的通话记录不被分页叠加
    async refreshCurrentChatAfterCall() {
      if (!this.currentChatWindow || !this.currentChatWindow.chatId) return;
      if (this.currentChatWindow.chatType !== 1) return;
      this.clearHistoryMes();
      await this.loadHistoryMessages();
    },

    // 滚动到底部
    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messagesContainer;
        if (container) {
          // 立即设置滚动位置，没有平滑效果
          container.style.scrollBehavior = 'auto';
          
          // 等待所有媒体元素（图片、音频、视频）加载完成
          const mediaElements = container.querySelectorAll('img, audio, video');
          const mediaPromises = Array.from(mediaElements).map(element => {
            if (element.tagName === 'IMG') {
              return new Promise((resolve) => {
                if (element.complete) {
                  resolve();
                } else {
                  element.onload = resolve;
                  element.onerror = resolve;
                }
              });
            } else if (element.tagName === 'AUDIO' || element.tagName === 'VIDEO') {
              return new Promise((resolve) => {
                if (element.readyState >= 1) {
                  resolve();
                } else {
                  element.onloadeddata = resolve;
                  element.onerror = resolve;
                }
              });
            }
            return Promise.resolve();
          });
          
          // 等待所有媒体元素加载完成后滚动到底部
          Promise.all(mediaPromises).then(() => {
            // 再次等待DOM更新，确保媒体元素渲染完成
            this.$nextTick(() => {
              container.scrollTop = container.scrollHeight;
            });
          });
        }
      });
    },

    // 关闭聊天窗口方法
    closeChatWindow() {
      closeChat(this.currentChatWindow.chatId).then(res => {
        if(res.code == 200){
          this.$emit('close', this.currentChatWindow.chatId);
        }
      })
      remove('currentChatWindow');
    },

    // 根据不同类型 处理收到的消息
    handleReceiveMessage(data) {
      const messageType = data.messageType;
      const subType = data.subType;
      if(messageType == 'chat' && subType == 'mes_receive'){ // 聊天消息
        this.receiveChatMessage(data);
      }else if(messageType == 'chat' && subType == 'mes_send_ack'){ // 消息发送确认
        this.receiveMessageSendAck(data);
      }else if(messageType == 'chat' && subType == 'mes_read'){ // 消息已读通知
        this.receiveMessageReadNotify(data);
      }else if(messageType == 'chat' && subType == 'system_mes'){ // 系统消息
        this.receiveSystemMessage(data);
      }else if(messageType == 'chat' && subType == 'mes_withdraw'){ // 消息撤回通知
        this.receiveMessageWithdraw(data);
      }else if(messageType == 'chat' && subType == 'mes_reaction'){ // 消息表情回应变更
        this.receiveMessageReaction(data);
      }else if(messageType == 'chat' && subType == 'mes_edit'){ // 消息被编辑
        this.receiveMessageEdit(data);
      }else if(messageType == 'chat' && subType == 'typing'){ // 对方正在输入
        this.receiveTyping(data);
      }
    },

    // 接收聊天消息
    receiveChatMessage(data){
      if(this.currentChatWindow == null){
        return;
      }
      // 消息到达即收掉对方的「正在输入」，避免和新消息同屏出现
      this.clearTypingOf(data && data.senderId, data && data.sender && data.sender.username);
      const senderId = data.senderId;
      // 判断是否在当前聊天窗口
      if(this.currentChatWindow.chatId !== senderId){
        return;
      }

      // 接收到消息push到当前窗口
      this.currentChatWindowPushMessage(data)
      this.loadMessageInteractions([data], true)

      // 滚动到底部
      this.$nextTick(() => {
        this.scrollToBottom();
        
        // 消息加载完成后立即检查已读状态
        setTimeout(() => {
          const container = this.$refs.messagesContainer;
          if (container) {
            this.processUnreadMessages(container);
          }
        }, 100);
      });

      // // 刷新左侧好友消息列表
      // if (this.$parent.getLastMes) {
      //   this.$parent.getLastMes()
      // }
    },

    // ===== 消息表情回应 =====
    isReactionable(message) {
      if (!message || !message.chatMessageId) return false
      if (this.isSystemMessage(message)) return false
      if (Number(message.withdrawn) === 1) return false
      const type = Number(message.messageType || message.contentType || 1)
      // 系统提示（6）与通话记录（9）不提供回应
      return type !== 6 && type !== 9
    },

    hasMyReaction(message, emoji) {
      if (!message || !Array.isArray(message.reactions)) return false
      return message.reactions.some(item => item.emoji === emoji && item.mine)
    },

    /**
     * 批量补齐当前屏消息的回应与收藏态；失败只降级为不展示，不阻塞消息本身。
     */
    async loadMessageInteractions(messages, quiet = false) {
      const list = (messages || []).filter(msg => this.isReactionable(msg))
      const ids = list.map(msg => String(msg.chatMessageId))
      if (!ids.length) return
      try {
        const res = await getMessageInteractions({ messageIds: ids })
        const data = res.data || {}
        const reactionMap = {}
        ;(data.reactions || []).forEach(summary => {
          reactionMap[String(summary.messageId)] = Array.isArray(summary.items) ? summary.items : []
        })
        const favoriteIds = new Set((data.favoriteIds || []).map(String))
        list.forEach(msg => {
          const id = String(msg.chatMessageId)
          msg.reactions = reactionMap[id] || []
          msg.favorited = favoriteIds.has(id)
        })
      } catch (error) {
        if (!quiet) console.warn('加载消息互动状态失败', error)
      }
    },

    receiveMessageReaction(data) {
      let payload = data && data.message
      try {
        payload = typeof payload === 'string' ? JSON.parse(payload) : payload
      } catch (e) {
        return
      }
      const messageId = payload && (payload.chatMessageId || payload.messageId)
      if (!messageId) return
      const target = this.chatMessages.find(msg => String(msg.chatMessageId) === String(messageId))
      if (!target) return
      this.loadMessageInteractions([target], true)
    },

    openReactionPicker(message, event) {
      if (!this.isReactionable(message)) return
      const rect = event.currentTarget.getBoundingClientRect()
      const width = 320
      const height = 142
      let x = rect.left
      let y = rect.bottom + 8
      if (x + width > window.innerWidth - 12) x = window.innerWidth - width - 12
      if (y + height > window.innerHeight - 12) y = rect.top - height - 8
      this.reactionPicker = { show: true, x: Math.max(12, x), y: Math.max(12, y), message }
    },

    closeReactionPicker() {
      if (this.reactionPicker.show) {
        this.reactionPicker.show = false
      }
    },

    async toggleReaction(message, emoji) {
      if (!this.isReactionable(message) || message.reactionPending) return
      message.reactionPending = true
      try {
        const res = await toggleMessageReaction({ messageId: message.chatMessageId, emoji })
        message.reactions = (res.data && res.data.items) || []
        this.closeReactionPicker()
        this.smartReplyMenu.show = false
      } catch (error) {
        ElMessage.error((error && error.message) || '回应失败，请重试')
      } finally {
        message.reactionPending = false
      }
    },

    // ===== 消息收藏 =====
    async toggleFavorite(message) {
      if (!this.isReactionable(message) || message.favoritePending) return
      message.favoritePending = true
      try {
        const res = await toggleMessageFavorite({ messageId: message.chatMessageId })
        message.favorited = !!res.data
        this.smartReplyMenu.show = false
        ElMessage.success(message.favorited ? '已收藏，可在收藏夹查看' : '已取消收藏')
      } catch (error) {
        ElMessage.error((error && error.message) || '收藏失败，请重试')
      } finally {
        message.favoritePending = false
      }
    },

    // 停止机器人响应
    stopBotResponse() {
      if (this.currentChatWindow.chatType === 3) {
        // 查找当前机器人的消息
        let botMessage = '';
        const botMessageIndex = this.chatMessages.findLastIndex(
            msg => msg.chatType === 3 && msg.senderId === this.currentChatWindow.chatId
        );
        if (botMessageIndex !== -1) {
          botMessage = this.chatMessages[botMessageIndex].message;
        }
        
        // 发送停止请求到服务端
        const message = {
          senderId: this.currentChatWindow.chatId,
          receiverId: this.currentUser.id,
          message: botMessage,
          chatType: 3,
          messageType: 'chat',
          subType: 'stop_bot_response'
        };
        getWebSocketClient().sendMessage(JSON.stringify(message));
        
        // 立即更新状态
        this.isBotTyping = false;
        this.isBotPaused = true;
        
        // 查找并移除加载中的机器人消息
        const loadingMessageIndex = this.chatMessages.findIndex(
            msg => msg.chatType === 3 && msg.isLoading
        );
        if (loadingMessageIndex !== -1) {
          this.chatMessages.splice(loadingMessageIndex, 1);
        }
      }
    },
    
    // 打开文件选择对话框
    openFileUpload() {
      if(this.currentChatWindow == null){
        return;
      }
      if(this.currentChatWindow.chatType == null){
        return;
      }
      if(this.currentChatWindow.chatType === 3){
        // 暂不支持发送文件
        this.$confirm({
          title: '功能提示',
          content: '暂不支持发送文件'
        })
        return;
      }

      this.$refs.fileInput.click();
    },
    
    // 获取文件类型
    getFileType(file) {
      const fileName = file.name.toLowerCase();
      const fileType = file.type;
      
      // 图片类型
      if (fileType.startsWith('image/')) {
        return 2;
      }
      
      // 音频类型
      if (fileType.startsWith('audio/')) {
        return 3;
      }
      
      // 视频类型
      if (fileType.startsWith('video/')) {
        return 4;
      }
      
      // 文件类型
      if (
        fileName.endsWith('.txt') ||
        fileName.endsWith('.md') ||
        fileName.endsWith('.html') ||
        fileName.endsWith('.css') ||
        fileName.endsWith('.js') ||
        fileName.endsWith('.json') ||
          fileName.endsWith('.doc') ||
          fileName.endsWith('.docx') ||
          fileName.endsWith('.xls') ||
          fileName.endsWith('.xlsx')
      ) {
        return 5;
      }
      
      // 默认文件类型
      return 1;
    },
    
    // 从相册/文件选择图片：复用粘贴图片的上传与预览链路
    handleImageSelect(event) {
      const files = Array.from((event.target && event.target.files) || []);
      files.forEach(file => this.handlePasteImage(file));
      if (event.target) event.target.value = '';
    },

    // 文件消息历史里只存 URL，文件名需要从对象存储路径还原（上传时格式为 {id}_{原始文件名}）
    fileDisplayName(message) {
      if (!message) return '文件';
      if (message.fileName) return message.fileName;
      const url = message.message || '';
      try {
        const base = decodeURIComponent((new URL(url, window.location.href)).pathname.split('/').pop() || '');
        if (!base) return '文件';
        const sep = base.indexOf('_');
        return sep >= 0 ? base.slice(sep + 1) : base;
      } catch (e) {
        return '文件';
      }
    },

    // 处理文件选择
    async handleFileSelect(event) {
      const file = event.target.files[0];
      if (!file) return;
      
      try {
        // 上传文件
        const response = await uploadFile(file);
        // 从返回结果中获取文件URL
        const fileUrl = response.data;
        
        // 获取文件类型
        const contentType = this.getFileType(file);
        
        // 发送文件消息
        const fileMessage = {
          senderId: this.currentUser.id,
          receiverId: this.currentChatWindow.chatId,
          message: fileUrl,
          chatType: this.currentChatWindow.chatType,
          messageType: 'chat',
          contentType: contentType,
          fileName: file.name,
          fileSize: file.size
        };
        
        // 调用发送消息方法
        this.sendMessage(fileMessage.message, contentType);
        
      } catch (error) {
        // 更友好的错误提示
        let errorMsg = '文件上传失败';
        if (error.message && error.message.includes('不允许的文件类型')) {
          errorMsg = '不允许的文件类型，请尝试其他文件格式';
        }
        ElMessage.error(errorMsg);
        console.error('文件上传失败:', error);
      }
      
      // 重置文件输入
      event.target.value = '';
    },

    // 接收消息发送确认
    receiveMessageSendAck(data){
      const message = JSON.parse(data.message);
      const key = Object.keys(message)[0];
      const value = Object.values(message)[0];

      // 转发到其他会话时消息不在当前 chatMessages 里，单独用待确认队列结算
      const pendingIndex = this.pendingForwardAcks.findIndex(p =>
        p.content === key && p.createTime == data.createTime
      );
      if (pendingIndex > -1) {
        const pending = this.pendingForwardAcks[pendingIndex];
        this.pendingForwardAcks.splice(pendingIndex, 1);
        pending.resolve(true);
      }

      // 找到对应的消息并更新消息id：优先按 clientMsgId 精确匹配，老消息回退内容+时间
      let msg = data.clientMsgId
        ? this.chatMessages.find(m => m.clientMsgId === data.clientMsgId)
        : null;
      if (!msg) {
        msg = this.chatMessages.find(m => m.message === key && m.createTime == data.createTime);
      }
      // 语音/媒体消息内容经过上传和格式化，服务端 ack 的 createTime 可能差 1 秒
      // 精确匹配不到时，按待确认队列兜底，避免已持久化的消息被误标为发送失败
      if (!msg) {
        const sameContent = this.chatMessages.filter(m => m.sendStatus === 'sending' && String(m.message).trim() === key);
        if (sameContent.length) msg = sameContent[sameContent.length - 1];
      }
      if (!msg) {
        const pending = this.chatMessages.filter(m => m.sendStatus === 'sending');
        if (pending.length === 1) msg = pending[0];
      }
      if (msg) {
        msg.chatMessageId = value || data.chatMessageId;
        msg.sendStatus = 'success';
      }
    },

    // 接收到的消息推到当前窗口
    currentChatWindowPushMessage(data) {
      const chatType = this.currentChatWindow.chatType;
      if(chatType == null){
        return
      }

      // 在聊天窗口添加新消息
      if(chatType === 1){
        this.pushPrivateMessage(data);
      }else if(chatType === 2){
        this.pushGroupMessage(data);
      }else if (chatType === 3){
        this.pushAIMessage(data);
      }else{
        return;
      }
    },

    // 单聊push消息到聊天窗口
    pushPrivateMessage(data){
      // 在聊天窗口添加新消息
      this.chatMessages.push({
        chatMessageId: data.chatMessageId,
        senderId: data.senderId,
        receiverId: data.receiverId,
        messageType: data.messageType,
        chatType: this.currentChatWindow.chatType,
        message: data.message,
        contentType: data.contentType,
        fileName: data.fileName,
        withdrawn: data.withdrawn !== undefined ? data.withdrawn : 0,
        replyToId: data.replyToId,
        replyContent: data.replyContent,
        atUsers: data.atUsers,
        createTime: this.formatDate(data.createTime),
        isRead: false
      });
    },

    // 单聊push消息到聊天窗口
    pushGroupMessage(data){
      this.chatMessages.push({
        chatMessageId: data.chatMessageId,
        senderId: data.senderId,
        // 群聊真正发送人
        sender: data.sender,
        receiverId: data.receiverId,
        messageType: data.messageType,
        chatType: this.currentChatWindow.chatType,
        message: data.message,
        contentType: data.contentType,
        fileName: data.fileName,
        withdrawn: data.withdrawn !== undefined ? data.withdrawn : 0,
        replyToId: data.replyToId,
        replyContent: data.replyContent,
        atUsers: data.atUsers,
        createTime: this.formatDate(data.createTime),
        isRead: false
      });
    },

    pushAIMessage(data){
      // 检查是否是结束标记
      const isDoneMessage = this.currentChatWindow && this.currentChatWindow.chatType === 3 && data.message === "[DONE]";
      
      // 如果是结束标记，不更新消息内容，只更新状态并发送结束消息
      if (isDoneMessage) {
        this.isBotTyping = false;
        this.isBotPaused = false; // 重置暂停状态
        
        // 查找当前机器人的消息
        let botMessage = '';
        const botMessageIndex = this.chatMessages.findLastIndex(
            msg => msg.chatType === 3 && msg.senderId === this.currentChatWindow.chatId
        );
        if (botMessageIndex !== -1) {
          botMessage = this.chatMessages[botMessageIndex].message;
        }
        
        // 向服务端发送结束消息
        const endMessage = {
          senderId: this.currentChatWindow.chatId,
          receiverId: this.currentUser.id,
          message: botMessage,
          chatType: 3,
          messageType: 'chat',
          subType: 'bot_response_end'
        };
        getWebSocketClient().sendMessage(JSON.stringify(endMessage));
        
        return;
      }

      // 如果机器人已被暂停，不处理消息
      if (this.isBotPaused) {
        return;
      }

      // 查找是否已存在相同chatMessageId的消息
      const existingMessageIndex = this.chatMessages.findIndex(
          msg => msg.chatMessageId === data.chatMessageId
      );

      // 查找是否存在加载中的机器人消息
      const loadingMessageIndex = this.chatMessages.findIndex(
          msg => msg.chatType === 3 && msg.isLoading
      );

      if (existingMessageIndex !== -1) {
        // 更新现有消息内容（追加模式）
        this.chatMessages[existingMessageIndex].message += data.message;
        // 确保加载状态关闭
        if (this.chatMessages[existingMessageIndex].isLoading) {
          this.chatMessages[existingMessageIndex].isLoading = false;
        }
        // 确保有isRead属性
        if (this.chatMessages[existingMessageIndex].isRead === undefined) {
          this.chatMessages[existingMessageIndex].isRead = false;
        }
      } else if (loadingMessageIndex !== -1) {
        // 替换加载消息为实际消息
        this.chatMessages[loadingMessageIndex] = {
          chatMessageId: data.chatMessageId,
          senderId: data.senderId,
          receiverId: data.receiverId,
          messageType: data.messageType,
          chatType: this.currentChatWindow ? this.currentChatWindow.chatType : 0,
          message: data.message,
          contentType: data.contentType,
          fileName: data.fileName,
          withdrawn: data.withdrawn !== undefined ? data.withdrawn : 0,
          replyToId: data.replyToId,
          replyContent: data.replyContent,
          atUsers: data.atUsers,
          createTime: this.formatDate(data.createTime),
          isLoading: false, // 初始状态为非加载中
          isRead: false
        };
      } else {
        // 添加新消息
        this.chatMessages.push({
          chatMessageId: data.chatMessageId,
          senderId: data.senderId,
          receiverId: data.receiverId,
          messageType: data.messageType,
          chatType: this.currentChatWindow ? this.currentChatWindow.chatType : 0,
          message: data.message,
          contentType: data.contentType,
          fileName: data.fileName,
          withdrawn: data.withdrawn !== undefined ? data.withdrawn : 0,
          replyToId: data.replyToId,
          replyContent: data.replyContent,
          atUsers: data.atUsers,
          createTime: this.formatDate(data.createTime),
          isLoading: false, // 初始状态为非加载中
          isRead: false
        });
      }
    },

    // 加载更多历史消息
    async loadMoreHistory() {
      await this.getHistoryMes();

      // todo 这里保持滚动位置有问题，获取到新消息时，虽然加载了历史消息但不要滚动页面
      // 保持滚动位置
      this.$nextTick(() => {
        const container = this.$refs.messagesContainer;
        if (container) {
          const prevHeight = container.scrollHeight;
          const prevScroll = container.scrollTop;
          container.scrollTop = container.scrollHeight - prevHeight + prevScroll;
        }
      });
    },

    // 分页查询历史记录
    async getHistoryMes(){
      const seq = ++this.historyRequestSeq;
      const targetChatId = this.currentChatWindow ? String(this.currentChatWindow.chatId) : '';
      const req = {
        senderId: this.currentUser.id,
        receiverId: this.currentChatWindow.chatId,
        chatType: this.currentChatWindow.chatType,
        pageNum: this.historyPageNum,
        pageSize: this.historyPageSize
      };

      // loading打开
      this.loadingHistory = true;
      this.historyLoadFailed = false;
      this.historyAccessDenied = false;
      try {
        // 调用接口查询
        const res = await getHistoryMes(req, { silent: true });
        // 快速切会话时旧请求可能后到，必须丢弃，否则会把上一个会话的消息串进当前窗口
        if (seq !== this.historyRequestSeq) return;
        if (!this.currentChatWindow || String(this.currentChatWindow.chatId) !== targetChatId) return;
        // 将新消息插入到数组开头，同时处理messageType到contentType的映射
        const processedMessages = res.data.rows.map(msg => ({
          ...msg,
          // 如果没有contentType但有messageType，则使用messageType作为contentType
          contentType: msg.contentType || msg.messageType,
          // 确保有isRead属性
          isRead: msg.isRead !== undefined ? msg.isRead : false
        }));
        this.chatMessages = [...processedMessages, ...this.chatMessages];
        this.loadMessageInteractions(processedMessages, true);

        // 群聊已读回填：服务端随历史消息带回 readCount / readers，
        // 刷新页面或重进会话时先铺一层，实时帧到达后再在其上累加
        if (this.currentChatWindow.chatType === 2) {
          const seed = {};
          processedMessages.forEach(msg => {
            if (msg.senderId !== this.currentUser.id) return;
            const readers = Array.isArray(msg.readers) ? msg.readers : [];
            if (!readers.length) return;
            const map = {};
            readers.forEach(r => {
              const key = r.id || r.name;
              if (key) map[key] = r.name || '';
            });
            seed[msg.chatMessageId] = {
              readers: map,
              total: msg.readCount || readers.length
            };
          });
          // 已有的实时状态优先，不被历史覆盖
          this.groupReaders = { ...seed, ...this.groupReaders };
        }

        // 判断是否还有更多数据  当前页码 < 总页数，说明还有更多数据
        if(this.historyPageNum < res.data.totalPage){
          this.hasMoreHistory = true;
        }else{
          this.hasMoreHistory = false
        }
      } catch (error) {
        if (seq !== this.historyRequestSeq) return;
        const reason = String((error && (error.msg || error.message)) || '');
        // 已退群/被移出群：历史接口会明确拒绝，这不是网络问题，重试也不会成功
        if (reason.includes('未加入群聊') || reason.includes('不是群成员')) {
          this.historyAccessDenied = true;
          this.historyLoadFailed = false;
          console.warn('当前用户已不在该群聊，停止加载历史', reason);
          return;
        }
        // 失败必须有可见出口，否则 loadingHistory 永远挂着「加载历史消息中...」
        this.historyLoadFailed = true;
        console.error("加载历史消息失败:", error);
      } finally {
        // 只允许最新请求关闭 loading，避免旧请求把新会话的 loading 提前关掉
        if (seq === this.historyRequestSeq) this.loadingHistory = false;
      }
    },

    // 已不在群聊的会话无法恢复，给用户一个「从列表移除」的干净出口
    removeUnavailableChat() {
      if (!this.currentChatWindow) {
        return;
      }
      this.$emit('hide-chat', String(this.currentChatWindow.chatId));
    },

    // 处理消息窗口滚动 （分页查询历史消息、提交未读到服务端）
    handleMessagesScroll(event) {
      const container = event.target;
      const now = Date.now();
      const scrollTop = container.scrollTop;

      // 计算滑动速度 (px/ms)
      if (this.lastScrollTime > 0) {
        const distance = Math.abs(scrollTop - this.lastScrollPosition);
        const timeDiff = now - this.lastScrollTime;
        this.scrollSpeed = distance / timeDiff;
      }

      // 更新滚动状态
      this.isScrollingUp = scrollTop < this.lastScrollTop;
      this.lastScrollTop = scrollTop;
      this.lastScrollPosition = scrollTop;
      this.lastScrollTime = now;

      // 如果没有更多消息并且用户正在向上滚动
      if (!this.hasMoreHistory && this.isScrollingUp) {
        // 收集可视区域未读消息
        this.processUnreadMessages(container);
        return;
      }

      // 防止重复触发加载（未向上滚动 / 正在加载 / 无更多消息）
      if (!this.isScrollingUp || this.loadingHistory) {
        return;
      }

      // 检查是否需要加载更多历史消息
      this.checkLoadMoreHistory(container);

      // 收集可视区域未读消息
      this.processUnreadMessages(container);
    },

    // 检查是否需要加载更多历史消息
    checkLoadMoreHistory(container){
      // 获取容器内的消息
      const messages = container.getElementsByClassName('message');
      if(messages == null || messages.length == 0 || messages.length < this.nextLoadMesIndex){
        return;
      }

      // 获取下一次加载消息 （每次分页加载20条，滑动到15条，加载下一次分页）
      const message = messages[this.nextLoadMesIndex];
      if(message == null){
        return;
      }
      const msgRect = message.getBoundingClientRect();
      const msgTop = msgRect.top;
      const msgBottom = msgRect.bottom;

      // 每15条消息 滑出聊天窗口顶部 加载下一次分页
      if (msgBottom > this.chatContainerTop) {
        // 防止重复加载同一页
        if (this.loadedPages.has(this.historyPageNum)) {
          return;
        }

        // 记录加载过的页码
        this.loadedPages.add(this.historyPageNum);
        this.historyPageNum ++;
        this.loadMoreHistory();
      }
    },

    // 收集可视区域未读消息
    processUnreadMessages(container){
      this.collectVisibleUnReadMessages(container);

      // 清除之前的定时器
      clearTimeout(this.scrollUnReadTimer);

      if (this.visibleUnReadMessages == null || this.visibleUnReadMessages.size == 0) {
        return
      }

      // 根据滑动速度决定提交策略
      const isFastScroll = this.scrollSpeed > this.fastScrollThreshold;
      const delay = isFastScroll ? this.fastSubmitDelay : this.slowSubmitDelay;

      this.scrollUnReadTimer = setTimeout(() => {
        this.sendUnReadMes();
      }, delay);
    },

    // 收集可视区域未读消息
    collectVisibleUnReadMessages(container){
      // 获取容器内的消息
      const messages = container.getElementsByClassName('message');
      if(messages == null || messages.length == 0){
        return;
      }

      // 确保容器边界已初始化
      if (!this.chatContainerTop) {
        this.chatContainerRect = container.getBoundingClientRect();
        this.chatContainerTop = this.chatContainerRect.top;
        this.chatContainerBottom = this.chatContainerTop + container.clientHeight;
      }

      for (let i = 0; i < messages.length; i++) {
        const messageEl = messages[i];
        const msgRect = messageEl.getBoundingClientRect();
        const msgTop = msgRect.top;
        const msgBottom = msgRect.bottom;
        
        // 消息在可视区域内（至少有一部分可见）
        if (msgBottom > this.chatContainerTop && msgTop < this.chatContainerBottom) {
          try {
            const messageObj = JSON.parse(messageEl.getAttribute('data-message'));
            
            // 消息未读且是发给当前用户的，并且是当前聊天窗口发送的
            if (messageObj.isRead == false && messageObj.receiverId == this.currentUser.id && messageObj.senderId == this.currentChatWindow.chatId) {
              this.visibleUnReadMessages.add(messageObj.chatMessageId);
            }
          } catch (error) {
            console.error('解析消息对象失败:', error);
          }
        }
      }
    },

    // 校验消息
    checkMessage(){
      const messageText = this.newMessage.trim();

      // 检查是否空白消息
      if (!messageText) {
        this.showEmptyWarn = true;
        setTimeout(() => {
          this.showEmptyWarn = false;
        }, 2000); // 2秒后自动隐藏错误提示
        return false;
      }

      return true;
    },

    // 发送消息（target 可选：定时消息指定目标窗口；为空则发送到当前聊天窗口）
    // ===== 语音消息 =====
    toggleVoiceMode() {
      this.voiceMode = !this.voiceMode
      if (this.isRecordingVoice) {
        this.stopVoiceRecord(true)
      }
    },
    async startVoiceRecord() {
      if (this.isRecordingVoice) return
      if (!navigator.mediaDevices || !window.MediaRecorder) {
        if (this.$message) this.$message.warning('当前浏览器不支持录音，请使用最新版 Chrome/Edge')
        else alert('当前浏览器不支持录音，请使用最新版 Chrome/Edge')
        return
      }
      try {
        this.voiceStream = await navigator.mediaDevices.getUserMedia({ audio: true })
      } catch (e) {
        if (this.$message) this.$message.warning('无法访问麦克风，请检查浏览器权限')
        else alert('无法访问麦克风，请检查浏览器权限')
        return
      }
      const mimeType = MediaRecorder.isTypeSupported('audio/webm')
          ? 'audio/webm'
          : (MediaRecorder.isTypeSupported('audio/ogg') ? 'audio/ogg' : 'audio/mp4')
      this.voiceRecorder = new MediaRecorder(this.voiceStream, mimeType ? { mimeType } : undefined)
      this.voiceChunks = []
      this.voiceRecorder.ondataavailable = (e) => {
        if (e.data && e.data.size > 0) this.voiceChunks.push(e.data)
      }
      this.voiceRecorder.start(250)
      this.voiceStartTime = Date.now()
      this.voiceDuration = 0
      this.voiceTranscript = ''
      this.isRecordingVoice = true
      // 转文字（浏览器原生语音识别）
      const SR = window.SpeechRecognition || window.webkitSpeechRecognition
      if (SR) {
        try {
          this.voiceRecognition = new SR()
          this.voiceRecognition.lang = 'zh-CN'
          this.voiceRecognition.continuous = true
          this.voiceRecognition.interimResults = true
          this.voiceRecognition.onresult = (e) => {
            let t = ''
            for (let i = e.resultIndex; i < e.results.length; i++) {
              t += e.results[i][0].transcript
            }
            this.voiceTranscript = t
          }
          this.voiceRecognition.start()
        } catch (e) {
          this.voiceRecognition = null
        }
      }
      this.voiceTimer = setInterval(() => {
        this.voiceDuration = Math.max(1, Math.round((Date.now() - this.voiceStartTime) / 1000))
        if (this.voiceDuration >= 60) this.stopVoiceRecord(false)
      }, 200)
    },
    async stopVoiceRecord(cancel = false) {
      if (!this.isRecordingVoice) return
      this.isRecordingVoice = false
      if (this.voiceTimer) { clearInterval(this.voiceTimer); this.voiceTimer = null }
      const duration = this.voiceDuration
      const transcript = this.voiceTranscript
      try {
        if (this.voiceRecorder && this.voiceRecorder.state !== 'inactive') {
          this.voiceRecorder.stop()
        }
      } catch (e) {
        // 录音器可能已自动停止或状态不可用，这里不阻断后续上传流程
        console.warn('停止录音失败', e)
      }
      if (this.voiceRecognition) {
        try { this.voiceRecognition.stop() } catch (e) {
          // 语音识别未启动时 stop 会抛错，忽略但不静默
          console.warn('停止语音识别失败', e)
        }
        this.voiceRecognition = null
      }
      if (this.voiceStream) {
        this.voiceStream.getTracks().forEach(t => t.stop())
        this.voiceStream = null
      }
      if (cancel || duration < 1) {
        this.voiceChunks = []
        return
      }
      const recorder = this.voiceRecorder
      this.voiceRecorder = null
      await new Promise(resolve => setTimeout(resolve, 300))
      if (!recorder || this.voiceChunks.length === 0) return
      const blob = new Blob(this.voiceChunks, { type: recorder.mimeType || 'audio/webm' })
      this.voiceChunks = []
      const suffix = blob.type.indexOf('ogg') !== -1 ? 'ogg' : (blob.type.indexOf('mp4') !== -1 ? 'm4a' : 'webm')
      const file = new File([blob], 'voice-' + Date.now() + '.' + suffix, { type: (blob.type || 'audio/webm').split(';')[0] })
      this.sendVoiceMessage(file, duration, transcript)
    },
    async sendVoiceMessage(file, duration, transcript) {
      let url
      try {
        const res = await uploadFile(file)
        url = res && res.data ? res.data : res
      } catch (e) {
        if (this.$message) this.$message.error('语音上传失败，请重试')
        return
      }
      if (!url || url === '上传失败') {
        if (this.$message) this.$message.error('语音上传失败，请重试')
        return
      }
      const voiceContent = JSON.stringify({
        url,
        duration: Math.max(1, duration),
        transcript: (transcript || '').trim()
      })
      this.sendMessage(voiceContent, 3)
    },
    parseVoiceMessage(message) {
      if (!message || !message.message) return null
      const raw = message.message
      if (typeof raw !== 'string') return null
      const trimmed = raw.trim()
      if (trimmed.startsWith('{')) {
        try {
          const obj = JSON.parse(trimmed)
          if (obj && obj.url) return obj
        } catch (e) { return null }
      }
      return null
    },
    isVoicePlaying(message) {
      return this.voicePlayingId === message.chatMessageId && this.voiceAudio && !this.voiceAudio.paused
    },
    toggleVoicePlay(message) {
      const voice = this.parseVoiceMessage(message)
      if (!voice || !voice.url) return
      if (!this.voiceAudio) {
        this.voiceAudio = new Audio()
        this.voiceAudio.addEventListener('ended', () => {
          this.voicePlayingId = null
        })
      }
      const audio = this.voiceAudio
      if (this.voicePlayingId === message.chatMessageId && !audio.paused) {
        audio.pause()
        this.voicePlayingId = null
        return
      }
      if (audio.src !== voice.url) {
        audio.src = voice.url
      }
      audio.currentTime = 0
      audio.play().catch(() => {})
      this.voicePlayingId = message.chatMessageId
    },
    // ===== 会话草稿：本地即时保存 + 后端按用户同步，换设备也能续写 =====
    draftKey(chat) {
      const userId = (this.currentUser && this.currentUser.id) || 'anon';
      const target = chat || this.currentChatWindow || {};
      if (!target.chatId) return '';
      return `chatDraft_${userId}_${target.chatId}`;
    },
    scheduleDraftSave(text) {
      if (this.draftRestoring) return;
      if (this.draftTimer) clearTimeout(this.draftTimer);
      this.draftTimer = setTimeout(() => this.flushDraft(false, String(text || '')), 450);
    },
    flushDraft(force, value) {
      if (this.draftTimer) {
        clearTimeout(this.draftTimer);
        this.draftTimer = null;
      }
      const chat = this.currentChatWindow || {};
      const key = this.draftKey(chat);
      if (!key) return;
      const text = value == undefined ? String(this.newMessage || '') : value;
      try {
        if (text.trim()) localStorage.setItem(key, text);
        else localStorage.removeItem(key);
      } catch (e) { /* 存储不可用时依赖后端 */ }
      if (!force && !text.trim()) {
        // 空草稿也要同步给后端，保证另一端点开时是干净输入框
      }
      saveConversationSetting({
        chatId: chat.chatId,
        chatType: chat.chatType,
        draft: text
      }).catch(() => {});
    },
    restoreDraft() {
      const chat = this.currentChatWindow || {};
      const key = this.draftKey(chat);
      if (!key) return;
      let local = '';
      try { local = localStorage.getItem(key) || ''; } catch (e) { local = ''; }
      if (local) {
        this.draftRestoring = true;
        this.newMessage = local;
        this.$nextTick(() => { this.draftRestoring = false; });
      }
      getConversationSetting({ chatId: chat.chatId, chatType: chat.chatType }).then(res => {
        const remote = res && res.data ? String(res.data.draft || '') : '';
        if (!remote) return;
        // 本机已有未同步内容时不覆盖
        if (String(this.newMessage || '').trim()) return;
        this.draftRestoring = true;
        this.newMessage = remote;
        try { localStorage.setItem(key, remote); } catch (e) {}
        this.$nextTick(() => { this.draftRestoring = false; });
      }).catch(() => {});
    },
    clearDraft() {
      const chat = this.currentChatWindow || {};
      const key = this.draftKey(chat);
      if (key) {
        try { localStorage.removeItem(key); } catch (e) {}
      }
      if (chat.chatId) {
        saveConversationSetting({ chatId: chat.chatId, chatType: chat.chatType, draft: '' }).catch(() => {});
      }
    },

    sendMessage(content = this.newMessage, contentType = 1, target = null, options = {}) {
      // 目标窗口：默认当前聊天窗口
      const chatWindow = target || this.currentChatWindow;
      const inCurrentWindow = !target || (this.currentChatWindow && target.chatId === this.currentChatWindow.chatId);
      if (!chatWindow || chatWindow.chatId == null) {
        return;
      }
      // 全员禁言：普通成员在发送入口就被拦住，并给出明确原因（服务端还有一道拦截兜底）
      // 只对当前窗口生效：转发到别的会话时，本群禁言状态不应该拦截对外转发
      if (inCurrentWindow && chatWindow.chatType === 2 && Number(this.groupMuteAll) === 1
          && Number(this.groupRole) !== 1 && Number(this.groupRole) !== 2) {
        ElMessage.warning('全员禁言中，仅群主和管理员可发言');
        return;
      }
      // 检查是否有内容或图片；跨会话发送（转发）不能把当前草稿里的待发图片一起带走
      const hasText = content.trim() !== '';
      const hasImages = !target && this.pastedImages.length > 0;
      
      if (!hasText && !hasImages) {
        this.showEmptyWarn = true;
        setTimeout(() => {
          this.showEmptyWarn = false;
        }, 2000);
        return;
      }

      if(this.currentUser == null){
        return;
      }
      const senderId = this.currentUser.id;
      if(senderId == null) {
        return;
      }

      // 限制发送消息频率
      if (!this.rateLimit()) {
        return;
      }

      // 发送文本消息
      let sentTextMessage = null;
      if (hasText) {
        const messageContent = content.trim();
        let message = {
          chatMessageId: '',
          clientMsgId: this.genClientMsgId(),
          senderId: senderId,
          receiverId: chatWindow.chatId,
          message: messageContent,
          chatType: chatWindow.chatType,
          messageType: 'chat',
          contentType: contentType,
          subType: 'mes_send',
          isRead: false,
          sendStatus: 'sending',
          replyToId: options.clearReply ? '' : (this.replyTarget ? this.replyTarget.chatMessageId : ''),
          replyContent: options.clearReply ? '' : (this.replyTarget ? this.replyTarget.message : ''),
          atUsers: options.clearAt ? '[]' : JSON.stringify(this.atUsers.map(u => u.id)),
          createTime : this.formatDate(Date.now())
        };

        // 仅当目标为当前窗口时立即显示到聊天窗口
        if (inCurrentWindow) {
          this.chatMessages.push(message);
        }
        
        // 发送消息
        getWebSocketClient().sendMessage(JSON.stringify(message));
        sentTextMessage = message;
        if (inCurrentWindow) {
          this.receiveAckTimer(message);
        }
      }

      // 发送图片消息
      if (hasImages) {
        this.pastedImages.forEach(image => {
          let message = {
            chatMessageId: '',
            clientMsgId: this.genClientMsgId(),
            senderId: senderId,
            receiverId: chatWindow.chatId,
            message: image.url,
            chatType: chatWindow.chatType,
            messageType: 'chat',
            contentType: image.contentType,
            fileName: image.fileName,
            fileSize: image.fileSize,
            subType: 'mes_send',
            isRead: false,
            sendStatus: 'sending',
            createTime : this.formatDate(Date.now())
          };

          if (inCurrentWindow) {
            this.chatMessages.push(message);
          }
          
          // 发送消息
          getWebSocketClient().sendMessage(JSON.stringify(message));

          if (inCurrentWindow) {
            this.receiveAckTimer(message);
          }
        });

        // 清空图片预览
        this.pastedImages = [];
      }

      // 清空输入框（仅当使用默认内容时）
      if (content === this.newMessage) {
        this.newMessage = '';
        this.clearDraft();
        // 发送完成清空引用回复与@提及
        this.replyTarget = null;
        this.atUsers = [];
        this.atPanelShow = false;
      }
      
      this.scrollToBottom();

      // 如果是机器人聊天，发送后立即设置正在输入状态并添加加载消息
      if (chatWindow.chatType === 3 && hasText && inCurrentWindow) {
        this.isBotTyping = true;
        this.isBotPaused = false; // 重置暂停状态，确保新消息能正常处理
        
        // 添加一个临时的机器人加载消息
        const loadingMessageId = `loading_${Date.now()}`;
        this.chatMessages.push({
          chatMessageId: loadingMessageId,
          senderId: this.currentChatWindow.chatId,
          receiverId: this.currentUser.id,
          messageType: 'chat',
          chatType: 3,
          message: '',
          createTime: this.formatDate(Date.now()),
          isLoading: true // 标记为加载中状态
        });
        
        // 滚动到底部
        this.$nextTick(() => {
          this.scrollToBottom();
        });
      }

      // 返回文本消息对象，供调用方（如转发）等待服务端 ack
      return sentTextMessage;
    },

    rateLimit(){
      const now = Date.now();
      const lastSendTime = this.lastSendTime || 0;
      const minInterval = 500; // 最小间隔

      // 检查是否在冷却期内
      if (now - lastSendTime < minInterval) {
        ElMessage.warning(`发送太频繁，请稍后再发送`);
        return false; // 拒绝发送
      }

      // 更新最后发送时间
      this.lastSendTime = now;
      return true; // 允许发送
    },

    // 重新发送消息
    resendMessage(message){
      message.sendStatus = 'sending'

      // 发送消息
      getWebSocketClient().sendMessage(JSON.stringify(message));

      // 接收服务端ack的定时器
      this.receiveAckTimer(message);
    },

    // ===== AI 智能回复 =====
    // 是否为文本消息（可触发智能回复）
    isTextMessage(message) {
      if (!message || message.isLoading || message.withdrawn === 1) return false;
      return (!message.contentType && !message.messageType) || message.contentType === 1 || message.messageType === 1;
    },
    // 只有收到的文本消息才展示 AI 智能回复区；自己发出的消息不请求也不显示失败态
    canSmartReplyMessage(message) {
      if (!this.isTextMessage(message) || !message) return false;
      const me = this.currentUser && this.currentUser.id;
      return !me || String(message.senderId || '') !== String(me);
    },

    // 打开消息操作菜单（右键 / hover 按钮触发）：引用回复 / 撤回 / 复制 / AI 智能回复
    openSmartReplyMenu(message, event) {
      if (!message) {
        return;
      }
      // 系统提示（成员进群 / 移出 / 角色变更）不提供撤回、引用、AI 回复等操作
      if (this.isSystemMessage(message)) {
        return;
      }
      // 撤回/引用/复制操作对任意消息可用（按条件显示），AI 智能回复仅对收到的文本消息
      this.showTranslateLangs = false;
      const cx = (event && event.clientX != null) ? event.clientX : 200;
      const cy = (event && event.clientY != null) ? event.clientY : 200;
      this.menuAnchor = { x: cx, y: cy };
      // 先挂出来，等真实高度出来再决定朝哪边展开
      this.smartReplyMenu = { show: true, message: message, x: cx, y: cy };
      this.smartReplyOptions = [];
      this.smartReplyLoading = false;
      this.$nextTick(() => this.positionActionMenu());
      // 仅对收到的文本消息自动请求 AI 建议
      if (this.isTextMessage(message) && message.senderId !== this.currentUser.id && message.message) {
        this.smartReplyLoading = true;
        this.fetchSmartReply();
      }
    },

    // 依据真实尺寸把菜单贴在鼠标旁：放不下就翻转，且绝不压住输入区
    positionActionMenu() {
      const anchor = this.menuAnchor;
      const el = this.$refs.actionMenu;
      if (!anchor || !el) {
        return;
      }
      const winW = window.innerWidth || 1200;
      const winH = window.innerHeight || 800;
      const gap = 8;
      const offset = 4;

      // 先量自然尺寸（清掉上一次的限高，否则会拿被截断的高度去算位置）
      el.style.maxHeight = 'none';
      const mw = el.offsetWidth || 250;
      const naturalH = el.offsetHeight || 320;

      // 可用底边：避开输入区，菜单不压住发送框
      const inputEl = document.querySelector('.input-container');
      let bottomLimit = winH;
      if (inputEl) {
        const ir = inputEl.getBoundingClientRect();
        if (ir.top > gap) {
          bottomLimit = Math.min(bottomLimit, ir.top);
        }
      }

      // 高度放不下就限高 + 内部滚动，而不是溢出被裁
      const maxH = Math.max(140, bottomLimit - gap * 2);
      if (naturalH > maxH) {
        el.style.maxHeight = maxH + 'px';
      }
      const mh = Math.min(naturalH, maxH);

      // 横向：贴着鼠标右侧展开，右侧放不下就翻到左侧
      let x = anchor.x + offset;
      if (x + mw + gap > winW) {
        x = anchor.x - mw - offset;
      }
      x = Math.max(gap, Math.min(x, winW - mw - gap));

      // 纵向：贴着鼠标下方展开，下方放不下就翻到上方；都放不下就夹进可用区
      let y = anchor.y + offset;
      if (y + mh + gap > bottomLimit) {
        const above = anchor.y - mh - offset;
        y = above >= gap ? above : Math.max(gap, bottomLimit - mh - gap);
      }
      y = Math.max(gap, Math.min(y, Math.max(gap, bottomLimit - mh - gap)));

      this.smartReplyMenu.x = x;
      this.smartReplyMenu.y = y;
    },

    // 关闭智能回复菜单
    closeSmartReplyMenu() {
      this.smartReplyMenu.show = false;
      this.smartReplyOptions = [];
      this.smartReplyLoading = false;
    },

    // ===== 消息撤回 / 引用回复 / 复制 =====
    // 是否可引用（有消息id且未被撤回）
    canQuoteMessage(message) {
      return !!(message && message.chatMessageId && message.withdrawn !== 1);
    },
    // 是否可撤回（本人发送、未被撤回、有消息id、且在 2 分钟时限内）
    canWithdrawMessage(message) {
      return !!(message && message.chatMessageId && message.withdrawn !== 1 && message.senderId === this.currentUser.id && this.isWithinWithdrawWindow(message));
    },
    // 编辑入口：仅自己发送 5 分钟内的文本消息
    canEditMessage(message) {
      return !!(message && message.chatMessageId && message.withdrawn !== 1
          && message.senderId === this.currentUser.id
          && Number(message.messageType) === 1
          && this.isWithinEditWindow(message));
    },
    isWithinEditWindow(message) {
      const created = this.parseMessageTime(message && message.createTime);
      if (!created) return true;
      return Date.now() - created <= 5 * 60 * 1000;
    },
    openEditDialog(message) {
      this.closeSmartReplyMenu && this.closeSmartReplyMenu();
      this.editDialog = { show: true, saving: false, message, draft: String(message.message || '') };
    },
    closeEditDialog() {
      if (this.editDialog.saving) return;
      this.editDialog = { show: false, saving: false, message: null, draft: '' };
    },
    async submitEditMessage() {
      const dialog = this.editDialog;
      if (!dialog.show || dialog.saving) return;
      const content = String(dialog.draft || '').trim();
      if (!content) {
        ElMessage.error('消息内容不能为空');
        return;
      }
      if (content === String(dialog.message.message || '')) {
        this.closeEditDialog();
        return;
      }
      dialog.saving = true;
      try {
        await editMessage({ messageId: dialog.message.chatMessageId, content });
        this.applyMessageEdit(dialog.message.chatMessageId, content, null);
        this.closeEditDialog();
        ElMessage.success('消息已更新');
      } catch (error) {
        ElMessage.error((error && error.message) || '编辑失败，请重试');
      } finally {
        if (this.editDialog.message === dialog.message) {
          this.editDialog.saving = false;
        }
      }
    },
    applyMessageEdit(messageId, content, editTime) {
      const target = (this.chatMessages || []).find(item => item.chatMessageId === messageId);
      if (!target) return;
      target.message = content;
      target.edited = 1;
      target.editTime = editTime || target.editTime || '';
    },
    // 收到别人/自己在其他端的编辑广播
    receiveMessageEdit(data) {
      if (!data || !data.chatMessageId) return;
      const chatId = data.chatType === 2 ? data.receiverId : data.senderId;
      if (!this.currentChatWindow || this.currentChatWindow.chatId !== chatId) return;
      this.applyMessageEdit(data.chatMessageId, data.message, data.createTime);
    },
    // 解析消息时间（兼容 "2026-11-06 14:34:03" 这类格式）
    parseMessageTime(value) {
      if (!value) return 0;
      if (typeof value === 'number') return value;
      const t = new Date(String(value).replace(/-/g, '/')).getTime();
      return isNaN(t) ? 0 : t;
    },
    // 撤回时限：2 分钟（与微信一致）；时间缺失时不拦截，避免误伤历史数据
    isWithinWithdrawWindow(message) {
      const t = this.parseMessageTime(message && message.createTime);
      if (!t) return true;
      return Date.now() - t <= 2 * 60 * 1000;
    },
    // 是否可复制（文本消息且未被撤回）
    canCopyMessage(message) {
      return !!(message && this.isTextMessage(message) && message.withdrawn !== 1 && message.message);
    },

    // 引用回复：设置回复目标并关闭菜单
    quoteMessage(message) {
      if (!this.canQuoteMessage(message)) {
        return;
      }
      this.replyTarget = {
        chatMessageId: message.chatMessageId,
        message: message.message || '[非文本消息]'
      };
      this.closeSmartReplyMenu();
      // 聚焦输入框
      this.$nextTick(() => {
        const ta = document.querySelector('.input-row textarea');
        if (ta) ta.focus();
      });
    },

    // 取消引用回复
    cancelReply() {
      this.replyTarget = null;
    },

    // 撤回消息：本地立即生效 + 发送 WS 撤回请求
    withdrawMessage(message) {
      if (!this.canWithdrawMessage(message)) {
        ElMessage.warning('只能撤回自己发送且未被撤回的消息');
        return;
      }
      if (!this.isWithinWithdrawWindow(message)) {
        ElMessage.warning('超过 2 分钟的消息不能撤回');
        return;
      }
      const req = {
        senderId: this.currentUser.id,
        receiverId: this.currentChatWindow.chatId,
        message: message.chatMessageId,
        chatType: this.currentChatWindow.chatType,
        messageType: 'chat',
        subType: 'mes_withdraw'
      };
      this.closeSmartReplyMenu();
      getWebSocketClient().sendMessage(JSON.stringify(req));
      // 本地立即置为撤回
      const idx = this.chatMessages.findIndex(m => m.chatMessageId === message.chatMessageId);
      if (idx !== -1) {
        this.chatMessages[idx].withdrawn = 1;
      }
      // 通知父组件同步左侧会话列表预览（仅当撤回的是该会话最后一条消息）
      const isLast = idx !== -1 && idx === this.chatMessages.length - 1;
      this.$emit('withdraw', {
        chatId: this.currentChatWindow.chatId,
        chatMessageId: message.chatMessageId,
        isLast: isLast,
        isSelf: true
      });
      ElMessage.success('已撤回');
    },

    // 复制消息文本
    copyMessage(message) {
      if (!this.canCopyMessage(message)) {
        return;
      }
      const text = typeof message.message === 'string' ? message.message : '';
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => {
          this.closeSmartReplyMenu();
          ElMessage.success('已复制');
        }).catch(() => {
          this.fallbackCopy(text);
        });
      } else {
        this.fallbackCopy(text);
      }
    },

    fallbackCopy(text) {
      try {
        const ta = document.createElement('textarea');
        ta.value = text;
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
        this.closeSmartReplyMenu();
        ElMessage.success('已复制');
      } catch (e) {
        ElMessage.error('复制失败');
      }
    },

    // 收到撤回通知：按 chatMessageId 将本地消息置为撤回
    receiveMessageWithdraw(data) {
      let payload = null;
      if (typeof data.message === 'string') {
        try {
          payload = JSON.parse(data.message);
        } catch (e) {
          payload = null;
        }
      } else if (data.message && typeof data.message === 'object') {
        payload = data.message;
      }
      if (!payload || !payload.chatMessageId) {
        return;
      }
      const idx = this.chatMessages.findIndex(m => m.chatMessageId === payload.chatMessageId);
      if (idx !== -1) {
        this.chatMessages[idx].withdrawn = 1;
        // 按本地消息的发送者判断是本人还是对方撤回（服务端会把本人的撤回广播回来，不能一律当对方）
        const local = this.chatMessages[idx];
        const isSelf = !!(local && this.currentUser && String(local.senderId) === String(this.currentUser.id));
        // 同步左侧会话列表预览
        this.$emit('withdraw', {
          chatId: payload.chatId || payload.receiverId || (this.currentChatWindow && this.currentChatWindow.chatId),
          chatMessageId: payload.chatMessageId,
          isLast: idx === this.chatMessages.length - 1,
          isSelf: isSelf
        });
      }
    },

    // ===== @ 提及 =====
    // 打开/关闭 @成员面板（点工具栏 @ 按钮：插入位置取当前末尾）
    toggleAtPanel() {
      if (!this.currentChatWindow || this.currentChatWindow.chatType !== 2) {
        return;
      }
      this.atPanelShow = !this.atPanelShow;
      this.atSearch = '';
      this.atInsertPos = this.atPanelShow ? String(this.newMessage || '').length : null;
      if (this.atPanelShow && this.atMembers.length === 0) {
        this.loadGroupMembers();
      }
      if (this.atPanelShow) {
        this.$nextTick(this.syncAtPanelBounds);
      }
    },

    // @ 面板：Teleport 到 body 后用视口坐标固定定位，避开玻璃合成层与输入区
    syncAtPanelBounds() {
      const host = document.querySelector('.chat-window') || this.$el;
      const btn = document.querySelector('.at-btn');
      if (!host || !btn || typeof host.getBoundingClientRect !== 'function') return;
      const rect = host.getBoundingClientRect();
      const btnRect = btn.getBoundingClientRect();
      if (!rect || rect.width <= 0 || rect.height <= 0) return;
      const maxLeft = Math.max(8, window.innerWidth - 248);
      const left = Math.min(Math.max(8, rect.left + 12), maxLeft);
      // 以面板底边锚定在 @ 按钮上方，避免成员列表异步渲染导致高度变化后压到输入区
      const bottom = Math.max(8, window.innerHeight - btnRect.top + 8);
      this.atPanelStyle = {
        position: 'fixed',
        left: `${left}px`,
        top: 'auto',
        bottom: `${bottom}px`,
        width: '240px'
      };
    },

    // 输入框里手动敲了 @：弹面板，并记住光标位置用于插入
    handleAtTrigger(payload) {
      if (!this.currentChatWindow || this.currentChatWindow.chatType !== 2) {
        return;
      }
      this.atInsertPos = payload && typeof payload.position === 'number' ? payload.position : String(this.newMessage || '').length;
      this.atSearch = '';
      if (this.atMembers.length === 0 && !this.atMemberLoading) {
        this.loadGroupMembers();
      }
      this.atPanelShow = true;
      this.$nextTick(this.syncAtPanelBounds);
    },

    // 加载群成员
    async loadGroupMembers() {
      const groupId = this.currentChatWindow.chatId;
      if (!groupId) {
        return;
      }
      this.atMemberLoading = true;
      try {
        const res = await getGroupMembers(groupId);
        if (res.code === 200) {
          this.atMembers = res.data || [];
        } else {
          this.atMembers = [];
        }
      } catch (e) {
        this.atMembers = [];
      } finally {
        this.atMemberLoading = false;
      }
    },

    // 选择 @ 成员：在光标处插入 @昵称 + 记录
    selectAtMember(member) {
      if (!member || member.id == null) {
        return;
      }
      // 避免重复@
      if (this.atUsers.some(u => u.id === member.id)) {
        this.atPanelShow = false;
        return;
      }
      const username = member.username || '用户';
      const suffix = `@${username} `;
      const text = String(this.newMessage || '');
      const pos = typeof this.atInsertPos === 'number'
          ? Math.max(0, Math.min(this.atInsertPos, text.length))
          : text.length;

      // 若触发位置前刚敲了一个孤立的 @（光标前只有 @ 和空白），把它替换成完整 @昵称
      const before = text.slice(0, pos);
      const lastAt = before.lastIndexOf('@');
      const between = lastAt >= 0 ? before.slice(lastAt + 1) : '';
      let newText;
      let caret;
      if (lastAt >= 0 && between.trim() === '') {
        const after = text.slice(pos).replace(/^\s+/, '');
        newText = text.slice(0, lastAt) + suffix + after;
        caret = lastAt + suffix.length;
      } else {
        newText = text.slice(0, pos) + suffix + text.slice(pos);
        caret = pos + suffix.length;
      }

      this.atUsers.push({ id: member.id, username });
      this.newMessage = newText;
      this.atPanelShow = false;
      this.atInsertPos = null;
      this.$nextTick(() => {
        if (this.$refs.chatInput && this.$refs.chatInput.focusAt) {
          this.$refs.chatInput.focusAt(caret);
        }
      });
    },

    // 取消已选 @ 成员：同步移除输入框中的 @片段
    removeAtUser(member) {
      if (!member) return;
      this.atUsers = this.atUsers.filter(u => u.id !== member.id);
      const name = String(member.username || '');
      if (!name) return;
      const esc = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      let text = String(this.newMessage || '');
      text = text.replace(new RegExp('@' + esc + '\\s*', 'g'), '');
      text = text.replace(/\s{2,}/g, ' ').trim();
      this.newMessage = text;
    },

    // 解析消息上的 atUsers（可能是 JSON 字符串、数组或空串），统一成字符串数组
    parseAtUsers(value) {
      if (!value) return [];
      if (Array.isArray(value)) return value.map(v => String(v));
      if (typeof value !== 'string') return [];
      const raw = value.trim();
      if (!raw) return [];
      try {
        const arr = JSON.parse(raw);
        return Array.isArray(arr) ? arr.map(v => String(v)) : [];
      } catch (e) {
        return [];
      }
    },

    // 这条消息是否 @ 了我（自己发的不算）
    isMentionMe(message) {
      if (!message || !this.currentUser) return false;
      if (String(message.senderId) === String(this.currentUser.id)) return false;
      const ids = this.parseAtUsers(message.atUsers);
      return ids.indexOf(String(this.currentUser.id)) !== -1;
    },

    // 点击引用块：滚到被引用的原消息并短暂高亮
    scrollToQuotedMessage(replyToId) {
      if (!replyToId) return;
      const el = document.querySelector('.message[data-message-id="' + String(replyToId) + '"]');
      if (!el) {
        ElMessage.info('原消息不在当前会话中');
        return;
      }
      el.scrollIntoView({ block: 'center', behavior: 'smooth' });
      el.classList.add('quote-flash');
      setTimeout(() => el.classList.remove('quote-flash'), 1000);
    },

    // 渲染消息文本：转义 + @ 提及高亮
    // 群聊发送者展示名：AI 助手不是真实用户，历史接口查不到资料时给出固定名称
    senderLabel(message) {
      if (!message) return '未知用户';
      const name = message.sender && message.sender.username;
      if (name) return name;
      const aiNames = {
        '243400000000000001': 'AI 小瓜',
        '243400000000000002': 'AI 老学究'
      };
      return aiNames[message.senderId] || '未知用户';
    },

    formatMessageText(message) {
      if (!message || typeof message.message !== 'string') {
        return '';
      }
      let text = message.message;
      // HTML 转义
      text = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
      // 只有真正携带了 @ 用户（非空数组）时才高亮；空数组 '[]' 不算
      if (this.parseAtUsers(message.atUsers).length > 0) {
        text = text.replace(/@([\u4e00-\u9fa5A-Za-z0-9_\-]+)/g, '<span class="mention">@$1</span>');
      }
      return text;
    },

    // 请求生成智能回复建议
    fetchSmartReply() {
      const message = this.smartReplyMenu.message;
      if (!message || !message.message) {
        return;
      }
      this.smartReplyLoading = true;
      smartReply({ message: message.message, scene: this.currentChatWindow.chatType === 2 ? 'group' : 'single' })
          .then(res => {
            if (res.code === 200 && res.data && res.data.length > 0) {
              this.smartReplyOptions = res.data.slice(0, 3);
            } else {
              this.smartReplyOptions = [];
            }
            this.smartReplyLoading = false;
            // 建议列表出现后菜单变高，重新校一次位置
            this.$nextTick(() => this.positionActionMenu());
          })
          .catch(() => {
            this.smartReplyOptions = [];
            this.smartReplyLoading = false;
            this.$nextTick(() => this.positionActionMenu());
          });
    },

    // 使用某条智能回复并直接发送
    // ===== 消息转发 =====
    canForwardMessage(message) {
      if (!message) return false;
      return !!(message.message || '').trim() && message.messageType !== 'notify';
    },
    async openForwardDialog(message) {
      const text = (message && message.message) ? String(message.message).trim() : '';
      if (!text) return;
      this.closeSmartReplyMenu();
      this.forwardMode = 'single';
      this.forwardPayload = null;
      this.forwardDialog = { show: true, text, list: [], loading: true };
      try {
        const res = await getLastMes({ pageNum: 1, pageSize: 20 });
        const raw = res && res.data;
        const rows = Array.isArray(raw) ? raw : (raw && (raw.rows || raw.list || raw.records)) || [];
        this.forwardDialog.list = rows.map(row => ({
          chatId: row.chatId || row.receiverId || row.senderId,
          chatType: row.chatType != null ? row.chatType : 2,
          name: row.receiverName || row.senderName || row.chatName || row.username || '会话',
          avatar: row.receiverAvatar || row.senderAvatar || row.avatar || '',
        })).filter(item => item.chatId)
          // 不把当前会话列进转发目标，避免误把消息再发回原会话
          .filter(item => !this.currentChatWindow || String(item.chatId) !== String(this.currentChatWindow.chatId));
      } catch (e) {
        this.forwardDialog.list = [];
      } finally {
        this.forwardDialog.loading = false;
      }
    },
    closeForwardDialog() {
      this.forwardDialog.show = false;
    },
    waitForSendAck(message, timeout = 4000) {
      if (!message) return Promise.resolve(false);
      return new Promise((resolve) => {
        const entry = {
          content: message.message,
          createTime: message.createTime,
          resolve: null
        };
        const timer = setTimeout(() => {
          const index = this.pendingForwardAcks.indexOf(entry);
          if (index > -1) this.pendingForwardAcks.splice(index, 1);
          resolve(false);
        }, timeout);
        entry.resolve = (ok) => {
          clearTimeout(timer);
          resolve(ok);
        };
        this.pendingForwardAcks.push(entry);
      });
    },
    async doForward(target) {
      if (!target || !target.chatId || this.forwardDialog.loading) return;
      let content = this.forwardDialog.text;
      let contentType = 1;
      if (this.forwardMode === 'merge' && this.forwardPayload) {
        // 前缀保证会话列表预览可读，卡片渲染时剥掉前缀再解析
        content = '【聊天记录】' + JSON.stringify(this.forwardPayload);
        contentType = 7;
      }
      if (!content) return;
      this.forwardDialog.loading = true;
      // 转发是独立的新消息，不能继承当前输入框里的引用/ @ 元数据
      const sent = this.sendMessage(content, contentType, { chatId: target.chatId, chatType: target.chatType }, {
        clearReply: true,
        clearAt: true
      });
      const ok = await this.waitForSendAck(sent, 4000);
      this.forwardDialog.loading = false;
      if (!ok) {
        // 未收到服务端 ack 时不关闭弹层，让用户可以重试；也不谎报成功
        this.$emit('refresh-conversation-list');
        ElMessage.error('转发失败，未收到服务端确认，请重试');
        return;
      }
      this.forwardDialog.show = false;
      this.exitSelectionMode();
      this.forwardMode = 'single';
      this.forwardPayload = null;
      // 发给其他会话时，侧栏的最后一条消息和排序需要同步刷新
      this.$emit('refresh-conversation-list');
      ElMessage.success('已转发给「' + target.name + '」');
    },

    // ===== 消息翻译 =====
    messageKeyOf(message) {
      if (!message) return '';
      return String(message.chatMessageId || (message.senderId + '_' + message.createTime));
    },

    translationOf(message) {
      const key = this.messageKeyOf(message);
      return key && this.translations[key] ? this.translations[key] : null;
    },

    toggleTranslateLangs() {
      if (!this.isTextMessage(this.smartReplyMenu.message)) {
        ElMessage.warning('仅文本消息支持翻译');
        return;
      }
      this.showTranslateLangs = !this.showTranslateLangs;
    },

    async doTranslate(lang) {
      const message = this.smartReplyMenu.message;
      if (!message || !(message.message || '').trim()) return;
      const key = this.messageKeyOf(message);
      this.showTranslateLangs = false;
      await this.requestTranslate(message, key, lang);
      this.closeSmartReplyMenu();
    },

    async requestTranslate(message, key, lang) {
      const labels = { zh: '中文', en: 'English', ja: '日本語' };
      const text = (message.message || '').trim();
      if (!text) {
        this.translations = { ...this.translations, [key]: { label: labels[lang] || '译文', text: '这条消息没有可翻译的文本', lang, error: true } };
        return;
      }
      if (text.length > 2000) {
        this.translations = { ...this.translations, [key]: { label: labels[lang] || '译文', text: '消息超过 2000 字，暂不支持翻译', lang, error: true } };
        return;
      }
      const current = this.translations[key];
      if (current && current.loading && current.lang === lang) return;
      this.translations = { ...this.translations, [key]: { label: labels[lang] || '译文', text: '翻译中…', lang, loading: true, error: false } };
      try {
        const res = await translateMessage({ text, target: lang });
        if (this.translations[key] && this.translations[key].lang !== lang) return;
        if (res && res.code === 200 && res.data) {
          this.translations = { ...this.translations, [key]: { label: labels[lang], text: res.data, lang, loading: false, error: false } };
        } else {
          this.translations = { ...this.translations, [key]: { label: labels[lang], text: (res && res.msg) || '翻译失败，请重试', lang, loading: false, error: true } };
        }
      } catch (e) {
        this.translations = { ...this.translations, [key]: { label: labels[lang], text: '翻译失败，请重试', lang, loading: false, error: true } };
      }
    },

    retryTranslate(message) {
      const key = this.messageKeyOf(message);
      const cur = this.translations[key];
      if (!cur) return;
      this.requestTranslate(message, key, cur.lang || 'en');
    },

    hideTranslation(message) {
      const key = this.messageKeyOf(message);
      if (!key) return;
      const next = { ...this.translations };
      delete next[key];
      this.translations = next;
    },

    copyTranslation(text) {
      const value = String(text || '');
      if (!value) return;
      this.copyByTextarea(value, () => ElMessage.success('译文已复制'));
    },

    copyByTextarea(value, done) {
      const ta = document.createElement('textarea');
      ta.value = value;
      ta.style.position = 'fixed';
      ta.style.opacity = '0';
      document.body.appendChild(ta);
      ta.select();
      let ok = false;
      try { ok = document.execCommand('copy'); } catch (e) { ok = false; }
      document.body.removeChild(ta);
      if (ok) {
        if (done) done();
      } else {
        ElMessage.warning('复制失败，请手动选择译文');
      }
    },

    // ===== 多选与合并转发 =====
    onMessageRowClick(message) {
      if (!this.selectionMode || this.isSystemMessage(message)) return;
      if (this.isMessageSelected(message)) {
        this.unselectMessage(message);
        return;
      }
      if (this.selectedMessages.length >= this.selectionLimit) {
        ElMessage.warning(`一次最多合并 ${this.selectionLimit} 条消息`);
        return;
      }
      this.selectedMessages.push(message);
    },

    isMessageSelected(message) {
      const key = this.messageKeyOf(message);
      return this.selectedMessages.some(m => this.messageKeyOf(m) === key);
    },

    unselectMessage(message) {
      const key = this.messageKeyOf(message);
      this.selectedMessages = this.selectedMessages.filter(m => this.messageKeyOf(m) !== key);
    },

    enterSelectionMode() {
      this.closeSmartReplyMenu();
      this.selectionMode = true;
      this.selectedMessages = [];
      ElMessage.success('已进入多选，点击消息勾选');
    },

    exitSelectionMode() {
      this.selectionMode = false;
      this.selectedMessages = [];
    },

    async forwardSelected() {
      if (!this.selectedMessages.length) return;
      const msgs = this.selectedMessages.slice().reverse(); // 按时间正序
      const payload = msgs.slice(0, this.selectionLimit).map(m => ({
        s: m.senderId === this.currentUser.id ? (this.currentUser.username || '我') : ((m.sender && m.sender.username) || m.chatName || '对方'),
        c: this.mergeTextOf(m),
        t: m.createTime || '',
      }));
      this.forwardMode = 'merge';
      this.forwardPayload = payload;
      this.forwardDialog = { show: true, text: '聊天记录（' + payload.length + ' 条）', list: [], loading: true };
      try {
        const res = await getLastMes({ pageNum: 1, pageSize: 20 });
        const raw = res && res.data;
        const rows = Array.isArray(raw) ? raw : (raw && (raw.rows || raw.list || raw.records)) || [];
        this.forwardDialog.list = rows.map(row => ({
          chatId: row.chatId || row.receiverId || row.senderId,
          chatType: row.chatType != null ? row.chatType : 2,
          name: row.receiverName || row.senderName || row.chatName || row.username || '会话',
          avatar: row.receiverAvatar || row.senderAvatar || row.avatar || '',
        })).filter(item => item.chatId)
          // 不把当前会话列进转发目标，避免误把消息再发回原会话
          .filter(item => !this.currentChatWindow || String(item.chatId) !== String(this.currentChatWindow.chatId));
      } catch (e) {
        this.forwardDialog.list = [];
      } finally {
        this.forwardDialog.loading = false;
      }
    },

    mergeTextOf(message) {
      const type = message.contentType || message.messageType;
      if (type === 2) return '[图片]';
      if (type === 3) return '[语音]';
      if (type === 4) return '[视频]';
      if (type === 5) return '[文件] ' + (message.fileName || '');
      if (type === 9 || this.isLegacyCallRecord(message)) return '[视频通话] ' + this.callRecordText(message);
      if (type === 7) return '[聊天记录]';
      if (type === 8) return '[投票] ' + (this.votePayload(message).title || '');
      return String(message.message || '').slice(0, 200);
    },

    parseMerged(message) {
      try {
        let raw = String(message.message || '[]');
        if (raw.indexOf('【聊天记录】') === 0) raw = raw.slice('【聊天记录】'.length);
        const arr = JSON.parse(raw);
        return Array.isArray(arr) ? arr : [];
      } catch (e) {
        return [];
      }
    },

    mergeCount(message) {
      return this.parseMerged(message).length;
    },

    mergePreview(message) {
      return this.parseMerged(message).slice(0, 3);
    },

    openMergeViewer(message) {
      this.mergeViewer = { show: true, items: this.parseMerged(message) };
    },

    closeMergeViewer() {
      this.mergeViewer.show = false;
    },

    // Esc：按「菜单 → 弹层 → 多选」的层级逐层退出
    handleGlobalKeydown(e) {
      if (e.key !== 'Escape') return;
      if (this.smartReplyMenu.show) { this.closeSmartReplyMenu(); return; }
      if (this.mergeViewer.show) { this.closeMergeViewer(); return; }
      if (this.voteDialog.show) { this.closeVoteDialog(); return; }
      if (this.forwardDialog.show) { this.closeForwardDialog(); return; }
      if (this.selectionMode) { this.exitSelectionMode(); }
    },

    // 旧版通话记录曾占用文案 6，仅当正文确为视频记录 JSON 时才认作通话，避免误伤群系统提示
    isLegacyCallRecord(message) {
      if (!message) return false;
      if (message.contentType !== 6 && message.messageType !== 6) return false;
      return this.callRecordPayload(message).kind === 'video';
    },

    // ===== 视频通话记录 =====
    callRecordPayload(message) {
      try {
        return JSON.parse(String((message && message.message) || '{}')) || {};
      } catch (e) {
        return {};
      }
    },

    callRecordText(message) {
      const payload = this.callRecordPayload(message);
      const outcome = payload.outcome || 'failed';
      const isSelf = String(message.senderId) === String(this.currentUser.id);
      if (outcome === 'completed') return '通话结束';
      if (outcome === 'rejected') return isSelf ? '对方已拒绝' : '已拒绝';
      if (outcome === 'busy') return isSelf ? '对方忙线中' : '忙线未接';
      if (outcome === 'timeout') return isSelf ? '对方无应答' : '未接来电';
      if (outcome === 'canceled') return isSelf ? '已取消' : '对方已取消';
      return '未接通';
    },

    callRecordDuration(message) {
      const payload = this.callRecordPayload(message);
      const seconds = Number(payload.duration) || 0;
      if (payload.outcome !== 'completed' || seconds <= 0) return '';
      const min = String(Math.floor(seconds / 60)).padStart(2, '0');
      const sec = String(seconds % 60).padStart(2, '0');
      return `${min}:${sec}`;
    },

    // ===== 群投票 =====
    isGroupChat() {
      const chat = this.currentChatWindow || {};
      if (Number(chat.chatType) === 2) return true;
      return (this.chatMessages || []).some(m => Number(m.chatType) === 2);
    },

    votePayload(message) {
      const raw = String(message.message || '');
      if (raw.indexOf('【投票】') === 0) {
        const body = raw.slice('【投票】'.length);
        const sep = body.lastIndexOf('||');
        if (sep > 0) {
          return { voteId: body.slice(sep + 2).trim(), title: body.slice(0, sep) || '群投票' };
        }
        return { voteId: null, title: body || '群投票' };
      }
      try {
        const data = JSON.parse(raw || '{}');
        return { voteId: data.voteId, title: data.title || '群投票' };
      } catch (e) {
        return { voteId: null, title: '群投票' };
      }
    },

    async ensureVote(voteId) {
      if (!voteId || this.voteDetails[voteId] || this.voteLoading[voteId]) return;
      this.voteLoading = { ...this.voteLoading, [voteId]: true };
      try {
        const res = await getVoteDetail(voteId);
        if (res && res.code === 200 && res.data) {
          this.voteDetails = { ...this.voteDetails, [voteId]: res.data };
        }
      } catch (e) {
        // 静默失败，点击可重试
      } finally {
        this.voteLoading = { ...this.voteLoading, [voteId]: false };
      }
    },

    isMyVote(detail, index) {
      return !!(detail && Array.isArray(detail.myOptions) && detail.myOptions.indexOf(index) >= 0);
    },

    myVoteCount(detail) {
      return detail && Array.isArray(detail.myOptions) ? detail.myOptions.length : 0;
    },

    // 取消投票：调用专用接口清空本人全部选择，避免用“再点同项”触发撤票
    async cancelVote(voteId) {
      const detail = this.voteDetails[voteId];
      if (!detail || detail.status === 1) return;
      if (!this.myVoteCount(detail)) return;
      try {
        const res = await cancelVoteApi(voteId);
        if (res && res.code === 200 && res.data) {
          this.voteDetails = { ...this.voteDetails, [voteId]: res.data };
          ElMessage.success(detail.multi ? '已清空选择' : '已取消投票');
        } else {
          ElMessage.warning((res && res.msg) || '取消投票失败');
        }
      } catch (e) {
        ElMessage.warning('取消投票失败，请重试');
      }
    },

    async doVote(voteId, index) {
      if (!voteId || index == null) return;
      if (this.votePending[voteId]) return; // 同一投票的上一次提交还没回来，忽略连点
      const detail = this.voteDetails[voteId];
      if (detail && detail.status === 1) {
        ElMessage.warning('投票已结束');
        return;
      }
      // 单选：已投同一项时不做就地撤销，避免手滑掉票；改投请点其他选项，取消请用底栏入口
      if (detail && !detail.multi && this.isMyVote(detail, index)) {
        ElMessage.info('已投该选项，点击其他选项可改投');
        return;
      }
      this.votePending = { ...this.votePending, [voteId]: true };
      try {
        const res = await submitVote({ voteId, optionIndex: index });
        if (res && res.code === 200 && res.data) {
          this.voteDetails = { ...this.voteDetails, [voteId]: res.data };
        } else {
          ElMessage.warning((res && res.msg) || '投票失败');
        }
      } catch (e) {
        ElMessage.warning('投票失败，请重试');
      } finally {
        this.votePending = { ...this.votePending, [voteId]: false };
      }
    },

    confirmCloseVote(voteId) {
      if (!voteId) return;
      if (this.voteConfirmId !== voteId) {
        this.voteConfirmId = voteId;
        setTimeout(() => {
          if (this.voteConfirmId === voteId) this.voteConfirmId = null;
        }, 3000);
        return;
      }
      this.voteConfirmId = null;
      this.closeVoteById(voteId);
    },

    async closeVoteById(voteId) {
      try {
        const res = await closeVote(voteId);
        if (res && res.code === 200) {
          const detail = await getVoteDetail(voteId);
          if (detail && detail.code === 200 && detail.data) {
            this.voteDetails = { ...this.voteDetails, [voteId]: detail.data };
          }
          ElMessage.success('投票已结束');
        } else {
          ElMessage.warning((res && res.msg) || '结束投票失败');
        }
      } catch (e) {
        ElMessage.warning('结束投票失败，请重试');
      }
    },

    openVoteDialog() {
      this.voteDialog = { show: true, title: '', options: ['', ''], multi: false, sending: false };
    },

    closeVoteDialog() {
      this.voteDialog.show = false;
    },

    addVoteOption() {
      if (this.voteDialog.options.length >= 10) return;
      this.voteDialog.options.push('');
    },

    removeVoteOption(index) {
      if (this.voteDialog.options.length <= 2) return;
      this.voteDialog.options.splice(index, 1);
    },

    async submitCreateVote() {
      const title = (this.voteDialog.title || '').trim();
      const options = (this.voteDialog.options || []).map(o => (o || '').trim()).filter(Boolean);
      if (!title) {
        ElMessage.warning('请填写投票标题');
        return;
      }
      if (options.length < 2) {
        ElMessage.warning('至少填写两个选项');
        return;
      }
      const seen = new Set();
      for (const opt of options) {
        const key = opt.toLowerCase();
        if (seen.has(key)) {
          ElMessage.warning('选项「' + opt + '」重复了');
          return;
        }
        seen.add(key);
      }
      const groupId = (this.currentChatWindow || {}).chatId || (this.currentChatWindow || {}).groupId;
      if (!groupId) {
        ElMessage.warning('未找到群聊信息');
        return;
      }
      this.voteDialog.sending = true;
      try {
        const res = await createVote({ groupId, title, options, multi: this.voteDialog.multi });
        if (res && res.code === 200 && res.data) {
          const vote = res.data;
          this.voteDetails = { ...this.voteDetails, [vote.voteId]: vote };
          this.sendMessage('【投票】' + title + '||' + vote.voteId, 8);
          this.closeVoteDialog();
          ElMessage.success('投票已发起');
        } else {
          ElMessage.warning((res && res.msg) || '发起投票失败');
        }
      } catch (e) {
        ElMessage.warning('发起投票失败，请重试');
      } finally {
        this.voteDialog.sending = false;
      }
    },

    sendSmartReply(text) {
      if (!text) {
        return;
      }
      const reply = text.trim();
      this.closeSmartReplyMenu();
      this.sendMessage(reply, 1);
      ElMessage.success('已使用 AI 智能回复发送');
    },

    // ===== AI 摘要与待办 =====
    // 群聊走服务端：由后端拉最近 50 条完整历史，不受本地分页只加载了 20 条的限制
    openSummaryDialog() {
      if (this.summaryLoading) {
        return;
      }
      const chatType = Number((this.currentChatWindow && this.currentChatWindow.chatType) || 2);
      if (chatType === 2) {
        const groupId = this.currentChatWindow && this.currentChatWindow.chatId;
        if (!groupId) {
          this.summaryDialog = { show: true, text: '', todos: [], messageCount: 0, generatedAt: '' };
          return;
        }
        this.summaryDialog = { show: true, text: '', todos: [], messageCount: 0, generatedAt: '' };
        this.summaryLoading = true;
        getGroupAiSummary(groupId)
            .then(res => {
              if (!res || res.code !== 200 || !res.data) {
                throw new Error((res && (res.msg || res.message)) || '摘要生成失败');
              }
              const data = res.data;
              this.summaryDialog = {
                show: true,
                text: data.summary || 'AI 没有生成有效摘要，请重试。',
                todos: Array.isArray(data.todos) ? data.todos : [],
                messageCount: data.messageCount || 0,
                generatedAt: data.generatedAt || ''
              };
            })
            .catch(error => {
              this.summaryDialog = {
                show: true,
                text: (error && (error.message || error.msg)) || '摘要生成失败，请检查网络后重试',
                todos: [],
                messageCount: 0,
                generatedAt: ''
              };
            })
            .finally(() => {
              this.summaryLoading = false;
            });
        return;
      }

      // 单聊/机器人保留原来的即时摘要：只取当前窗口已加载的消息
      const msgs = (this.chatMessages || [])
          .filter(m => m.message && typeof m.message === 'string' && Number(m.contentType || 1) === 1 && !m.withdrawn)
          .slice(-50)
          .map(m => ({
            name: this.senderLabel(m),
            content: m.message
          }));
      if (!msgs.length) {
        this.summaryDialog = { show: true, text: '', todos: [], messageCount: 0, generatedAt: '' };
        return;
      }
      this.summaryDialog = { show: true, text: '', todos: [], messageCount: 0, generatedAt: '' };
      this.summaryLoading = true;
      chatSummary({
        chatType,
        messages: msgs
      })
          .then(res => {
            this.summaryDialog = {
              show: true,
              text: (res.code === 200 && res.data) ? res.data : '摘要生成失败，请稍后重试',
              todos: [],
              messageCount: 0,
              generatedAt: ''
            };
          })
          .catch(() => {
            this.summaryDialog = {
              show: true,
              text: '摘要生成失败，请检查网络后重试',
              todos: [],
              messageCount: 0,
              generatedAt: ''
            };
          })
          .finally(() => {
            this.summaryLoading = false;
          });
    },
    closeSummaryDialog() {
      this.summaryDialog.show = false;
    },

    // ===== 定时消息 =====
    // 打开定时发送对话框
    openTimedDialog() {
      this.timedContent = this.newMessage || '';
      this.timedSendAt = '';
      this.showTimedDialog = true;
    },

    // 快捷设置定时时间（分钟后）
    setQuickTimed(minutes) {
      const t = new Date(Date.now() + minutes * 60 * 1000);
      const pad = (n) => String(n).padStart(2, '0');
      this.timedSendAt = `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())}T${pad(t.getHours())}:${pad(t.getMinutes())}`;
    },

    // 创建定时消息（落库到服务端，到点由服务端调度发送）
    async createTimedMessage() {
      const content = (this.timedContent || '').trim();
      if (!content) {
        ElMessage.warning('请填写要定时发送的内容');
        return;
      }
      const sendAt = this.timedSendAt;
      if (!sendAt) {
        ElMessage.warning('请选择发送时间');
        return;
      }
      const sendTs = new Date(sendAt).getTime();
      if (isNaN(sendTs) || sendTs <= Date.now()) {
        ElMessage.warning('发送时间必须晚于当前时间');
        return;
      }
      if (!this.currentUser || !this.currentUser.id || !this.currentChatWindow) {
        ElMessage.warning('当前会话信息不完整，无法预约');
        return;
      }
      // datetime-local 的值形如 2026-11-06T18:30，补上秒
      const sendAtText = sendAt.replace('T', ' ') + ':00';
      let saved;
      try {
        const res = await createTimedMessageApi({
          senderId: this.currentUser.id,
          receiverId: this.currentChatWindow.chatId,
          chatType: this.currentChatWindow.chatType,
          content: content,
          sendAt: sendAtText
        });
        saved = res.data;
      } catch (e) {
        // 请求拦截器已提示错误
        return;
      }
      this.timedMessages.push({
        id: saved.id,
        content: saved.content,
        chatId: saved.receiverId,
        chatType: saved.chatType,
        sendAt: new Date(sendAt.replace('T', ' ').replace(/-/g, '/')).getTime(),
        createTime: Date.now()
      });
      this.showTimedDialog = false;
      // 若内容来自输入框，发送后清空输入框
      if (this.newMessage && content === this.newMessage.trim()) {
        this.newMessage = '';
      }
      ElMessage.success(`已预约 ${this.formatTimedTime(sendTs)} 自动发送`);
    },

    // 取消定时消息（服务端同步取消）
    async cancelTimedMessage(id) {
      if (!this.currentUser || !this.currentUser.id) {
        return;
      }
      try {
        await cancelTimedMessageApi({ id: id, senderId: this.currentUser.id });
      } catch (e) {
        return;
      }
      this.timedMessages = this.timedMessages.filter(tm => tm.id !== id);
      ElMessage.info('已取消定时消息');
    },

    // 定时发送已迁移到服务端调度，此方法保留兼容旧调用
    checkTimedMessages() {
      this.loadTimedMessages();
    },

    // 从服务端加载待发送的定时消息
    async loadTimedMessages() {
      if (!this.currentUser || !this.currentUser.id) {
        return;
      }
      // 首次加载时，把旧版 localStorage 里遗留的任务迁移到服务端
      await this.migrateLocalTimedMessages();
      try {
        const res = await listTimedMessagesApi({ senderId: this.currentUser.id });
        const list = res.data || [];
        this.timedMessages = list.map(t => ({
          id: t.id,
          content: t.content,
          chatId: t.receiverId,
          chatType: t.chatType,
          sendAt: new Date(String(t.sendAt).replace(/-/g, '/')).getTime(),
          createTime: Date.now()
        }));
      } catch (e) {
        // 请求失败时保留现有列表，避免界面闪烁
      }
    },

    // 把旧版本浏览器本地保存的待发任务迁移到服务端，成功后清空本地
    async migrateLocalTimedMessages() {
      try {
        const raw = localStorage.getItem('xigua_timed_messages');
        if (!raw) {
          return;
        }
        const list = JSON.parse(raw) || [];
        const pending = list.filter(tm => tm.sendAt > Date.now());
        for (const tm of pending) {
          try {
            await createTimedMessageApi({
              senderId: this.currentUser.id,
              receiverId: tm.chatId,
              chatType: tm.chatType,
              content: tm.content,
              sendAt: this.formatSendAtText(tm.sendAt)
            });
          } catch (e) {
            // 单条迁移失败忽略，不阻塞列表加载
          }
        }
        localStorage.removeItem('xigua_timed_messages');
      } catch (e) {
        // 本地数据损坏时忽略
      }
    },

    // 把毫秒时间戳转成 yyyy-MM-dd HH:mm:ss
    formatSendAtText(ts) {
      const d = new Date(ts);
      const pad = (n) => String(n).padStart(2, '0');
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:00`;
    },

    // 持久化已迁移到服务端，保留空实现兼容旧调用
    persistTimedMessages() {
      // no-op
    },

    // 格式化定时时间显示
    formatTimedTime(ts) {
      const d = new Date(ts);
      const pad = (n) => String(n).padStart(2, '0');
      return `${d.getMonth() + 1}月${d.getDate()}日 ${pad(d.getHours())}:${pad(d.getMinutes())}`;
    },

    // 接收服务端ack的定时器
    receiveAckTimer(message){
      // 发送消息后，如果1.5秒后没接收到服务端回传的消息id，提示消息发送失败
      // 因为没有接收到服务端的ack，所以服务端没有将这条消息持久化，虽然页面可以重新发送，那是浏览器内存，随着浏览器刷新，消息就丢失了
      // 微信将消息存到本地，关闭微信或再次打开微信仍然有发送失败的消息，服务端可能也没接收到消息，目标人也是看不到消息的
      setTimeout(() => {
        const target = (message.clientMsgId
          && this.chatMessages.find(m => m.clientMsgId === message.clientMsgId))
          || this.chatMessages.find(m => m.message === message.message && m.createTime == message.createTime)
        if(!target || target.chatMessageId == null || target.chatMessageId == ''){
          if (target) target.sendStatus = 'failed';
          ElMessage({ message: '消息发送失败', type: 'error', plain: true})
        }
      }, 1500)
    },

    // 客户端消息唯一键：重发时复用，服务端据此幂等
    genClientMsgId() {
      return 'pc-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 10);
    },

    // 格式化时间戳
    formatDate(timestamp) {
      if (timestamp == null || timestamp == '') {
        return '';
      }
      const date = new Date(timestamp);

      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');

      const formattedDate = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
      return formattedDate;
    },

    // 处理键盘输入
    handleKeyDown(event) {
      if(event.key !== 'Enter') {
        return;
      }

      // 校验消息
      const checkFlag = this.checkMessage();
      if(!checkFlag){
        event.preventDefault();
        return;
      }

      // Shift+Enter 或 Ctrl+Enter 换行 - 允许默认行为
      if (event.shiftKey || event.ctrlKey) {
        return; // 让浏览器执行默认的换行行为
      }

      // 普通Enter键发送消息
      event.preventDefault(); // 阻止默认换行行为

      // 只有当有内容时才发送
      if (this.newMessage.trim()) {
        this.sendMessage();
      }
    },

    // 滚动事件处理
    friendScroll(event) {
      // todo 按滚动做分页查询
    },

    // 清空历史消息所需要的参数
    clearHistoryMes(){
      this.historyRequestSeq++;
      this.chatMessages = [];
      this.systemMessages = [];
      this.historyPageNum = 1;
      this.historyPageSize = 20;
      this.hasMoreHistory = false;
      this.loadingHistory = false;
      this.isScrollingUp = false;
      this.loadedPages = new Set();
      this.visibleUnReadMessages = new Set();
    },

    addEmoji(emoji) {
      this.newMessage += emoji.i; // 在输入框中添加表情
      this.showEmojiPicker = false; // 选择后关闭表情框
    },

    toggleEmojiPicker() {
      this.showEmojiPicker = !this.showEmojiPicker;
    },

    // 接收消息已读通知
    receiveMessageReadNotify(data){
      const receiverId = data.receiverId;
      let message = {};
      try {
        message = JSON.parse(data.message) || {};
      } catch (e) {
        return;
      }
      let readChatMessageIds = [];
      try {
        readChatMessageIds = JSON.parse(message.readChatMessageIds) || [];
      } catch (e) {
        return;
      }
      if(this.currentUser.id != receiverId){
        return;
      }
      if(!Array.isArray(readChatMessageIds) || readChatMessageIds.length === 0){
        return;
      }

      // 群聊：按「谁读了」逐条累计，同一人重复上报只算一次
      const readerId = message.readerId ? String(message.readerId) : '';
      if(readerId && readerId !== String(this.currentUser.id)){
        const readerName = message.readerName || '有人';
        const total = Number(message.groupMemberCount) || 0;
        const next = { ...this.groupReaders };
        readChatMessageIds.forEach(id => {
          const key = String(id);
          const cur = next[key] || { readers: {}, total: total };
          next[key] = {
            readers: { ...cur.readers, [readerId]: readerName },
            total: total || cur.total
          };
        });
        this.groupReaders = next;
      }

      // 单聊：直接把消息标记为已读（双勾变蓝）
      this.chatMessages.forEach(message => {
        if(readChatMessageIds.includes(message.chatMessageId)){
          message.isRead = true;
        }
      })
    },

    // 群聊已读：某条消息的已读人昵称列表
    groupReadNames(msgId){
      const item = this.groupReaders[msgId];
      if(!item || !item.readers){
        return [];
      }
      return Object.keys(item.readers).map(k => item.readers[k]);
    },

    // 群聊已读：该消息应读的人数（群成员数，不含自己）
    groupReadTotal(msgId){
      const item = this.groupReaders[msgId];
      return item ? item.total : 0;
    },

    // 展开/收起已读名单
    toggleGroupReaders(msgId){
      this.activeGroupReadId = this.activeGroupReadId === msgId ? '' : msgId;
      if(this.activeGroupReadId){
        // 展开后把消息顶到可视区，避免被输入框挡住
        this.$nextTick(() => this.scrollToBottom());
      }
    },

    // 点击页面其他位置收起名单 / 消息操作菜单
    closeGroupReaders(e){
      if(this.activeGroupReadId){
        this.activeGroupReadId = '';
      }
      if (this.smartReplyMenu && this.smartReplyMenu.show) {
        const menu = this.$refs.actionMenu;
        if (!menu || !menu.contains(e && e.target)) {
          this.closeSmartReplyMenu();
        }
      }
    },

    // 获取可视区域的消息
    getVisibleMessages(){
      const container = this.$refs.messagesContainer;
      if (!container) return;

      // 计算可视区域的消息范围
      const messages = container.getElementsByClassName('message');

      if (!messages || messages.length === 0) {
        return;
      }

      // 获取容器的可视区域边界
      this.chatContainerRect = container.getBoundingClientRect();
      this.chatContainerTop = this.chatContainerRect.top;
      this.chatContainerBottom = this.chatContainerTop + container.clientHeight;

      // 检查每条消息是否在可视区域内
      Array.from(messages).forEach((mes) => {
        const msgRect = mes.getBoundingClientRect();
        const msgTop = msgRect.top;
        const msgBottom = msgRect.bottom;

        // 判断消息是否至少有一部分在可视区域内
        if (msgBottom > this.chatContainerTop && msgTop < this.chatContainerBottom) {
            const message = JSON.parse(mes.getAttribute('data-message'));

            // 如果消息是当前聊天窗口发送的且未读
            if (message.chatType === 1 && message.senderId == this.currentChatWindow.chatId && message.receiverId == this.currentUser.id && message.isRead == false) {
              this.visibleUnReadMessages.add(message.chatMessageId);
            }else if(message.chatType === 2 && message.receiverId == this.currentChatWindow.chatId && message.isRead == false){
              this.visibleUnReadMessages.add(message.chatMessageId);
            }
          }
      });

      // 发送给服务端标记已读
      if (this.visibleUnReadMessages.size > 0) {
        // 好友未读消息清零
        this.currentChatWindow.unreadCount = 0;
        this.sendUnReadMes();
      }
    },

    // 发送未读消息
    sendUnReadMes(){
      let message = {
        senderId: this.currentUser.id,
            receiverId: this.currentChatWindow.chatId,
          message: JSON.stringify(Array.from(this.visibleUnReadMessages)),
          chatType: this.currentChatWindow.chatType,
          messageType: 'chat',
          subType:'submit_unread'
      }
      getWebSocketClient().sendMessage(JSON.stringify(message));

      // 立即更新本地消息的已读状态
      const readMessageIds = Array.from(this.visibleUnReadMessages);
      this.chatMessages.forEach(msg => {
        if (readMessageIds.includes(msg.chatMessageId)) {
          msg.isRead = true;
        }
      });

      // 清空集合
      this.visibleUnReadMessages.clear();
    },

    // 会话名为空或服务端兜底的「未知会话」「?」时，用更可读的匿名名称，避免标题像坏数据
    displayChatName(chat) {
      if (!chat) {
        return '';
      }
      const name = String(chat.chatName == null ? '' : chat.chatName).trim();
      if (name === '' || name === '?' || name === '未知会话' || name === '未命名会话' || name === '未命名群聊') {
        return Number(chat.chatType) === 2 ? '未命名群聊' : '未命名会话';
      }
      return name;
    },

    // 是否系统提示消息（群成员变更等）：contentType / messageType 为 6
    isSystemMessage(message) {
      if (this.isLegacyCallRecord(message)) return false;
      return !!message && (message.contentType === 6 || message.messageType === 6);
    },

    // 接收系统消息
    receiveSystemMessage(systemMsg){
      // 群成员变更系统提示：落到当前群窗口的聊天记录里，实时可见
      const isCurrentGroup = this.currentChatWindow &&
        this.currentChatWindow.chatType === 2 &&
        String(systemMsg.receiverId) === String(this.currentChatWindow.chatId);
      if (isCurrentGroup) {
        this.currentChatWindowPushMessage({
          chatMessageId: systemMsg.chatMessageId,
          senderId: systemMsg.senderId,
          receiverId: systemMsg.receiverId,
          message: systemMsg.message,
          chatType: 2,
          messageType: 6,
          contentType: 6,
          createTime: systemMsg.createTime,
          sender: systemMsg.sender || { username: '' }
        });
        this.$nextTick(() => {
          this.scrollToBottom();
        });
      }

      // 不在当前群窗口时，才在消息流末尾追加一条提示，避免与聊天内的系统条重复
      if (!isCurrentGroup) {
        this.systemMessages.push(systemMsg);
      }
    },

    // 移除预览图片
    removePastedImage(index) {
      this.pastedImages.splice(index, 1);
    },

    // 处理backspace键删除图片
    handleBackspace() {
      if (this.pastedImages.length > 0) {
        this.pastedImages.pop();
      }
    },

    // 打开图片查看器
    openImageViewer(message, event) {
      event.stopPropagation();
      // 只存储当前点击的图片
      this.currentImage = {
        url: message.message,
        fileName: message.fileName
      };
      
      // 显示图片查看器
      this.showImageViewer = true;
    },

    // 关闭图片查看器
    closeImageViewer() {
      this.showImageViewer = false;
      this.currentImage = null;
    },

    // 打开粘贴图片查看器
    openPastedImageViewer(index) {
      this.pastedImageIndex = index;
      this.showPastedImageViewer = true;
    },

    // 关闭粘贴图片查看器
    closePastedImageViewer() {
      this.showPastedImageViewer = false;
      this.pastedImageIndex = 0;
    },

    // 查看上一张粘贴图片
    prevPastedImage() {
      if (this.pastedImageIndex > 0) {
        this.pastedImageIndex--;
      }
    },

    // 查看下一张粘贴图片
    nextPastedImage() {
      if (this.pastedImageIndex < this.pastedImages.length - 1) {
        this.pastedImageIndex++;
      }
    },

    // 处理粘贴图片
    async handlePasteImage(file) {
      // 检查是否是机器人聊天
      if (this.currentChatWindow.chatType === 3) {
        ElMessage.warning('暂不支持向机器人发送图片');
        return;
      }

      // 检查图片数量限制
      if (this.pastedImages.length >= this.maxImages) {
        ElMessage.warning(`最多支持上传${this.maxImages}张图片`);
        return;
      }

      // 创建上传任务
      const uploadTask = {
        id: Date.now(),
        file: file,
        status: 'uploading',
        url: null
      };

      this.uploadingImages.push(uploadTask);

      // 设置上传超时定时器
      const timeoutPromise = new Promise((_, reject) => {
        setTimeout(() => reject(new Error('上传超时')), this.maxUploadTime);
      });

      try {
        // 并行执行上传和超时检查
        const response = await Promise.race([
          uploadFile(file),
          timeoutPromise
        ]);

        // 上传成功
        const fileUrl = response.data;

        // 更新上传任务状态
        const taskIndex = this.uploadingImages.findIndex(task => task.id === uploadTask.id);
        if (taskIndex !== -1) {
          this.uploadingImages.splice(taskIndex, 1);
        }

        // 添加到已上传图片列表（仅预览，不直接发送）
        this.pastedImages.push({
          id: uploadTask.id,
          url: fileUrl,
          fileName: file.name,
          fileSize: file.size,
          contentType: 2 // 图片类型
        });

      } catch (error) {
        // 上传失败
        const taskIndex = this.uploadingImages.findIndex(task => task.id === uploadTask.id);
        if (taskIndex !== -1) {
          this.uploadingImages.splice(taskIndex, 1);
        }

        let errorMsg = '图片上传失败';
        if (error.message === '上传超时') {
          errorMsg = '图片上传超时，请重试';
        }
        ElMessage.error(errorMsg);
        console.error('图片上传失败:', error);
      }
    },

    // 群聊入口：4 人以内 mesh 群语音 / 群视频
    openGroupCall(video) {
      const chat = this.currentChatWindow
      if (!chat || chat.chatType !== 2) return
      this.groupCall = {
        open: true,
        groupId: String(chat.chatId),
        groupName: chat.chatName || chat.name || '群通话',
        video: !!video
      }
    },
    closeGroupCall() {
      this.groupCall = { open: false, groupId: '', groupName: '', video: false }
      this.$emit('open-group-call-closed')
    },

    // 点击语音图标打开语音窗口
    openVoiceChat() {
      if(this.currentChatWindow == null){
        return;
      }
      if(this.currentChatWindow.chatType == null){
        return;
      }
      if(this.currentChatWindow.chatType != 1){
        this.$confirm({
          title: '功能提示',
          content: '暂不支持语音通话'
        })
        return;
      }

      const voiceStore = useVoiceWindowStore();
      let caller = null;
      if (this.currentChatWindow) {
        caller = {
          userId: this.currentChatWindow.chatId,
          name: this.currentChatWindow.chatName,
          avatar: this.currentChatWindow.avatar
        }
      }

      navigator.mediaDevices.getUserMedia({ audio: true })
        .then(stream => {
          stream.getTracks().forEach(track => track.stop());
          voiceStore.openVoiceWindow('outgoing', caller);
        })
        .catch(error => {
          console.error('麦克风权限被拒绝:', error);
          this.$confirm({
            title: '权限提示',
            content: '请允许访问麦克风以进行语音通话'
          })
        });
    },

    openVideoChat() {
      if(this.currentChatWindow == null){
        return;
      }
      if(this.currentChatWindow.chatType == null){
        return;
      }
      if(this.currentChatWindow.chatType != 1){
        this.$confirm({
          title: '功能提示',
          content: '暂不支持视频通话'
        })
        return;
      }

      const videoStore = useVideoWindowStore();
      let caller = null;
      if (this.currentChatWindow) {
        caller = {
          userId: this.currentChatWindow.chatId,
          name: this.currentChatWindow.chatName,
          avatar: this.currentChatWindow.avatar
        }
      }

      videoStore.openVideoWindow('outgoing', caller);
    },

    // 打开群管理页：仅群聊标题可触发，父组件负责全屏承载并保留当前会话
    openGroupManage() {
      if (!this.currentChatWindow || this.currentChatWindow.chatType !== 2) {
        return;
      }
      this.$emit('open-group-manage', String(this.currentChatWindow.chatId));
    },

    // 打开搜索聊天记录面板
    openHistorySearch() {
      // 全屏面板打开时收起其他浮层，避免 @ 成员面板/消息菜单压在搜索结果上
      this.atPanelShow = false;
      this.smartReplyMenu.show = false;
      // 记录打开前的焦点，关闭时还给原控件，键盘用户不会掉焦点
      this.historySearchReturnFocus = document.activeElement;
      this.syncHistoryPanelBounds();
      this.showHistorySearch = true;
      this.$nextTick(this.syncHistoryPanelBounds);
    },

    // 面板用固定定位覆盖聊天窗口：玻璃主题下绝对定位会被后绘制的内容盖住
    syncHistoryPanelBounds() {
      const host = document.querySelector('.chat-window') || this.$el;
      if (!host || typeof host.getBoundingClientRect !== 'function') return;
      const rect = host.getBoundingClientRect();
      if (!rect || rect.width <= 0 || rect.height <= 0) return;
      // 固定定位以视口为基准，直接使用聊天窗口的视口坐标；
      // 不能再减去 .main-content 的偏移，否则面板会左移到侧栏上方并漏出右侧消息。
      this.historyPanelStyle = {
        position: 'fixed',
        top: `${rect.top}px`,
        left: `${rect.left}px`,
        width: `${rect.width}px`,
        height: `${rect.height}px`,
        right: 'auto',
        bottom: 'auto'
      };
    },

    // 关闭搜索聊天记录面板
    closeHistorySearch() {
      this.showHistorySearch = false;
      const prev = this.historySearchReturnFocus;
      this.historySearchReturnFocus = null;
      this.$nextTick(() => {
        if (prev && prev.isConnected && typeof prev.focus === 'function') {
          prev.focus();
        }
      });
    },

    // 从全局搜索跳进来：必要时继续向前翻页，直到目标消息真正加载出来
    async jumpToMessage(messageId) {
      const targetId = String(messageId || '');
      if (!targetId || !this.currentChatWindow) return;
      const hasTarget = () => (this.chatMessages || []).some(
        m => String(m.chatMessageId) === targetId
      );
      let guard = 0;
      // 每页 20 条，最多补 80 页（1600 条），避免异常数据把界面卡死
      while (!hasTarget() && this.hasMoreHistory && guard < 80) {
        this.historyPageNum++;
        await this.getHistoryMes();
        guard++;
        await this.$nextTick();
      }
      await this.$nextTick();
      setTimeout(() => {
        this.locateMessage({ chatMessageId: targetId });
      }, 260);
    },

    // 定位到某条历史消息（找不到时明确提示，不假装成功）
    locateMessage(item) {
      this.showHistorySearch = false;
      this.historySearchReturnFocus = null;

      this.$nextTick(() => {
        const container = this.$refs.messagesContainer;
        if (container == null || item == null) {
          return;
        }

        const target = container.querySelector(`[data-message-id="${item.chatMessageId}"]`);
        if (target == null) {
          ElMessage.info({ message: '未能在当前会话中定位到这条消息，可能已被撤回或删除', plain: true });
          return;
        }

        target.scrollIntoView({ behavior: 'smooth', block: 'center' });
        target.classList.add('highlight-message');
        setTimeout(() => {
          target.classList.remove('highlight-message');
        }, 1600);
      });
    },
  }
}
</script>

<style scoped>
/* ===== 聊天窗口 · iOS 设计语言 ===== */
.chat-window {
  position: relative;
  height: 100%;
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  background: var(--bg);
}

/* 顶部标题栏 · iOS 毛玻璃导航栏 */
.chat-header {
  height: 52px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  background: var(--bg-blur);
  -webkit-backdrop-filter: saturate(180%) blur(28px);
  backdrop-filter: saturate(180%) blur(28px);
  border-bottom: 1px solid var(--separator);
  flex-shrink: 0;
}

/* 窄屏返回按钮：桌面不占位，<=600px 才出现 */
.mobile-back {
  display: none;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  margin-right: 4px;
  padding: 0;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--label);
  cursor: pointer;
  flex-shrink: 0;
}

.mobile-back:active {
  background: var(--fill);
}

.chat-title-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}

/* 群聊标题可点进群管理：chevron 是可见的入口提示，不只靠 hover */
.chat-title-wrap.manageable {
  cursor: pointer;
  border-radius: 8px;
  margin: -4px 6px -4px -6px;
  padding: 4px 6px;
  transition: background .16s ease;
}
.chat-title-wrap.manageable:active { background: var(--fill); }
.chat-title-wrap.manageable:focus-visible {
  outline: 2px solid var(--accent, #007aff);
  outline-offset: 1px;
}

.chat-title-row {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}
.title-chevron {
  flex: 0 0 auto;
  color: var(--label-tertiary);
}

.chat-title {
  font-size: 17px;
  font-weight: 700;
  flex: 0 1 auto;
  min-width: 0;
  text-align: left;
  color: var(--label);
  letter-spacing: -0.3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 全员禁言标记：常驻标题旁，进群就能看到状态，不靠颜色单独传意 */
.mute-chip {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  height: 18px;
  padding: 0 6px;
  border-radius: 9px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0;
  color: #a55a00;
  background: rgba(255, 159, 10, 0.16);
}

/* 群公告横幅：标题栏下一条，默认单行省略，点击展开到 4 行 */
.group-announce-banner {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 8px 14px;
  box-sizing: border-box;
  background: color-mix(in srgb, #ff9f0a 7%, var(--card));
  border-bottom: 1px solid var(--separator);
  position: relative;
  transition: background .16s ease;
}

.group-announce-banner::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: #ff9f0a;
}

.group-announce-banner.clickable {
  cursor: pointer;
}

.group-announce-banner.clickable:active {
  background: color-mix(in srgb, #ff9f0a 12%, var(--card));
}

.group-announce-banner:focus-visible {
  outline: 2px solid var(--accent, #007aff);
  outline-offset: -2px;
}

.gab-label {
  flex: 0 0 auto;
  font-size: 11px;
  font-weight: 700;
  color: #b26a00;
}

.gab-text {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  line-height: 17px;
  color: var(--label-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.group-announce-banner.expanded .gab-text {
  white-space: normal;
  overflow: visible;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
  line-clamp: 4;
}

.gab-toggle {
  flex: 0 0 auto;
  font-size: 11px;
  font-weight: 600;
  color: var(--blue, #007aff);
}

html[data-theme="dark"] .gab-label { color: #ffb340; }
html[data-theme="dark"] .mute-chip { color: #ffb340; background: rgba(255, 159, 10, 0.2); }

/* 对方正在输入：标题下方一行小字，不改变 52px 导航栏的整体高度 */
.typing-line {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 1px;
  font-size: 11px;
  line-height: 1.25;
  color: var(--label-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.typing-dots {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  flex: 0 0 auto;
}

.typing-dots i {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--label-tertiary);
  animation: typingBounce 1.2s infinite ease-in-out;
}

.typing-dots i:nth-child(2) { animation-delay: 0.2s; }
.typing-dots i:nth-child(3) { animation-delay: 0.4s; }

@keyframes typingBounce {
  0%, 60%, 100% { opacity: 0.3; transform: translateY(0); }
  30% { opacity: 1; transform: translateY(-2px); }
}

.chat-actions {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 已不在群聊：占满消息区，给出明确的移除出口，而不是「重试」 */
.chat-access-blocked {
  flex: 1;
  min-height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 32px 24px;
}

.chat-access-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--label-secondary, #8e8e93);
  background: color-mix(in srgb, var(--fill, rgba(118, 118, 128, 0.12)) 70%, transparent);
  margin-bottom: 14px;
}

.chat-access-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--label, #1c1c1e);
  margin-bottom: 6px;
}

.chat-access-desc {
  max-width: 300px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--label-secondary, #8e8e93);
  margin-bottom: 18px;
}

.chat-access-btn {
  height: 34px;
  padding: 0 18px;
  border: 0;
  border-radius: 17px;
  background: var(--fill, rgba(118, 118, 128, 0.12));
  color: var(--label, #1c1c1e);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.14s ease;
}

.chat-access-btn:hover {
  background: color-mix(in srgb, var(--fill, rgba(118, 118, 128, 0.12)) 70%, var(--label, #1c1c1e) 12%);
}

.loading-history {
  padding: 12px;
  text-align: center;
  color: var(--label-tertiary);
  font-size: 13px;
}

.loading-history--failed {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #b45309;
}

.history-retry {
  color: var(--blue, #007aff);
  cursor: pointer;
  font-weight: 600;
}

/* 消息区 · iOS Messages 式细腻渐变背景 */
.messages {
  flex: 1;
  padding: 16px 14px;
  overflow-y: auto;
  background: linear-gradient(180deg, var(--bg) 0%, color-mix(in srgb, var(--bg) 55%, var(--card) 45%) 100%);
  scroll-behavior: smooth;
  display: flex;
  flex-direction: column;
  /* 顶部留一条渐隐带：会话打开时滚到底，最上方气泡常被标题栏硬切一半 */
  -webkit-mask-image: linear-gradient(180deg, transparent 0, #000 14px, #000 100%);
  mask-image: linear-gradient(180deg, transparent 0, #000 14px, #000 100%);
}

.messages::-webkit-scrollbar {
  display: none;
}

/* 机器人会话空态 */
.bot-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 24px 28px 40px;
  text-align: center;
  color: var(--label-secondary);
}

.bot-empty-icon {
  width: 64px;
  height: 64px;
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3a7bfd;
  background: color-mix(in srgb, #3a7bfd 12%, transparent);
  margin-bottom: 4px;
}

.bot-empty-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--label);
}

.bot-empty-desc {
  font-size: 13px;
  line-height: 1.65;
  max-width: 300px;
  color: var(--label-secondary);
}

.bot-empty-btn {
  margin-top: 8px;
  padding: 9px 20px;
  border: none;
  border-radius: 999px;
  background: #3a7bfd;
  color: #fff;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.18s ease, opacity 0.18s ease;
}

.bot-empty-btn:hover { opacity: 0.88; }
.bot-empty-btn:active { transform: scale(0.97); }

/* 消息行 */
.message {
  margin-bottom: 14px;
  max-width: 72%;
  display: flex;
  align-self: flex-start;
  animation: fadeIn 0.3s ease-out;
}

/* 最新一条消息：Apple Messages 弹性入场 */
.message.bubble-pop {
  animation: bubblePop 0.45s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

/* 新消息弹性入场：只做位移与缩放，opacity 恒为 1（冻结在首帧也看得见内容） */
@keyframes bubblePop {
  0% {
    transform: translateY(10px) scale(0.86);
  }
  60% {
    transform: translateY(-2px) scale(1.04);
  }
  100% {
    transform: translateY(0) scale(1);
  }
}

/* 已读双勾：打勾回弹 + iOS 蓝 */
.read-tick .tick {
  color: var(--accent);
  animation: tickPop 0.4s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

@keyframes tickPop {
  0% {
    transform: scale(0.4);
  }
  70% {
    transform: scale(1.25);
  }
  100% {
    transform: scale(1);
  }
}

@media (prefers-reduced-motion: reduce) {
  .message.bubble-pop,
  .read-tick .tick {
    animation: none;
  }
}

.message.receiver {
  align-self: flex-start;
  text-align: left;
}

.message.sender {
  margin-left: auto;
  justify-content: flex-end;
  align-self: flex-end;
  text-align: right;
  align-items: flex-start;
}

.message-content {
  max-width: 320px;
  display: inline-block;
}

/* iOS 气泡 */
.message .content {
  padding: 10px 14px;
  border-radius: 18px;
  display: inline-block;
  position: relative;
  white-space: pre-wrap;
  word-break: break-word;
  max-width: 100%;
  font-size: 15px;
  line-height: 1.5;
  color: var(--label);
}

.message.receiver .content {
  background: var(--card);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border-top-left-radius: 4px;
}

.message.sender .content {
  background: var(--bubble-outgoing);
  color: #fff;
  box-shadow: 0 1px 3px var(--separator);
  border-top-right-radius: 4px;
}

/* 非文本消息去掉边框和背景 */
.message.receiver .content:has(.message-image),
.message.receiver .content:has(.message-audio),
.message.receiver .content:has(.message-video),
.message.receiver .content:has(.message-file) {
  background: transparent;
  border: none;
  padding: 0;
  box-shadow: none;
}

.message.sender .content:has(.message-image),
.message.sender .content:has(.message-audio),
.message.sender .content:has(.message-video),
.message.sender .content:has(.message-file) {
  background: transparent;
  border: none;
  padding: 0;
  box-shadow: none;
}

.message.receiver .content:has(.bot-loading) {
  background: transparent;
  border: none;
  padding: 0;
  box-shadow: none;
}

/* 文件相关 */
.message-image {
  max-width: 100%;
  max-height: 220px;
  border-radius: 14px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.message-audio,
.message-video,
.message-file {
  max-width: 100%;
}

.message-audio audio,
.message-video video {
  max-width: 100%;
  margin-bottom: 5px;
  border-radius: 12px;
}

/* ===== 语音消息气泡（精致版） ===== */
.message-audio {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 200px;
  max-width: 270px;
  padding: 10px 13px;
  border-radius: 16px;
  background: var(--card);
  box-shadow: 0 1px 4px var(--separator);
  box-sizing: border-box;
}

.message.sender .message-audio {
  background: var(--bubble-outgoing);
  box-shadow: 0 2px 8px rgba(100, 181, 246, 0.25);
}

.voice-play-btn {
  flex: none;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s ease, background 0.2s ease;
  outline: none;
}

.voice-play-btn:hover {
  transform: scale(1.06);
}

.voice-play-btn.playing {
  background: #f56c6c;
}

.message.sender .voice-play-btn {
  background: rgba(255, 255, 255, 0.25);
}

.message.sender .voice-play-btn.playing {
  background: rgba(255, 255, 255, 0.42);
}

.voice-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.voice-wave {
  display: flex;
  align-items: center;
  gap: 2.5px;
  height: 22px;
}

.voice-wave span {
  width: 3px;
  border-radius: 2px;
  background: var(--accent);
  opacity: 0.45;
  height: 6px;
  transition: height 0.2s ease;
}

.voice-wave.playing span {
  animation: voiceBar 1s ease-in-out infinite alternate;
}

@keyframes voiceBar {
  from { height: 5px; opacity: 0.4; }
  to { height: 20px; opacity: 1; }
}

.message.sender .voice-wave span {
  background: #fff;
}

.voice-meta {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}

.voice-duration {
  font-size: 12px;
  color: var(--label-secondary, #888);
  font-variant-numeric: tabular-nums;
  flex: none;
}

.message.sender .voice-duration {
  color: rgba(255, 255, 255, 0.85);
}

.voice-transcript {
  font-size: 12.5px;
  color: var(--label);
  line-height: 1.45;
  word-break: break-all;
  background: rgba(0, 0, 0, 0.045);
  padding: 3px 8px;
  border-radius: 8px;
  display: inline-block;
  max-width: 100%;
}

.message.sender .voice-transcript {
  color: rgba(255, 255, 255, 0.92);
  background: rgba(255, 255, 255, 0.16);
}

.file-link {
  display: flex;
  align-items: center;
  text-decoration: none;
  color: var(--label);
  padding: 5px 0;
}

.file-icon {
  width: 20px;
  height: 20px;
  margin-right: 8px;
  color: var(--accent);
  flex-shrink: 0;
}

.file-name {
  font-size: 14px;
  word-break: break-word;
}

/* 输入区工具条 */
.input-tools {
  display: flex;
  justify-content: space-between;
  width: 100%;
  padding-bottom: 8px;
  height: 20%;
}

.left-tools {
  display: flex;
}

.icon-tool {
  margin-right: 15px;
  font-size: 20px;
  cursor: pointer;
  color: var(--label-secondary);
  transition: all 0.3s cubic-bezier(0.32, 0.72, 0, 1);
  padding: 6px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-tool:hover {
  background: var(--fill);
  color: #007aff;
}

.icon-tool:focus-visible {
  outline: 2px solid #007aff;
  outline-offset: 2px;
  background: var(--fill);
}

.tool-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
  vertical-align: middle;
  filter: brightness(0) opacity(0.5);
}

.emoji-trigger-container {
  position: relative;
  display: inline-block;
}

.emoji-trigger {
  cursor: pointer;
  font-size: 20px;
  padding: 5px 8px;
  transition: all 0.2s;
  border-radius: 8px;
}

.emoji-trigger:hover {
  background: var(--fill);
}

.emoji-picker-floating {
  position: absolute;
  bottom: 44px;
  left: 0;
  background: rgba(255, 255, 255, 0.96);
  -webkit-backdrop-filter: blur(24px);
  backdrop-filter: blur(24px);
  border-radius: 16px;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.18);
  z-index: 1000;
  overflow: hidden;
  animation: popup 0.2s ease-out;
}

.right-tools {
  margin-right: 0;
  display: flex;
  align-items: center;
}

.right-tools .icon-tool {
  margin-right: 15px;
  margin-left: 0;
}
/* 窄屏：工具条收紧，保证输入区不被顶出容器 */
@media (max-width: 640px) {
  .right-tools .icon-tool { margin-right: 6px; font-size: 17px; padding: 4px; }
}

/* 输入区 · iOS 毛玻璃底栏 */
.input-container {
  padding: 10px 12px 14px;
  background: rgba(249, 249, 252, 0.88);
  -webkit-backdrop-filter: saturate(180%) blur(28px);
  backdrop-filter: saturate(180%) blur(28px);
  border-top: 1px solid var(--separator);
  flex-shrink: 0;
}

.input-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 0 8px;
  width: 95%;
  position: relative;
  justify-content: center;
  align-items: center;
  padding: 0 10px;
  margin-top: -5px;
}

.main-content {
  overflow: hidden;
}

/* iOS 输入框 */
.input-row textarea {
  width: 100%;
  height: 100%;
  padding: 9px 14px;
  border: none;
  border-radius: 19px;
  background: var(--card);
  resize: none;
  box-sizing: border-box;
  line-height: 1.5;
  overflow-y: auto;
  outline: none;
  font-family: inherit;
  font-size: 15px;
  color: var(--label);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.3s cubic-bezier(0.32, 0.72, 0, 1);
}

.input-row textarea:focus {
  box-shadow: 0 0 0 2px rgba(0, 122, 255, 0.25), 0 1px 4px rgba(0, 0, 0, 0.06);
}

.input-row textarea::-webkit-scrollbar {
  display: none;
}

.input-row textarea.error {
  box-shadow: 0 0 0 2px rgba(255, 59, 48, 0.3);
}

.message-empty-warn {
  padding: 4px 10px;
  background: rgba(255, 59, 48, 0.1);
  color: #ff3b30;
  border-radius: 8px;
  font-size: 12px;
  white-space: nowrap;
  animation: fadeIn 0.3s;
  position: absolute;
  bottom: calc(100% + 5px);
}

.error-arrow {
  position: absolute;
  bottom: -6px;
  right: 12px;
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-top: 6px solid rgba(255, 59, 48, 0.3);
}

@keyframes inputFocus {
  from {
    transform: translateY(10px);
  }
  to {
    transform: translateY(0);
  }
}

/* 头像 · iOS 圆形 */
.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(145deg, #4da3ff, #007aff);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  overflow: hidden;
  box-shadow: 0 2px 6px rgba(0, 122, 255, 0.2);
  flex-shrink: 0;
}

.message-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-text {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: white;
  font-weight: 600;
  font-size: 16px;
  border-radius: 50%;
  text-transform: uppercase;
  line-height: 1;
  white-space: nowrap;
  overflow: hidden;
  padding: 0;
  margin: 0;
  box-sizing: border-box;
}

.close-chat {
  cursor: pointer;
  font-size: 22px;
  font-weight: 300;
  color: var(--label-secondary);
  padding: 5px;
  line-height: 1;
  border: 0;
  background: transparent;
  border-radius: 50%;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}

.search-history-btn:focus-visible,
.close-chat:focus-visible {
  outline: 2px solid rgba(0, 122, 255, 0.7);
  outline-offset: 1px;
}

.close-chat:hover {
  color: #ff3b30;
  background: rgba(255, 59, 48, 0.1);
}

.search-history-btn {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  color: var(--label-secondary);
  padding: 6px;
  border: 0;
  background: transparent;
  line-height: 1;
  border-radius: 50%;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}

.search-history-btn:hover {
  color: #007aff;
  background: rgba(0, 122, 255, 0.1);
}

.search-icon {
  width: 18px;
  height: 18px;
}

/* 消息头部 */
.message-header {
  margin-bottom: 5px;
  align-items: center;
  display: flex;
  gap: 6px;
}

.message-header .sender {
  text-align: right;
}

.message-header.receiver {
  text-align: left;
}

.message-sender-username {
  font-weight: 600;
  font-size: 12px;
  color: var(--label-secondary);
}

.message.sender-username .message-header {
  justify-content: flex-end;
}

.message.sender-username .message-sender-username {
  display: none;
}

.message-time {
  font-size: 11.5px;
  color: var(--label-tertiary);
}

.message-edited-tag {
  margin-left: 6px;
  font-size: 11px;
  color: var(--label-secondary);
  opacity: .85;
}

.edit-message-mask {
  position: fixed;
  inset: 0;
  z-index: 4000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, .42);
  backdrop-filter: blur(2px);
}

.edit-message-dialog {
  width: min(480px, calc(100vw - 40px));
  padding: 18px 20px 16px;
  border-radius: 16px;
  background: var(--bg-elevated, #fff);
  box-shadow: 0 24px 60px rgba(0, 0, 0, .22);
}

.edit-message-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
  color: var(--label-primary, #111);
}

.edit-message-head button {
  width: 26px;
  height: 26px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--label-secondary, #666);
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}

.edit-message-input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid var(--separator, #e5e5ea);
  border-radius: 10px;
  background: var(--bg-input, #f7f7fa);
  color: var(--label-primary, #111);
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  outline: none;
}

.edit-message-input:focus {
  border-color: var(--accent, #3b82f6);
}

.edit-message-tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--label-tertiary, #8e8e93);
}

.edit-message-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}

.edit-message-btn {
  min-width: 78px;
  height: 34px;
  padding: 0 16px;
  border: 1px solid var(--separator, #e5e5ea);
  border-radius: 9px;
  background: var(--bg-input, #f7f7fa);
  color: var(--label-primary, #111);
  font-size: 13px;
  cursor: pointer;
}

.edit-message-btn.primary {
  border-color: transparent;
  background: var(--accent, #3b82f6);
  color: #fff;
}

.edit-message-btn:disabled {
  opacity: .6;
  cursor: default;
}

.read-status {
  font-size: 12px;
  color: var(--accent);
}

/* 群聊已读：自己发的消息下方「N 人已读」，点开看名单 */
.message-bottom {
  position: relative;
  flex-wrap: wrap;
}

.group-read-status {
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.group-read-status:hover {
  opacity: 0.72;
}

/* 名单在消息下方就地展开，不遮挡气泡 */
.group-read-pop {
  flex-basis: 100%;
  margin-top: 6px;
  min-width: 132px;
  width: max-content;
  max-width: 100%;
  padding: 8px 10px 6px;
  background: var(--fill);
  border-radius: 8px;
  text-align: left;
  box-sizing: border-box;
}

.grp-title {
  font-size: 11.5px;
  color: var(--label-secondary);
  padding-bottom: 6px;
  border-bottom: 1px solid var(--separator);
}

.grp-row {
  font-size: 12.5px;
  color: var(--label);
  padding-top: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.message.sender .message-content {
  order: -1;
  margin-right: 10px;
}

.message.receiver .message-content {
  margin-left: 10px;
}

/* 高亮搜索消息 */
@keyframes highlight-message-flash {
  0%, 100% { background-color: transparent; }
  25%, 75% { background-color: rgba(0, 122, 255, 0.15); }
}

.message.highlight-message .content {
  animation: highlight-message-flash 1.6s ease-in-out;
  box-shadow: 0 0 0 2px #fff, 0 0 0 4px rgba(0, 122, 255, 0.55), 0 6px 20px rgba(0, 122, 255, 0.28);
}

/* 发送状态 */
.send-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--label-secondary);
  font-size: 12px;
}

.failed-icon {
  margin-right: 4px;
  animation: pulse 1.5s infinite;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.failed-icon span {
  color: #ff3b30;
  font-size: 12px;
  font-weight: bold;
  line-height: 1;
}

.failed-icon:hover {
  background: rgba(255, 59, 48, 0.12);
  transform: scale(1.15);
}

.failed-icon:active {
  transform: scale(0.9);
}

.failed-text {
  color: var(--label-secondary);
}

/* 加载动画 */
@keyframes pulse {
  0%, 100% { opacity: 0.8; }
  50% { opacity: 1; transform: scale(1.1); }
}

.loading-spinner {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid rgba(0, 122, 255, 0.2);
  border-radius: 50%;
  border-top-color: #007aff;
  animation: spin 1s linear infinite;
  margin-right: 6px;
}

.sending-text {
  color: var(--label-secondary);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 机器人思考中 */
.bot-loading {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: var(--card);
  border-radius: 18px;
  border-top-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  animation: fadeIn 0.3s ease-out;
  max-width: 180px;
}

.loading-text {
  color: var(--label-secondary);
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0.5px;
}

/* 系统消息 · iOS 时间戳样式 */
.system-message {
  display: flex;
  justify-content: center;
  margin: 8px 0;
  padding: 0 20px;
}

.system-message-content {
  color: var(--label-tertiary);
  font-size: 12.5px;
  text-align: center;
  max-width: 80%;
  word-break: break-word;
  line-height: 1.4;
  background: var(--fill-strong);
  padding: 4px 12px;
  border-radius: 999px;
}

/* 图片预览 */
.image-preview-container {
  padding: 10px 15px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  background: transparent;
  margin-bottom: 10px;
}

.image-preview-item {
  position: relative;
  width: 60px;
  height: 60px;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
  background: white;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.2s;
}

.preview-image:hover {
  transform: scale(1.05);
}

.image-remove-btn {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 18px;
  height: 18px;
  background: var(--label-secondary);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.image-remove-btn:hover {
  background: rgba(0, 0, 0, 0.75);
  transform: scale(1.1);
}

/* 图片查看器 */
.image-viewer-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.image-viewer-container {
  width: 90%;
  max-width: 1000px;
  height: 90%;
  max-height: 800px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.image-viewer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: rgba(255, 255, 255, 0.08);
  color: white;
}

.image-counter {
  font-size: 14px;
  opacity: 0.8;
}

.close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.2s;
}

.close-btn:hover {
  background: var(--fill);
}

.image-viewer-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
}

.nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: var(--overlay);
  border: none;
  color: white;
  font-size: 24px;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  z-index: 10;
}

.nav-btn:hover:not(:disabled) {
  background: rgba(0, 0, 0, 0.8);
  transform: translateY(-50%) scale(1.1);
}

.nav-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.prev-btn {
  left: 20px;
}

.next-btn {
  right: 20px;
}

.image-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  max-width: calc(100% - 100px);
}

.viewer-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 8px;
}

.message-image {
  cursor: pointer;
  transition: transform 0.2s;
}

.message-image:hover {
  transform: scale(1.02);
}

/* 入场只做位移，不碰 opacity：动画时钟冻结时消息气泡与浮层提示依然可读 */
@keyframes fadeIn {
  from { transform: translateY(6px); }
  to { transform: translateY(0); }
}

@keyframes menu-fade {
  from {
    transform: translateY(5px);
  }
  to {
    transform: translateY(0);
  }
}

/* 入场只做位移与缩放，不碰 opacity：动画时钟冻结时面板依然完整可读 */
@keyframes popup {
  from {
    transform: scale(0.96) translateY(6px);
  }
  to {
    transform: scale(1) translateY(0);
  }
}

/* ===== AI 智能回复 ===== */
.ai-reply-float {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 4px 9px;
  border: none;
  border-radius: 12px;
  background: rgba(0, 122, 255, 0.92);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  opacity: 0;
  transition: opacity .18s ease, transform .18s ease;
  z-index: 5;
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.35);
}
.message.sender .ai-reply-float { left: -84px; }
.message.receiver .ai-reply-float { right: -84px; }
.message:hover .ai-reply-float { opacity: 1; }
/* 窄屏让位：气泡两侧的悬浮入口会顶出容器，改为不显示（右键菜单内仍可触发 AI 回复） */
@media (max-width: 640px) {
  .message.sender .ai-reply-float,
  .message.receiver .ai-reply-float { display: none; }
}

.smart-reply-menu {
  position: fixed;
  z-index: 1000;
  width: 288px;
  max-width: calc(100vw - 16px);
  box-sizing: border-box;
  max-height: calc(100vh - 16px);
  overflow-y: auto;
  overscroll-behavior: contain;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 14px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.16);
  padding: 10px;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  animation: popup .18s ease;
}
.smart-reply-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 600;
  color: #1c1c1e;
  padding: 2px 4px 6px;
}
.smart-reply-close {
  border: none;
  background: rgba(0, 0, 0, 0.06);
  color: #666;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
}
.smart-reply-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 6px;
  font-size: 13px;
  color: #8e8e93;
}
.smart-reply-options {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.smart-reply-option {
  padding: 8px 10px;
  background: #f2f2f7;
  border-radius: 10px;
  cursor: pointer;
  transition: background .15s ease;
}
.smart-reply-option:hover { background: #e5e5ea; }
.smart-reply-option-text {
  font-size: 13px;
  color: #1c1c1e;
  line-height: 1.4;
}
.smart-reply-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 6px;
  font-size: 13px;
  color: #8e8e93;
}
.smart-reply-retry {
  border: none;
  background: #007aff;
  color: #fff;
  padding: 5px 12px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 12px;
}

/* ===== 消息操作菜单（引用/撤回/复制） ===== */
.message-action-menu { width: 210px; }
/* AI 回复菜单比普通操作菜单宽，避免三条建议换行过多被裁切 */
.message-action-menu.smart-reply-menu { width: 320px; max-width: calc(100vw - 16px); }
.message-action-list {
  display: flex;
  flex-direction: column;
  padding: 2px 0;
}
.message-action-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 10px;
  border-radius: 9px;
  font-size: 14px;
  color: #1c1c1e;
  cursor: pointer;
  transition: background .15s ease;
}
.message-action-item:hover { background: #f2f2f7; }
.message-action-item svg { flex-shrink: 0; color: #666; }
.smart-reply-divider {
  height: 1px;
  background: rgba(0, 0, 0, 0.06);
  margin: 4px 0 8px;
}

/* ===== 消息翻译 ===== */
.msg-translation {
  margin-top: 6px;
  padding: 7px 10px;
  border-radius: 10px;
  background: rgba(0, 122, 255, 0.07);
  border-left: 2px solid rgba(0, 122, 255, 0.45);
  font-size: 13px;
  line-height: 1.55;
  color: #3a3a3c;
  width: 200px;
  max-width: 100%;
  box-sizing: border-box;
  word-break: break-word;
}
.msg-translation.is-error { background: rgba(255, 59, 48, 0.06); border-left-color: rgba(255, 59, 48, 0.45); }
.mt-head { display: flex; align-items: center; justify-content: space-between; gap: 6px; margin-bottom: 3px; }
.mt-tools { display: inline-flex; align-items: center; gap: 2px; }
.mt-btn {
  border: none;
  background: transparent;
  color: #007aff;
  font-size: 11px;
  padding: 1px 4px;
  border-radius: 6px;
  cursor: pointer;
  line-height: 1.4;
}
.mt-btn:hover { background: rgba(0, 122, 255, 0.12); }
.mt-close { color: #8e8e93; font-size: 13px; padding: 0 4px; }
.mt-close:hover { background: rgba(0, 0, 0, 0.06); }
.msg-translation.is-error .mt-btn { color: #ff3b30; }
.mt-lang {
  display: inline-block;
  margin-right: 6px;
  padding: 1px 6px;
  border-radius: 6px;
  background: rgba(0, 122, 255, 0.14);
  color: #007aff;
  font-size: 11px;
  vertical-align: 1px;
}
.mt-text { color: #3a3a3c; }
.translate-langs { display: inline-flex; gap: 4px; margin-left: auto; }
.translate-langs button {
  border: none;
  background: #007aff;
  color: #fff;
  font-size: 11px;
  padding: 3px 7px;
  border-radius: 7px;
  cursor: pointer;
}

/* ===== 多选 ===== */
.message.select-mode {
  position: relative;
  cursor: pointer;
  border-radius: 12px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  padding: 4px 8px;
  transition: background .15s ease;
}
/* 多选态下内容不响应自身点击，统一交给整行处理（卡片、图片、文件链接都不会误打开） */
.message.select-mode .content { pointer-events: none; }
.message.select-mode:hover { background: rgba(0, 122, 255, 0.06); }
.message.select-on { background: rgba(0, 122, 255, 0.1); }
/* 自己的消息原本整体右对齐，多选态把勾选框固定到行首 */
.message.select-mode.sender { justify-content: space-between; }
.msg-select-box {
  order: -1;
  flex-shrink: 0;
  align-self: center;
  width: 20px;
  height: 20px;
  margin-right: 8px;
  border-radius: 50%;
  border: 1.5px solid rgba(0, 0, 0, 0.2);
  background: #fff;
  color: transparent;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
  z-index: 3;
}
.msg-select-box.checked { background: #007aff; border-color: #007aff; color: #fff; }
.merge-bar {
  position: fixed;
  right: 16px;
  bottom: 96px;
  z-index: 1200;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 14px;
  border-radius: 14px;
  background: rgba(28, 28, 30, 0.92);
  backdrop-filter: blur(20px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.28);
  color: #fff;
  font-size: 13px;
}
.merge-bar-count { color: #fff; }
.merge-bar-cancel {
  border: none;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  padding: 5px 12px;
  border-radius: 9px;
  cursor: pointer;
  font-size: 12px;
}
.merge-bar-ok {
  border: none;
  background: #007aff;
  color: #fff;
  padding: 5px 14px;
  border-radius: 9px;
  cursor: pointer;
  font-size: 12px;
}
.merge-bar-ok:disabled { opacity: .45; cursor: not-allowed; }

/* 卡片类消息：外层气泡不占位、不画底色，卡片自身提供白底与描边 */
.message .content.is-card {
  padding: 0;
  background: transparent;
  max-width: 100%;
}

/* ===== 视频通话记录 ===== */
.call-card {
  width: 208px;
  max-width: 100%;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.call-card-icon {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #007aff;
  background: rgba(0, 122, 255, 0.1);
  flex-shrink: 0;
}
.call-card-main { flex: 1; min-width: 0; }
.call-card-title { font-size: 13px; font-weight: 600; color: #1c1c1e; line-height: 1.35; }
.call-card-sub { font-size: 11px; color: #8e8e93; line-height: 1.45; }
.call-card-duration { font-size: 11px; color: #8e8e93; flex-shrink: 0; }

/* ===== 合并转发卡片 ===== */
.merge-card {
  width: 220px;
  max-width: 100%;
  box-sizing: border-box;
  padding: 11px 12px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  cursor: pointer;
}
.merge-head { display: flex; align-items: center; gap: 6px; margin-bottom: 7px; }
.merge-icon { color: #8e8e93; font-size: 13px; }
.merge-title { font-size: 13px; font-weight: 600; color: #1c1c1e; }
.merge-line {
  display: flex;
  gap: 6px;
  font-size: 12px;
  color: #3a3a3c;
  line-height: 1.5;
  margin-bottom: 4px;
}
.merge-who { color: #8e8e93; flex-shrink: 0; max-width: 62px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.merge-text { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.merge-foot {
  display: flex;
  justify-content: space-between;
  margin-top: 7px;
  padding-top: 7px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  font-size: 11px;
  color: #8e8e93;
}
.merge-open { color: #007aff; }
.merge-viewer { width: 360px; max-width: 90vw; }
.merge-viewer-list { max-height: 55vh; overflow-y: auto; padding: 4px 2px; }
.mv-item { padding: 8px 4px; border-bottom: 1px solid rgba(0, 0, 0, 0.05); }
.mv-head { display: flex; justify-content: space-between; font-size: 11px; color: #8e8e93; margin-bottom: 3px; }
.mv-sender { color: #007aff; }
.mv-text { font-size: 13px; color: #1c1c1e; line-height: 1.5; word-break: break-word; }
.mv-count { margin-left: 8px; font-size: 11px; font-weight: 400; color: #8e8e93; }

/* ===== 群投票卡片 ===== */
.vote-card {
  width: 220px;
  max-width: 100%;
  box-sizing: border-box;
  padding: 12px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  cursor: pointer;
}
.vote-head { display: flex; gap: 6px; align-items: flex-start; margin-bottom: 9px; }
.vote-badge {
  flex-shrink: 0;
  font-size: 10px;
  color: #fff;
  background: #ff9500;
  padding: 2px 6px;
  border-radius: 6px;
  margin-top: 1px;
}
.vote-title { font-size: 13px; font-weight: 600; color: #1c1c1e; line-height: 1.45; flex: 1; }
.vote-status {
  flex-shrink: 0;
  font-size: 10px;
  color: #8e8e93;
  background: rgba(0, 0, 0, 0.05);
  padding: 2px 6px;
  border-radius: 6px;
  margin-top: 1px;
}
.vote-loading { font-size: 12px; color: #8e8e93; padding: 6px 0; }
.vote-option {
  position: relative;
  padding: 7px 9px;
  border-radius: 9px;
  background: #f2f2f7;
  margin-bottom: 6px;
  overflow: hidden;
  cursor: pointer;
  transition: background .15s ease;
}
.vote-option.mine { background: rgba(0, 122, 255, 0.1); }
.vote-option.mine .vote-text { color: #007aff; }
.vote-option.disabled { cursor: default; opacity: .72; }
.vote-option.pending { pointer-events: none; opacity: .78; }
.vote-option.disabled:hover { background: #f2f2f7; }
.vote-option.disabled.mine:hover { background: rgba(0, 122, 255, 0.1); }
.vote-mine {
  display: inline-block;
  margin-left: 4px;
  padding: 0 5px;
  border-radius: 5px;
  font-size: 10px;
  color: #007aff;
  background: rgba(0, 122, 255, 0.14);
  vertical-align: 1px;
}
.vote-bar {
  position: absolute;
  left: 0; top: 0; bottom: 0;
  background: rgba(0, 122, 255, 0.16);
  transition: width .3s ease;
}
.vote-line { position: relative; display: flex; justify-content: space-between; gap: 8px; font-size: 12px; color: #1c1c1e; }
.vote-text { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.vote-count { color: #8e8e93; flex-shrink: 0; }
.vote-foot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  row-gap: 4px;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 11px;
  color: #8e8e93;
}
.vote-actions { display: inline-flex; align-items: center; gap: 6px; margin-left: auto; }
.vote-tip { color: #007aff; }
.vote-tip.ended { color: #8e8e93; }
.vote-cancel {
  border: none;
  background: transparent;
  color: #007aff;
  font-size: 11px;
  padding: 2px 4px;
  border-radius: 6px;
  cursor: pointer;
}
.vote-cancel:hover { background: rgba(0, 122, 255, 0.1); }
.vote-close {
  border: none;
  background: transparent;
  color: #ff3b30;
  font-size: 11px;
  padding: 2px 4px;
  border-radius: 6px;
  cursor: pointer;
}
.vote-close:hover { background: rgba(255, 59, 48, 0.1); }
.vote-dialog { width: 340px; max-width: 92vw; }
.vote-input {
  width: 100%;
  box-sizing: border-box;
  padding: 9px 11px;
  margin-bottom: 8px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 10px;
  font-size: 13px;
  outline: none;
  background: #fff;
}
.vote-input:focus { border-color: #007aff; }
.vote-opt-row { display: flex; gap: 6px; align-items: center; }
.vote-opt-row .vote-input { flex: 1; }
.vote-opt-del {
  border: none;
  background: rgba(255, 59, 48, 0.1);
  color: #ff3b30;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  cursor: pointer;
  flex-shrink: 0;
  margin-bottom: 8px;
}
.vote-opt-add {
  border: none;
  background: rgba(0, 122, 255, 0.1);
  color: #007aff;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 12px;
  cursor: pointer;
  margin: 2px 0 8px;
}
.vote-multi { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #3a3a3c; margin-bottom: 4px; }

/* ===== 消息撤回提示 ===== */
.withdraw-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  font-size: 13px;
  color: #8e8e93;
  border-radius: 8px;
  user-select: none;
}
.withdraw-tip svg { flex-shrink: 0; }

/* ===== 引用回复块 ===== */
.reply-quote {
  display: flex;
  align-items: stretch;
  gap: 8px;
  padding: 5px 10px;
  margin-bottom: 4px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.04);
  max-width: 100%;
  cursor: pointer;
  transition: background .15s ease;
}
.reply-quote:hover { background: rgba(0, 122, 255, 0.10); }

/* 跳转后被引用消息的短暂高亮 */
.quote-flash {
  animation: quoteFlash 1s ease;
  border-radius: 10px;
}
@keyframes quoteFlash {
  0%   { background: rgba(0, 122, 255, 0.22); box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.28); }
  100% { background: transparent; box-shadow: 0 0 0 0 rgba(0, 122, 255, 0); }
}

/* 被 @ 的消息角标 */
.mention-me-badge {
  display: inline-flex;
  align-items: center;
  padding: 1px 6px;
  margin-right: 6px;
  border-radius: 8px;
  background: rgba(255, 59, 48, 0.12);
  color: #ff3b30;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
}
.reply-quote-line {
  width: 3px;
  flex-shrink: 0;
  border-radius: 2px;
  background: #007aff;
  opacity: .7;
}
.reply-quote-text {
  font-size: 12.5px;
  color: #8e8e93;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-all;
}

/* ===== @ 提及高亮 ===== */
.mention {
  color: #007aff;
  font-weight: 600;
}

/* ===== 引用预览条 ===== */
.reply-preview-bar {
  display: flex;
  grid-column: 1 / -1;
  width: 100%;
  box-sizing: border-box;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 10px;
  margin-bottom: 8px;
  background: rgba(0, 122, 255, 0.08);
  border: 1px solid rgba(0, 122, 255, 0.18);
  border-radius: 10px;
  font-size: 13px;
}
.reply-preview-info {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  flex: 1;
}
.reply-preview-label { color: #007aff; font-weight: 600; flex-shrink: 0; }
.reply-preview-text {
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.reply-preview-close {
  border: none;
  background: rgba(0, 0, 0, 0.06);
  color: #666;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  flex-shrink: 0;
}

/* ===== @ 提及成员面板 ===== */
.at-member-panel {
  position: absolute;
  bottom: 100%;
  left: 12px;
  z-index: 800;
  width: 240px;
  max-height: 260px;
  overflow-y: auto;
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 14px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.16);
}
.at-member-title {
  padding: 10px 12px 6px;
  font-size: 12px;
  font-weight: 600;
  color: #8e8e93;
}
.at-member-search-wrap { padding: 0 8px 6px; }
.at-member-search {
  width: 100%;
  box-sizing: border-box;
  padding: 7px 10px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 9px;
  background: #f2f2f7;
  font-size: 13px;
  color: #333;
  outline: none;
}
.at-member-search:focus { border-color: #007aff; background: #fff; }
.at-member-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 12px;
  font-size: 13px;
  color: #8e8e93;
}
.at-member-list { padding: 2px 6px 8px; }
.at-member-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 8px;
  border-radius: 9px;
  cursor: pointer;
  transition: background .15s ease;
}
.at-member-item:hover { background: #f2f2f7; }
.at-member-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  background: #e5e5ea;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #666;
  flex-shrink: 0;
}
.at-member-name {
  font-size: 14px;
  color: #1c1c1e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
/* 搜索命中的片段：蓝底加粗，避免默认的黄色 mark 与整体色调打架 */
.at-member-hit {
  background: rgba(37, 99, 235, 0.14);
  color: #1d4ed8;
  font-weight: 600;
  border-radius: 3px;
  padding: 0 1px;
}
.at-member-empty {
  padding: 14px 12px;
  font-size: 13px;
  color: #8e8e93;
  text-align: center;
}
html[data-theme="dark"] .at-member-panel {
  background: rgba(28, 28, 30, 0.97);
  border-color: rgba(255, 255, 255, 0.12);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.5);
}
html[data-theme="dark"] .at-member-name { color: #f2f2f7; }
html[data-theme="dark"] .at-member-search { background: #2c2c2e; border-color: rgba(255,255,255,0.1); color: #f2f2f7; }
html[data-theme="dark"] .at-member-item:hover { background: #2c2c2e; }
html[data-theme="dark"] .at-member-hit { background: rgba(10, 132, 255, 0.24); color: #9ecbff; }

/* ===== 输入工具栏（@ 按钮 + 已选标签） ===== */
.input-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 6px 0;
}
.at-btn {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  border: none;
  background: #f2f2f7;
  color: #1c1c1e;
  padding: 4px 10px;
  border-radius: 16px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: background .15s ease;
}
.at-btn:hover { background: #e5e5ea; }
.at-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}
.at-tag {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: #007aff;
  background: rgba(0, 122, 255, 0.1);
  padding: 2px 4px 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
}
.at-tag-remove {
  width: 14px;
  height: 14px;
  line-height: 1;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #007aff;
  font-size: 12px;
  cursor: pointer;
}
.at-tag-remove:hover { background: rgba(0, 122, 255, 0.18); }

/* ===== 定时消息 ===== */
.timed-message-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(0, 122, 255, 0.05);
  max-height: 120px;
  overflow-y: auto;
}
.timed-message-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #fff;
  border: 1px solid rgba(0, 122, 255, 0.18);
  border-radius: 10px;
  font-size: 13px;
}
.timed-icon { color: #007aff; flex: none; }
.timed-text {
  flex: 1;
  color: #1c1c1e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.timed-time { flex: none; color: #007aff; font-size: 12px; }
.timed-cancel {
  flex: none;
  border: none;
  background: rgba(255, 59, 48, 0.1);
  color: #ff3b30;
  padding: 3px 10px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
}
.timed-cancel:hover { background: rgba(255, 59, 48, 0.18); }

.timed-dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 999;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
}
.timed-dialog {
  width: 380px;
  max-width: calc(100vw - 40px);
  background: #fff;
  border-radius: 16px;
  padding: 18px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.22);
  animation: popup .2s ease;
}
.timed-dialog-title {
  font-size: 16px;
  font-weight: 700;
  color: #1c1c1e;
  margin-bottom: 12px;
}
.summary-dialog {
  width: 420px;
}
.summary-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 22px 0;
  color: #8e8e93;
  font-size: 13px;
}
.summary-ring {
  width: 26px;
  height: 26px;
  border: 3px solid rgba(52, 120, 246, 0.25);
  border-top-color: #3478f6;
  border-radius: 50%;
  animation: summary-spin 0.9s linear infinite;
  margin-bottom: 10px;
}
@keyframes summary-spin { to { transform: rotate(360deg); } }
.summary-result {
  background: #f5f6fa;
  border-radius: 12px;
  padding: 14px;
  font-size: 14px;
  line-height: 1.7;
  color: #1c1c1e;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 240px;
  overflow-y: auto;
}
.summary-empty {
  padding: 24px 0;
  text-align: center;
  color: #8e8e93;
  font-size: 13px;
}
.summary-todos {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f5f6fa;
  max-height: 190px;
  overflow-y: auto;
}
.summary-todos-title {
  font-size: 12px;
  font-weight: 700;
  color: #636366;
  margin-bottom: 4px;
}
.summary-todo {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  padding: 6px 0;
  border-top: 1px solid rgba(60, 60, 67, 0.1);
}
.summary-todo:first-of-type { border-top: 0; }
.summary-todo-dot {
  width: 17px;
  height: 17px;
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(52, 120, 246, 0.14);
  color: #3478f6;
  font-size: 10px;
  font-weight: 700;
}
.summary-todo-text {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  line-height: 1.5;
  color: #1c1c1e;
  word-break: break-word;
}
.summary-todo-meta {
  flex: none;
  max-width: 96px;
  font-size: 10px;
  color: #8e8e93;
  text-align: right;
}
.summary-foot {
  margin-top: 8px;
  font-size: 11px;
  color: #8e8e93;
  text-align: right;
}
.timed-dialog-textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d1d6;
  border-radius: 10px;
  padding: 10px;
  font-size: 14px;
  font-family: inherit;
  resize: none;
  outline: none;
  background: #f8f8fa;
  color: #1c1c1e;
}
.timed-dialog-textarea:focus { border-color: #007aff; background: #fff; }
.timed-dialog-input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d1d6;
  border-radius: 10px;
  padding: 9px 10px;
  font-size: 14px;
  margin-top: 10px;
  outline: none;
  background: #f8f8fa;
  color: #1c1c1e;
}
.timed-dialog-quick { display: flex; gap: 8px; margin-top: 10px; }
.timed-quick-btn {
  padding: 5px 12px;
  border-radius: 14px;
  cursor: pointer;
  background: #f2f2f7;
  color: #007aff;
  font-size: 12px;
}
.timed-quick-btn:hover { background: #e5e5ea; }
.timed-dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}
.timed-dialog-cancel {
  border: none;
  background: #f2f2f7;
  color: #1c1c1e;
  padding: 8px 18px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 14px;
}
.timed-dialog-ok {
  border: none;
  background: #007aff;
  color: #fff;
  padding: 8px 18px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
}
.timed-dialog-ok:hover { background: #0066d6; }

/* ===== 窄屏自适应 =====
   聊天窗被压到 300px 上下时：消息头名字单行省略、时间与「@我」角标保持可见，
   悬浮入口与工具条已在上面按 640px 断点收紧。 */
@media (max-width: 640px) {
  .chat-window { overflow: hidden; }
  .message-header { gap: 4px; flex-wrap: wrap; row-gap: 2px; }
  .message-sender-username { flex: 0 0 auto; max-width: 100%; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .mention-me-badge { flex: 0 0 auto; }
  .message-time { flex: 0 0 auto; font-size: 10.5px; }
}

/* ===== 消息转发弹层 ===== */
.forward-dialog { display: flex; flex-direction: column; max-height: 72vh; }
.forward-preview {
  margin: 0 0 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--fill-secondary, rgba(120, 120, 128, 0.12));
  font-size: 12.5px;
  line-height: 1.5;
  color: var(--secondary-label, #8e8e93);
  max-height: 92px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  word-break: break-all;
}
.forward-hint {
  padding: 18px 0;
  text-align: center;
  font-size: 12.5px;
  color: var(--secondary-label, #8e8e93);
}
.forward-list {
  flex: 1;
  overflow-y: auto;
  max-height: 46vh;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.forward-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.16s ease;
}
.forward-item:hover { background: var(--fill-tertiary, rgba(120, 120, 128, 0.08)); }
.forward-avatar {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  object-fit: cover;
  flex: 0 0 30px;
  background: var(--fill-secondary, rgba(120, 120, 128, 0.12));
}
.forward-avatar--text {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--label, #1c1c1e);
}
.forward-name {
  flex: 1;
  font-size: 13px;
  color: var(--label, #1c1c1e);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.forward-actions { display: flex; justify-content: flex-end; padding-top: 10px; }
.forward-cancel {
  border: none;
  background: var(--fill-secondary, rgba(120, 120, 128, 0.12));
  color: var(--label, #1c1c1e);
  font-size: 13px;
  padding: 7px 18px;
  border-radius: 9px;
  cursor: pointer;
}

/* ===== 系统提示（群成员变更）最终覆盖层：必须排在 .message.sender .content 之后 ===== */
.message.system-message,
.message.sender.system-message,
.message.receiver.system-message {
  width: 100%;
  max-width: 100%;
  margin: 8px 0 14px;
  align-self: stretch;
  justify-content: center;
  align-items: center;
  text-align: center;
}

.message.system-message .message-content {
  max-width: 84%;
  margin: 0 auto;
  text-align: center;
}

.message.system-message .content,
.message.sender.system-message .content,
.message.receiver.system-message .content {
  padding: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
  border-radius: 0 !important;
  border: none !important;
  white-space: normal;
  text-align: center;
}

.message.system-message .system-message-body {
  display: flex;
  justify-content: center;
  width: 100%;
}

.message.system-message .system-message-text {
  display: inline-block;
  max-width: 100%;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(120, 120, 128, 0.12);
  color: var(--label-secondary);
  font-size: 12.5px;
  line-height: 1.55;
  text-align: center;
  word-break: break-word;
}
/* 窄屏下聊天窗口独占整屏，返回按钮接管「回到会话列表」 */
@media (max-width: 520px) {
  .mobile-back {
    display: inline-flex;
  }

  .chat-header {
    padding: 0 10px;
  }

  .close-chat {
    display: none;
  }
}

/* ===== 消息表情回应 ===== */
.message {
  position: relative;
}

.message-content {
  position: relative;
}

.reaction-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 5px;
  max-width: 100%;
}

.reaction-row.sender {
  justify-content: flex-end;
}

.reaction-pill {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  height: 23px;
  padding: 0 8px;
  border-radius: 11.5px;
  border: 1px solid var(--separator, rgba(60, 60, 67, 0.16));
  background: var(--card, rgba(255, 255, 255, 0.86));
  color: var(--label, #111);
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  transition: transform 0.16s ease, background 0.16s ease, border-color 0.16s ease;
}

.reaction-pill:hover {
  transform: translateY(-1px);
}

.reaction-pill.mine {
  background: color-mix(in srgb, var(--accent, #007aff) 13%, transparent);
  border-color: color-mix(in srgb, var(--accent, #007aff) 42%, transparent);
}

.reaction-pill.pending {
  opacity: 0.55;
  pointer-events: none;
}

.reaction-emoji {
  font-size: 14px;
}

.reaction-count {
  font-size: 11.5px;
  font-weight: 600;
  color: var(--secondary-label, #6e6e73);
}

.reaction-pill.mine .reaction-count {
  color: var(--accent, #007aff);
}

.reaction-trigger {
  position: absolute;
  top: -9px;
  right: -9px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 1px solid var(--separator, rgba(60, 60, 67, 0.16));
  background: var(--card, rgba(255, 255, 255, 0.95));
  box-shadow: 0 2px 7px rgba(0, 0, 0, 0.1);
  color: var(--secondary-label, #6e6e73);
  font-size: 13px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transform: scale(0.86);
  transition: opacity 0.15s ease, transform 0.15s ease;
  z-index: 3;
}

.message:hover .reaction-trigger,
.reaction-trigger:focus-visible {
  opacity: 1;
  transform: scale(1);
}

.message.sender .reaction-trigger {
  right: auto;
  left: -9px;
}

.reaction-strip {
  display: flex;
  gap: 2px;
  padding: 7px 8px 8px;
  border-bottom: 1px solid var(--separator, rgba(60, 60, 67, 0.12));
  flex-wrap: wrap;
}

.reaction-strip-btn {
  width: 32px;
  height: 32px;
  border: 1px solid transparent;
  border-radius: 9px;
  background: transparent;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  transition: background 0.14s ease, transform 0.14s ease;
}

.reaction-strip-btn:hover {
  background: var(--fill, rgba(118, 118, 128, 0.12));
  transform: translateY(-1px);
}

.reaction-strip-btn.active {
  background: color-mix(in srgb, var(--accent, #007aff) 14%, transparent);
  border-color: color-mix(in srgb, var(--accent, #007aff) 32%, transparent);
}

.reaction-picker {
  position: fixed;
  width: 320px;
  padding: 10px 10px 12px;
  border-radius: 16px;
  border: 1px solid var(--separator, rgba(60, 60, 67, 0.14));
  background: var(--card, rgba(255, 255, 255, 0.98));
  box-shadow: 0 16px 42px rgba(0, 0, 0, 0.18);
  z-index: 3300;
  backdrop-filter: blur(20px);
}

.reaction-picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2px 7px;
  font-size: 12px;
  font-weight: 600;
  color: var(--secondary-label, #6e6e73);
}

.reaction-picker-close {
  width: 22px;
  height: 22px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--secondary-label, #6e6e73);
  font-size: 17px;
  line-height: 1;
  cursor: pointer;
}

.reaction-picker-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 5px;
}

.reaction-picker-btn {
  height: 44px;
  border: 1px solid transparent;
  border-radius: 11px;
  background: var(--fill, rgba(118, 118, 128, 0.09));
  font-size: 23px;
  line-height: 1;
  cursor: pointer;
  transition: transform 0.14s ease, background 0.14s ease;
}

.reaction-picker-btn:hover {
  transform: translateY(-2px) scale(1.04);
  background: var(--fill-strong, rgba(118, 118, 128, 0.16));
}

.reaction-picker-btn.active {
  background: color-mix(in srgb, var(--accent, #007aff) 16%, transparent);
  border-color: color-mix(in srgb, var(--accent, #007aff) 38%, transparent);
}
</style>