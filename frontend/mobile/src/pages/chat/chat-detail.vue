<template>
	<view class="chat-detail-container">
			<WebSocketKeeper />
		<!-- 顶部导航栏 -->
		<view class="top-nav safe-top-bar">
			<view class="nav-content">
				<!-- 左侧：返回按钮 -->
				<view class="back-btn" @click="goBack" hover-class="btn-hover">
					<text class="back-icon"><</text>
				</view>

				<!-- 中间：用户名称 -->
				<view class="center-section" @click="goToUserProfile">
					<text class="user-name">{{ chatInfo.name }}</text>
					<text v-if="chatInfo.typing" class="typing-status">正在输入...</text>
				</view>

				<!-- 右侧：视频通话 + 更多按钮 -->
				<view class="nav-actions">
					<view v-if="chatType === 2 && !accessDenied" class="video-call-btn" @click="startGroupCall(false)" hover-class="btn-hover">
						<image src="/static/icons/call/phone.svg" class="video-call-icon-img" mode="aspectFit" />
					</view>
					<view v-if="chatType === 2 && !accessDenied" class="video-call-btn" @click="startGroupCall(true)" hover-class="btn-hover">
						<image src="/static/icons/call/video.svg" class="video-call-icon-img" mode="aspectFit" />
					</view>
					<view v-if="chatType === 1 && !accessDenied" class="video-call-btn" @click="startVideoCall" hover-class="btn-hover">
						<image src="/static/icons/call/video.svg" class="video-call-icon-img" mode="aspectFit" />
					</view>
					<view class="more-btn" @click="showMore" hover-class="btn-hover">
						<text class="more-icon">⋮</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 消息列表区域 -->
		<scroll-view
			class="message-list"
			:class="{ 'message-list-with-emoji': showEmoji }"
			scroll-y
			:scroll-into-view="scrollToView"
			:scroll-with-animation="false"
			:scroll-top="scrollTopValue"
			@scroll="onMessagesScroll"
		>
			<view class="message-content">
					<!-- 加载中提示 -->
					<view v-if="loading && pageNum > 1" class="loading-more-tip">
						<text class="loading-more-text">加载中...</text>
					</view>

				<!-- 已退群/被移出：历史接口会拒绝，不能只留一个空白会话 -->
				<view v-if="accessDenied" class="access-blocked">
					<view class="access-icon">
						<image class="access-icon-img" src="/static/icons/user/group.svg" mode="aspectFit" />
					</view>
					<text class="access-title">你已不在该群聊</text>
					<text class="access-desc">群成员才能查看聊天记录，这个会话已经没有可访问的消息了。</text>
					<button class="access-btn" hover-class="access-btn-hover" @click="removeUnavailableChat">从会话列表移除</button>
				</view>

				<!-- 加载完成提示 -->
				<view v-if="!hasMore && messageList.length > 0 && !accessDenied" class="load-end-tip">
					<text class="load-end-text">没有更多消息了</text>
				</view>

				<view
					v-for="(item, index) in messageList"
					:key="item.id || index"
					:id="'msg-' + (item.id || index)"
				>
					<!-- 时间戳 -->
					<view v-if="item.showTime" class="timestamp-wrapper">
						<view class="timestamp-tag">{{ item.time }}</view>
					</view>

					<!-- 对方消息 -->
					<view v-if="!item.isSelf" class="message-row message-row-other" :class="{ 'select-mode': selectionMode, 'select-on': isMessageSelected(item), 'has-reaction': showReactionBar(item) }" @click="onMessageRowClick(item)">
						<view v-if="selectionMode && canSelectMessage(item)" class="msg-select-box" :class="{ checked: isMessageSelected(item) }">
							<text v-if="isMessageSelected(item)" class="msg-select-check">✓</text>
						</view>
						<avatar :src="chatInfo.avatar" :size="64" />
						<view class="message-bubbles" :class="{ 'is-withdrawn': item.withdrawn }" @longpress="!selectionMode && openMessageMenu(item)">
								<view v-if="item.replyContent && !item.withdrawn" class="reply-quote" @click.stop="scrollToQuoted(item.replyToId)">
									<text class="reply-quote-text">{{ item.replyContent }}</text>
								</view>
								<view v-if="item.withdrawn" class="withdraw-tip">
									<text>{{ senderLabel(item) }}撤回了一条消息</text>
								</view>
							<!-- 文字消息 -->
							<view v-if="item.type === 'text' && !item.withdrawn" class="message-bubble bubble-other">
								<text class="message-text">{{ item.content }}</text>
							</view>

							<!-- 视频通话记录 -->
							<view v-else-if="item.type === 'call'" class="call-record-card">
								<view class="call-record-icon">
									<image src="/static/icons/call/video.svg" class="call-record-svg" mode="aspectFit" />
								</view>
								<view class="call-record-info">
									<text class="call-record-title">{{ callRecordOf(item).title }}</text>
									<text class="call-record-sub">{{ callRecordOf(item).subtitle }}</text>
								</view>
								<text v-if="callRecordOf(item).duration" class="call-record-duration">{{ callRecordOf(item).duration }}</text>
							</view>

							<!-- 群投票卡片 -->
							<view v-else-if="item.type === 'vote'" class="vote-message-card" @click="openVotePanel(item)">
								<view class="vote-card-head">
									<text class="vote-card-badge">群投票</text>
									<text class="vote-card-title">{{ voteTitleOf(item) }}</text>
								</view>
								<text class="vote-card-hint">{{ voteHintOf(item) }}</text>
							</view>

							<!-- 图片消息 -->
							<view v-else-if="item.type === 'image'" class="message-bubble bubble-other bubble-image">
								<image
									class="message-image"
									:src="item.content"
									mode="widthFix"
									@click="previewImage(item.content)"
									@error="imageLoadError"
									@load="imageLoadSuccess"
								/>
							</view>

							<!-- 语音消息 -->
							<view
								v-else-if="item.type === 'voice'"
								class="message-bubble bubble-other bubble-file-card"
								:class="{ 'voice-card-playing': voicePlayingId === item.id }"
								@click="playVoice(item)"
							>
								<view class="file-card-container">
									<view class="file-icon-circle">
										<image src="/static/icons/media/music-note.svg" class="file-icon-svg" mode="aspectFit" />
									</view>
									<view class="file-info">
										<text class="file-title">{{ item.duration || item.fileName || '语音' }}</text>
										<view class="file-meta">
											<text class="file-type-label">语音消息</text>
											<view class="audio-wave">
												<view class="wave-bar" style="height: 40%;"></view>
												<view class="wave-bar" style="height: 70%;"></view>
												<view class="wave-bar" style="height: 50%;"></view>
												<view class="wave-bar" style="height: 90%;"></view>
												<view class="wave-bar" style="height: 60%;"></view>
												<view class="wave-bar" style="height: 40%;"></view>
											</view>
											<text class="file-size">{{ item.fileSize || '12.4 MB' }}</text>
										</view>
									</view>
									<image src="/static/icons/media/play-arrow.svg" class="file-action-icon-svg" mode="aspectFit" />
								</view>
							</view>

							<!-- 视频消息 -->
							<view v-else-if="item.type === 'video'" class="message-bubble bubble-other bubble-file-card" @click="playVideo(item)">
								<view class="file-card-container">
									<view class="file-icon-circle">
										<image src="/static/icons/media/video-file.svg" class="file-icon-svg" mode="aspectFit" />
									</view>
									<view class="file-info">
										<text class="file-title">{{ item.fileName || 'video.mp4' }}</text>
										<view class="file-meta">
											<text class="file-type-label">视频文件</text>
											<text class="file-size">{{ item.fileSize || '45.8 MB' }}</text>
										</view>
									</view>
									<image src="/static/icons/media/play-arrow.svg" class="file-action-icon-svg" mode="aspectFit" />
								</view>
							</view>

							<!-- 文件消息 -->
							<view v-else-if="item.type === 'file'" class="message-bubble bubble-other bubble-file-card" @click="downloadFile(item.content, item.fileName)">
								<view class="file-card-container">
									<view class="file-icon-circle">
										<image src="/static/icons/media/document-file.svg" class="file-icon-svg" mode="aspectFit" />
									</view>
									<view class="file-info">
										<text class="file-title">{{ item.fileName || 'document.pdf' }}</text>
										<view class="file-meta">
											<text class="file-type-label">文档</text>
											<text class="file-size">{{ item.fileSize || '1.2 MB' }}</text>
										</view>
									</view>
									<image src="/static/icons/media/download.svg" class="file-action-icon-svg" mode="aspectFit" />
								</view>
							</view>

							<!-- 合并转发卡片 -->
							<view v-else-if="item.type === 'chatRecord'" class="merge-card" :class="{ 'merge-card-self': item.isSelf }" @click.stop="openMergeViewer(item)">
								<view class="merge-card-head">
									<text class="merge-card-title">聊天记录</text>
									<text class="merge-card-count">{{ mergeCountOf(item) }} 条</text>
								</view>
								<text class="merge-card-preview">{{ mergePreviewOf(item) }}</text>
								<view class="merge-card-foot">
									<text>聊天记录</text>
									<text class="merge-card-arrow">›</text>
								</view>
							</view>
							<!-- 未知类型 -->
							<view v-else-if="!item.withdrawn" class="message-bubble bubble-other">
								<text class="message-text" style="color: red;">未知类型: {{ item.type }} - {{ item.content }}</text>
							</view>

							<!-- 译文（PC 同款消息翻译） -->
							<view v-if="item.type === 'text' && item.translation && !item.withdrawn" class="translation-box">
								<view class="translation-head">
									<text class="translation-label">{{ item.translationLabel || '译文' }}</text>
									<text class="translation-close" @click.stop="hideTranslation(item)">收起</text>
								</view>
								<text class="translation-text">{{ item.translation }}</text>
							</view>
						</view>
							<!-- 表情回应聚合条 -->
							<!-- 已编辑标记 -->
							<view v-if="item.edited && !item.withdrawn" class="edited-tag edited-tag-other">
								<text>已编辑</text>
							</view>
							<view v-if="showReactionBar(item)" class="reaction-row reaction-row-other">
								<view
									v-for="reaction in item.reactions"
									:key="reaction.emoji"
									class="reaction-pill"
									:class="{ 'reaction-pill-mine': reaction.mine }"
									@click.stop="toggleReaction(item, reaction.emoji)"
								>
									<text class="reaction-emoji">{{ reaction.emoji }}</text>
									<text class="reaction-count">{{ reaction.count }}</text>
								</view>
								<view class="reaction-add" @click.stop="openReactionSheet(item)">
									<text>☺</text>
								</view>
							</view>
					</view>

					<!-- 自己消息 -->
					<view v-else class="message-row message-row-self" :class="{ 'select-mode': selectionMode, 'select-on': isMessageSelected(item), 'has-reaction': showReactionBar(item) }" @click="onMessageRowClick(item)">
						<view v-if="selectionMode && canSelectMessage(item)" class="msg-select-box" :class="{ checked: isMessageSelected(item) }">
							<text v-if="isMessageSelected(item)" class="msg-select-check">✓</text>
						</view>
						<view class="message-bubbles message-bubbles-self" :class="{ 'is-withdrawn': item.withdrawn }" @longpress="!selectionMode && openMessageMenu(item)">
								<view v-if="item.replyContent && !item.withdrawn" class="reply-quote" @click.stop="scrollToQuoted(item.replyToId)">
									<text class="reply-quote-text">{{ item.replyContent }}</text>
								</view>
								<view v-if="item.withdrawn" class="withdraw-tip">
									<text>{{ senderLabel(item) }}撤回了一条消息</text>
								</view>
							<!-- 文字消息 -->
							<view v-if="item.type === 'text' && !item.withdrawn" class="message-bubble bubble-self">
								<text class="message-text message-text-self">{{ item.content }}</text>
							</view>

							<!-- 视频通话记录 -->
							<view v-else-if="item.type === 'call'" class="call-record-card call-record-card-self">
								<view class="call-record-icon call-record-icon-self">
									<image src="/static/icons/call/video.svg" class="call-record-svg" mode="aspectFit" />
								</view>
								<view class="call-record-info">
									<text class="call-record-title call-record-title-self">{{ callRecordOf(item).title }}</text>
									<text class="call-record-sub call-record-sub-self">{{ callRecordOf(item).subtitle }}</text>
								</view>
								<text v-if="callRecordOf(item).duration" class="call-record-duration call-record-duration-self">{{ callRecordOf(item).duration }}</text>
							</view>

							<!-- 群投票卡片 -->
							<view v-else-if="item.type === 'vote'" class="vote-message-card vote-message-card-self" @click="openVotePanel(item)">
								<view class="vote-card-head">
									<text class="vote-card-badge">群投票</text>
									<text class="vote-card-title">{{ voteTitleOf(item) }}</text>
								</view>
								<text class="vote-card-hint">{{ voteHintOf(item) }}</text>
							</view>

							<!-- 图片消息 -->
							<view v-else-if="item.type === 'image'" class="message-bubble bubble-self bubble-image">
								<image
									class="message-image"
									:src="item.content"
									mode="widthFix"
									@click="previewImage(item.content)"
									@error="imageLoadError"
									@load="imageLoadSuccess"
								/>
							</view>

							<!-- 语音消息 -->
							<view
								v-else-if="item.type === 'voice'"
								class="message-bubble bubble-self bubble-file-card"
								:class="{ 'voice-card-playing': voicePlayingId === item.id }"
								@click="playVoice(item)"
							>
								<view class="file-card-container">
									<view class="file-icon-circle file-icon-circle-self">
										<image src="/static/icons/media/music-note.svg" class="file-icon-svg" mode="aspectFit" />
									</view>
									<view class="file-info">
										<text class="file-title file-title-self">{{ item.duration || item.fileName || '语音' }}</text>
										<view class="file-meta">
											<text class="file-type-label file-type-label-self">语音消息</text>
											<view class="audio-wave">
												<view class="wave-bar wave-bar-self" style="height: 40%;"></view>
												<view class="wave-bar wave-bar-self" style="height: 70%;"></view>
												<view class="wave-bar wave-bar-self" style="height: 50%;"></view>
												<view class="wave-bar wave-bar-self" style="height: 90%;"></view>
												<view class="wave-bar wave-bar-self" style="height: 60%;"></view>
												<view class="wave-bar wave-bar-self" style="height: 40%;"></view>
											</view>
											<text class="file-size file-size-self">{{ item.fileSize || '12.4 MB' }}</text>
										</view>
									</view>
									<image src="/static/icons/media/play-arrow.svg" class="file-action-icon-svg file-action-icon-svg-self" mode="aspectFit" />
								</view>
							</view>

							<!-- 视频消息 -->
							<view v-else-if="item.type === 'video'" class="message-bubble bubble-self bubble-file-card" @click="playVideo(item)">
								<view class="file-card-container">
									<view class="file-icon-circle file-icon-circle-self">
										<image src="/static/icons/media/video-file.svg" class="file-icon-svg" mode="aspectFit" />
									</view>
									<view class="file-info">
										<text class="file-title file-title-self">{{ item.fileName || 'video.mp4' }}</text>
										<view class="file-meta">
											<text class="file-type-label file-type-label-self">视频文件</text>
											<text class="file-size file-size-self">{{ item.fileSize || '45.8 MB' }}</text>
										</view>
									</view>
									<image src="/static/icons/media/play-arrow.svg" class="file-action-icon-svg file-action-icon-svg-self" mode="aspectFit" />
								</view>
							</view>

							<!-- 文件消息 -->
							<view v-else-if="item.type === 'file'" class="message-bubble bubble-self bubble-file-card" @click="downloadFile(item.content, item.fileName)">
								<view class="file-card-container">
									<view class="file-icon-circle file-icon-circle-self">
										<image src="/static/icons/media/document-file.svg" class="file-icon-svg" mode="aspectFit" />
									</view>
									<view class="file-info">
										<text class="file-title file-title-self">{{ item.fileName || 'document.pdf' }}</text>
										<view class="file-meta">
											<text class="file-type-label file-type-label-self">文档</text>
											<text class="file-size file-size-self">{{ item.fileSize || '1.2 MB' }}</text>
										</view>
									</view>
									<image src="/static/icons/media/download.svg" class="file-action-icon-svg file-action-icon-svg-self" mode="aspectFit" />
								</view>
							</view>

							<!-- 合并转发卡片 -->
							<view v-else-if="item.type === 'chatRecord'" class="merge-card" :class="{ 'merge-card-self': item.isSelf }" @click.stop="openMergeViewer(item)">
								<view class="merge-card-head">
									<text class="merge-card-title">聊天记录</text>
									<text class="merge-card-count">{{ mergeCountOf(item) }} 条</text>
								</view>
								<text class="merge-card-preview">{{ mergePreviewOf(item) }}</text>
								<view class="merge-card-foot">
									<text>聊天记录</text>
									<text class="merge-card-arrow">›</text>
								</view>
							</view>
							<!-- 未知类型 -->
							<view v-else-if="!item.withdrawn" class="message-bubble bubble-self">
								<text class="message-text message-text-self" style="color: #ff0000;">未知类型: {{ item.type }} - {{ item.content }}</text>
							</view>

							<!-- 译文 -->
							<view v-if="item.type === 'text' && item.translation && !item.withdrawn" class="translation-box translation-box-self">
								<view class="translation-head">
									<text class="translation-label">{{ item.translationLabel || '译文' }}</text>
									<text class="translation-close" @click.stop="hideTranslation(item)">收起</text>
								</view>
								<text class="translation-text">{{ item.translation }}</text>
							</view>

							<!-- 已编辑标记 -->
							<view v-if="item.edited && !item.withdrawn" class="edited-tag">
								<text>已编辑</text>
							</view>

							<!-- 表情回应聚合条 -->
							<view v-if="showReactionBar(item)" class="reaction-row reaction-row-self">
								<view
									v-for="reaction in item.reactions"
									:key="reaction.emoji"
									class="reaction-pill"
									:class="{ 'reaction-pill-mine': reaction.mine }"
									@click.stop="toggleReaction(item, reaction.emoji)"
								>
									<text class="reaction-emoji">{{ reaction.emoji }}</text>
									<text class="reaction-count">{{ reaction.count }}</text>
								</view>
								<view class="reaction-add" @click.stop="openReactionSheet(item)">
									<text>☺</text>
								</view>
							</view>
							<!-- 已读状态 -->
							<view v-if="item.read && !item.withdrawn" class="read-status">
								<text class="read-icon">✓✓</text>
								<text class="read-text">已读</text>
							</view>

							<!-- 群聊已读人数与名单 -->
							<view
								v-if="chatType === 2 && item.id && item.isSelf && !item.withdrawn"
								class="group-read-status"
								@click.stop="toggleGroupReaders(item.id)"
							>
								<text class="group-read-text">{{ groupReadLabel(item.id) }}</text>
								<view v-if="activeReadId === item.id" class="group-read-names">
									<text v-if="!readNamesOf(item.id).length" class="group-read-empty">暂无成员已读</text>
									<text v-for="(name, rIndex) in readNamesOf(item.id)" :key="rIndex" class="group-read-name">{{ name }}</text>
								</view>
							</view>
						</view>
					</view>
				</view>
			</view>
		</scroll-view>

		<!-- 表情面板 -->
		<view v-if="showEmoji" class="emoji-panel">
			<scroll-view class="emoji-scroll" scroll-y>
				<view class="emoji-grid">
					<view
						v-for="emoji in emojiList"
						:key="emoji"
						class="emoji-item"
						hover-class="emoji-item-hover"
						@click="insertEmoji(emoji)"
					>
						<text class="emoji-char">{{ emoji }}</text>
					</view>
				</view>
			</scroll-view>
		</view>

		<!-- 引用回复预览 -->
		<view v-if="replyTarget" class="reply-bar">
			<view class="reply-bar-main">
				<text class="reply-bar-label">回复 {{ replyTarget.name || '对方' }}</text>
				<text class="reply-bar-text">{{ replyTarget.message }}</text>
			</view>
			<text class="reply-bar-close" @click="cancelReply">×</text>
		</view>

		<!-- 多选操作条 -->
		<view v-if="selectionMode" class="selection-bar">
			<view class="selection-info">
				<text class="selection-count">已选 {{ selectedMessages.length }} 条</text>
				<text class="selection-hint">点击消息继续选择</text>
			</view>
			<view class="selection-cancel" @click.stop="exitSelectionMode">取消</view>
			<view class="selection-forward" :class="{ disabled: !selectedMessages.length }" @click.stop="forwardSelected">合并转发</view>
		</view>

		<!-- 底部输入区域 -->
		<view class="input-area" v-if="!accessDenied">
			<view class="input-content">
				<!-- 附件按钮 -->
				<view class="attach-btn" @click="showAttachMenu" hover-class="attach-btn-hover">
					<text class="attach-icon">+</text>
				</view>

				<!-- 输入框容器 -->
				<view class="input-wrapper">
					<textarea
						class="text-input"
						v-model="inputText"
						placeholder="说点什么..."
						placeholder-style="color: rgba(90, 95, 103, 0.5); font-size: 28rpx;"
						:adjust-position="true"
						:auto-height="true"
						:maxlength="500"
						@focus="onInputFocus"
						@blur="onInputBlur"
					/>
					<view class="emoji-btn" @click="showEmojiPanel" hover-class="emoji-btn-hover">
						<image src="/static/icons/profile/emoji.svg" class="emoji-icon" mode="aspectFit" />
					</view>
					<view v-if="chatType === 2" class="at-btn-inline" @click="openAtPanel">
						<text class="at-icon">@</text>
					</view>
				</view>

				<!-- 语音按钮 -->
				<view
					class="voice-btn"
					:class="{ 'voice-btn-recording': isRecording }"
					@touchstart.stop.prevent="startRecording"
					@touchmove.stop.prevent="onRecordingMove"
					@touchend.stop.prevent="stopRecording"
					@touchcancel.stop.prevent="cancelRecording"
				>
					<text class="voice-icon">🎤</text>
				</view>

				<!-- 发送按钮 -->
				<view
					class="send-btn"
					:class="{ 'send-btn-active': canSend }"
					@click="sendMessage"
					hover-class="send-btn-hover"
				>
          <text class="send-icon">➤</text>
				</view>
			</view>
		</view>

		<!-- 录音浮层 -->
		<view v-if="isRecording" class="record-overlay">
			<view class="record-panel">
				<view class="record-pulse"></view>
				<text class="record-time">{{ recordSeconds }}″</text>
				<text class="record-hint">{{ recordWillCancel ? '松开手指，取消发送' : '松开发送，上滑取消' }}</text>
			</view>
		</view>

		<!-- @提及成员 -->
		<view v-if="atPanel.show" class="sheet-mask" @click="closeAtPanel">
			<view class="sheet-panel" @click.stop>
				<view class="sheet-head">
					<text class="sheet-title">@ 群成员</text>
					<text class="sheet-close" @click="closeAtPanel">×</text>
				</view>
				<scroll-view class="sheet-scroll" scroll-y>
					<view v-if="atPanel.loading" class="sheet-empty">加载成员中…</view>
					<view v-else-if="!atPanel.members.length" class="sheet-empty">暂无可提及成员</view>
					<view
						v-for="member in atPanel.members"
						:key="member.id"
						class="member-row"
						@click="selectAtMember(member)"
					>
						<avatar :src="member.avatar" :size="64" />
						<view class="member-meta">
							<text class="member-name">{{ member.name }}</text>
							<text class="member-role">{{ member.roleLabel }}</text>
						</view>
					</view>
				</scroll-view>
			</view>
		</view>

		<!-- 转发会话选择 -->
		<view v-if="flowPanel.show" class="sheet-mask" @click="closeFlowPanel">
			<view class="sheet-panel" @click.stop>
				<view class="sheet-head">
					<text class="sheet-title">{{ forwardMode === 'merge' ? '合并转发给…' : '转发给…' }}</text>
					<text class="sheet-close" @click="closeFlowPanel">×</text>
				</view>
				<scroll-view class="sheet-scroll" scroll-y>
					<view v-if="flowPanel.loading" class="sheet-empty">加载会话中…</view>
					<view v-else-if="!flowPanel.list.length" class="sheet-empty">暂无可转发的会话</view>
					<view
						v-for="chat in flowPanel.list"
						:key="chat.chatId"
						class="member-row"
						@click="forwardToChat(chat)"
					>
						<avatar :src="chat.avatar" :size="64" />
						<view class="member-meta">
							<text class="member-name">{{ chat.name }}</text>
							<text class="member-role">{{ chat.chatType === 2 ? '群聊' : '好友' }}</text>
						</view>
					</view>
				</scroll-view>
			</view>
		</view>

		<!-- 群投票详情 -->
		<view v-if="votePanel.show" class="sheet-mask" @click="closeVotePanel">
			<view class="sheet-panel vote-panel" @click.stop>
				<view class="sheet-head">
					<text class="sheet-title">{{ voteDetail ? voteDetail.title : '群投票' }}</text>
					<text class="sheet-close" @click="closeVotePanel">×</text>
				</view>
				<scroll-view class="sheet-scroll" scroll-y>
					<view v-if="voteLoading[votePanel.voteId]" class="sheet-empty">加载投票中…</view>
					<view v-else-if="voteDetail" class="vote-detail-body">
						<view
							v-for="opt in (voteDetail.options || [])"
							:key="opt.index"
							class="vote-option"
							:class="{ 'vote-option-mine': isMyVote(opt.index), 'vote-option-closed': voteDetail.status === 1 }"
							@click="castVote(opt.index)"
						>
							<view class="vote-option-head">
								<text class="vote-option-text">{{ opt.text }}</text>
								<text class="vote-option-count">{{ opt.count || 0 }} 票</text>
							</view>
							<view class="vote-bar"><view class="vote-bar-fill" :style="{ width: votePercent(opt) + '%' }"></view></view>
							<text v-if="isMyVote(opt.index)" class="vote-mine">已投</text>
						</view>
						<view class="vote-detail-foot">
							<text>{{ voteDetail.total || 0 }} 人参与{{ voteDetail.multi ? ' · 多选' : '' }}</text>
							<text v-if="voteDetail.status === 1" class="vote-closed">已结束</text>
						</view>
						<view class="vote-actions">
							<text v-if="voteDetail.status === 0 && hasMyVote" class="vote-action" @click="cancelMyVote">取消投票</text>
							<text v-if="voteDetail.status === 0 && isVoteCreator" class="vote-action" @click="endVote">结束投票</text>
						</view>
					</view>
					<view v-else class="sheet-empty">投票不存在或已删除</view>
				</scroll-view>
			</view>
		</view>
		<!-- 表情回应选择器 -->
		<view v-if="reactionSheet.show" class="reaction-sheet-mask" @click="closeReactionSheet">
			<view class="reaction-sheet" @click.stop>
				<view class="reaction-sheet-handle"></view>
				<text class="reaction-sheet-title">选择一个回应</text>
				<view class="reaction-grid">
					<view
						v-for="emoji in reactionEmojis"
						:key="emoji"
						class="reaction-choice"
						:class="{ 'reaction-choice-active': hasMyReaction(reactionSheet.target, emoji) }"
						@click="toggleReaction(reactionSheet.target, emoji)"
					>
						<text>{{ emoji }}</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 合并转发查看器 -->
		<view v-if="mergeViewer.show" class="merge-viewer-mask" @click="closeMergeViewer">
			<view class="merge-viewer" @click.stop>
				<view class="merge-viewer-head">
					<text class="merge-viewer-title">{{ mergeViewer.title }}</text>
					<text class="merge-viewer-close" @click="closeMergeViewer">完成</text>
				</view>
				<scroll-view scroll-y class="merge-viewer-scroll">
					<view v-for="row in mergeViewer.items" :key="row.id" class="merge-viewer-row">
						<text class="merge-viewer-sender">{{ row.sender }}</text>
						<text class="merge-viewer-content">{{ row.content }}</text>
					</view>
					<view v-if="!mergeViewer.items.length" class="merge-viewer-empty">暂无内容</view>
				</scroll-view>
			</view>
		</view>

		<!-- 编辑消息浮层 -->
		<view v-if="editPanel.show" class="edit-message-mask" @click="closeEditPanel">
			<view class="edit-message-panel" @click.stop>
				<view class="edit-message-head">
					<text class="edit-message-title">编辑消息</text>
					<text class="edit-message-close" @click="closeEditPanel">×</text>
				</view>
				<textarea
					v-model="editPanel.draft"
					class="edit-message-textarea"
					maxlength="20000"
					:auto-height="true"
					placeholder="输入新的内容"
				/>
				<view class="edit-message-tip">5 分钟内可编辑，保存后对方会同步看到</view>
				<view class="edit-message-actions">
					<button class="edit-message-btn" @click="closeEditPanel">取消</button>
					<button class="edit-message-btn primary" :disabled="editPanel.saving" @click="submitEditMessage">{{ editPanel.saving ? '保存中…' : '保存' }}</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import Avatar from '@/components/avatar/avatar.vue';
import { normalizeMediaUrl } from '@/utils/media.js';
import { getHistoryMes, getLastMes, getMessageInteractions, toggleMessageReaction, toggleMessageFavorite, editMessage, saveConversationSetting, getConversationSetting, hideChat } from '@/api/chat.js';
import {  uploadFile, getFileType } from '@/api/file.js';
import { smartReply, translateMessage } from '@/api/ai.js';
import { getGroupMembers, getVoteDetail, submitVote, cancelVote, closeVote } from '@/api/group.js';
import { getWebSocketClient, subscribeMessage, unsubscribeMessage } from '@/utils/websocket.js';
import {
	getMessageType as resolveMessageType,
	parseMessageContent as parseMessageContentModel,
	getFileNameFromUrl as getFileNameFromUrlModel,
	optimizeFileName as optimizeFileNameModel,
	getDefaultFileName as getDefaultFileNameModel,
	formatFileSize as formatFileSizeModel,
	formatDuration as formatDurationModel
} from '@/models/message.js';
import WebSocketKeeper from '@/components/WebSocketKeeper.vue'
import { prepareVideoStream, releasePendingStream, describeMediaError, isMediaSupported } from '@/utils/videoCallMedia.js'
import { getActiveCall, createCallId, prepareCallIntent, clearCallIntent } from '@/utils/videoCallBridge.js'
import { primeCallAudio } from '@/utils/callAudio.js'

export default {
	components: {
		WebSocketKeeper,
		Avatar
	},
	data() {
			return {
				chatId: '',
				friendId: '', // 好友ID（receiverId）
				chatType: 1, // 聊天类型（1：单聊；2：群聊）
				chatInfo: {
					name: '',
					avatar: '',
					online: false,
					typing: false
				},
				userInfo: {
					userId: '',
					avatar: ''
				},
				messageList: [],
				inputText: '',
				draftTimer: null,
				draftRestoring: false,
				replyTarget: null,
				// PC 对齐能力：转发、翻译、AI 回复、@提及、投票、群已读
				translations: {},
				translateLoading: {},
				atUsers: [],
				atPanel: { show: false, loading: false, members: [] },
				groupReaders: {},
				activeReadId: '',
				groupMemberCount: 0,
				voteDetails: {},
				voteLoading: {},
				votePending: false,
				votePanel: { show: false, voteId: '' },
				flowLoading: false,
				actionTarget: null,
				editPanel: { show: false, saving: false, target: null, draft: '' },
				flowPanel: { show: false, loading: false, list: [] },
				// 多选与合并转发（与 PC 共用同一套消息协议）
				selectionMode: false,
				selectedMessages: [],
				selectionLimit: 100,
				forwardMode: 'single',
				forwardPayload: null,
				// 消息表情回应与收藏（与 PC 共用同一套接口）
				reactionEmojis: ['👍', '❤️', '😂', '😮', '😢', '👏', '🔥', '🎉'],
				reactionSheet: { show: false, target: null },
				reactionPending: {},
				favoritePending: {},
				// 合并转发查看器
				mergeViewer: { show: false, items: [], title: '聊天记录' },
				// 录音与语音播放状态
				isRecording: false,
				recordSeconds: 0,
				recordTimer: null,
				recordStartY: 0,
				recordWillCancel: false,
				recordStartTime: 0,
				recordPendingStop: false,
				recordDiscard: false,
				mediaRecorder: null,
				recordStream: null,
				audioChunks: [],
				nativeRecorder: null,
				voiceAudio: null,
				voicePlayingId: '',
				showEmoji: false,
				emojiList: [
					'😀', '😄', '😁', '😆', '😅', '🤣', '😂', '🙂',
					'😉', '😊', '😍', '🥰', '😘', '😋', '😎', '🤩',
					'😏', '😢', '😭', '😤', '😡', '🥺', '😴', '🤔',
					'🤗', '🤭', '🤫', '😬', '🙄', '😮', '😱', '😇',
					'👍', '👎', '👏', '🙏', '🤝', '💪', '👌', '✌️',
					'❤️', '💔', '🔥', '✨', '🎉', '🎁', '🌹', '🍉',
					'☕', '🍜', '🌙', '☀️', '⭐', '💯', '✅', '❌'
				],
				scrollToView: '',
				scrollTopValue: 0,
				anchorMessageId: '', // 锚点消息ID（用于恢复滚动位置）
				preloadThreshold: 800, // 预加载阈值（距离顶部800px时触发，提前加载）
				isPreloading: false, // 是否正在预加载
				lastTriggerScrollTop: -1, // 上次触发预加载的scrollTop
				keyboardHeight: 0,
				// 分页相关
				pageNum: 1,
				pageSize: 20,
				totalPage: 0,
				hasMore: true,
				loading: false,
				// 已退群/被移出后历史接口会拒绝，界面要给明确出口而不是空白
				accessDenied: false,
				firstShowDone: false,
				// 滚动节流
				scrollThrottleTimer: null,
				// 已读回执相关
				visibleUnReadMessages: new Set(),
				scrollUnReadTimer: null,
				lastScrollTop: 0,
				lastScrollTime: 0,
				scrollSpeed: 0,
				fastScrollThreshold: 2, // px/ms
				fastSubmitDelay: 1000,
				slowSubmitDelay: 500
				};
			},
		computed: {
			canSend() {
				return this.inputText.trim().length > 0;
			},
			voteDetail() {
				const id = this.votePanel && this.votePanel.voteId
				return id ? (this.voteDetails[id] || null) : null
			},
			hasMyVote() {
				if (!this.voteDetail || !this.voteDetail.options) return false
				return this.voteDetail.options.some(opt => this.isMyVote(opt.index))
			},
			isVoteCreator() {
				const detail = this.voteDetail
				if (!detail || detail.creatorId == null) return false
				return String(detail.creatorId) === String(this.userInfo.userId)
			}
		},
		onLoad(options) {
			if (options.id) {
				this.chatId = options.id;
				this.friendId = options.id; // 好友ID即为聊天ID
			}
			if (options.name) {
				this.chatInfo.name = decodeURIComponent(options.name);
			}
			if (options.chatType) {
				this.chatType = parseInt(options.chatType);
			}
			if (options.avatar) {
				this.chatInfo.avatar = decodeURIComponent(options.avatar);
			}

			this.loadUserInfo();
			this.loadChatInfo();
			this.loadHistoryMessages();
			this.restoreDraft();
			// 通话记录页“回拨”：进入会话后自动发起视频通话，等页面与成员信息就绪
			if (options.autoVideoCall === '1' && this.chatType === 1) {
				this.autoVideoCallTimer = setTimeout(() => {
					this.startVideoCall();
				}, 900);
			}
		},
		onShow() {
			// 订阅 WebSocket 消息
			subscribeMessage(this.handleWebSocketMessage);
			// 从通话页返回时补齐通话记录消息；首次打开由 onLoad 负责，避免重复请求。
			if (this.firstShowDone) {
				this.refreshHistoryMessages();
			} else {
				this.firstShowDone = true;
			}
		},
		onHide() {
		// 离开会话时落一次草稿
		this.flushDraft();
		// 取消订阅 WebSocket 消息
		unsubscribeMessage(this.handleWebSocketMessage);
	},
	onUnload() {
		// 取消订阅 WebSocket 消息
		unsubscribeMessage(this.handleWebSocketMessage);
		// 清理定时器
		if (this.autoVideoCallTimer) {
			clearTimeout(this.autoVideoCallTimer);
			this.autoVideoCallTimer = null;
		}
		if (this.scrollThrottleTimer) {
			clearTimeout(this.scrollThrottleTimer);
		}
		if (this.scrollUnReadTimer) {
			clearTimeout(this.scrollUnReadTimer);
		}
	},
	watch: {
		inputText(val) {
			if (this.draftRestoring) return;
			if (this.draftTimer) clearTimeout(this.draftTimer);
			this.draftTimer = setTimeout(() => this.flushDraft(String(val || '')), 500);
		}
	},
	methods: {
		draftStorageKey() {
			return `chatDraft_m_${this.chatId || ''}`;
		},
		flushDraft(value) {
			if (this.draftTimer) {
				clearTimeout(this.draftTimer);
				this.draftTimer = null;
			}
			if (!this.chatId) return;
			const text = value == undefined ? String(this.inputText || '') : value;
			try {
				if (text.trim()) uni.setStorageSync(this.draftStorageKey(), text);
				else uni.removeStorageSync(this.draftStorageKey());
			} catch (e) {}
			saveConversationSetting({ chatId: this.chatId, chatType: this.chatType, draft: text }).catch(() => {});
		},
		restoreDraft() {
			if (!this.chatId) return;
			let local = '';
			try { local = uni.getStorageSync(this.draftStorageKey()) || ''; } catch (e) {}
			if (local) {
				this.draftRestoring = true;
				this.inputText = local;
				this.$nextTick(() => { this.draftRestoring = false; });
			}
			getConversationSetting({ chatId: this.chatId, chatType: this.chatType }).then(res => {
				const remote = res && res.data ? String(res.data.draft || '') : '';
				if (!remote) return;
				if (String(this.inputText || '').trim()) return;
				this.draftRestoring = true;
				this.inputText = remote;
				try { uni.setStorageSync(this.draftStorageKey(), remote); } catch (e) {}
				this.$nextTick(() => { this.draftRestoring = false; });
			}).catch(() => {});
		},
		clearDraft() {
			if (!this.chatId) return;
			try { uni.removeStorageSync(this.draftStorageKey()); } catch (e) {}
			saveConversationSetting({ chatId: this.chatId, chatType: this.chatType, draft: '' }).catch(() => {});
		},

		loadUserInfo() {
			const userInfoStr = uni.getStorageSync('userInfo');
			if (userInfoStr) {
				try {
					const info = JSON.parse(userInfoStr);
					this.userInfo = {
						userId: info.id,
						avatar: info.avatar || ''
					};
				} catch (e) {
					console.error('解析用户信息失败:', e);
				}
			}
		},
		loadChatInfo() {
			// 从localStorage读取当前聊天窗口信息
			try {
				const currentChatStr = uni.getStorageSync('currentChat');
				if (currentChatStr) {
					const currentChat = JSON.parse(currentChatStr);
					const sameChat = String(currentChat.chatId || '') === String(this.chatId || '');
					if (!this.chatId || sameChat) {
						console.log('读取到存储的聊天信息:', currentChat);

						// 使用存储的聊天信息
						this.chatInfo = {
							name: currentChat.chatName || this.chatInfo.name || '未知用户',
							avatar: currentChat.avatar || this.chatInfo.avatar || '',
							online: currentChat.isOnline || false,
							typing: false
						};

						// 更新chatId和chatType
						if (currentChat.chatId) {
							this.chatId = currentChat.chatId;
							this.friendId = currentChat.chatId;
						}
						if (currentChat.chatType) {
							this.chatType = currentChat.chatType;
						}

						console.log('聊天信息已加载:', this.chatInfo);
					}
				} else {
					console.warn('未找到存储的聊天信息，使用默认值');
					// 使用URL参数或默认值
					this.chatInfo = {
						name: this.chatInfo.name || '未知用户',
						avatar: '',
						online: false,
						typing: false
					};
				}
			} catch (e) {
				console.error('读取聊天信息失败:', e);
				// 使用URL参数或默认值
				this.chatInfo = {
					name: this.chatInfo.name || '未知用户',
					avatar: '',
					online: false,
					typing: false
				};
			}
		},
			/**
			 * 加载历史消息
			 */
			/**
			 * 已退群/被移出的会话无法恢复，给用户一个干净的移除出口
			 */
			async removeUnavailableChat() {
				try {
					const res = await hideChat(this.friendId);
					if (res && (res.code === 200 || res.code === 0)) {
						uni.showToast({ title: '已从会话列表移除', icon: 'none' });
						setTimeout(() => uni.navigateBack(), 400);
					} else {
						uni.showToast({ title: (res && res.msg) || '移除失败', icon: 'none' });
					}
				} catch (error) {
					uni.showToast({ title: (error && error.msg) || '移除失败', icon: 'none' });
				}
			},
			async loadHistoryMessages() {
				if (this.loading || !this.hasMore) return;

				this.loading = true;
				this.accessDenied = false;
				try {
					const res = await getHistoryMes({
						pageNum: this.pageNum,
						pageSize: this.pageSize,
						senderId: this.userInfo.userId,
						receiverId: this.friendId,
						chatType: this.chatType
					}, { silent: true });

					if (res.code === 200 && res.data && res.data.rows) {
						const messages = this.formatMessages(res.data.rows);

						// 群聊已读回填：与 PC 一样使用历史消息里的 readers / readCount
						if (this.chatType === 2) {
							const seed = {};
							res.data.rows.forEach(msg => {
								if (String(msg.senderId) !== String(this.userInfo.userId)) return;
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
							this.groupReaders = { ...seed, ...this.groupReaders };
						}

						if (this.pageNum === 1) {
							// 第一页：直接替换并滚动到底部
							this.messageList = messages;
							this.$nextTick(() => {
								this.scrollToBottom();
								// 如果消息数量较少，直接发送所有未读消息
								if (messages.length <= 20) {
									this.submitAllUnreadMessages();
								}
							});
						} else {
							// 后续页：插入到顶部，通过高度差值恢复滚动位置
							const savedScrollTop = this.lastScrollTop;

							// 获取插入前的内容高度
							const oldHeight = await this.getContentHeight();

							// 将新消息插入到顶部
							this.messageList = [...messages, ...this.messageList];

							// 等待 DOM 更新，然后恢复滚动位置
							this.$nextTick(() => {
								this.getContentHeight().then(newHeight => {
									const heightDiff = newHeight - oldHeight;
									// 清除 scroll-into-view 避免冲突
									this.scrollToView = '';
									// 通过 scrollTop 精确恢复位置，不会跳动
									this.scrollTopValue = savedScrollTop + heightDiff;
									this.isPreloading = false;
								});
							});
						}

						// 更新分页信息
						this.totalPage = res.data.totalPage;
						this.hasMore = this.pageNum < this.totalPage;

						console.log(`✓ 加载第${this.pageNum}页消息，还有${this.hasMore ? '更多' : '0'}条`);
					}
				} catch (error) {
					const reason = String((error && (error.msg || error.message)) || '');
					if (reason.indexOf('未加入群聊') >= 0 || reason.indexOf('不是群成员') >= 0) {
						this.accessDenied = true;
						this.hasMore = false;
						console.warn('当前用户已不在该群聊，停止加载历史', reason);
						return;
					}
					console.error('加载历史消息失败:', error);
					uni.showToast({ title: '加载聊天记录失败，请稍后重试', icon: 'none' });
					this.isPreloading = false;
				} finally {
					this.loading = false;
				}
			},
			/**
			 * 回到聊天页时重新拉取最新一页，补齐通话记录等离开期间产生的消息
			 */
			async refreshHistoryMessages() {
				if (this.loading) return;
				this.pageNum = 1;
				this.hasMore = true;
				await this.loadHistoryMessages();
			},
		/**
		 * 格式化消息数据
		 */
		formatMessages(rows) {
			const messages = [];
			let lastTime = null;

			// 直接遍历，后端返回的已是按时间正序（旧→新）
			rows.forEach((item, index) => {
				const messageTime = new Date(item.createTime);
				const formattedTime = this.formatMessageTime(messageTime);

				// 判断是否需要显示时间戳（间隔5分钟以上）
				let showTime = false;
				if (!lastTime || (messageTime - lastTime) > 5 * 60 * 1000) {
					showTime = true;
					lastTime = messageTime;
				}

				// 转换消息类型
				let messageType = this.getMessageType(item.contentType || item.messageType);
				// 兼容旧版实验数据：正文是视频通话 JSON 但类型仍是旧文案 6，按通话卡片展示
				if (messageType === 'text' && String(item.message || '').indexOf('"kind":"video"') !== -1) {
					try {
						if (JSON.parse(item.message).kind === 'video') messageType = 'call';
					} catch (err) {}
				}
				const isSelf = item.senderId === this.userInfo.userId;
				const content = this.parseMessageContent(item.message, messageType);

				messages.push({
					id: item.chatMessageId,
					type: messageType,
					content: content,
					fileName: this.optimizeFileName(item.fileName || this.getFileNameFromUrl(content, messageType)),
					fileSize: item.fileSize || this.formatFileSize(item.fileSize),
					duration: item.duration || this.formatDuration(item.duration),
					isSelf: isSelf,
					senderId: item.senderId,
					sender: item.sender,
					time: formattedTime,
					showTime: showTime,
					read: item.isRead,
					withdrawn: item.withdrawn === 1 || item.withdrawn === true,
					replyToId: item.replyToId || '',
					replyContent: item.replyContent || '',
					translation: '',
					translationLabel: '',
					rawMessageType: item.contentType || item.messageType,
					reactions: [],
					favorited: false,
					createTime: item.createTime
				});
			});

			if (messages.length) this.loadMessageInteractions(messages);
			return messages;
		},
		/**
		 * 从 URL 提取文件名
		 */
		getFileNameFromUrl(url, type) {
			return getFileNameFromUrlModel(url, type);
		},
		/**
		 * 优化文件名显示（过长时截断）
		 */
		optimizeFileName(fileName) {
			return optimizeFileNameModel(fileName);
		},
		/**
		 * 获取默认文件名
		 */
		getDefaultFileName(type) {
			return getDefaultFileNameModel(type);
		},
		/**
		 * 格式化文件大小
		 */
		formatFileSize(bytes) {
			return formatFileSizeModel(bytes);
		},
		/**
		 * 格式化音频时长（秒数转为 XX秒 或 XX分XX秒）
		 */
		formatDuration(seconds) {
			return formatDurationModel(seconds);
		},
		/**
		 * 获取消息类型
		 * @param {Number|String} messageType - 消息类型（1文本 2图片 3语音 4视频 5文件）
		 * @returns {String} 'text' | 'image' | 'voice' | 'video' | 'file'
		 */
		getMessageType(messageType) {
			const raw = String(messageType == null ? '' : messageType)
			if (raw === '8') return 'vote'
			if (raw === '7') return 'chatRecord'
			if (raw === '9') return 'call'
			return resolveMessageType(messageType);
		},
		/**
		 * 解析消息内容
		 */
		parseMessageContent(message, type) {
			return parseMessageContentModel(message, type);
		},
		/**
		 * 格式化消息时间
		 */
			formatMessageTime(date) {
				const now = new Date();
				const messageDate = new Date(date);

				// 今天
				if (now.toDateString() === messageDate.toDateString()) {
					const hours = messageDate.getHours();
					const minutes = messageDate.getMinutes().toString().padStart(2, '0');

					// 确定时间段
					let period = '';
					if (hours >= 0 && hours < 6) {
						period = '凌晨';
					} else if (hours >= 6 && hours < 12) {
						period = '上午';
					} else if (hours >= 12 && hours < 18) {
						period = '下午';
					} else {
						period = '晚上';
					}

					// 转换为12小时制
					const hour12 = hours > 12 ? hours - 12 : (hours === 0 ? 12 : (hours === 12 ? 12 : hours));
					return `${period} ${hour12}:${minutes}`;
				}

			// 昨天
			const yesterday = new Date(now);
			yesterday.setDate(yesterday.getDate() - 1);
			if (yesterday.toDateString() === messageDate.toDateString()) {
				return '昨天';
			}

			// 更早
			const month = (messageDate.getMonth() + 1).toString().padStart(2, '0');
			const day = messageDate.getDate().toString().padStart(2, '0');
			return `${month}-${day}`;
		},
		goBack() {
			console.log('=== 点击返回按钮 ===');

			// 获取当前页面栈
			const pages = getCurrentPages();
			console.log('当前页面栈长度:', pages.length);
			console.log('当前页面:', pages[pages.length - 1].route);

			if (pages.length > 1) {
				console.log('上一个页面:', pages[pages.length - 2].route);
				uni.navigateBack({
					delta: 1,
					success: () => {
						console.log('✓ navigateBack 成功');
					},
					fail: (err) => {
						console.error('✗ navigateBack 失败:', err);
						uni.redirectTo({
							url: '/pages/chat/chat-list'
						});
					}
				});
			} else {
				console.warn('页面栈只有一个页面，直接跳转到聊天列表');
				uni.redirectTo({
					url: '/pages/chat/chat-list'
				});
			}
		},
		goToUserProfile() {
			const info = this.chatInfo || {};
			const targetId = info.id || this.friendId
			const targetName = info.name || this.chatName || ''
			const targetAvatar = encodeURIComponent(info.avatar || '')
			if (!targetId) {
				uni.showToast({ title: '暂无资料', icon: 'none' })
				return
			}
			if (this.chatType === 2) {
				uni.navigateTo({ url: `/pages/contacts/group-detail?id=${targetId}&name=${encodeURIComponent(targetName)}&avatar=${targetAvatar}` })
				return
			}
			uni.navigateTo({ url: `/pages/contacts/friend-detail?id=${targetId}&name=${encodeURIComponent(targetName)}&avatar=${targetAvatar}` })
		},
		showMore() {
			const items = this.chatType === 1 ? ['视频通话', '查看资料'] : ['查看资料']
			uni.showActionSheet({
				itemList: items,
				success: (res) => {
					if (this.chatType === 1 && res.tapIndex === 0) {
						this.startVideoCall()
						return
					}
					this.goToUserProfile()
				}
			});
		},
		startGroupCall(video) {
			if (this.chatType !== 2) {
				uni.showToast({ title: '仅群聊可发起群通话', icon: 'none' })
				return
			}
			const groupId = this.chatInfo.id || this.chatId
			if (!groupId) {
				uni.showToast({ title: '群信息不完整', icon: 'none' })
				return
			}
			const name = encodeURIComponent(this.chatInfo.name || '群通话')
			uni.navigateTo({
				url: `/pages/chat/group-call?groupId=${groupId}&video=${video ? 1 : 0}&name=${name}`
			})
		},
		startVideoCall() {
			if (this.chatType !== 1) {
				uni.showToast({ title: '群聊暂不支持视频通话', icon: 'none' })
				return
			}
			const targetId = this.chatInfo.id || this.friendId || this.chatId
			if (!targetId) {
				uni.showToast({ title: '对方信息不完整', icon: 'none' })
				return
			}
			const active = getActiveCall()
			if (active && active.callId) {
				uni.showToast({ title: '当前正在通话中', icon: 'none' })
				return
			}
			if (!isMediaSupported()) {
				uni.showToast({ title: '当前浏览器不支持视频通话', icon: 'none' })
				return
			}

			// 在用户点击的同步调用栈里预热音频，导航到通话页后回铃音才不会被自动播放策略挂起。
			primeCallAudio()
			const callId = createCallId()

			uni.showLoading({ title: '正在准备摄像头...', mask: true })
			prepareVideoStream('user').then(() => {
				uni.hideLoading()
				const name = this.chatInfo.name || '好友'
				const avatar = normalizeMediaUrl(this.chatInfo.avatar || '')
				prepareCallIntent({
					callId,
					mode: 'outgoing',
					peerId: targetId
				})
				uni.navigateTo({
					url: '/pages/call/video-call?mode=outgoing'
						+ '&callId=' + encodeURIComponent(callId)
						+ '&userId=' + encodeURIComponent(String(targetId))
						+ '&name=' + encodeURIComponent(name)
						+ '&avatar=' + encodeURIComponent(avatar),
					fail: () => {
						clearCallIntent(callId)
						releasePendingStream()
						uni.showToast({ title: '打开通话页面失败', icon: 'none' })
					}
				})
			}).catch((err) => {
				uni.hideLoading()
				releasePendingStream()
				console.warn('准备视频通话失败:', err)
				uni.showToast({ title: describeMediaError(err), icon: 'none', duration: 3000 })
			})
		},
		senderLabel(item) {
			if (!item) return '对方'
			if (item.isSelf) return '你'
			if (typeof item.sender === 'string' && item.sender.trim()) return item.sender.trim()
			if (item.sender && typeof item.sender === 'object') {
				return item.sender.username || item.sender.nickname || item.sender.name || this.chatInfo.name || '对方'
			}
			return this.chatInfo.name || '对方'
		},
		messageSummary(item) {
			if (!item) return '[消息]'
			switch (item.type) {
				case 'text': return item.content
				case 'image': return '[图片]'
				case 'voice': return '[语音]'
				case 'video': return '[视频]'
				case 'file': return '[文件]'
				case 'vote': return '[群投票] ' + this.voteTitleOf(item)
				case 'call': return '[视频通话] ' + this.callRecordOf(item).subtitle
				case 'chatRecord': return '[聊天记录]'
				default: return '[消息]'
			}
		},
		messageTimeValue(item) {
			const value = item && item.createTime
			if (!value) return 0
			if (typeof value === 'number') return value
			const t = new Date(String(value).replace(/-/g, '/')).getTime()
			return Number.isNaN(t) ? 0 : t
		},
			/* ==================== 消息表情回应与收藏 ==================== */
			canInteractMessage(item) {
				if (!item || !item.id || item.withdrawn) return false
				return String(item.rawMessageType || '') !== '6'
			},
			showReactionBar(item) {
				return this.canInteractMessage(item) && Array.isArray(item.reactions) && item.reactions.length > 0
			},
			async loadMessageInteractions(messages) {
				const ids = (messages || [])
					.filter(item => this.canInteractMessage(item))
					.map(item => String(item.id))
				if (!ids.length) return
				try {
					const res = await getMessageInteractions({ messageIds: ids })
					if (res.code !== 200 || !res.data) return
					const reactionMap = {}
					;(res.data.reactions || []).forEach(group => {
						reactionMap[String(group.messageId)] = group.items || []
					})
					const favoriteIds = new Set((res.data.favoriteIds || []).map(id => String(id)))
					this.messageList.forEach(item => {
						if (!item.id) return
						const key = String(item.id)
						if (reactionMap[key]) item.reactions = reactionMap[key]
						item.favorited = favoriteIds.has(key)
					})
				} catch (err) {
					console.warn('加载消息互动状态失败', err)
				}
			},
			hasMyReaction(item, emoji) {
				if (!item || !Array.isArray(item.reactions)) return false
				return item.reactions.some(reaction => reaction.emoji === emoji && reaction.mine)
			},
			openReactionSheet(item) {
				if (!this.canInteractMessage(item)) return
				this.reactionSheet.target = item
				this.reactionSheet.show = true
			},
			closeReactionSheet() {
				this.reactionSheet.show = false
				this.reactionSheet.target = null
			},
			async toggleReaction(item, emoji) {
				if (!this.canInteractMessage(item) || !emoji) return
				const key = String(item.id) + ':' + emoji
				if (this.reactionPending[key]) return
				this.reactionPending[key] = true
				try {
					const res = await toggleMessageReaction({ messageId: item.id, emoji })
					if (res.code !== 200) {
						uni.showToast({ title: res.message || '操作失败', icon: 'none' })
						return
					}
					item.reactions = (res.data && res.data.items) || []
					this.closeReactionSheet()
				} catch (err) {
					uni.showToast({ title: '回应失败，请重试', icon: 'none' })
				} finally {
					this.reactionPending[key] = false
				}
			},
			async toggleFavorite(item) {
				if (!this.canInteractMessage(item)) return
				if (this.favoritePending[String(item.id)]) return
				this.favoritePending[String(item.id)] = true
				try {
					const res = await toggleMessageFavorite({ messageId: item.id })
					if (res.code !== 200) {
						uni.showToast({ title: res.message || '操作失败', icon: 'none' })
						return
					}
					item.favorited = !!res.data
					uni.showToast({ title: item.favorited ? '已收藏' : '已取消收藏', icon: 'none' })
				} catch (err) {
					uni.showToast({ title: '收藏失败，请重试', icon: 'none' })
				} finally {
					this.favoritePending[String(item.id)] = false
				}
			},
			handleMessageReaction(message) {
				const id = String(message.chatMessageId || '')
				if (!id) return
				const target = this.messageList.find(item => String(item.id) === id)
				if (!target) return
				this.loadMessageInteractions([target])
			},
			/* ==================== 合并转发 ==================== */
			parseMergeItems(item) {
				if (!item || !item.content) return []
				let raw = item.content
				try {
					if (typeof raw === 'string') {
						raw = raw.trim()
						// 与 PC 保持一致：会话列表预览前缀不参与解析
						if (raw.indexOf('【聊天记录】') === 0) raw = raw.slice(6)
					}
					if (typeof raw === 'string') raw = JSON.parse(raw)
					if (typeof raw === 'string') raw = JSON.parse(raw)
				} catch (err) {
					return []
				}
				if (!Array.isArray(raw)) return []
				return raw.map((row, index) => ({
					id: index,
					sender: row.s || row.sender || '未知',
					content: String(row.c != null ? row.c : (row.content || '')),
					type: row.t || row.type || 'text'
				}))
			},
			mergeCountOf(item) {
				return this.parseMergeItems(item).length
			},
			mergePreviewOf(item) {
				const items = this.parseMergeItems(item)
				if (!items.length) return '查看聊天记录'
				const first = items[0]
				return (first.sender ? first.sender + '：' : '') + (first.content || '[消息]').slice(0, 36)
			},
			openMergeViewer(item) {
				const items = this.parseMergeItems(item)
				if (!items.length) {
					uni.showToast({ title: '聊天记录解析失败', icon: 'none' })
					return
				}
				this.mergeViewer.items = items
				this.mergeViewer.title = '聊天记录'
				this.mergeViewer.show = true
			},
			closeMergeViewer() {
				this.mergeViewer.show = false
				this.mergeViewer.items = []
			},
		canWithdrawMessage(item) {
			if (!item || !item.id || item.withdrawn || !item.isSelf) return false
			const t = this.messageTimeValue(item)
			if (!t) return true
			return Date.now() - t <= 2 * 60 * 1000
		},
		openMessageMenu(item) {
			if (!item || item.withdrawn) return
			this.actionTarget = item
			const actions = []
			if (item.id) actions.push({ key: 'quote', label: '引用回复' })
			if (item.type !== 'call' && item.id && item.content) actions.push({ key: 'forward', label: '转发' })
			if (item.type === 'text' && item.content) actions.push({ key: 'translate', label: '翻译' })
			if (item.type === 'text' && item.content && !item.isSelf) actions.push({ key: 'smartReply', label: 'AI 智能回复' })
			if (item.type === 'text' && item.content) actions.push({ key: 'copy', label: '复制' })
			if (this.canInteractMessage(item)) actions.push({ key: 'react', label: '表情回应' })
			if (this.canInteractMessage(item)) actions.push({ key: 'favorite', label: item.favorited ? '取消收藏' : '收藏' })
			if (this.canSelectMessage(item)) actions.push({ key: 'select', label: '多选' })
			if (this.canEditMessage(item)) actions.push({ key: 'edit', label: '编辑' })
			if (this.canWithdrawMessage(item)) actions.push({ key: 'withdraw', label: '撤回' })
			if (!actions.length) return
			uni.showActionSheet({
				itemList: actions.map(a => a.label),
				success: (res) => {
					const action = actions[res.tapIndex]
					if (!action) return
					if (action.key === 'quote') this.quoteMessage(item)
					else if (action.key === 'withdraw') this.withdrawMessage(item)
					else if (action.key === 'copy') this.copyMessage(item)
					else if (action.key === 'forward') this.openFlowPanel(item)
					else if (action.key === 'translate') this.chooseTranslateLang(item)
					else if (action.key === 'smartReply') this.requestSmartReply(item)
					else if (action.key === 'react') this.openReactionSheet(item)
					else if (action.key === 'favorite') this.toggleFavorite(item)
					else if (action.key === 'select') this.enterSelectionMode(item)
					else if (action.key === 'edit') this.openEditPanel(item)
				}
			})
		},
		canEditMessage(item) {
			if (!item || !item.id || item.withdrawn) return false
			if (!item.isSelf || item.type !== 'text') return false
			const t = this.messageTimeValue(item)
			if (!t) return true
			return Date.now() - t <= 5 * 60 * 1000
		},
		openEditPanel(item) {
			if (!item) return
			this.editPanel = { show: true, saving: false, target: item, draft: String(item.content || '') }
		},
		closeEditPanel() {
			if (this.editPanel.saving) return
			this.editPanel = { show: false, saving: false, target: null, draft: '' }
		},
		async submitEditMessage() {
			const panel = this.editPanel
			if (!panel.show || panel.saving) return
			const content = String(panel.draft || '').trim()
			if (!content) {
				uni.showToast({ title: '消息内容不能为空', icon: 'none' })
				return
			}
			if (content === String(panel.target.content || '')) {
				this.closeEditPanel()
				return
			}
			this.editPanel.saving = true
			try {
				await editMessage({ messageId: panel.target.id, content })
				this.applyMessageEdit(panel.target.id, content, '')
				this.editPanel = { show: false, saving: false, target: null, draft: '' }
				uni.showToast({ title: '已保存', icon: 'none' })
			} catch (e) {
				uni.showToast({ title: e && e.message ? e.message : '编辑失败，请重试', icon: 'none' })
			} finally {
				if (this.editPanel.target === panel.target) this.editPanel.saving = false
			}
		},
		applyMessageEdit(messageId, content, editTime) {
			const target = (this.messageList || []).find(m => String(m.id) === String(messageId))
			if (!target) return
			target.content = content
			target.edited = 1
			if (editTime) target.editTime = editTime
			this.$forceUpdate && this.$forceUpdate()
		},
		quoteMessage(item) {
			if (!item || !item.id) return
			this.replyTarget = {
				id: item.id,
				name: this.senderLabel(item),
				message: this.messageSummary(item)
			}
		},
		cancelReply() {
			this.replyTarget = null
		},
		copyMessage(item) {
			const text = item && item.content ? String(item.content) : this.messageSummary(item)
			uni.setClipboardData({
				data: text,
				success: () => {
					uni.showToast({ title: '已复制', icon: 'none' })
				}
			})
		},
		/* ==================== 消息翻译 ==================== */
		chooseTranslateLang(item) {
			if (!item || item.type !== 'text' || !item.content) {
				uni.showToast({ title: '仅文本消息支持翻译', icon: 'none' })
				return
			}
			const langs = [
				{ key: 'zh', label: '中文' },
				{ key: 'en', label: 'English' },
				{ key: 'ja', label: '日本語' }
			]
			uni.showActionSheet({
				itemList: langs.map(l => l.label),
				success: (res) => {
					const target = langs[res.tapIndex]
					if (target) this.requestTranslate(item, target)
				}
			})
		},
		async requestTranslate(item, target) {
			if (!item || !target) return
			const text = String(item.content || '').trim()
			if (!text) return
			if (text.length > 2000) {
				uni.showToast({ title: '消息超过 2000 字，暂不支持翻译', icon: 'none' })
				return
			}
			item.translationLabel = target.label + '（翻译中…）'
			item.translation = ' '
			try {
				const res = await translateMessage({ text, target: target.key })
				if (res && (res.code === 200 || res.code === 0) && res.data) {
					item.translation = String(res.data)
					item.translationLabel = target.label
				} else {
					item.translation = ''
					item.translationLabel = ''
					uni.showToast({ title: (res && res.msg) || '翻译失败，请重试', icon: 'none' })
				}
			} catch (e) {
				item.translation = ''
				item.translationLabel = ''
				uni.showToast({ title: '翻译失败，请重试', icon: 'none' })
			}
		},
		hideTranslation(item) {
			if (!item) return
			item.translation = ''
			item.translationLabel = ''
		},
		/* ==================== AI 智能回复 ==================== */
		async requestSmartReply(item) {
			if (!item || item.type !== 'text' || !item.content) return
			uni.showLoading({ title: 'AI 正在思考…', mask: true })
			try {
				const res = await smartReply({
					message: String(item.content).slice(0, 1000),
					scene: this.chatType === 2 ? 'group' : 'single'
				})
				uni.hideLoading()
				const list = (res && (res.code === 200 || res.code === 0) && Array.isArray(res.data))
					? res.data.filter(Boolean).slice(0, 3)
					: []
				if (!list.length) {
					uni.showToast({ title: '暂时没有生成回复，请重试', icon: 'none' })
					return
				}
				uni.showActionSheet({
					itemList: list,
					success: (res2) => {
						const text = list[res2.tapIndex]
						if (!text) return
						this.inputText = String(text).slice(0, 500)
						uni.showToast({ title: '已填入输入框，可编辑后发送', icon: 'none' })
					}
				})
			} catch (e) {
				uni.hideLoading()
				uni.showToast({ title: 'AI 回复生成失败，请重试', icon: 'none' })
			}
		},
		/* ==================== 消息转发与合并转发 ==================== */
		async openFlowPanel(item) {
			if (!item || !item.content) return
			this.actionTarget = item
			this.forwardMode = 'single'
			this.forwardPayload = null
			this.flowPanel = { show: true, loading: true, list: [] }
			await this.loadFlowTargets()
		},
		async loadFlowTargets() {
			try {
				const res = await getLastMes({ pageNum: 1, pageSize: 30 })
				const raw = res && res.data
				const rows = Array.isArray(raw) ? raw : ((raw && (raw.rows || raw.list || raw.records)) || [])
				const list = rows.map(row => ({
					chatId: row.chatId || row.receiverId || row.senderId,
					chatType: row.chatType != null ? row.chatType : 1,
					name: row.receiverName || row.senderName || row.chatName || row.username || '会话',
					avatar: row.receiverAvatar || row.senderAvatar || row.avatar || ''
				}))
					.filter(row => row.chatId)
					.filter(row => String(row.chatId) !== String(this.chatId))
				const seen = {}
				this.flowPanel.list = list.filter(row => {
					const key = String(row.chatId) + '_' + String(row.chatType)
					if (seen[key]) return false
					seen[key] = true
					return true
				})
			} catch (e) {
				this.flowPanel.list = []
			} finally {
				this.flowPanel.loading = false
			}
		},
		closeFlowPanel() {
			this.flowPanel.show = false
			if (!this.selectionMode) {
				this.forwardMode = 'single'
				this.forwardPayload = null
			}
		},
		/* ==================== 多选与合并转发 ==================== */
		canSelectMessage(item) {
			return !!item && !!item.id && !item.withdrawn && String(item.rawMessageType || '') !== '6'
		},
		isMessageSelected(item) {
			if (!item || !item.id) return false
			return this.selectedMessages.some(row => String(row.id) === String(item.id))
		},
		onMessageRowClick(item) {
			if (!this.selectionMode) return
			if (!this.canSelectMessage(item)) {
				uni.showToast({ title: '系统消息不支持多选', icon: 'none' })
				return
			}
			const index = this.selectedMessages.findIndex(row => String(row.id) === String(item.id))
			if (index >= 0) {
				this.selectedMessages.splice(index, 1)
				return
			}
			if (this.selectedMessages.length >= this.selectionLimit) {
				uni.showToast({ title: '一次最多合并 ' + this.selectionLimit + ' 条消息', icon: 'none' })
				return
			}
			this.selectedMessages.push(item)
		},
		enterSelectionMode(item) {
			this.selectionMode = true
			this.selectedMessages = []
			if (this.canSelectMessage(item)) this.selectedMessages.push(item)
			uni.showToast({ title: '已进入多选，点击消息勾选', icon: 'none' })
		},
		exitSelectionMode() {
			this.selectionMode = false
			this.selectedMessages = []
			this.forwardMode = 'single'
			this.forwardPayload = null
		},
		async forwardSelected() {
			if (!this.selectedMessages.length) return
			const ordered = this.messageList
				.filter(row => this.isMessageSelected(row))
				.slice(0, this.selectionLimit)
			const payload = ordered.map(row => ({
				s: this.senderLabel(row),
				c: this.messageSummary(row),
				t: row.createTime || row.time || ''
			}))
			if (!payload.length) return
			this.forwardMode = 'merge'
			this.forwardPayload = payload
			this.flowPanel = { show: true, loading: true, list: [] }
			await this.loadFlowTargets()
		},
		forwardContentType(type) {
			switch (type) {
				case 'image': return 2
				case 'voice': return 3
				case 'video': return 4
				case 'file': return 5
				case 'vote': return 8
				default: return 1
			}
		},
		async forwardToChat(target) {
			const item = this.actionTarget
			if (!target || !target.chatId) return
			const mergeMode = this.forwardMode === 'merge' && Array.isArray(this.forwardPayload) && this.forwardPayload.length > 0
			if (!mergeMode && (!item || !item.content)) return
			let content = String(item && item.content ? item.content : '')
			let contentType = item ? this.forwardContentType(item.type) : 1
			let fileName = item ? (item.fileName || '') : ''
			let fileSize = item ? (item.fileSize || '') : ''
			if (mergeMode) {
				content = '【聊天记录】' + JSON.stringify(this.forwardPayload)
				contentType = 7
				fileName = ''
				fileSize = ''
			}
			const wsClient = getWebSocketClient()
			wsClient.sendMessage(JSON.stringify({
				chatMessageId: '',
				clientMsgId: this.genClientMsgId(),
				senderId: this.userInfo.userId,
				receiverId: target.chatId,
				message: content,
				chatType: target.chatType || 1,
				messageType: 'chat',
				contentType,
				fileName,
				fileSize,
				subType: 'mes_send',
				isRead: false,
				sendStatus: 'sending',
				replyToId: '',
				replyContent: '',
				atUsers: '[]',
				createTime: this.getCurrentTime()
			}))
			this.flowPanel.show = false
			if (mergeMode) {
				this.exitSelectionMode()
				uni.showToast({ title: '聊天记录已合并转发给「' + target.name + '」', icon: 'none' })
			} else {
				uni.showToast({ title: '已转发给「' + target.name + '」', icon: 'none' })
				this.forwardMode = 'single'
				this.forwardPayload = null
			}
		},
		/* ==================== @提及 ==================== */
		async openAtPanel() {
			if (this.chatType !== 2) return
			this.atPanel.show = true
			if (this.atPanel.members.length || this.atPanel.loading) return
			this.atPanel.loading = true
			try {
				const res = await getGroupMembers(this.chatId)
				const raw = res && res.data
				const rows = Array.isArray(raw) ? raw : ((raw && (raw.members || raw.rows || raw.list)) || [])
				this.groupMemberCount = rows.length
				this.atPanel.members = rows.map(member => {
					const id = member.userId || member.id
					const role = Number(member.role)
					return {
						id,
						name: member.username || member.nickname || member.name || '成员',
						avatar: member.avatar || '',
						roleLabel: role === 1 ? '群主' : (role === 2 ? '管理员' : '')
					}
				}).filter(member => member.id && String(member.id) !== String(this.userInfo.userId))
			} catch (e) {
				this.atPanel.members = []
			} finally {
				this.atPanel.loading = false
			}
		},
		closeAtPanel() {
			this.atPanel.show = false
		},
		selectAtMember(member) {
			if (!member || !member.name) return
			const token = '@' + member.name + ' '
			if (!this.inputText.trim()) this.inputText = token
			else this.inputText = this.inputText.replace(/\s*$/, '') + ' ' + token
			if (!this.atUsers.some(u => String(u.id) === String(member.id))) {
				this.atUsers.push({ id: member.id, name: member.name })
			}
			this.atPanel.show = false
		},
		/* ==================== 群已读 ==================== */
		groupReadLabel(id) {
			const entry = this.groupReaders[String(id)]
			const count = entry && entry.readers ? Object.keys(entry.readers).length : 0
			const total = entry && entry.total ? entry.total : this.groupMemberCount
			if (!count) return '0 人已读'
			return count + (total ? '/' + total : '') + ' 人已读'
		},
		readNamesOf(id) {
			const entry = this.groupReaders[String(id)]
			if (!entry || !entry.readers) return []
			return Object.keys(entry.readers).map(key => entry.readers[key]).filter(Boolean)
		},
		toggleGroupReaders(id) {
			this.activeReadId = this.activeReadId === id ? '' : id
		},
		/* ==================== 视频通话记录 ==================== */
		callRecordOf(item) {
			let payload = {}
			try {
				payload = JSON.parse(String((item && item.content) || '{}')) || {}
			} catch (err) {
				payload = {}
			}
			const outcome = payload.outcome || 'failed'
			const isSelf = !!(item && item.isSelf)
			const labels = {
				completed: '通话结束',
				rejected: isSelf ? '对方已拒绝' : '已拒绝',
				busy: isSelf ? '对方忙线中' : '忙线未接',
				timeout: isSelf ? '对方无应答' : '未接来电',
				canceled: isSelf ? '已取消' : '对方已取消',
				failed: '未接通'
			}
			const seconds = Number(payload.duration) || 0
			return {
				title: '视频通话',
				subtitle: labels[outcome] || '未接通',
				duration: outcome === 'completed' && seconds > 0 ? this.formatDuration(seconds) : ''
			}
		},
		/* ==================== 群投票 ==================== */
		votePayloadOf(item) {
			const text = String((item && item.content) || '').replace(/^【投票】/, '')
			const sep = text.lastIndexOf('||')
			if (sep < 0) return { voteId: '', title: text || '群投票' }
			return {
				voteId: text.slice(sep + 2).trim(),
				title: text.slice(0, sep).trim() || '群投票'
			}
		},
		voteTitleOf(item) {
			return this.votePayloadOf(item).title
		},
		voteHintOf(item) {
			const payload = this.votePayloadOf(item)
			const detail = payload.voteId ? this.voteDetails[payload.voteId] : null
			if (detail) return `${detail.total || 0} 人参与 · 点击查看`
			return '点击查看结果并投票'
		},
		async openVotePanel(item) {
			const payload = this.votePayloadOf(item)
			if (!payload.voteId) {
				uni.showToast({ title: '投票信息异常', icon: 'none' })
				return
			}
			this.votePanel.show = true
			this.votePanel.voteId = payload.voteId
			await this.loadVoteDetail(payload.voteId)
		},
		closeVotePanel() {
			this.votePanel.show = false
		},
		async loadVoteDetail(voteId) {
			if (!voteId || this.voteLoading[voteId]) return
			this.voteLoading = { ...this.voteLoading, [voteId]: true }
			try {
				const res = await getVoteDetail(voteId)
				if (res && (res.code === 200 || res.code === 0) && res.data) {
					this.voteDetails = { ...this.voteDetails, [voteId]: res.data }
				}
			} catch (e) {
				// 保留空态，用户可以关闭后重试
			} finally {
				this.voteLoading = { ...this.voteLoading, [voteId]: false }
			}
		},
		isMyVote(index) {
			const detail = this.voteDetail
			return !!(detail && Array.isArray(detail.myOptions) && detail.myOptions.indexOf(index) >= 0)
		},
		votePercent(opt) {
			if (!opt) return 0
			if (typeof opt.ratio === 'number') return opt.ratio
			const detail = this.voteDetail
			if (!detail || !detail.total) return 0
			return Math.round(((opt.count || 0) / detail.total) * 100)
		},
		async castVote(index) {
			const detail = this.voteDetail
			const voteId = this.votePanel.voteId
			if (!detail || !voteId || index == null) return
			if (detail.status === 1) {
				uni.showToast({ title: '投票已结束', icon: 'none' })
				return
			}
			if (!detail.multi && this.isMyVote(index)) {
				uni.showToast({ title: '已投该选项，点击其他选项可改投', icon: 'none' })
				return
			}
			if (this.votePending) return
			this.votePending = true
			try {
				const res = await submitVote({ voteId, optionIndex: index })
				if (res && (res.code === 200 || res.code === 0) && res.data) {
					this.voteDetails = { ...this.voteDetails, [voteId]: res.data }
				} else {
					uni.showToast({ title: (res && res.msg) || '投票失败', icon: 'none' })
				}
			} catch (e) {
				uni.showToast({ title: '投票失败，请重试', icon: 'none' })
			} finally {
				this.votePending = false
			}
		},
		async cancelMyVote() {
			const voteId = this.votePanel.voteId
			if (!voteId || this.votePending) return
			this.votePending = true
			try {
				const res = await cancelVote(voteId)
				if (res && (res.code === 200 || res.code === 0) && res.data) {
					this.voteDetails = { ...this.voteDetails, [voteId]: res.data }
				}
			} catch (e) {
				uni.showToast({ title: '取消失败，请重试', icon: 'none' })
			} finally {
				this.votePending = false
			}
		},
		async endVote() {
			const voteId = this.votePanel.voteId
			if (!voteId || this.votePending) return
			this.votePending = true
			try {
				const res = await closeVote(voteId)
				const detail = await getVoteDetail(voteId)
				if (detail && (detail.code === 200 || detail.code === 0) && detail.data) {
					this.voteDetails = { ...this.voteDetails, [voteId]: detail.data }
				} else if (res && (res.code === 200 || res.code === 0) && res.data) {
					this.voteDetails = { ...this.voteDetails, [voteId]: res.data }
				}
			} catch (e) {
				uni.showToast({ title: '结束投票失败，请重试', icon: 'none' })
			} finally {
				this.votePending = false
			}
		},
		withdrawMessage(item) {
			if (!this.canWithdrawMessage(item)) {
				uni.showToast({ title: '超过 2 分钟的消息不能撤回', icon: 'none' })
				return
			}
			const wsClient = getWebSocketClient()
			wsClient.sendMessage(JSON.stringify({
				senderId: this.userInfo.userId,
				receiverId: this.chatId,
				message: item.id,
				chatType: this.chatType,
				messageType: 'chat',
				subType: 'mes_withdraw'
			}))
			const idx = this.messageList.findIndex(m => m.id === item.id)
			if (idx !== -1) {
				this.messageList[idx].withdrawn = true
				this.messageList[idx].replyContent = ''
			}
			uni.showToast({ title: '已撤回', icon: 'none' })
		},
		scrollToQuoted(messageId) {
			if (!messageId) return
			const exists = this.messageList.some(m => String(m.id) === String(messageId))
			if (!exists) {
				uni.showToast({ title: '原消息不在当前记录中', icon: 'none' })
				return
			}
			this.scrollToView = ''
			this.$nextTick(() => {
				this.scrollToView = 'msg-' + messageId
			})
		},
		handleMessageWithdraw(message) {
			let payload = null
			if (typeof message.message === 'string') {
				try { payload = JSON.parse(message.message) } catch (e) { payload = null }
			} else if (message.message && typeof message.message === 'object') {
				payload = message.message
			}
			const id = payload && (payload.chatMessageId || payload.message)
			if (!id) return
			const idx = this.messageList.findIndex(m => String(m.id) === String(id))
			if (idx !== -1) {
				this.messageList[idx].withdrawn = true
				this.messageList[idx].replyContent = ''
			}
		},
		handleMessageEdit(message) {
			const id = message && (message.chatMessageId || (message.message && message.message.chatMessageId))
			if (!id) return
			const content = message.message && typeof message.message === 'object' ? message.message.content : message.message
			const idx = this.messageList.findIndex(m => String(m.id) === String(id))
			if (idx !== -1 && content != null) {
				this.messageList[idx].content = String(content)
				this.messageList[idx].edited = 1
				if (message.createTime) this.messageList[idx].editTime = message.createTime
				this.$forceUpdate && this.$forceUpdate()
			}
		},
		sendMessage() {
			if (!this.canSend) return;
			if (!this.userInfo.userId) {
				uni.showToast({
					title: '用户信息不存在',
					icon: 'none'
				});
				return;
			}

			const messageContent = this.inputText.trim();

			// 构造消息对象
			const message = {
				chatMessageId: '', // 服务端生成
				clientMsgId: this.genClientMsgId(),
				senderId: this.userInfo.userId,
				receiverId: this.friendId,
				message: messageContent,
				chatType: this.chatType,
				messageType: 'chat',
				contentType: 1, // 文本
				subType: 'mes_send',
				isRead: false,
				sendStatus: 'sending',
				replyToId: this.replyTarget ? this.replyTarget.id : '',
				replyContent: this.replyTarget ? this.replyTarget.message : '',
				atUsers: JSON.stringify(this.atUsers.map(u => u.id)),
				createTime: Date.now()
			};

			// 立即显示到聊天窗口（乐观更新）
			this.messageList.push({
				id: message.chatMessageId,
				clientMsgId: message.clientMsgId,
				type: 'text',
				content: messageContent,
				isSelf: true,
				senderId: message.senderId,
				time: this.getCurrentTime(),
				showTime: true,
				read: false,
				withdrawn: false,
				replyToId: message.replyToId,
				replyContent: message.replyContent,
				sendStatus: 'sending',
				createTime: message.createTime
			});

			// 清空输入框
			this.inputText = '';
			this.clearDraft();
			this.showEmoji = false;
			this.replyTarget = null;
			this.atUsers = [];

			// 滚动到底部
			this.scrollToBottom();

			// 通过 WebSocket 发送消息
			const wsClient = getWebSocketClient();
			wsClient.sendMessage(JSON.stringify(message));

			// 启动 ACK 超时检测（1.5秒）
			this.receiveAckTimer(message);
		},
		/**
		 * ACK 超时检测
		 */
		receiveAckTimer(message, localContent) {
			const expected = localContent !== undefined ? localContent : message.message;
			// 15 秒后仍未收到服务端 ACK，才标记为发送失败
			setTimeout(() => {
				const msg = (message.clientMsgId
					&& [...this.messageList].reverse().find(m =>
						m.isSelf && m.clientMsgId === message.clientMsgId))
					|| [...this.messageList].reverse().find(m =>
						m.isSelf &&
						m.content === expected &&
						m.sendStatus === 'sending'
					);

				// 如果没有收到 chatMessageId，标记为发送失败
				if (msg && (!msg.id || msg.id === '')) {
					msg.sendStatus = 'failed';
					uni.showToast({
						title: '消息发送失败',
						icon: 'none',
						duration: 2000
					});
				}
			}, 15000);
		},
		/* ==================== 语音录制与播放 ==================== */
		pickAudioMime() {
			if (typeof MediaRecorder === 'undefined') return ''
			const candidates = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4']
			for (let i = 0; i < candidates.length; i += 1) {
				if (MediaRecorder.isTypeSupported(candidates[i])) return candidates[i]
			}
			return ''
		},
		startRecording(e) {
			if (this.isRecording) return
			if (!this.userInfo.userId) {
				uni.showToast({ title: '用户信息不存在', icon: 'none' })
				return
			}
			const touch = (e.touches && e.touches[0]) || {}
			this.isRecording = true
			this.recordSeconds = 0
			this.recordWillCancel = false
			this.recordDiscard = false
			this.recordPendingStop = false
			this.recordStartY = touch.clientY || 0
			this.recordStartTime = Date.now()
			this.recordTimer = setInterval(() => {
				this.recordSeconds = Math.min(60, Math.round((Date.now() - this.recordStartTime) / 1000))
			}, 200)

			// #ifdef H5
			if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
				this.abortRecording()
				uni.showToast({ title: '当前环境不支持录音，请使用 HTTPS 打开', icon: 'none' })
				return
			}
			navigator.mediaDevices.getUserMedia({ audio: true }).then(stream => {
				if (!this.isRecording) {
					stream.getTracks().forEach(track => track.stop())
					return
				}
				this.recordStream = stream
				const mime = this.pickAudioMime()
				this.mediaRecorder = mime
					? new MediaRecorder(stream, { mimeType: mime })
					: new MediaRecorder(stream)
				this.audioChunks = []
				this.mediaRecorder.ondataavailable = event => {
					if (event.data && event.data.size > 0) this.audioChunks.push(event.data)
				}
				this.mediaRecorder.onstop = () => this.handleH5RecordStop()
				this.mediaRecorder.start()
				if (this.recordPendingStop) this.mediaRecorder.stop()
			}).catch(err => {
				console.error('获取麦克风失败:', err)
				this.abortRecording()
				uni.showToast({ title: '无法使用麦克风，请检查权限', icon: 'none' })
			})
			// #endif

			// #ifndef H5
			if (!this.nativeRecorder) {
				this.nativeRecorder = uni.getRecorderManager()
				this.nativeRecorder.onStop(res => this.handleNativeRecordStop(res))
			}
			this.nativeRecorder.start({ format: 'mp3', duration: 60000, sampleRate: 16000, numberOfChannels: 1 })
			// #endif
		},
		onRecordingMove(e) {
			if (!this.isRecording) return
			const touch = (e.touches && e.touches[0]) || {}
			const delta = this.recordStartY - (touch.clientY || this.recordStartY)
			this.recordWillCancel = delta > 70
		},
		stopRecording() {
			if (!this.isRecording) return
			this.isRecording = false
			this.stopRecordTimer()
			// #ifdef H5
			if (this.mediaRecorder && this.mediaRecorder.state === 'recording') {
				this.recordDiscard = this.recordWillCancel
				this.mediaRecorder.stop()
			} else {
				this.recordPendingStop = true
			}
			// #endif

			// #ifndef H5
			this.recordDiscard = this.recordWillCancel
			if (this.nativeRecorder) this.nativeRecorder.stop()
			// #endif
		},
		cancelRecording() {
			if (!this.isRecording) return
			this.isRecording = false
			this.recordDiscard = true
			this.stopRecordTimer()
			// #ifdef H5
			if (this.mediaRecorder && this.mediaRecorder.state === 'recording') this.mediaRecorder.stop()
			// #endif
			// #ifndef H5
			if (this.nativeRecorder) this.nativeRecorder.stop()
			// #endif
		},
		stopRecordTimer() {
			if (this.recordTimer) {
				clearInterval(this.recordTimer)
				this.recordTimer = null
			}
		},
		abortRecording() {
			this.isRecording = false
			this.stopRecordTimer()
			this.stopRecordStream()
		},
		stopRecordStream() {
			if (this.recordStream) {
				this.recordStream.getTracks().forEach(track => track.stop())
				this.recordStream = null
			}
		},
		handleH5RecordStop() {
			const seconds = Math.max(1, Math.round((Date.now() - this.recordStartTime) / 1000))
			const discard = this.recordDiscard
			const recorderMime = (this.mediaRecorder && this.mediaRecorder.mimeType) || ''
			const chunkMime = (this.audioChunks[0] && this.audioChunks[0].type) || ''
			const rawMime = chunkMime || recorderMime || 'audio/webm'
			const mime = rawMime.indexOf('mp4') >= 0 ? 'audio/mp4' : 'audio/webm'
			const ext = mime === 'audio/mp4' ? 'm4a' : 'webm'
			const blob = new Blob(this.audioChunks, { type: mime })
			this.audioChunks = []
			this.mediaRecorder = null
			this.recordPendingStop = false
			this.stopRecordStream()
			if (discard || !blob.size) return
			const fileName = `voice-${Date.now()}.${ext}`
			this.uploadVoiceBlob(blob, fileName, seconds)
		},
		handleNativeRecordStop(res) {
			const seconds = Math.max(1, Math.round((Date.now() - this.recordStartTime) / 1000))
			if (this.recordDiscard || !res || !res.tempFilePath) {
				this.recordPendingStop = false
				return
			}
			const fileName = `voice-${Date.now()}.mp3`
			this.uploadVoiceNative(res.tempFilePath, fileName, res.fileSize || 0, seconds)
		},
		async uploadVoiceBlob(blob, fileName, seconds) {
			const localUrl = URL.createObjectURL(blob)
			const message = this.appendVoiceBubble(localUrl, fileName, blob.size, seconds, 'uploading')
			try {
				const res = await uploadFile({ _file: blob, name: fileName, size: blob.size })
				await this.finishVoiceSend(res.data, fileName, blob.size, seconds, localUrl, message)
			} catch (error) {
				message.sendStatus = 'failed'
				uni.showToast({ title: '语音上传失败', icon: 'none' })
			}
		},
		async uploadVoiceNative(path, fileName, size, seconds) {
			const message = this.appendVoiceBubble(path, fileName, size, seconds, 'uploading')
			try {
				const res = await uploadFile({ path, name: fileName, size })
				await this.finishVoiceSend(res.data, fileName, size, seconds, path, message)
			} catch (error) {
				message.sendStatus = 'failed'
				uni.showToast({ title: '语音上传失败', icon: 'none' })
			}
		},
		appendVoiceBubble(content, fileName, size, seconds, sendStatus) {
			const message = {
				id: '',
				type: 'voice',
				content,
				fileName,
				fileSize: this.formatFileSize(size),
				duration: this.formatDuration(seconds),
				isSelf: true,
				senderId: this.userInfo.userId,
				time: this.getCurrentTime(),
				showTime: true,
				read: false,
				sendStatus: sendStatus || 'sending',
				createTime: this.getCurrentTime()
			}
			this.messageList.push(message)
			this.scrollToBottom()
			return message
		},
		finishVoiceSend(fileUrl, fileName, size, seconds, localUrl, localMessage) {
			// 与 PC 端约定：语音正文是 JSON 字符串，裸 URL 在 PC 端不会渲染成语音气泡
			const voiceContent = JSON.stringify({ url: fileUrl, duration: seconds, transcript: '' })
			const message = {
				chatMessageId: '',
				clientMsgId: this.genClientMsgId(),
				senderId: this.userInfo.userId,
				receiverId: this.friendId,
				message: voiceContent,
				fileName,
				fileSize: size,
				duration: seconds,
				chatType: this.chatType,
				messageType: 'chat',
				contentType: 3,
				subType: 'mes_send',
				isRead: false,
				sendStatus: 'sending',
				createTime: localMessage.createTime
			}
			localMessage.content = fileUrl
			localMessage.clientMsgId = message.clientMsgId
			localMessage.sendStatus = 'sending'
			const wsClient = getWebSocketClient()
			wsClient.sendMessage(JSON.stringify(message))
			this.receiveAckTimer(message, fileUrl)
		},
		playVoice(item) {
			const url = normalizeMediaUrl(item.content)
			if (!url) return
			if (this.voicePlayingId === item.id && this.voiceAudio) {
				this.stopVoice()
				return
			}
			this.stopVoice()
			const audio = uni.createInnerAudioContext()
			this.voiceAudio = audio
			this.voicePlayingId = item.id
			audio.src = url
			audio.onEnded(() => this.stopVoice())
			audio.onError(() => {
				this.stopVoice()
				uni.showToast({ title: '语音播放失败', icon: 'none' })
			})
			audio.play()
		},
		stopVoice() {
			if (this.voiceAudio) {
				try {
					this.voiceAudio.stop()
					this.voiceAudio.destroy()
				} catch (e) {}
				this.voiceAudio = null
			}
			this.voicePlayingId = ''
		},
		playVideo(item) {
			const url = normalizeMediaUrl(item.content)
			if (!url) return
			// #ifdef H5
			const opened = window.open(url, '_blank')
			if (!opened) uni.showToast({ title: '请允许弹出窗口后重试', icon: 'none' })
			// #endif
			// #ifndef H5
			uni.showToast({ title: '请下载视频后播放', icon: 'none' })
			// #endif
		},
		showAttachMenu() {
			uni.showActionSheet({
				itemList: ['拍照', '相册', '文件'],
				success: (res) => {
					if (res.tapIndex === 0) {
						this.takePhoto();
					} else if (res.tapIndex === 1) {
						this.chooseImage();
					} else if (res.tapIndex === 2) {
						this.chooseFile();
					}
				}
			});
		},
		takePhoto() {
			uni.chooseImage({
				count: 1,
				sourceType: ['camera'],
				success: (res) => {
					this.sendImageMessage(res.tempFilePaths[0]);
				}
			});
		},
		chooseImage() {
			uni.chooseImage({
				count: 9,
				sourceType: ['album'],
				success: (res) => {
					res.tempFilePaths.forEach(path => {
						this.sendImageMessage(path);
					});
				}
			});
		},
		chooseFile() {
			if (!this.userInfo.userId) {
				uni.showToast({
					title: '用户信息不存在',
					icon: 'none'
				});
				return;
			}

			// #ifdef MP-WEIXIN
			// 微信小程序使用 chooseMessageFile
			uni.chooseMessageFile({
				count: 5,
				success: (res) => {
					const tempFiles = res.tempFiles || [];
					tempFiles.forEach(file => {
						this.uploadAndSendFile(file);
					});
				},
				fail: (err) => {
					console.error('选择文件失败:', err);
				}
			});
			// #endif

			// #ifndef MP-WEIXIN
			// H5 和其他平台使用 input 元素选择所有类型的文件
			const input = document.createElement('input');
			input.type = 'file';
			input.multiple = true;
			input.accept = '*/*';  // 接受所有类型的文件

			input.onchange = (e) => {
				const files = e.target.files;
				if (files && files.length > 0) {
					Array.from(files).forEach((file, index) => {
						const fileObj = {
							path: URL.createObjectURL(file),
							name: file.name,
							size: file.size,
							type: file.type,
							// 添加原始文件对象供上传使用
							_file: file
						};
						this.uploadAndSendFile(fileObj);
					});
				}
				// 清理
				document.body.removeChild(input);
			};

			document.body.appendChild(input);
			input.click();
			// #endif
		},
		/**
		 * 上传并发送文件
		 */
		async uploadAndSendFile(file) {
			try {
				// 计算文件大小
				const fileSize = file.size || 0;
				const formattedSize = this.formatFileSize(fileSize);

				// 获取文件类型
				const fileType = this.getFileTypeFromName(file.name);

				// 先显示上传中的文件消息
				const uploadMessage = {
					id: '',
					type: fileType,
					content: file.path,
					fileName: file.name,
					fileSize: formattedSize,
					duration: '01秒', // 音频默认时长，后端会返回实际时长
					isSelf: true,
					senderId: this.userInfo.userId,
					time: this.getCurrentTime(),
					showTime: true,
					read: false,
					sendStatus: 'uploading',
					createTime: this.getCurrentTime()
				};

				this.messageList.push(uploadMessage);
				this.scrollToBottom();

				// 上传文件
				const res = await uploadFile(file);
				const fileUrl = res.data;

				console.log('文件上传成功:', fileUrl);

				// 获取文件类型编码
				const contentType = getFileType(file.name, file.type);

				// 构造 WebSocket 消息
				const message = {
					chatMessageId: '',
					clientMsgId: this.genClientMsgId(),
					senderId: this.userInfo.userId,
					receiverId: this.friendId,
					message: fileUrl,
					fileName: file.name,
					fileSize: fileSize,
					duration: 0, // 音频时长，后端需要解析音频文件获取
					chatType: this.chatType,
					messageType: 'chat',
					contentType: contentType, // 文件类型（1文本 2图片 3语音 4视频 5文件）
					subType: 'mes_send',
					isRead: false,
					sendStatus: 'sending',
					createTime: uploadMessage.createTime
				};

				// 更新消息状态
				uploadMessage.content = fileUrl;
				uploadMessage.clientMsgId = message.clientMsgId;
				uploadMessage.sendStatus = 'sending';

				// 通过 WebSocket 发送消息
				const wsClient = getWebSocketClient();
				wsClient.sendMessage(JSON.stringify(message));

				// 启动 ACK 超时检测
				this.receiveAckTimer(message);

			} catch (error) {
				console.error('文件上传失败:', error);
				uni.showToast({
					title: '文件上传失败',
					icon: 'error',
					duration: 2000
				});
			}
		},
		/**
		 * 从文件名获取类型
		 */
		getFileTypeFromName(fileName) {
			const name = fileName.toLowerCase();
			// 图片
			if (name.match(/\.(jpg|jpeg|png|gif|bmp|webp|svg)$/)) return 'image';
			// 音频
			if (name.match(/\.(mp3|wav|aac|m4a|ogg|flac|wma|opus)$/)) return 'voice';
			// 视频
			if (name.match(/\.(mp4|mov|avi|mkv|flv|wmv|webm|3gp)$/)) return 'video';
			// 文件
			if (name.match(/\.(txt|md|html|css|js|json|doc|docx|xls|xlsx|pdf|zip|rar|7z|tar|gz|ppt|pptx|csv|xml|py|java|c|cpp|h|go|rs|ts|tsx|jsx|vue|php|sql|rb|swift|kt|dart)$/)) return 'file';
			// 默认为文件
			return 'file';
		},
		sendImageMessage(imagePath) {
			if (!this.userInfo.userId) {
				uni.showToast({
					title: '用户信息不存在',
					icon: 'none'
				});
				return;
			}

			// 先显示上传中的图片消息
			const uploadMessage = {
				id: '',
				type: 'image',
				content: imagePath,
				isSelf: true,
				senderId: this.userInfo.userId,
				time: this.getCurrentTime(),
				showTime: true,
				read: false,
				sendStatus: 'uploading',
				createTime: this.getCurrentTime()
			};

			this.messageList.push(uploadMessage);
			this.scrollToBottom();

			// 上传图片
			this.uploadAndSendImage(imagePath, uploadMessage);
		},
		/**
		 * 上传并发送图片
		 */
		async uploadAndSendImage(imagePath, uploadMessage) {
			try {
				// 构造文件对象
				const file = {
					path: imagePath,
					name: 'image.jpg'
				};

				// 上传文件
				const res = await uploadFile(file);
				const fileUrl = res.data;

				console.log('图片上传成功:', fileUrl);

				// 构造 WebSocket 消息
				const message = {
					chatMessageId: '',
					clientMsgId: this.genClientMsgId(),
					senderId: this.userInfo.userId,
					receiverId: this.friendId,
					message: fileUrl,
					chatType: this.chatType,
					messageType: 'chat',
					contentType: 2, // 图片
					subType: 'mes_send',
					sendStatus: 'sending',
					createTime: uploadMessage.createTime
				};

				// 更新消息状态
				uploadMessage.content = fileUrl;
				uploadMessage.clientMsgId = message.clientMsgId;
				uploadMessage.sendStatus = 'sending';

				// 通过 WebSocket 发送消息
				const wsClient = getWebSocketClient();
				wsClient.sendMessage(JSON.stringify(message));

				// 启动 ACK 超时检测
				this.receiveAckTimer(message);

			} catch (error) {
				console.error('图片上传失败:', error);

				// 更新消息状态为失败
				uploadMessage.sendStatus = 'failed';

				uni.showToast({
					title: '图片上传失败',
					icon: 'error',
					duration: 2000
				});
			}
		},
		previewImage(url) {
			uni.previewImage({
				urls: [url],
				current: url
			});
		},
		imageLoadError(e) {
			console.error('图片加载失败:', e);
			uni.showToast({
				title: '图片加载失败',
				icon: 'none',
				duration: 2000
			});
		},
		imageLoadSuccess(e) {
			console.log('图片加载成功:', e);
		},
		downloadFile(url, fileName) {
			uni.showLoading({
				title: '下载中...',
				mask: true
			});

			uni.downloadFile({
				url: url,
				success: (res) => {
					if (res.statusCode === 200) {
						uni.hideLoading();
						uni.showToast({
							title: '下载成功',
							icon: 'success'
						});
						// 可以选择打开文件
						uni.openDocument({
							filePath: res.tempFilePath,
							showMenu: true,
							success: () => {
								console.log('打开文件成功');
							}
						});
					}
				},
				fail: (err) => {
					uni.hideLoading();
					uni.showToast({
						title: '下载失败',
						icon: 'none'
					});
					console.error('下载文件失败:', err);
				}
			});
		},
		showEmojiPanel() {
			this.showEmoji = !this.showEmoji;
			if (this.showEmoji) {
				uni.hideKeyboard();
			}
		},
		insertEmoji(emoji) {
			if (this.inputText.length + emoji.length > 500) {
				return;
			}
			this.inputText += emoji;
			this.scrollToBottom();
		},
		onInputFocus() {
			this.showEmoji = false;
			setTimeout(() => {
				this.scrollToBottom();
			}, 300);
		},
		onInputBlur() {
				console.log('输入框失去焦点');
			},
			/**
			 * 消息列表滚动事件（节流优化）
			 */
			onMessagesScroll(event) {
				const scrollTop = event.detail.scrollTop;
				const now = Date.now();

				// 计算滑动速度 (px/ms)
				if (this.lastScrollTime > 0) {
					const distance = Math.abs(scrollTop - this.lastScrollTop);
					const timeDiff = now - this.lastScrollTime;
					this.scrollSpeed = distance / timeDiff;
				}

				// 更新滚动状态
				this.lastScrollTop = scrollTop;
				this.lastScrollTime = now;

				// 直接检查预加载（内部已有 loading/isPreloading/距离保护，不会重复触发）
				this.checkAndPreload(scrollTop);

				// 收集可视区域未读消息
				this.processUnreadMessages();
			},

			/**
			 * 检查并预加载历史消息
			 */
			checkAndPreload(scrollTop) {
				// 条件：滚动到顶部附近 && 还有更多数据 && 未在加载中 && 未在预加载中 && 不是同一位置重复触发
				if (
					scrollTop < this.preloadThreshold &&
					this.hasMore &&
					!this.loading &&
					!this.isPreloading &&
					Math.abs(scrollTop - this.lastTriggerScrollTop) > 50 // 避免在相似位置重复触发
				) {
					console.log('🚀 触发预加载，scrollTop:', scrollTop);
					this.isPreloading = true;
					this.lastTriggerScrollTop = scrollTop;
					this.pageNum++;
					this.loadHistoryMessages();
				}

				// 重置触发位置（当用户向下滚动较多时）
				if (scrollTop > this.preloadThreshold * 2) {
					this.lastTriggerScrollTop = -1;
				}
			},
		/**
		 * 处理未读消息
		 */
		processUnreadMessages() {
			// 收集可视区域未读消息
			this.collectVisibleUnReadMessages();

			// 清除之前的定时器
			if (this.scrollUnReadTimer) {
				clearTimeout(this.scrollUnReadTimer);
			}

			if (this.visibleUnReadMessages.size === 0) return;

			// 根据滑动速度决定提交策略
			const isFastScroll = this.scrollSpeed > this.fastScrollThreshold;
			const delay = isFastScroll ? this.fastSubmitDelay : this.slowSubmitDelay;

			this.scrollUnReadTimer = setTimeout(() => {
				this.sendUnReadMes();
			}, delay);
		},
		/**
		 * 收集可视区域未读消息
		 * 注意：在 uni-app 中，获取元素位置比较困难，这里简化处理
		 * 实际项目中可能需要使用 uni.createSelectorQuery()
		 */
		collectVisibleUnReadMessages() {
			// 由于 uni-app 中获取可视区域元素位置较为复杂，
			// 这里简化处理：收集所有未读消息
			// 实际实现应该使用 uni.createSelectorQuery() 获取元素位置

			this.messageList.forEach((msg, index) => {
				// 消息未读且是发给当前用户的，并且是当前聊天窗口发送的
				if (
					msg.read === false &&
					msg.senderId === this.chatId &&
					msg.id && // 有消息ID
					!this.visibleUnReadMessages.has(msg.id)
				) {
					this.visibleUnReadMessages.add(msg.id);
				}
			});

			console.log('可视区域未读消息数量:', this.visibleUnReadMessages.size);
		},
		/**
		 * 发送未读消息 ID 到服务端
		 */
		sendUnReadMes() {
			if (this.visibleUnReadMessages.size === 0) return;

			// 构造 WebSocket 消息
			const message = {
				senderId: this.userInfo.userId,
				receiverId: this.friendId,
				message: JSON.stringify(Array.from(this.visibleUnReadMessages)),
				chatType: this.chatType,
				messageType: 'chat',
				subType: 'submit_unread'
			};

			console.log('提交未读消息:', message);

			// 发送 WebSocket 消息
			const wsClient = getWebSocketClient();
			wsClient.sendMessage(JSON.stringify(message));

			// 立即更新本地消息的已读状态
			const readMessageIds = Array.from(this.visibleUnReadMessages);
			this.messageList.forEach(msg => {
				if (readMessageIds.includes(msg.id)) {
					msg.read = true;
				}
			});

			// 清空集合
			this.visibleUnReadMessages.clear();
		},
		/**
		 * 提交所有未读消息到服务端
		 * 用于消息数量较少时，直接标记所有消息为已读
		 */
		submitAllUnreadMessages() {
			// 收集所有未读消息ID
			const unreadIds = [];
			this.messageList.forEach(msg => {
				if (msg.read === false && msg.id && !msg.isSelf) {
					unreadIds.push(msg.id);
				}
			});

			if (unreadIds.length === 0) {
				console.log('没有未读消息需要提交');
				return;
			}

			// 构造 WebSocket 消息
			const message = {
				senderId: this.userInfo.userId,
				receiverId: this.friendId,
				message: JSON.stringify(unreadIds),
				chatType: this.chatType,
				messageType: 'chat',
				subType: 'submit_unread'
			};

			console.log('提交所有未读消息:', message);

			// 发送 WebSocket 消息
			const wsClient = getWebSocketClient();
			wsClient.sendMessage(JSON.stringify(message));

			// 立即更新本地消息的已读状态
			this.messageList.forEach(msg => {
				if (unreadIds.includes(msg.id)) {
					msg.read = true;
				}
			});

			// 清空集合
			this.visibleUnReadMessages.clear();
		},
		scrollToBottom() {
			this.$nextTick(() => {
				const lastIndex = this.messageList.length - 1;
				if (lastIndex >= 0) {
					const lastMessage = this.messageList[lastIndex];
					// 使用消息ID而非index进行定位
					this.scrollToView = 'msg-' + (lastMessage.id || lastIndex);
					// 延时重置 scroll-into-view，避免影响后续滚动
					setTimeout(() => {
						this.scrollToView = '';
					}, 500);
				}
			});
		},
		/**
		 * 获取消息内容区域的总高度
		 */
		getContentHeight() {
			return new Promise((resolve) => {
				uni.createSelectorQuery()
					.in(this)
					.select('.message-content')
					.boundingClientRect((rect) => {
						resolve(rect ? rect.height : 0);
					})
					.exec();
			});
		},
		// 客户端消息唯一键：断线重发时保持不变，服务端据此幂等
		genClientMsgId() {
			return 'mob-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 10);
		},
		getCurrentTime() {
			const now = new Date();
			const hours = now.getHours();
			const minutes = now.getMinutes().toString().padStart(2, '0');
			const period = hours >= 12 ? '下午' : '上午';
			const hour12 = hours > 12 ? hours - 12 : hours;
			return `${period} ${hour12}:${minutes}`;
		},
		/**
		 * 处理 WebSocket 消息
		 */
		handleWebSocketMessage(data) {
			console.log('收到 WebSocket 消息:', data);

			try {
				const message = JSON.parse(data);
				const messageType = message.messageType;
				const subType = message.subType;

				// 根据 messageType 和 subType 分发处理
				if (messageType === 'chat') {
					if (subType === 'mes_receive') {
						// 接收聊天消息
						this.handleChatMessageReceive(message);
					} else if (subType === 'mes_send_ack') {
						// 消息发送确认
						this.handleMessageSendAck(message);
					} else if (subType === 'mes_read') {
						// 消息已读通知
						this.handleMessageReadNotify(message);
					} else if (subType === 'mes_withdraw') {
						// 消息撤回通知
						this.handleMessageWithdraw(message);
					} else if (subType === 'mes_reaction') {
						// 消息表情回应变化
						this.handleMessageReaction(message);
					} else if (subType === 'mes_edit') {
						// 消息被编辑（自己多端或对方）
						this.handleMessageEdit(message);
					} else {
						console.log('未知 subType:', subType);
					}
				} else if (messageType === 'notify') {
					console.log('收到通知消息:', message);
				} else if (messageType === 'heart_beat') {
					console.log('收到心跳响应');
				} else {
					console.log('未知消息类型:', messageType);
				}
			} catch (error) {
				console.error('解析 WebSocket 消息失败:', error);
			}
		},
		/**
		 * 处理接收到的聊天消息 (mes_receive)
		 */
		handleChatMessageReceive(message) {
			console.log('处理接收到的聊天消息:', message);

			// 只处理当前聊天的消息（对方发送给我的）
			// mes_receive 消息中，senderId 是发送者（对方），receiverId 是接收者（我）
			// 单聊：发送者必须是当前聊天对象；群聊：接收方必须是当前群
			if (this.chatType === 2) {
				if (String(message.receiverId) !== String(this.chatId)) {
					console.log('不是当前群聊的消息，忽略', {
						receiverId: message.receiverId,
						currentChatId: this.chatId
					});
					return;
				}
			} else if (String(message.senderId) !== String(this.chatId)) {
				console.log('不是当前聊天对象发来的消息，忽略', {
					messageSenderId: message.senderId,
					currentChatId: this.chatId
				});
				return;
			}

			// 转换消息类型（使用 contentType 而不是 messageType）
			const messageType = this.getMessageType(message.contentType);
			const isSelf = message.senderId === this.userInfo.userId;

			// 构造消息对象
			const msg = {
				id: message.chatMessageId,
				type: messageType,
				content: normalizeMediaUrl(this.parseMessageContent(message.message, messageType)),
				fileName: this.optimizeFileName(message.fileName || this.getFileNameFromUrl(message.message, messageType)),
				fileSize: this.formatFileSize(message.fileSize),
				duration: this.formatDuration(message.duration),
				isSelf: isSelf,
				senderId: message.senderId,
				sender: message.sender,
				time: message.createTime, // 直接使用已格式化的时间
				showTime: true, // 可以根据时间间隔判断
				read: message.isRead || false,
				withdrawn: message.withdrawn === 1 || message.withdrawn === true,
				replyToId: message.replyToId || '',
				replyContent: message.replyContent || '',
				translation: '',
				translationLabel: '',
				rawMessageType: message.contentType || message.messageType,
				reactions: [],
				favorited: false,
				createTime: message.createTime
			};

			// 添加到消息列表
			this.messageList.push(msg);

			// 滚动到底部
			this.$nextTick(() => {
				this.scrollToBottom();
			});
		},
		/**
		 * 处理消息发送确认 (mes_send_ack)
		 */
		handleMessageSendAck(message) {
			console.log('处理消息发送确认:', message);

			// 解析服务端返回的消息 ID 映射
			let messageIdMap = {};
			try {
				messageIdMap = JSON.parse(message.message);
			} catch (e) {
				console.error('解析消息 ID 映射失败:', e);
			}

			const key = Object.keys(messageIdMap)[0];
			const value = Object.values(messageIdMap)[0];

			// 语音消息的 ACK key 是 JSON 字符串，需要拆出 url 再匹配
			let ackContent = key;
			if (typeof key === 'string' && key.indexOf('{') === 0) {
				try {
					const parsed = JSON.parse(key);
					ackContent = parsed.url || parsed.path || key;
				} catch (e) {}
			}

			// 优先按 clientMsgId 精确匹配，旧消息再回退内容匹配
			const msg = (message.clientMsgId
				&& [...this.messageList].reverse().find(m => m.clientMsgId === message.clientMsgId))
				|| [...this.messageList].reverse().find(m =>
					m.isSelf &&
					(m.content === key || m.content === ackContent) &&
					m.sendStatus === 'sending'
				);

			if (msg) {
				msg.id = value || message.chatMessageId;
				msg.sendStatus = 'success';
				console.log('消息发送成功，ID:', value);
			} else {
				console.log('未找到待确认的消息:', key);
			}
		},
		/**
		 * 处理消息已读通知 (mes_read)
		 */
		handleMessageReadNotify(message) {
			console.log('处理消息已读通知:', message);

			const receiverId = message.receiverId;

			// 必须是发给当前用户的已读通知
			if (this.userInfo.userId != receiverId) return;

			// 解析已读消息ID列表
			let readMessageIds = [];
			let readPayload = {};
			try {
				const messageContent = JSON.parse(message.message);
				readPayload = messageContent || {};
				if (messageContent.readChatMessageIds) {
					readMessageIds = JSON.parse(messageContent.readChatMessageIds);
				}
			} catch (e) {
				console.error('解析已读消息ID失败:', e);
			}

			// 更新消息的已读状态
			let hasUpdated = false;
			this.messageList.forEach(msg => {
				if (readMessageIds.includes(msg.id)) {
					msg.read = true;
					hasUpdated = true;
				}
			});

			// 群聊已读：按读者累计，同一人重复上报只算一次
			const readerId = readPayload.readerId ? String(readPayload.readerId) : '';
			if (readerId && readerId !== String(this.userInfo.userId)) {
				const readerName = readPayload.readerName || '有人';
				const total = Number(readPayload.groupMemberCount) || this.groupMemberCount || 0;
				const next = { ...this.groupReaders };
				readMessageIds.forEach(id => {
					const key = String(id);
					const cur = next[key] || { readers: {}, total: total };
					next[key] = {
						readers: { ...(cur.readers || {}), [readerId]: readerName },
						total: total || cur.total
					};
				});
				this.groupReaders = next;
			}

			console.log('已读消息状态已更新');

			// 如果有消息被标记为已读，滚动到底部以显示完整的已读标记
			if (hasUpdated) {
				this.$nextTick(() => {
					this.scrollToBottom();
				});
			}
		}
	}
};
</script>

<style lang="scss" scoped>
/* ==================== 基础容器 ==================== */
.chat-detail-container {
	width: 100%;
	height: 100vh;
	max-height: 100vh;
	overflow: hidden;
	background-color: #ffffff;
	display: flex;
	flex-direction: column;
}

/* ==================== 顶部导航栏 ==================== */
.top-nav {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 100;
	background: rgba(255, 255, 255, 0.8);
	backdrop-filter: blur(96rpx);
	-webkit-backdrop-filter: blur(96rpx);
	border-bottom: 1rpx solid rgba(192, 199, 213, 0.3);
}

.nav-content {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 16rpx 24rpx;
	padding: 16rpx 24rpx;
	position: relative;
	min-height: 88rpx;
}

.nav-actions {
	display: flex;
	align-items: center;
	gap: 4rpx;
	z-index: 2;
}

.back-btn,
.video-call-btn,
.more-btn {
	width: 64rpx;
	height: 64rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 50%;
	transition: all 0.2s;
	flex-shrink: 0;
	z-index: 2;
}

.video-call-icon-img {
	width: 44rpx;
	height: 44rpx;
	display: block;
}

.btn-hover {
	background-color: rgba(192, 199, 213, 0.5);
	transform: scale(0.95);
}

.back-icon {
	font-size: 48rpx;
	font-weight: 700;
	color: #1c1c1e;
	line-height: 1;
}

.more-icon {
	font-size: 48rpx;
	font-weight: 600;
	color: #1c1c1e;
	line-height: 1;
}

.center-section {
	position: absolute;
	left: 50%;
	top: 50%;
	transform: translate(-50%, -50%);
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 4rpx;
	z-index: 1;
}

.user-name {
	font-family: 'Manrope', 'Plus Jakarta Sans', -apple-system, sans-serif;
	font-size: 36rpx;
	font-weight: 700;
	color: #1c1c1e;
	letter-spacing: -0.02em;
	line-height: 1.2;
	text-align: center;
}

.typing-status {
	font-family: 'Plus Jakarta Sans', -apple-system, sans-serif;
	font-size: 20rpx;
	font-weight: 500;
	color: #111111;
	text-transform: uppercase;
	letter-spacing: 0.15em;
	text-align: center;
}

/* ==================== 消息列表区域 ==================== */
.message-list {
	flex: 1;
	height: 0;
	padding-top: calc(var(--status-bar-height) + 100rpx);
	padding-bottom: 24rpx;
	overflow-y: auto;
	-webkit-overflow-scrolling: touch;
}

.message-content {
	padding: 16rpx 32rpx 32rpx 32rpx;
	display: flex;
	flex-direction: column;
	gap: 32rpx;
}

/* 已退群空态：占满可视区，明确说明原因与唯一出口 */
.access-blocked {
	min-height: 68vh;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	text-align: center;
	padding: 0 48rpx;
	gap: 18rpx;
}

.access-icon {
	width: 112rpx;
	height: 112rpx;
	border-radius: 50%;
	background: #f2f2f4;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-bottom: 8rpx;
}

.access-icon-img {
	width: 48rpx;
	height: 48rpx;
	opacity: 0.72;
}

.access-title {
	font-size: 32rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.access-desc {
	max-width: 520rpx;
	font-size: 26rpx;
	line-height: 1.65;
	color: #8e8e93;
}

.access-btn {
	margin-top: 12rpx;
	height: 68rpx;
	line-height: 68rpx;
	padding: 0 40rpx;
	border: none;
	border-radius: 34rpx;
	background: #f2f2f4;
	color: #1c1c1e;
	font-size: 26rpx;
	font-weight: 500;
}

.access-btn::after {
	border: none;
}

.access-btn-hover {
	background: #e5e5ea;
}

/* 加载中提示 */
.loading-more-tip {
	display: flex;
	justify-content: center;
	align-items: center;
	margin: 16rpx 0;
	gap: 12rpx;
}

.loading-more-text {
	background: rgba(17, 17, 17, 0.1);
	color: #111111;
	font-size: 22rpx;
	font-weight: 500;
	padding: 10rpx 28rpx;
	border-radius: 9999rpx;
	letter-spacing: 0.08em;
	font-family: 'Plus Jakarta Sans', -apple-system, sans-serif;
}

/* 加载完成提示 */
.load-end-tip {
	display: flex;
	justify-content: center;
	margin: 32rpx 0 24rpx 0;
}

.load-end-text {
	background: rgba(192, 199, 213, 0.5);
	color: rgba(90, 95, 103, 0.7);
	font-size: 20rpx;
	font-weight: 500;
	padding: 8rpx 24rpx;
	border-radius: 9999rpx;
	letter-spacing: 0.08em;
	font-family: 'Plus Jakarta Sans', -apple-system, sans-serif;
}

/* 时间戳 */
.timestamp-wrapper {
	display: flex;
	justify-content: center;
	margin: 24rpx 0 32rpx 0;
}

.timestamp-tag {
	background: rgba(192, 199, 213, 0.7);
	color: #5a5f67;
	font-size: 22rpx;
	font-weight: 500;
	padding: 8rpx 24rpx;
	border-radius: 9999rpx;
	letter-spacing: 0.08em;
	font-family: 'Plus Jakarta Sans', -apple-system, sans-serif;
}

/* 消息行 */
.message-row {
	position: relative;
	display: flex;
	align-items: flex-end;
	gap: 24rpx;
	margin-bottom: 8rpx;
}

.message-row.has-reaction {
	padding-bottom: 56rpx;
}

.message-row-other {
	flex-direction: row;
}

.message-row-self {
	flex-direction: row-reverse;
	justify-content: flex-start;
}

.message-bubbles {
	display: flex;
	flex-direction: column;
	gap: 8rpx;
	max-width: 75%;
}

.message-bubbles-self {
	align-items: flex-end;
}

.message-bubble {
	padding: 24rpx 32rpx;
	font-family: 'Plus Jakarta Sans', -apple-system, sans-serif;
	font-size: 28rpx;
	line-height: 1.6;
	word-wrap: break-word;
	word-break: break-all;
}

/* 对方消息气泡 */
.bubble-other {
	background: #ffffff;
	color: #1c1c1e;
	border-radius: 48rpx 48rpx 48rpx 8rpx;
	box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
	border: 1rpx solid #f1f3f7;
}

/* 自己消息气泡 */
.bubble-self {
	background: #111111;
	color: #ffffff;
	border-radius: 48rpx 48rpx 8rpx 48rpx;
	box-shadow: 0 8rpx 24rpx rgba(17, 17, 17, 0.12);
}

/* 图片消息气泡 */
.bubble-image {
	padding: 0;
	overflow: hidden;
	background: transparent !important;
	box-shadow: none !important;
}

/* 文件卡片样式（音频/视频/文件统一样式） */
.bubble-file-card {
	padding: 12rpx;
	background: #ffffff !important;
	border: 1rpx solid rgba(192, 199, 213, 0.3);
	box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
	border-radius: 24rpx;
	max-width: 540rpx;
}

.bubble-self.bubble-file-card {
	background: #ffffff !important;
	border: 1rpx solid rgba(17, 17, 17, 0.2);
}

.file-card-container {
	display: flex;
	align-items: center;
	gap: 16rpx;
	background: #ffffff;
	border: 1rpx solid rgba(192, 199, 213, 0.3);
	padding: 16rpx 18rpx;
	border-radius: 16rpx;
	box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
	min-height: 88rpx;
	width: 100%;
	box-sizing: border-box;
}

.file-icon-circle {
	width: 56rpx;
	height: 56rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 50%;
	background: rgba(17, 17, 17, 0.1);
	flex-shrink: 0;
}

.file-icon-circle-self {
	background: rgba(17, 17, 17, 0.15);
}

/* SVG 图标样式 */
.file-icon-svg {
	width: 28rpx;
	height: 28rpx;
	/* SVG 文件中已设置为浅蓝色 #111111 */
}

.file-info {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 6rpx;
	justify-content: center;
}

.file-title {
	font-family: 'Plus Jakarta Sans', -apple-system, sans-serif;
	font-size: 26rpx;
	font-weight: 600;
	color: #1c1c1e;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	line-height: 1.4;
	max-width: 340rpx;
}

.file-title-self {
	color: #1c1c1e;
}

.file-meta {
	display: flex;
	align-items: center;
	gap: 12rpx;
	flex-wrap: nowrap;
}

.file-type-label {
	font-family: 'Plus Jakarta Sans', -apple-system, sans-serif;
	font-size: 20rpx;
	font-weight: 500;
	color: #111111;
	text-transform: none;
	letter-spacing: 0;
	line-height: 1.3;
	white-space: nowrap;
}

.file-type-label-self {
	color: #111111;
}

/* 音频波形动画 */
.audio-wave {
	display: flex;
	align-items: center;
	gap: 3rpx;
	height: 18rpx;
	flex-shrink: 0;
}

.wave-bar {
	width: 3rpx;
	background: #111111;
	border-radius: 1rpx;
	transition: height 0.3s ease;
}

.wave-bar-self {
	background: #111111;
}

.file-size {
	font-family: 'Plus Jakarta Sans', -apple-system, sans-serif;
	font-size: 20rpx;
	color: rgba(90, 95, 103, 0.7);
	margin-left: auto;
	line-height: 1.3;
	white-space: nowrap;
	flex-shrink: 0;
}

.file-size-self {
	color: rgba(90, 95, 103, 0.7);
}

/* SVG 操作图标样式 */
.file-action-icon-svg {
	width: 28rpx;
	height: 28rpx;
	flex-shrink: 0;
	/* SVG 文件中已设置为浅灰色 #ADAFB3 */
}

.file-action-icon-svg-self {
	/* SVG 文件中已设置为浅灰色 #ADAFB3 */
}

.message-image {
	width: 400rpx;
	min-height: 200rpx;
	border-radius: 36rpx;
	max-width: 560rpx;
	display: block;
	background: rgba(0, 0, 0, 0.05);
}

.message-text {
	word-wrap: break-word;
	word-break: break-all;
}

.message-text-self {
	color: #ffffff;
}

/* 已读状态 */
.read-status {
	display: flex;
	align-items: center;
	gap: 8rpx;
	margin-top: 8rpx;
}

.read-icon {
	font-size: 24rpx;
	color: #111111;
	font-weight: 700;
}

.read-text {
	font-size: 20rpx;
	color: rgba(90, 95, 103, 0.6);
	font-weight: 500;
	font-family: 'Plus Jakarta Sans', -apple-system, sans-serif;
}

.message-list-with-emoji {
	padding-bottom: 24rpx !important;
}

/* 表情面板 */
.emoji-panel {
	position: relative;
	left: 0;
	right: 0;
	bottom: auto;
	height: 340rpx;
	flex-shrink: 0;
	background: #ffffff;
	border-top: 1rpx solid rgba(60, 60, 67, 0.12);
	z-index: 10;
	box-sizing: border-box;
}

.emoji-scroll {
	height: 100%;
}

.emoji-grid {
	display: flex;
	flex-wrap: wrap;
	padding: 16rpx 20rpx calc(20rpx + env(safe-area-inset-bottom));
}

.emoji-item {
	width: 12.5%;
	height: 78rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 12rpx;
}

.emoji-item-hover {
	background: #f2f2f7;
}

.emoji-char {
	font-size: 40rpx;
	line-height: 1;
}

/* ==================== 底部输入区域 ==================== */
.input-area {
	position: relative;
	left: 0;
	right: 0;
	bottom: auto;
	z-index: 10;
	flex-shrink: 0;
	width: 100%;
	background: rgba(255, 255, 255, 0.95);
	backdrop-filter: blur(128rpx);
	-webkit-backdrop-filter: blur(128rpx);
	padding: 16rpx 24rpx;
	padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
	border-top: 1rpx solid rgba(192, 199, 213, 0.3);
	box-shadow: 0 -8rpx 32rpx rgba(0, 0, 0, 0.06);
	box-sizing: border-box;
}

.input-content {
	max-width: 1280rpx;
	margin: 0 auto;
	display: flex;
	align-items: center;
	gap: 12rpx;
	min-height: 64rpx;
}

.attach-btn {
	width: 64rpx;
	height: 64rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 50%;
	background: rgba(241, 243, 247, 0.5);
	flex-shrink: 0;
	transition: all 0.2s;
}

.attach-btn-hover {
	transform: scale(1.1);
}

.attach-icon {
	font-size: 52rpx;
	color: #5a5f67;
	font-weight: 400;
	line-height: 1;
}

.input-wrapper {
	flex: 1;
	min-width: 0;
	background: rgba(241, 243, 247, 0.5);
	border-radius: 48rpx;
	padding: 6rpx;
	display: flex;
	align-items: center;
	gap: 6rpx;
	min-height: 64rpx;
}

.text-input {
	flex: 1;
	min-width: 0;
	background: transparent;
	border: none;
	font-family: 'Plus Jakarta Sans', -apple-system, sans-serif;
	font-size: 28rpx;
	color: #1c1c1e;
	padding: 8rpx 16rpx;
	line-height: 1.35;
	min-height: 40rpx;
	max-height: 160rpx;
}

.input-wrapper ::v-deep .uni-textarea-wrapper,
.input-wrapper ::v-deep .uni-textarea-textarea {
	height: auto !important;
	min-height: 40rpx !important;
	max-height: 160rpx !important;
	line-height: 1.35 !important;
}

.text-input::placeholder {
	color: rgba(90, 95, 103, 0.5);
}

.emoji-btn {
	width: 64rpx;
	height: 64rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 48rpx;
	flex-shrink: 0;
	transition: all 0.2s;
}

.emoji-btn-hover {
	background: rgba(192, 199, 213, 0.3);
}

.emoji-icon {
	width: 48rpx;
	height: 48rpx;
}

.send-btn {
	width: 64rpx;
	height: 64rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 50%;
	background: #e0e3e6;
	flex-shrink: 0;
	transition: all 0.2s;
}

.send-btn-active {
	background: #111111;
}

.send-btn-hover {
	transform: scale(1.05);
}

.send-icon {
	font-size: 32rpx;
	color: rgba(60, 60, 67, 0.16);
	font-weight: 600;
	line-height: 1;
}

.send-btn-active .send-icon {
	color: #ffffff;
}

/* ==================== 录音按钮与浮层 ==================== */
.voice-btn {
	width: 64rpx;
	height: 64rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 50%;
	background: rgba(241, 243, 247, 0.5);
	flex-shrink: 0;
	transition: all 0.2s;
	user-select: none;
	-webkit-user-select: none;
	touch-action: none;
}

.voice-btn-recording {
	background: #111111;
	transform: scale(1.08);
}

.voice-icon {
	font-size: 30rpx;
	line-height: 1;
}

.voice-btn-recording .voice-icon {
	filter: grayscale(1) brightness(2);
}

.voice-card-playing {
	background: rgba(17, 17, 17, 0.06);
}

.record-overlay {
	position: fixed;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
	z-index: 999;
	display: flex;
	align-items: flex-end;
	justify-content: center;
	padding-bottom: 220rpx;
	background: rgba(0, 0, 0, 0.18);
	pointer-events: none;
}

.record-panel {
	min-width: 300rpx;
	padding: 32rpx 40rpx;
	border-radius: 32rpx;
	background: rgba(28, 28, 30, 0.92);
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 12rpx;
}

.record-pulse {
	width: 48rpx;
	height: 48rpx;
	border-radius: 50%;
	background: #34c759;
	animation: record-pulse 1s ease-in-out infinite;
}

@keyframes record-pulse {
	0% { transform: scale(0.85); opacity: 0.65; }
	50% { transform: scale(1.15); opacity: 1; }
	100% { transform: scale(0.85); opacity: 0.65; }
}

.record-time {
	font-size: 40rpx;
	font-weight: 700;
	color: #ffffff;
}

.record-hint {
	font-size: 24rpx;
	color: rgba(255, 255, 255, 0.75);
}

/* 引用回复与撤回 */
.reply-quote {
	max-width: 70%;
	margin-bottom: 8rpx;
	padding: 8rpx 16rpx;
	border-radius: 12rpx;
	background: rgba(118, 118, 128, 0.12);
	border-left: 4rpx solid rgba(10, 132, 255, 0.65);
}

.message-bubbles-self .reply-quote {
	align-self: flex-end;
}

.reply-quote-text {
	font-size: 24rpx;
	line-height: 1.4;
	color: rgba(60, 60, 67, 0.62);
	display: -webkit-box;
	-webkit-line-clamp: 2;
	-webkit-box-orient: vertical;
	overflow: hidden;
}

.withdraw-tip {
	max-width: 70%;
	padding: 8rpx 16rpx;
	border-radius: 12rpx;
	background: rgba(118, 118, 128, 0.12);
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.55);
}

.message-bubbles.is-withdrawn .message-bubble,
.message-bubbles.is-withdrawn .call-record-card,
.message-bubbles.is-withdrawn .vote-message-card,
.message-bubbles.is-withdrawn .translation-block,
.message-bubbles.is-withdrawn .translation-toggle {
	display: none;
}

.reply-bar {
	display: flex;
	align-items: center;
	gap: 16rpx;
	margin: 0 24rpx;
	padding: 16rpx 20rpx;
	border-radius: 16rpx 16rpx 0 0;
	background: rgba(118, 118, 128, 0.12);
}

.reply-bar-main {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 4rpx;
}

.reply-bar-label {
	font-size: 22rpx;
	font-weight: 600;
	color: rgba(60, 60, 67, 0.62);
}

.reply-bar-text {
	font-size: 24rpx;
	color: #1c1c1e;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}

.reply-bar-close {
	font-size: 36rpx;
	line-height: 1;
	color: rgba(60, 60, 67, 0.5);
	padding: 4rpx 8rpx;
}

/* ==================== PC 能力对齐：翻译 / 投票 / 群已读 / @ / 转发 ==================== */
.translation-box {
	max-width: 560rpx;
	margin-top: 8rpx;
	padding: 14rpx 18rpx;
	border-radius: 16rpx;
	background: rgba(10, 132, 255, 0.08);
	border-left: 4rpx solid rgba(10, 132, 255, 0.55);
}

.translation-box-self {
	align-self: flex-end;
	background: rgba(255, 255, 255, 0.14);
	border-left-color: rgba(255, 255, 255, 0.6);
}

.translation-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 4rpx;
}

.translation-label {
	font-size: 20rpx;
	font-weight: 600;
	color: rgba(10, 132, 255, 0.85);
}

.translation-close {
	font-size: 20rpx;
	color: rgba(60, 60, 67, 0.45);
}

.translation-text {
	font-size: 26rpx;
	line-height: 1.5;
	color: #1c1c1e;
}

.translation-box-self .translation-text,
.translation-box-self .translation-close {
	color: rgba(255, 255, 255, 0.86);
}

.call-record-card {
	display: flex;
	align-items: center;
	width: 430rpx;
	padding: 18rpx 20rpx;
	border-radius: 20rpx;
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.1);
	box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.06);
	gap: 16rpx;
}

.call-record-card-self {
	background: rgba(255, 255, 255, 0.16);
	border-color: rgba(255, 255, 255, 0.28);
}

.call-record-icon {
	width: 64rpx;
	height: 64rpx;
	border-radius: 50%;
	background: rgba(10, 132, 255, 0.12);
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.call-record-icon-self {
	background: rgba(255, 255, 255, 0.16);
}

.call-record-svg {
	width: 34rpx;
	height: 34rpx;
}

.call-record-info {
	flex: 1;
	display: flex;
	flex-direction: column;
	gap: 4rpx;
	min-width: 0;
}

.call-record-title {
	font-size: 27rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.call-record-title-self {
	color: #ffffff;
}

.call-record-sub {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.55);
}

.call-record-sub-self {
	color: rgba(255, 255, 255, 0.7);
}

.call-record-duration {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.55);
	flex-shrink: 0;
}

.call-record-duration-self {
	color: rgba(255, 255, 255, 0.7);
}

.vote-message-card {
	width: 460rpx;
	padding: 20rpx;
	border-radius: 20rpx;
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.1);
	box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.06);
}

.vote-message-card-self {
	background: rgba(255, 255, 255, 0.16);
	border-color: rgba(255, 255, 255, 0.28);
}

.vote-card-head {
	display: flex;
	align-items: center;
	gap: 10rpx;
	margin-bottom: 12rpx;
}

.vote-card-badge {
	font-size: 20rpx;
	font-weight: 700;
	color: #0a84ff;
	background: rgba(10, 132, 255, 0.12);
	border-radius: 8rpx;
	padding: 2rpx 8rpx;
}

.vote-card-title {
	flex: 1;
	font-size: 28rpx;
	font-weight: 600;
	color: #1c1c1e;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.vote-card-hint {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.55);
}

.vote-message-card-self .vote-card-title {
	color: #ffffff;
}

.vote-message-card-self .vote-card-hint {
	color: rgba(255, 255, 255, 0.7);
}

.group-read-status {
	margin-top: 6rpx;
	display: flex;
	flex-direction: column;
	align-items: flex-end;
	gap: 4rpx;
}

.group-read-text {
	font-size: 20rpx;
	color: rgba(60, 60, 67, 0.5);
}

.group-read-names {
	max-width: 420rpx;
	padding: 10rpx 14rpx;
	border-radius: 12rpx;
	background: rgba(118, 118, 128, 0.12);
	display: flex;
	flex-wrap: wrap;
	justify-content: flex-end;
	gap: 8rpx;
}

.group-read-name {
	font-size: 20rpx;
	color: rgba(60, 60, 67, 0.66);
	background: rgba(255, 255, 255, 0.7);
	border-radius: 8rpx;
	padding: 2rpx 8rpx;
}

.group-read-empty {
	font-size: 20rpx;
	color: rgba(60, 60, 67, 0.45);
}

.at-btn-inline {
	width: 48rpx;
	height: 64rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.at-icon {
	font-size: 30rpx;
	font-weight: 700;
	color: #0a84ff;
	line-height: 1;
}

.sheet-mask {
	position: fixed;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
	z-index: 1000;
	background: rgba(0, 0, 0, 0.35);
	display: flex;
	align-items: flex-end;
}

.sheet-panel {
	width: 100%;
	max-height: 68vh;
	background: #ffffff;
	border-radius: 28rpx 28rpx 0 0;
	padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
	box-sizing: border-box;
	display: flex;
	flex-direction: column;
}

.sheet-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding-bottom: 14rpx;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.1);
}

.sheet-title {
	font-size: 30rpx;
	font-weight: 700;
	color: #1c1c1e;
}

.sheet-close {
	font-size: 40rpx;
	line-height: 1;
	color: rgba(60, 60, 67, 0.45);
	padding: 0 8rpx;
}

.sheet-scroll {
	max-height: 58vh;
	margin-top: 8rpx;
}

.sheet-empty {
	padding: 48rpx 0;
	text-align: center;
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.5);
}

.member-row {
	display: flex;
	align-items: center;
	gap: 18rpx;
	padding: 16rpx 6rpx;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.06);
}

.member-meta {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 4rpx;
}

.member-name {
	font-size: 28rpx;
	color: #1c1c1e;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.member-role {
	font-size: 20rpx;
	color: rgba(60, 60, 67, 0.45);
}

.vote-panel {
	max-height: 74vh;
}

.vote-detail-body {
	padding-top: 18rpx;
}

.vote-option {
	position: relative;
	overflow: hidden;
	padding: 16rpx;
	border-radius: 16rpx;
	background: rgba(241, 243, 247, 0.7);
	margin-bottom: 12rpx;
}

.vote-option-mine {
	box-shadow: inset 0 0 0 2rpx rgba(10, 132, 255, 0.45);
}

.vote-option-closed {
	opacity: 0.75;
}

.vote-option-head {
	position: relative;
	z-index: 2;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12rpx;
}

.vote-option-text {
	flex: 1;
	font-size: 26rpx;
	color: #1c1c1e;
}

.vote-option-count {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.58);
}

.vote-bar {
	position: absolute;
	left: 0;
	top: 0;
	bottom: 0;
	width: 100%;
	background: transparent;
	z-index: 1;
	pointer-events: none;
}

.vote-bar-fill {
	height: 100%;
	background: rgba(10, 132, 255, 0.18);
}

.vote-mine {
	position: relative;
	z-index: 2;
	margin-left: 8rpx;
	font-size: 20rpx;
	color: #0a84ff;
	font-weight: 600;
}

.vote-detail-foot {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 10rpx 4rpx;
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.55);
}

.vote-closed {
	color: rgba(255, 59, 48, 0.8);
	font-weight: 600;
}

.vote-actions {
	display: flex;
	justify-content: flex-end;
	gap: 24rpx;
	padding-top: 8rpx;
}

.vote-action {
	font-size: 24rpx;
	color: #0a84ff;
	padding: 8rpx;
}

/* ==================== 消息表情回应 ==================== */
.reaction-row {
	position: absolute;
	bottom: 4rpx;
	display: flex;
	flex-wrap: wrap;
	align-items: center;
	gap: 8rpx;
	max-width: calc(100% - 66px);
}

/* ===== 多选与合并转发 ===== */
.message-row.select-mode {
	position: relative;
	padding-left: 56rpx;
	border-radius: 20rpx;
}

.message-row.select-mode.select-on {
	background: rgba(17, 17, 17, 0.05);
}

.message-row.select-mode .message-bubbles {
	pointer-events: none;
}

.msg-select-box {
	position: absolute;
	left: 8rpx;
	top: 50%;
	transform: translateY(-50%);
	width: 36rpx;
	height: 36rpx;
	border-radius: 50%;
	border: 2rpx solid rgba(17, 17, 17, 0.18);
	background: #ffffff;
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 3;
	box-sizing: border-box;
}

.msg-select-box.checked {
	background: #111111;
	border-color: #111111;
}

.msg-select-check {
	font-size: 24rpx;
	line-height: 1;
	color: #ffffff;
}

.selection-bar {
	position: fixed;
	left: 24rpx;
	right: 24rpx;
	bottom: calc(150rpx + env(safe-area-inset-bottom));
	z-index: 120;
	display: flex;
	align-items: center;
	gap: 16rpx;
	padding: 18rpx 22rpx;
	border-radius: 26rpx;
	background: rgba(20, 20, 20, 0.94);
	box-shadow: 0 12rpx 36rpx rgba(0, 0, 0, 0.24);
	backdrop-filter: blur(16rpx);
}

.selection-info {
	flex: 1;
	display: flex;
	flex-direction: column;
	gap: 2rpx;
	min-width: 0;
}

.selection-count {
	font-size: 28rpx;
	font-weight: 600;
	color: #ffffff;
}

.selection-hint {
	font-size: 20rpx;
	color: rgba(255, 255, 255, 0.55);
}

.selection-cancel {
	font-size: 24rpx;
	color: rgba(255, 255, 255, 0.72);
	padding: 12rpx 16rpx;
}

.selection-forward {
	font-size: 24rpx;
	font-weight: 600;
	color: #111111;
	background: #ffffff;
	padding: 14rpx 24rpx;
	border-radius: 16rpx;
}

.selection-forward.disabled {
	opacity: 0.38;
}

.reaction-row-other {
	left: calc(33px + 24rpx);
	right: 12rpx;
	justify-content: flex-start;
	padding-left: 0;
}

.reaction-row-self {
	right: calc(33px + 24rpx);
	left: 12rpx;
	justify-content: flex-end;
	padding-right: 0;
}

.reaction-pill {
	display: flex;
	align-items: center;
	gap: 6rpx;
	padding: 4rpx 14rpx;
	border-radius: 999rpx;
	background: rgba(0, 0, 0, 0.05);
	border: 1rpx solid rgba(0, 0, 0, 0.06);
	transition: background 0.15s ease, transform 0.15s ease;
}

.reaction-pill:active {
	transform: scale(0.94);
}

.reaction-pill-mine {
	background: #111111;
	border-color: #111111;
}

.reaction-emoji {
	font-size: 24rpx;
	line-height: 1;
}

.reaction-count {
	font-size: 22rpx;
	color: #555555;
	line-height: 1;
}

.reaction-pill-mine .reaction-count {
	color: #ffffff;
}

.reaction-add {
	width: 44rpx;
	height: 44rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 50%;
	background: rgba(0, 0, 0, 0.04);
	border: 1rpx solid rgba(0, 0, 0, 0.06);
	color: #666666;
	font-size: 24rpx;
}

/* ==================== 合并转发卡片 ==================== */
.merge-card {
	width: 460rpx;
	padding: 24rpx;
	border-radius: 20rpx;
	background: #ffffff;
	border: 1rpx solid rgba(0, 0, 0, 0.08);
	box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.05);
}

.merge-card-self {
	background: #f6f6f7;
}

.merge-card-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.merge-card-title {
	font-size: 27rpx;
	font-weight: 600;
	color: #111111;
}

.merge-card-count {
	font-size: 21rpx;
	color: #8a8a8e;
}

.merge-card-preview {
	display: block;
	margin: 14rpx 0 18rpx;
	font-size: 23rpx;
	line-height: 1.5;
	color: #666666;
	word-break: break-all;
}

.merge-card-foot {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding-top: 14rpx;
	border-top: 1rpx solid rgba(0, 0, 0, 0.06);
	font-size: 21rpx;
	color: #8a8a8e;
}

.merge-card-arrow {
	font-size: 30rpx;
	color: #b0b0b5;
	line-height: 1;
}

/* ==================== 回应选择器 ==================== */
.reaction-sheet-mask {
	position: fixed;
	top: 0;
	right: 0;
	bottom: 0;
	left: 0;
	z-index: 1000;
	background: rgba(0, 0, 0, 0.42);
	display: flex;
	align-items: flex-end;
}

.reaction-sheet {
	width: 100%;
	background: #ffffff;
	border-radius: 28rpx 28rpx 0 0;
	padding: 16rpx 32rpx 42rpx;
	box-sizing: border-box;
}

.reaction-sheet-handle {
	width: 72rpx;
	height: 8rpx;
	margin: 0 auto 18rpx;
	border-radius: 999rpx;
	background: rgba(0, 0, 0, 0.16);
}

.reaction-sheet-title {
	display: block;
	text-align: center;
	font-size: 26rpx;
	color: #666666;
	margin-bottom: 22rpx;
}

.reaction-grid {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12rpx;
}

.reaction-choice {
	flex: 1;
	height: 92rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 20rpx;
	background: #f5f5f7;
	font-size: 42rpx;
	transition: transform 0.15s ease, background 0.15s ease;
}

.reaction-choice:active {
	transform: scale(0.92);
}

.reaction-choice-active {
	background: #111111;
}

/* ==================== 合并转发查看器 ==================== */
.merge-viewer-mask {
	position: fixed;
	top: 0;
	right: 0;
	bottom: 0;
	left: 0;
	z-index: 1000;
	background: rgba(0, 0, 0, 0.42);
	display: flex;
	align-items: flex-end;
}

.merge-viewer {
	width: 100%;
	height: 74vh;
	background: #ffffff;
	border-radius: 28rpx 28rpx 0 0;
	display: flex;
	flex-direction: column;
	overflow: hidden;
}

.merge-viewer-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 28rpx 32rpx 20rpx;
	border-bottom: 1rpx solid rgba(0, 0, 0, 0.06);
}

.merge-viewer-title {
	font-size: 30rpx;
	font-weight: 600;
	color: #111111;
}

.merge-viewer-close {
	font-size: 26rpx;
	color: #666666;
}

.merge-viewer-scroll {
	flex: 1;
	padding: 8rpx 32rpx 32rpx;
	box-sizing: border-box;
}

.merge-viewer-row {
	padding: 20rpx 0;
	border-bottom: 1rpx solid rgba(0, 0, 0, 0.05);
}

.merge-viewer-sender {
	display: block;
	font-size: 22rpx;
	color: #8a8a8e;
	margin-bottom: 8rpx;
}

.merge-viewer-content {
	display: block;
	font-size: 26rpx;
	line-height: 1.55;
	color: #222222;
	word-break: break-all;
}

.merge-viewer-empty {
	text-align: center;
	padding: 80rpx 0;
	font-size: 24rpx;
	color: #999999;
}


/* 已编辑标记 */
.edited-tag {
	margin-top: 8rpx;
	font-size: 20rpx;
	color: rgba(17, 17, 17, 0.42);
}
.edited-tag-other {
	margin-left: 88rpx;
}
.edited-tag text {
	font-size: 20rpx;
	color: rgba(17, 17, 17, 0.42);
}

/* 编辑消息浮层 */
.edit-message-mask {
	position: fixed;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
	z-index: 999;
	background: rgba(0, 0, 0, 0.42);
	display: flex;
	align-items: center;
	justify-content: center;
}
.edit-message-panel {
	width: 622rpx;
	box-sizing: border-box;
	padding: 32rpx;
	border-radius: 28rpx;
	background: #ffffff;
}
.edit-message-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 20rpx;
}
.edit-message-title {
	font-size: 30rpx;
	font-weight: 600;
	color: #111111;
}
.edit-message-close {
	font-size: 40rpx;
	line-height: 1;
	color: #8e8e93;
	padding: 0 6rpx;
}
.edit-message-textarea {
	width: 100%;
	min-height: 180rpx;
	box-sizing: border-box;
	padding: 20rpx 24rpx;
	border-radius: 18rpx;
	background: #f7f7fa;
	border: 1rpx solid rgba(17, 17, 17, 0.1);
	font-size: 28rpx;
	color: #111111;
	line-height: 1.6;
}
.edit-message-tip {
	margin-top: 14rpx;
	font-size: 22rpx;
	color: #8e8e93;
}
.edit-message-actions {
	display: flex;
	gap: 20rpx;
	margin-top: 28rpx;
}
.edit-message-btn {
	flex: 1;
	height: 84rpx;
	line-height: 84rpx;
	border-radius: 16rpx;
	background: #f2f2f7;
	color: #111111;
	font-size: 28rpx;
	border: none;
}
.edit-message-btn::after {
	border: none;
}
.edit-message-btn.primary {
	background: #111111;
	color: #ffffff;
}
</style>
