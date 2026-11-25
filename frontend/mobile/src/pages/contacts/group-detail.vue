<template>
	<view class="page">
		<WebSocketKeeper />
		<view class="top-bar safe-top-bar">
			<view class="top-bar-inner">
				<view class="back-btn" @click="goBack">
					<image class="back-icon" src="/static/icons/arrow/left.svg" mode="aspectFit"></image>
				</view>
				<text class="page-title">群详情</text>
				<view class="top-bar-placeholder"></view>
			</view>
		</view>

		<scroll-view class="main-content" scroll-y>
			<view class="content">
				<!-- 首屏加载骨架：避免接口未返回时先渲染“0 名成员 / 未知” -->
				<view v-if="detailLoading && !hasLoaded" class="detail-skeleton" aria-busy="true">
					<view class="detail-skeleton-hero">
						<view class="skeleton-avatar"></view>
						<view class="skeleton-lines">
							<view class="skeleton-line w45"></view>
							<view class="skeleton-line w70"></view>
							<view class="skeleton-line w55"></view>
						</view>
					</view>
					<view class="detail-skeleton-card">
						<view class="skeleton-line w30"></view>
						<view v-for="i in 5" :key="i" class="skeleton-row"></view>
					</view>
					<view class="detail-skeleton-card short">
						<view class="skeleton-line w55"></view>
					</view>
				</view>

				<!-- 首屏加载失败：给明确错误和重试，不伪装成空群 -->
				<view v-else-if="detailError && !hasLoaded" class="detail-error-state">
					<text class="detail-error-icon">!</text>
					<text class="detail-error-title">群资料加载失败</text>
					<text class="detail-error-text">{{ detailError }}</text>
					<view class="detail-error-retry" @click="loadDetail()">重新加载</view>
				</view>

				<view v-else class="content-body">
					<!-- 后台刷新失败：保留上次可用数据，只做降级提示 -->
					<view v-if="detailWarning" class="detail-warning">
						<text class="detail-warning-text">群资料同步失败：{{ detailWarning }}</text>
						<text class="detail-warning-retry" @click="loadDetail()">重试</text>
					</view>

					<!-- 群资料 -->
					<view class="hero-card">
					<text v-if="canManage" class="hero-edit" @click="openProfileEditor">编辑</text>
					<avatar :src="detail.groupAvatar" :size="160" shape="square" />
					<view class="hero-name-row">
						<text class="hero-name">{{ detail.groupName || groupName || '群聊' }}</text>
						<text v-if="myRoleText" class="hero-role" :class="rolePillClass">{{ myRoleText }}</text>
					</view>
					<text class="hero-meta">{{ memberCount }} 名成员 · 群主 {{ detail.groupOwner || detail.ownerId || '未知' }}<text v-if="Number(detail.muteAll) === 1"> · 全员禁言中</text></text>
					<text v-if="detail.description" class="hero-desc">{{ detail.description }}</text>
				</view>

				<!-- 群公告与状态 -->
				<view class="info-card">
					<view class="section-row" :class="{ 'section-row-action': canManage }" @click="openAnnouncementEditor">
						<text class="section-label">群公告</text>
						<view class="section-main">
							<text class="section-value">{{ detail.announcement || announcementEmptyText }}</text>
							<text v-if="hasAnnouncement" class="section-meta">{{ detail.announcementEditor || '群主' }} · {{ formatDateTime(detail.announcementTime) }}</text>
						</view>
						<text v-if="canManage" class="section-arrow">›</text>
					</view>
					<view class="info-row">
						<text class="info-label">群号</text>
						<view class="info-copy">
							<text class="info-value info-mono">{{ detail.groupId || groupId }}</text>
							<text class="copy-link" @click="copyGroupId">复制</text>
						</view>
					</view>
					<view class="info-row">
						<text class="info-label">创建时间</text>
						<text class="info-value">{{ formatDateTime(detail.createTime) }}</text>
					</view>
					<view class="info-row">
						<text class="info-label">消息免打扰</text>
						<switch class="info-switch" color="#0a84ff" :checked="prefMuted" @change="onMuteSettingChange" />
					</view>
					<view class="info-row">
						<text class="info-label">置顶聊天</text>
						<switch class="info-switch" color="#0a84ff" :checked="prefPinned" @change="onPinSettingChange" />
					</view>
					<view class="info-row">
						<view class="info-label-wrap">
							<text class="info-label">全员禁言</text>
							<text class="info-sub">开启后仅群主和管理员可发言</text>
						</view>
						<switch
							v-if="canManage"
							class="info-switch"
							color="#0a84ff"
							:checked="Number(detail.muteAll) === 1"
							:disabled="mutePending"
							@change="toggleMuteAll"
						/>
						<text v-else class="info-value">{{ Number(detail.muteAll) === 1 ? '已开启' : '未开启' }}</text>
					</view>
					<view class="info-row">
						<text class="info-label">我的角色</text>
						<text class="info-value">{{ myRoleText }}</text>
					</view>
					<view class="info-row">
						<text class="info-label">加入时间</text>
						<text class="info-value">{{ formatDateTime(detail.joinTime) }}</text>
					</view>
				</view>

				<!-- 群管理入口 -->
				<view class="info-card manage-card">
					<view class="manage-row" @click="openInviteSheet">
						<text class="manage-label">邀请成员</text>
						<text class="manage-value">从好友中选择</text>
						<text class="manage-arrow">›</text>
					</view>
					<view class="manage-row" @click="openGroupQr">
						<text class="manage-label">群二维码</text>
						<text class="manage-value">扫码申请入群</text>
						<text class="manage-arrow">›</text>
					</view>
					<view class="manage-row" @click="openGroupFiles">
						<text class="manage-label">群文件</text>
						<text class="manage-value">{{ filesError ? '加载失败' : (fileTotal ? fileTotal + ' 个文件' : '暂无文件') }}</text>
						<text class="manage-arrow">›</text>
					</view>
					<view class="manage-row" @click="openAiSummary">
						<text class="manage-label">AI 摘要</text>
						<text class="manage-value" :class="{ muted: !canManage }">{{ canManage ? (aiSummary ? '已生成' : '整理最近聊天') : '仅群主/管理员可用' }}</text>
						<text class="manage-arrow">›</text>
					</view>
					<view class="manage-row" @click="openVoteSheet">
						<text class="manage-label">发起群投票</text>
						<text class="manage-value">让群成员一起选</text>
						<text class="manage-arrow">›</text>
					</view>
				</view>

				<!-- 入群申请（群主 / 管理员审批） -->
				<view v-if="canManage && joinRequests.length" class="members-card join-requests-card">
					<view class="members-header">
						<text class="section-label">入群申请</text>
						<text class="members-count">{{ joinRequests.length }} 条</text>
					</view>
					<view v-for="req in joinRequests" :key="req.id" class="join-request-row">
						<view class="join-request-avatar"><text>{{ (req.username || '用')[0] }}</text></view>
						<view class="join-request-info">
							<text class="join-request-name">{{ req.username || req.userId }}</text>
							<text class="join-request-meta">{{ req.reason || '申请加入群聊' }}</text>
						</view>
						<view class="join-request-actions">
							<text class="join-btn approve" @click="onHandleJoinRequest(req, true)">同意</text>
							<text class="join-btn reject" @click="onHandleJoinRequest(req, false)">拒绝</text>
						</view>
					</view>
				</view>

				<!-- 群成员 -->
				<view class="members-card">
					<view class="members-header">
						<text class="section-label">群成员</text>
						<view class="members-header-actions">
							<text class="members-count">{{ visibleMembers.length === memberList.length ? memberList.length + ' 人' : visibleMembers.length + '/' + memberList.length + ' 人' }}</text>
							<text v-if="canManage" class="members-batch-toggle" @click="toggleMemberBatch">{{ memberBatchMode ? '完成' : '批量管理' }}</text>
						</view>
					</view>
					<view class="member-tools">
						<view class="member-search-wrap">
							<text class="member-search-icon">⌕</text>
							<input class="member-search" v-model="memberKeyword" confirm-type="search" placeholder="搜索成员" />
							<text v-if="memberKeyword" class="member-search-clear" @click="memberKeyword = ''">×</text>
						</view>
						<scroll-view class="member-filter-scroll" scroll-x>
							<view class="member-filter-row">
								<text
									v-for="option in memberRoleOptions"
									:key="option.value"
									class="member-filter-chip"
									:class="{ active: memberRoleFilter === option.value }"
									@click="memberRoleFilter = option.value"
								>{{ option.label }}<text class="member-filter-count">{{ option.count }}</text></text>
							</view>
						</scroll-view>
					</view>
					<view v-if="memberBatchMode" class="member-batch-info">
						<text class="member-batch-count">已选 {{ selectedMemberIds.length }} 人</text>
						<view class="member-batch-tools">
							<text class="member-batch-link" @click="selectAllVisibleMembers">全选当前</text>
							<text class="member-batch-link" @click="clearMemberSelection">清空</text>
						</view>
					</view>
					<scroll-view class="members-scroll" scroll-x>
						<view class="members-row">
							<view
								v-for="(member, index) in visibleMembers"
								:key="member.userId || index"
								class="member-item"
								:class="{ selected: isMemberSelected(member), selectable: memberBatchMode && canSelectMember(member) }"
								@click="onMemberTap(member)"
							>
								<view class="member-avatar-wrap">
									<avatar :src="member.avatar" :size="88" shape="circle" />
									<view v-if="memberBatchMode" class="member-check" :class="{ checked: isMemberSelected(member), disabled: !canSelectMember(member) }">
										<text v-if="isMemberSelected(member)" class="member-check-icon">✓</text>
									</view>
								</view>
								<text class="member-name">{{ member.username || '成员' }}</text>
								<text v-if="member.role === 1" class="member-role">群主</text>
								<text v-else-if="member.role === 2" class="member-role">管理员</text>
							</view>
							<view v-if="canManage && !memberBatchMode" class="member-invite" @click="openInviteSheet">
								<text class="member-invite-icon">+</text>
								<text class="member-invite-text">邀请</text>
							</view>
							<view v-if="!visibleMembers.length" class="members-empty">
								<text class="members-empty-text">{{ memberList.length ? '没有符合条件的成员' : '暂无成员信息' }}</text>
							</view>
						</view>
					</scroll-view>
					<view v-if="memberBatchMode" class="member-batch-actions">
						<view
							v-if="isOwner"
							class="member-batch-btn"
							:class="{ disabled: !batchAdminTargets.length || batchBusy }"
							@click="batchChangeRole(2)"
						>设为管理员</view>
						<view
							v-if="isOwner"
							class="member-batch-btn"
							:class="{ disabled: !batchRemoveAdminTargets.length || batchBusy }"
							@click="batchChangeRole(3)"
						>取消管理员</view>
						<view
							class="member-batch-btn danger"
							:class="{ disabled: !batchKickTargets.length || batchBusy }"
							@click="confirmBatchKick"
						>{{ batchBusy ? '处理中…' : '移出群聊' }}</view>
					</view>
				</view>

				<!-- 操作 -->
				<view v-if="canManage" class="permission-note">
					<text class="permission-note-icon">i</text>
					<text class="permission-note-text">{{ isOwner ? '你是群主，可编辑群资料、发布公告、设置管理员、转让群主或解散群聊' : '你是管理员，可编辑群资料、发布公告、邀请成员、移出普通成员' }}</text>
				</view>
				<view class="action-group">
					<view class="primary-btn" hover-class="primary-btn-hover" @click="sendMessage">发消息</view>
					<view v-if="isOwner" class="danger-btn" hover-class="danger-btn-hover" @click="dissolveGroup">解散群聊</view>
					<view v-else class="secondary-btn" hover-class="secondary-btn-hover" @click="leaveGroup">退出群聊</view>
				</view>

				<text class="page-hint">{{ isOwner ? '解散后群聊和群内消息将不可访问。' : '退出后你将不再接收该群消息。' }}</text>
			</view>
			</view>
		</scroll-view>

		<!-- 群公告编辑 -->
		<view v-if="showAnnouncementEditor" class="sheet-mask" @click="closeAnnouncement">
			<view class="sheet" @click.stop>
				<view class="sheet-head">
					<view class="sheet-head-title-wrap">
						<text class="sheet-title">编辑群公告</text>
						<text v-if="announcementDirty" class="sheet-dirty">未保存</text>
					</view>
					<view class="sheet-head-actions">
							<text v-if="hasAnnouncement" class="announcement-delete" @click="deleteAnnouncement">{{ deletingAnnouncement ? '删除中…' : '删除' }}</text>
							<text class="sheet-close" @click="closeAnnouncement">×</text>
						</view>
				</view>
				<textarea
					class="sheet-textarea"
					v-model="announcementDraft"
					maxlength="500"
					placeholder="写一条群公告，成员进群后可以看到"
				/>
				<text class="sheet-counter">{{ announcementDraft.length }}/500</text>
				<view class="sheet-actions">
					<view class="sheet-btn ghost" @click="closeAnnouncement">取消</view>
					<view class="sheet-btn primary" :class="{ disabled: savingAnnouncement }" @click="saveAnnouncement">
						{{ savingAnnouncement ? '发布中…' : '发布公告' }}
					</view>
				</view>
			</view>
		</view>

		<!-- 编辑群资料 -->
		<view v-if="showProfileSheet" class="sheet-mask" @click="closeProfileEditor">
			<view class="sheet sheet-tall" @click.stop>
				<view class="sheet-head">
					<view class="sheet-head-title-wrap">
						<text class="sheet-title">编辑群资料</text>
						<text v-if="profileDirty" class="sheet-dirty">未保存</text>
					</view>
					<text class="sheet-close" @click="closeProfileEditor">×</text>
				</view>
				<scroll-view class="sheet-list profile-form" scroll-y>
					<view class="profile-avatar-row" @click="pickProfileAvatar">
						<avatar :src="profileAvatar" :size="128" shape="square" />
						<view class="profile-avatar-copy">
							<text class="profile-avatar-title">{{ profileAvatarUploading ? '头像上传中…' : '更换群头像' }}</text>
							<text class="profile-avatar-hint">支持 JPG、PNG，最大 5MB</text>
						</view>
					</view>
					<view class="profile-field">
						<view class="profile-field-head">
							<text class="profile-field-label">群名称</text>
							<text class="profile-field-counter">{{ profileName.length }}/30</text>
						</view>
						<input class="profile-input" v-model="profileName" maxlength="30" placeholder="给群起个名字" />
					</view>
					<view class="profile-field">
						<view class="profile-field-head">
							<text class="profile-field-label">群简介</text>
							<text class="profile-field-counter">{{ profileDesc.length }}/200</text>
						</view>
						<textarea class="profile-textarea" v-model="profileDesc" maxlength="200" placeholder="介绍一下这个群" />
					</view>
				</scroll-view>
				<view class="sheet-actions">
					<view class="sheet-btn ghost" @click="closeProfileEditor">取消</view>
					<view class="sheet-btn primary" :class="{ disabled: profileSaving || !profileName.trim() }" @click="saveProfile">
						{{ profileSaving ? (profileAvatarUploading ? '上传头像中…' : '保存中…') : '保存' }}
					</view>
				</view>
			</view>
		</view>

		<!-- 邀请成员 -->
		<view v-if="showInviteSheet" class="sheet-mask" @click="closeSheets">
			<view class="sheet sheet-tall" @click.stop>
				<view class="sheet-head">
					<text class="sheet-title">邀请成员</text>
					<text class="sheet-close" @click="closeSheets">×</text>
				</view>
				<input class="sheet-search" v-model="friendKeyword" placeholder="搜索好友" />
				<scroll-view class="sheet-list" scroll-y>
					<view v-for="friend in invitableFriends" :key="friend.userId" class="friend-row" @click="toggleInvite(friend.userId)">
						<avatar :src="friend.avatar" :size="72" shape="circle" />
						<text class="friend-name">{{ friend.username || '好友' }}</text>
						<text class="friend-check" :class="{ checked: inviteSelected.includes(String(friend.userId)) }">✓</text>
					</view>
					<view v-if="!invitableFriends.length" class="sheet-empty">没有可邀请的好友</view>
				</scroll-view>
				<view class="sheet-actions">
					<view class="sheet-btn ghost" @click="closeSheets">取消</view>
					<view class="sheet-btn primary" :class="{ disabled: inviting || !inviteSelected.length }" @click="confirmInvite">
						{{ inviting ? '邀请中…' : `邀请${inviteSelected.length ? '（' + inviteSelected.length + '）' : ''}` }}
					</view>
				</view>
			</view>
		</view>

		<!-- 群文件 -->
		<view v-if="showFilesSheet" class="sheet-mask" @click="closeFilesSheet">
			<view class="sheet sheet-tall" @click.stop>
				<view class="sheet-head">
					<text class="sheet-title">群文件</text>
					<view class="sheet-head-actions">
						<text class="file-total">{{ filesError ? '—' : (filesLoading ? '…' : fileTotal + ' 个') }}</text>
						<text class="sheet-close" @click="closeFilesSheet">×</text>
					</view>
				</view>
				<view class="file-tools">
					<view class="file-search-wrap">
						<text class="file-search-icon">⌕</text>
						<input class="file-search" v-model="fileKeyword" confirm-type="search" placeholder="搜索文件名" @input="onFileKeywordInput" />
						<text v-if="fileKeyword" class="file-search-clear" @click="clearFileKeyword">×</text>
					</view>
					<scroll-view class="file-type-scroll" scroll-x>
						<view class="file-type-row">
							<text
								v-for="option in fileTypeOptions"
								:key="option.value"
								class="file-type-chip"
								:class="{ active: fileTypeFilter === option.value }"
								@click="changeFileType(option.value)"
							>{{ option.label }}</text>
						</view>
					</scroll-view>
				</view>
				<view v-if="fileUploading" class="file-upload-card">
					<view class="file-upload-line">
						<text class="file-upload-name">{{ uploadingFileName || '文件' }}</text>
						<text class="file-upload-phase">{{ fileUploadPhase === 'saving' ? '保存中…' : fileUploadProgress + '%' }}</text>
					</view>
					<view class="file-upload-track">
						<view class="file-upload-fill" :style="{ width: Math.max(4, fileUploadProgress) + '%' }"></view>
					</view>
					<view class="file-upload-actions">
						<text class="file-upload-cancel" @click="cancelFileUpload">{{ fileUploadPhase === 'saving' ? '请稍候' : '取消上传' }}</text>
					</view>
				</view>
				<view v-if="!fileUploading" class="file-upload-entry" @click="pickFile">
					<text class="file-upload-plus">+</text>
					<text class="file-upload-label">上传文件</text>
					<text class="file-upload-hint">最大 200MB</text>
				</view>
				<scroll-view class="sheet-list file-list" scroll-y @scrolltolower="loadMoreFiles">
					<view v-if="filesError" class="file-error-card">
							<text class="file-error-title">群文件加载失败</text>
							<text class="file-error-text">{{ filesError }}</text>
							<text class="file-error-retry" @click="loadFiles(true)">重新加载</text>
						</view>
						<view v-for="file in (filesError ? [] : groupFiles)" :key="file.fileId || file.id" class="file-row" @click="openGroupFile(file)">
						<view class="file-icon" :class="fileIconClass(file)">{{ fileIconText(file) }}</view>
						<view class="file-main">
							<text class="file-name">{{ file.fileName || file.name || '文件' }}</text>
							<text class="file-meta">{{ fileTypeLabel(file) }} · {{ formatSize(file.fileSize || file.size) }}</text>
							<text class="file-sub-meta">{{ file.uploaderName || '群成员' }} · {{ formatDateTime(file.createTime) }}</text>
						</view>
						<text v-if="canDeleteFile(file)" class="file-more" @click.stop="openGroupFileActions(file)">···</text>
							<text class="file-arrow">›</text>
					</view>
					<view v-if="!filesError && filesLoading && !groupFiles.length" class="sheet-empty">正在加载文件…</view>
					<view v-else-if="!filesError && !groupFiles.length" class="sheet-empty">{{ fileKeyword || fileTypeFilter !== 'all' ? '没有符合条件的文件' : '还没有群文件，上传后群成员都能下载' }}</view>
					<view v-if="groupFiles.length && !fileHasMore && !filesLoading && !filesError" class="file-list-end">共 {{ fileTotal }} 个文件</view>
					<view v-if="!filesError && filesLoading && groupFiles.length" class="file-list-end">加载中…</view>
				</scroll-view>
			</view>
		</view>

		<!-- AI 摘要 -->
		<view v-if="showAiSheet" class="sheet-mask" @click="closeSheets">
			<view class="sheet sheet-tall" @click.stop>
				<view class="sheet-head">
					<text class="sheet-title">群聊 AI 摘要</text>
					<view class="sheet-head-actions">
						<text
							class="ai-regen"
							:class="{ disabled: aiLoading || aiCooldownLeft > 0 }"
							@click="onAiAction"
						>{{ aiActionText }}</text>
						<text class="sheet-close" @click="closeSheets">×</text>
					</view>
				</view>
				<scroll-view class="sheet-list" scroll-y>
					<view v-if="aiLoading" class="sheet-empty">正在整理最近聊天…</view>
					<view v-else-if="aiError" class="ai-error">
						<text class="ai-error-title">摘要生成失败</text>
						<text class="ai-error-text">{{ aiError }}</text>
						<view class="ai-retry" @click="generateAiSummary">重试</view>
					</view>
					<template v-else>
						<text v-if="aiSummary" class="ai-summary">{{ aiSummary }}</text>
						<view v-for="(todo, index) in aiTodos" :key="index" class="ai-todo">
							<text class="ai-todo-index">{{ index + 1 }}</text>
							<view class="ai-todo-main">
								<text class="ai-todo-text">{{ todo && (todo.content || todo.text || todo) }}</text>
								<view v-if="todo && (todo.owner || todo.due || todo.dueTime)" class="ai-todo-meta">
									<text v-if="todo.owner">负责人 {{ todo.owner }}</text>
									<text v-if="todo.due || todo.dueTime">截止 {{ todo.due || todo.dueTime }}</text>
								</view>
							</view>
						</view>
						<view v-if="aiSummary && !aiTodos.length" class="ai-empty-todo">最近消息里没有明确的待办事项</view>
						<text v-if="aiSummary" class="ai-meta">
							基于最近 {{ aiMessageCount }} 条消息{{ aiGeneratedAt ? ' · ' + formatDateTime(aiGeneratedAt) : '' }}
						</text>
						<view v-if="!aiSummary && !aiTodos.length" class="sheet-empty">暂时没有可整理的内容</view>
					</template>
				</scroll-view>
			</view>
		</view>
		<!-- 发起群投票 -->
		<view v-if="showVoteSheet" class="sheet-mask" @click="closeSheets">
			<view class="sheet sheet-tall" @click.stop>
				<view class="sheet-head">
					<text class="sheet-title">发起群投票</text>
					<text class="sheet-close" @click="closeSheets">×</text>
				</view>
				<scroll-view class="sheet-list" scroll-y>
					<input class="sheet-search" v-model="voteTitle" maxlength="60" placeholder="投票主题" />
					<view v-for="(option, index) in voteOptions" :key="index" class="vote-option-row">
						<input class="vote-option-input" v-model="voteOptions[index]" maxlength="40" :placeholder="`选项 ${index + 1}`" />
						<text v-if="voteOptions.length > 2" class="vote-option-remove" @click="removeVoteOption(index)">×</text>
					</view>
					<view class="vote-tools">
						<text class="vote-add" @click="addVoteOption">+ 添加选项</text>
						<view class="vote-multi">
							<text class="vote-multi-label">多选</text>
							<switch color="#0a84ff" :checked="voteMulti" @change="onVoteMultiChange" />
						</view>
					</view>
				</scroll-view>
				<view class="sheet-actions">
					<view class="sheet-btn ghost" @click="closeSheets">取消</view>
					<view class="sheet-btn primary" :class="{ disabled: voteSending }" @click="confirmCreateVote">
						{{ voteSending ? '发起中…' : '发起投票' }}
					</view>
				</view>
			</view>
		</view>
		<!-- 群二维码 -->
		<view v-if="showGroupQr" class="sheet-mask qr-mask" @click="closeGroupQr">
			<view class="qr-dialog" @click.stop>
				<text class="qr-title">群二维码</text>
				<text class="qr-subtitle">扫码打开入群页，提交申请后由群主审批</text>
				<view class="qr-frame">
					<image v-if="groupQrUrl" class="qr-image" :src="groupQrUrl" mode="aspectFit"></image>
					<text v-else class="qr-loading">{{ groupQrLoading ? '生成中…' : '二维码不可用' }}</text>
				</view>
				<view class="qr-group">
					<text class="qr-group-name">{{ detail.groupName || groupName || '群聊' }}</text>
					<text class="qr-group-no">群号 {{ detail.groupId || groupId }}</text>
				</view>
				<view class="qr-actions">
					<text class="qr-action ghost" @click="closeGroupQr">关闭</text>
					<text class="qr-action primary" @click="copyGroupQrLink">复制入群链接</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import Avatar from '@/components/avatar/avatar.vue'
import WebSocketKeeper from '@/components/WebSocketKeeper.vue'
import { firstLetterFriends } from '@/api/contact.js'
import { getWebSocketClient } from '@/utils/websocket.js'
import { loadChatPrefs, setPref, isMuted, isPinned } from '@/utils/chatPrefs.js'
import {
	getGroupDetail,
	quitGroup,
	disbandGroup,
	inviteMembers,
	kickMember,
	setMemberRole,
	muteMember,
	updateAnnouncement,
	updateGroupInfo,
	updateMuteAll,
	transferOwner,
	getGroupAiSummary,
	getGroupFiles,
	saveGroupFile,
	deleteGroupFile,
	createVote,
	getJoinRequests,
	handleJoinRequest
} from '@/api/group.js'
import { uploadFile } from '@/api/file.js'
import { makeGroupQrDataUrl, groupQrContent } from '@/utils/groupQr.js'

export default {
	components: {
		Avatar,
		WebSocketKeeper
	},
	data() {
		return {
			groupId: '',
			groupName: '',
			groupAvatar: '',
			showGroupQr: false,
			groupQrUrl: '',
			groupQrLoading: false,
			detail: {
				groupId: '',
				groupName: '',
				groupAvatar: '',
				description: '',
				announcement: '',
				announcementEditor: '',
				announcementTime: '',
				createTime: '',
				muteAll: 0,
				ownerId: '',
				groupOwner: '',
				currentMember: 0,
				role: 3,
				roleName: '',
				joinTime: '',
				members: []
			},
			mutePending: false,
			joinRequests: [],
			joinPendingId: '',
			detailLoading: true,
			detailError: '',
			detailWarning: '',
			detailLoadSeq: 0,
			chatPrefs: {},
			prefMuted: false,
			prefPinned: false,
			memberKeyword: '',
			memberRoleFilter: 'all',
			memberBatchMode: false,
			selectedMemberIds: [],
			batchBusy: false,
			showAnnouncementEditor: false,
			announcementDraft: '',
			savingAnnouncement: false,
			deletingAnnouncement: false,
			showProfileSheet: false,
			profileName: '',
			profileDesc: '',
			profileAvatar: '',
			profileAvatarFile: null,
			profileSaving: false,
			profileAvatarUploading: false,
			showInviteSheet: false,
			friendKeyword: '',
			friends: [],
			inviteSelected: [],
			inviting: false,
			showFilesSheet: false,
			groupFiles: [],
			fileKeyword: '',
			fileTypeFilter: 'all',
			fileTypeOptions: [
				{ label: '全部', value: 'all' },
				{ label: '图片', value: 'image' },
				{ label: '文档', value: 'doc' },
				{ label: 'PDF', value: 'pdf' },
				{ label: '表格', value: 'sheet' },
				{ label: '压缩包', value: 'zip' },
				{ label: '视频', value: 'video' },
				{ label: '音频', value: 'audio' },
				{ label: '其他', value: 'other' }
			],
			filePage: 1,
			filePageSize: 20,
			fileTotal: 0,
			fileHasMore: false,
			filesLoading: false,
			filesError: '',
			fileLoadSeq: 0,
			fileSearchTimer: null,
			fileUploading: false,
			fileUploadProgress: 0,
			fileUploadPhase: '',
			uploadingFileName: '',
			fileUploadTask: null,
			fileUploadCancelRequested: false,
			showAiSheet: false,
			aiLoading: false,
			aiSummary: '',
			aiTodos: [],
			aiMessageCount: 0,
			aiGeneratedAt: '',
			aiError: '',
			aiCooldownLeft: 0,
			aiCooldownTimer: null,
			hasLoaded: false,
			lastDetailAt: 0,
			refreshTimer: null,
			refreshing: false,
			showVoteSheet: false,
			voteTitle: '',
			voteOptions: ['', ''],
			voteMulti: false,
			voteSending: false
		}
	},
	computed: {
		memberList() {
			return Array.isArray(this.detail.members) ? this.detail.members : []
		},
		visibleMembers() {
			const keyword = String(this.memberKeyword || '').trim().toLowerCase()
			const rank = (member) => {
				const role = Number(member && member.role)
				if (role === 1) return 0
				if (role === 2) return 1
				return 2
			}
			return this.memberList.filter(member => {
				if (!member) return false
				if (this.memberRoleFilter !== 'all' && Number(member.role) !== Number(this.memberRoleFilter)) return false
				if (!keyword) return true
				return String(member.username || '').toLowerCase().includes(keyword)
			}).slice().sort((a, b) => {
				const diff = rank(a) - rank(b)
				if (diff) return diff
				return String(a.username || '').localeCompare(String(b.username || ''), 'zh-Hans-CN')
			})
		},
		memberRoleOptions() {
			const counts = { 1: 0, 2: 0, 3: 0 }
			this.memberList.forEach(member => {
				const role = Number(member && member.role)
				if (counts[role] != null) counts[role] += 1
			})
			return [
				{ label: '全部', value: 'all', count: this.memberList.length },
				{ label: '群主', value: 1, count: counts[1] },
				{ label: '管理员', value: 2, count: counts[2] },
				{ label: '成员', value: 3, count: counts[3] }
			]
		},
		selectedMembers() {
			const selected = new Set(this.selectedMemberIds.map(id => String(id)))
			return this.memberList.filter(member => member && selected.has(String(member.userId)))
		},
		batchAdminTargets() {
			return this.selectedMembers.filter(member => this.canSetRoleFor(member, 2))
		},
		batchRemoveAdminTargets() {
			return this.selectedMembers.filter(member => this.canSetRoleFor(member, 3) && Number(member.role) === 2)
		},
		batchKickTargets() {
			return this.selectedMembers.filter(member => this.canKickMember(member))
		},
		announcementDirty() {
			const saved = String(this.detail.announcement || '').trim()
			return String(this.announcementDraft || '').trim() !== saved
		},
		hasAnnouncement() {
			return Boolean(String(this.detail.announcement || '').trim())
		},
		announcementEmptyText() {
			return this.canManage ? '还没有群公告，发布后所有成员都能看到' : '群主还没有发布公告'
		},
		profileDirty() {
			const savedName = String(this.detail.groupName || this.groupName || '').trim()
			const savedDesc = String(this.detail.description || '').trim()
			return String(this.profileName || '').trim() !== savedName ||
				String(this.profileDesc || '').trim() !== savedDesc ||
				Boolean(this.profileAvatarFile)
		},
		memberCount() {
			return this.memberList.length || this.detail.currentMember || 0
		},
		myUserId() {
			const userInfo = this.getUserInfo()
			return userInfo && userInfo.id ? String(userInfo.id) : ''
		},
		isOwner() {
			return Number(this.detail.role) === 1 || (this.myUserId && String(this.detail.ownerId) === this.myUserId)
		},
		canManage() {
			return Number(this.detail.role) === 1 || Number(this.detail.role) === 2
		},
		roleText() {
			if (Number(this.detail.role) === 1) return '群主'
			if (Number(this.detail.role) === 2) return '管理员'
			if (Number(this.detail.role) === 3) return '成员'
			return ''
		},
		myRoleText() {
			return this.roleText || this.detail.roleName || ''
		},
		rolePillClass() {
			if (Number(this.detail.role) === 1) return 'owner'
			if (Number(this.detail.role) === 2) return 'admin'
			return ''
		},
		aiActionText() {
			if (this.aiLoading) return '生成中…'
			if (this.aiCooldownLeft > 0) return `冷却 ${this.aiCooldownLeft}s`
			return this.aiSummary ? '重新生成' : '生成摘要'
		},
		invitableFriends() {
			const joined = new Set(this.memberList.map(m => String(m.userId)))
			const keyword = this.friendKeyword.trim().toLowerCase()
			return (this.friends || []).filter(friend => {
				if (!friend || !friend.userId || joined.has(String(friend.userId))) return false
				if (!keyword) return true
				return String(friend.username || '').toLowerCase().includes(keyword)
			})
		}
	},
	onLoad(options) {
		this.groupId = decodeURIComponent(options.id || '')
		this.groupName = decodeURIComponent(options.name || '')
		this.groupAvatar = decodeURIComponent(options.avatar || '')
		this.detail.groupId = this.groupId
		this.detail.groupName = this.groupName
		this.detail.groupAvatar = this.groupAvatar
		this.loadDetail()
		this.loadFiles(true)
		this.loadChatSettings()
	},
	onShow() {
		this.loadChatSettings()
		this.startRealtime()
		if (this.hasLoaded && Date.now() - this.lastDetailAt > 2000) this.refreshSilently()
	},
	onHide() {
		this.stopRealtime()
	},
	onUnload() {
		this.stopRealtime()
		if (this.fileSearchTimer) clearTimeout(this.fileSearchTimer)
		if (this.aiCooldownTimer) clearInterval(this.aiCooldownTimer)
		this.revokeProfilePreview()
	},
	methods: {
		getUserInfo() {
			try {
				const raw = uni.getStorageSync('userInfo')
				return raw ? JSON.parse(raw) : null
			} catch (error) {
				return null
			}
		},
		/** 读取本账号在当前群上的置顶/免打扰偏好 */
		loadChatSettings() {
			const chatId = String(this.groupId || this.detail.groupId || '')
			this.chatPrefs = loadChatPrefs()
			this.prefMuted = isMuted(this.chatPrefs, chatId)
			this.prefPinned = isPinned(this.chatPrefs, chatId)
		},
		onMuteSettingChange(event) {
			this.applyChatSetting('muted', event && event.detail ? event.detail.value : false)
		},
		onPinSettingChange(event) {
			this.applyChatSetting('pinned', event && event.detail ? event.detail.value : false)
		},
		applyChatSetting(field, value) {
			const chatId = String(this.groupId || this.detail.groupId || '')
			if (!chatId) return
			this.chatPrefs = setPref(this.chatPrefs, chatId, field, value)
			this.prefMuted = isMuted(this.chatPrefs, chatId)
			this.prefPinned = isPinned(this.chatPrefs, chatId)
			const label = field === 'muted'
				? (this.prefMuted ? '已开启消息免打扰' : '已关闭消息免打扰')
				: (this.prefPinned ? '已置顶该聊天' : '已取消置顶')
			uni.showToast({ title: label, icon: 'none' })
		},
		async openGroupQr() {
			const id = String(this.detail.groupId || this.groupId || '')
			if (!id) {
				uni.showToast({ title: '群号暂不可用', icon: 'none' })
				return
			}
			this.showGroupQr = true
			if (this.groupQrUrl) return
			this.groupQrLoading = true
			try {
				this.groupQrUrl = await makeGroupQrDataUrl(id, 480)
			} catch (e) {
				uni.showToast({ title: '二维码生成失败，请重试', icon: 'none' })
				this.showGroupQr = false
			} finally {
				this.groupQrLoading = false
			}
		},
		closeGroupQr() {
			this.showGroupQr = false
		},
		copyGroupQrLink() {
			const id = String(this.detail.groupId || this.groupId || '')
			if (!id) return
			const link = groupQrContent(id)
			uni.setClipboardData({
				data: link,
				success: () => uni.showToast({ title: '入群链接已复制', icon: 'none' })
			})
		},
		copyGroupId() {
			const id = String(this.detail.groupId || this.groupId || '')
			if (!id) {
				uni.showToast({ title: '群号暂不可用', icon: 'none' })
				return
			}
			// #ifdef H5
			if (typeof navigator !== 'undefined' && navigator.clipboard && navigator.clipboard.writeText) {
				navigator.clipboard.writeText(id).then(() => {
					uni.showToast({ title: '群号已复制', icon: 'none' })
				}).catch(() => {
					uni.setClipboardData({ data: id })
				})
				return
			}
			// #endif
			uni.setClipboardData({ data: id })
		},
		async loadDetail(options = {}) {
			if (!this.groupId) {
				this.detailLoading = false
				this.detailError = '群号无效，请返回后重新进入'
				return
			}
			const background = options.background === true
			const seq = ++this.detailLoadSeq
			if (!background) this.detailError = ''
			if (!this.hasLoaded) this.detailLoading = true
			try {
				const res = await getGroupDetail(this.groupId, { silent: true })
				if (seq !== this.detailLoadSeq) return
				if (!res || res.code !== 200 || !res.data) {
					throw new Error((res && (res.msg || res.message)) || '接口未返回群资料')
				}
				if (background && this.memberBatchMode) {
					const { members, ...rest } = res.data
					this.detail = { ...this.detail, ...rest }
				} else {
					this.detail = { ...this.detail, ...res.data }
				}
				this.hasLoaded = true
				this.lastDetailAt = Date.now()
				this.detailWarning = ''
				this.detailError = ''
				if (this.canManage) this.loadJoinRequests()
			} catch (error) {
				if (seq !== this.detailLoadSeq) return
				console.error('获取群详情失败:', error)
				const status = error && (error.statusCode || (error.response && error.response.status))
				const raw = String((error && (error.msg || error.message || (error.data && error.data.msg))) || '')
				const message = status
					? `服务暂时不可用（${status}），请稍后重试`
					: (/[\u4e00-\u9fa5]/.test(raw) ? raw : '网络异常，请稍后重试')
				// 已有数据时只做降级提示，避免一次后台刷新失败把整页清空
				if (this.hasLoaded) this.detailWarning = message
				else this.detailError = message
			} finally {
				if (seq === this.detailLoadSeq) this.detailLoading = false
			}
		},
		async loadJoinRequests() {
			if (!this.canManage) return
			try {
				const res = await getJoinRequests(this.groupId)
				this.joinRequests = (res && res.data) || []
			} catch (e) {
				this.joinRequests = []
			}
		},
		async onHandleJoinRequest(req, approve) {
			if (this.joinPendingId) return
			this.joinPendingId = String(req.id)
			try {
				const res = await handleJoinRequest({ requestId: req.id, approve: !!approve })
				if (res && res.code === 200) {
					uni.showToast({ title: approve ? '已同意入群' : '已拒绝申请', icon: 'none' })
					this.joinRequests = this.joinRequests.filter(i => String(i.id) !== String(req.id))
					if (approve) this.loadDetail({ background: true })
				}
			} catch (e) {
				/* 请求层已提示 */
			} finally {
				this.joinPendingId = ''
			}
		},
		startRealtime() {
			this.stopRealtime()
			this.refreshTimer = setInterval(() => {
				if (typeof document !== 'undefined' && document.hidden) return
				this.refreshSilently()
			}, 30000)
		},
		stopRealtime() {
			if (this.refreshTimer) {
				clearInterval(this.refreshTimer)
				this.refreshTimer = null
			}
		},
		refreshSilently() {
			if (this.refreshing) return
			if (
				this.showAnnouncementEditor ||
				this.showProfileSheet ||
				this.showInviteSheet ||
				this.showFilesSheet ||
				this.showAiSheet ||
				this.showVoteSheet ||
				this.memberBatchMode ||
				this.fileUploading ||
				this.savingAnnouncement ||
				this.deletingAnnouncement ||
				this.profileSaving ||
				this.mutePending
			) return
			this.refreshing = true
			this.loadDetail({ background: true }).finally(() => {
				this.refreshing = false
			})
		},
		formatDateTime(value) {
			if (!value) return '未知'
			const date = new Date(value)
			if (Number.isNaN(date.getTime())) return String(value)
			const year = date.getFullYear()
			const month = String(date.getMonth() + 1).padStart(2, '0')
			const day = String(date.getDate()).padStart(2, '0')
			return `${year}-${month}-${day}`
		},
		formatSize(size) {
			const bytes = Number(size)
			if (!bytes || Number.isNaN(bytes)) return ''
			if (bytes < 1024) return `${bytes} B`
			if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
			return `${(bytes / 1024 / 1024).toFixed(1)} MB`
		},
		sendMessage() {
			const currentChat = {
				chatId: this.groupId,
				chatType: 2,
				chatName: this.detail.groupName || this.groupName || '群聊',
				avatar: this.detail.groupAvatar || this.groupAvatar || '',
				lastMessageContent: null,
				unreadCount: 0,
				isOnline: false,
				updateTime: Date.now()
			}
			uni.setStorageSync('currentChat', JSON.stringify(currentChat))
			uni.navigateTo({
				url: `/pages/chat/chat-detail?id=${encodeURIComponent(this.groupId)}&name=${encodeURIComponent(currentChat.chatName)}&chatType=2`
			})
		},
		leaveGroup() {
			uni.showModal({
				title: '退出群聊',
				content: `确定退出“${this.detail.groupName || this.groupName}”吗？`,
				confirmText: '退出',
				confirmColor: '#d92d20',
				success: async (res) => {
					if (!res.confirm) return
					try {
						await quitGroup(this.groupId)
						this.afterLeave('已退出群聊')
					} catch (error) {
						console.error('退出群聊失败:', error)
					}
				}
			})
		},
		dissolveGroup() {
			uni.showModal({
				title: '解散群聊',
				content: '解散后该群将不可恢复，确定继续吗？',
				confirmText: '解散',
				confirmColor: '#d92d20',
				success: async (res) => {
					if (!res.confirm) return
					try {
						await disbandGroup(this.groupId)
						this.afterLeave('群聊已解散')
					} catch (error) {
						console.error('解散群聊失败:', error)
					}
				}
			})
		},
		afterLeave(message) {
			uni.showToast({ title: message, icon: 'success' })
			uni.setStorageSync('contactsRefresh', 1)
			setTimeout(() => {
				uni.navigateBack({
					fail: () => uni.redirectTo({ url: '/pages/contacts/contact-list' })
				})
			}, 900)
		},
		goBack() {
			uni.navigateBack({
				fail: () => uni.redirectTo({ url: '/pages/contacts/contact-list' })
			})
		},
		closeSheets() {
			this.showAnnouncementEditor = false
			this.showInviteSheet = false
			this.showFilesSheet = false
			this.showAiSheet = false
			this.showVoteSheet = false
		},
		openVoteSheet() {
			this.voteTitle = ''
			this.voteOptions = ['', '']
			this.voteMulti = false
			this.showVoteSheet = true
		},
		addVoteOption() {
			if (this.voteOptions.length >= 6) {
				uni.showToast({ title: '最多 6 个选项', icon: 'none' })
				return
			}
			this.voteOptions.push('')
		},
		removeVoteOption(index) {
			if (this.voteOptions.length <= 2) return
			this.voteOptions.splice(index, 1)
		},
		onVoteMultiChange(event) {
			this.voteMulti = Boolean(event.detail && event.detail.value)
		},
		async confirmCreateVote() {
			if (this.voteSending) return
			const title = this.voteTitle.trim()
			const options = this.voteOptions.map(item => String(item || '').trim()).filter(Boolean)
			if (!title) {
				uni.showToast({ title: '请填写投票主题', icon: 'none' })
				return
			}
			if (options.length < 2) {
				uni.showToast({ title: '至少填写 2 个选项', icon: 'none' })
				return
			}
			if (new Set(options).size !== options.length) {
				uni.showToast({ title: '选项不能重复', icon: 'none' })
				return
			}
			this.voteSending = true
			try {
				const res = await createVote({
					groupId: String(this.groupId),
					title,
					options,
					multi: this.voteMulti
				})
				const vote = res && res.data
				if (res && res.code === 200 && vote && vote.voteId) {
					this.showVoteSheet = false
					const wsClient = getWebSocketClient()
					const frame = {
						chatMessageId: '',
						senderId: this.myUserId,
						receiverId: this.groupId,
						message: `【投票】${title}||${vote.voteId}`,
						chatType: 2,
						messageType: 'chat',
						contentType: 8,
						subType: 'mes_send',
						isRead: false,
						sendStatus: 'sending',
						replyToId: '',
						replyContent: '',
						atUsers: '[]',
						createTime: Date.now()
					}
					const sent = wsClient.sendMessage(JSON.stringify(frame))
					uni.showToast({
						title: sent ? '投票已发起' : '投票已创建，消息暂未发出',
						icon: sent ? 'success' : 'none'
					})
					setTimeout(() => this.sendMessage(), 500)
				} else {
					uni.showToast({ title: (res && res.msg) || '发起投票失败', icon: 'none' })
				}
			} catch (error) {
				console.error('发起投票失败:', error)
				uni.showToast({ title: '发起投票失败，请稍后重试', icon: 'none' })
			} finally {
				this.voteSending = false
			}
		},
		openAnnouncementEditor() {
			if (!this.canManage) return
			this.announcementDraft = this.detail.announcement || ''
			this.showAnnouncementEditor = true
		},
		closeAnnouncement() {
			if (!this.showAnnouncementEditor) return
			if (!this.announcementDirty) {
				this.showAnnouncementEditor = false
				return
			}
			uni.showModal({
				title: '放弃修改？',
				content: '公告内容还没有保存，关闭后本次修改会丢失。',
				confirmText: '放弃修改',
				confirmColor: '#d92d20',
				cancelText: '继续编辑',
				success: (res) => {
					if (res.confirm) this.showAnnouncementEditor = false
				}
			})
		},
		async saveAnnouncement() {
			if (this.savingAnnouncement) return
			const text = this.announcementDraft.trim()
			this.savingAnnouncement = true
			try {
				const res = await updateAnnouncement(this.groupId, text)
				if (res && res.code === 200) {
					this.detail.announcement = text
					await this.loadDetail({ background: true })
					this.showAnnouncementEditor = false
					uni.showToast({ title: text ? '公告已发布' : '公告已清空', icon: 'none' })
				} else {
					uni.showToast({ title: res.msg || '发布失败', icon: 'none' })
				}
			} catch (error) {
				console.error('发布群公告失败:', error)
				uni.showToast({ title: '发布失败，请稍后重试', icon: 'none' })
			} finally {
				this.savingAnnouncement = false
			}
		},
		deleteAnnouncement() {
			if (this.deletingAnnouncement || !this.hasAnnouncement) return
			uni.showModal({
				title: '删除群公告',
				content: '删除后群成员将看不到这条公告，确定继续吗？',
				confirmText: '删除',
				confirmColor: '#d92d20',
				success: async (res) => {
					if (!res.confirm) return
					this.deletingAnnouncement = true
					try {
						const result = await updateAnnouncement(this.groupId, '')
						if (!result || result.code !== 200) {
							throw new Error((result && (result.msg || result.message)) || '删除公告失败')
						}
						this.detail.announcement = ''
						this.announcementDraft = ''
						this.showAnnouncementEditor = false
						uni.showToast({ title: '群公告已删除', icon: 'none' })
					} catch (error) {
						console.error('删除群公告失败:', error)
						uni.showToast({ title: (error && (error.msg || error.message)) || '删除失败，请稍后重试', icon: 'none' })
					} finally {
						this.deletingAnnouncement = false
					}
				}
			})
		},
		openProfileEditor() {
			if (!this.canManage) return
			this.revokeProfilePreview()
			this.profileName = this.detail.groupName || this.groupName || ''
			this.profileDesc = this.detail.description || ''
			this.profileAvatar = this.detail.groupAvatar || this.groupAvatar || ''
			this.profileAvatarFile = null
			this.profileSaving = false
			this.profileAvatarUploading = false
			this.showProfileSheet = true
		},
		revokeProfilePreview() {
			if (
				this.profileAvatar &&
				String(this.profileAvatar).startsWith('blob:') &&
				typeof URL !== 'undefined' &&
				typeof URL.revokeObjectURL === 'function'
			) {
				URL.revokeObjectURL(this.profileAvatar)
			}
		},
		pickProfileAvatar() {
			if (this.profileSaving || this.profileAvatarUploading) return
			// #ifdef H5
			if (typeof document !== 'undefined') {
				const el = document.createElement('input')
				el.type = 'file'
				el.accept = 'image/*'
				el.style.position = 'fixed'
				el.style.left = '-9999px'
				el.style.width = '1px'
				el.style.height = '1px'
				el.style.opacity = '0'
				const cleanup = () => {
					setTimeout(() => {
						if (el.parentNode) el.parentNode.removeChild(el)
					}, 100)
				}
				el.addEventListener('change', (event) => {
					const picked = event && event.target && event.target.files && event.target.files[0]
					if (event && event.target) event.target.value = ''
					cleanup()
					if (picked) this.applyProfileAvatar(picked)
				})
				if (typeof window !== 'undefined') {
					window.addEventListener('focus', cleanup, { once: true })
				}
				document.body.appendChild(el)
				el.click()
			}
			return
			// #endif
			// #ifndef H5
			uni.chooseImage({
				count: 1,
				sizeType: ['compressed'],
				sourceType: ['album', 'camera'],
				success: (res) => {
					const path = res && res.tempFilePaths && res.tempFilePaths[0]
					const meta = res && res.tempFiles && res.tempFiles[0]
					if (path) this.applyProfileAvatar(path, meta)
				}
			})
			// #endif
		},
		applyProfileAvatar(file, meta = {}) {
			const size = Number((file && file.size) || (meta && meta.size) || 0)
			const type = String((file && file.type) || (meta && meta.type) || '')
			if (type && !/^image\//i.test(type)) {
				uni.showToast({ title: '请选择图片文件', icon: 'none' })
				return
			}
			if (size && size > 5 * 1024 * 1024) {
				uni.showToast({ title: '头像不能超过 5MB', icon: 'none' })
				return
			}
			this.revokeProfilePreview()
			this.profileAvatarFile = file
			const isBlob = typeof Blob !== 'undefined' && file instanceof Blob
			this.profileAvatar = isBlob && typeof URL !== 'undefined'
				? URL.createObjectURL(file)
				: String((file && (file.path || file.tempFilePath)) || file)
		},
		closeProfileEditor() {
			if (!this.showProfileSheet || this.profileSaving) return
			if (!this.profileDirty) {
				this.discardProfile()
				return
			}
			uni.showModal({
				title: '放弃修改？',
				content: '群名称、头像或简介还没有保存，关闭后本次修改会丢失。',
				confirmText: '放弃修改',
				confirmColor: '#d92d20',
				cancelText: '继续编辑',
				success: (res) => {
					if (res.confirm) this.discardProfile()
				}
			})
		},
		discardProfile() {
			this.revokeProfilePreview()
			this.profileAvatarFile = null
			this.showProfileSheet = false
		},
		async saveProfile() {
			if (this.profileSaving || this.profileAvatarUploading) return
			const name = String(this.profileName || '').trim()
			if (!name) {
				uni.showToast({ title: '群名称不能为空', icon: 'none' })
				return
			}
			this.profileSaving = true
			try {
				let avatar = this.detail.groupAvatar || this.groupAvatar || ''
				if (this.profileAvatarFile) {
					this.profileAvatarUploading = true
					const uploaded = await uploadFile(this.profileAvatarFile)
					const payload = uploaded && uploaded.data
					const url = typeof payload === 'string'
						? payload
						: (payload && (payload.url || payload.fileUrl || payload.path))
					if (!url) throw new Error('头像上传失败')
					avatar = url
				}
				const res = await updateGroupInfo({
					groupId: this.groupId,
					groupName: name,
					groupAvatar: avatar,
					description: String(this.profileDesc || '').trim()
				})
				if (!res || res.code !== 200) {
					throw new Error((res && (res.msg || res.message)) || '群资料保存失败')
				}
				this.detail.groupName = name
				this.detail.groupAvatar = avatar
				this.detail.description = String(this.profileDesc || '').trim()
				this.groupName = name
				this.groupAvatar = avatar
				this.revokeProfilePreview()
				this.profileAvatarFile = null
				this.profileAvatar = avatar
				this.showProfileSheet = false
				this.syncCurrentChatProfile(name, avatar)
				uni.setStorageSync('contactsRefresh', 1)
				uni.showToast({ title: '群资料已更新', icon: 'success' })
			} catch (error) {
				console.error('保存群资料失败:', error)
				uni.showToast({ title: (error && (error.msg || error.message)) || '保存失败，请稍后重试', icon: 'none' })
			} finally {
				this.profileSaving = false
				this.profileAvatarUploading = false
			}
		},
		syncCurrentChatProfile(name, avatar) {
			try {
				const raw = uni.getStorageSync('currentChat')
				if (!raw) return
				const chat = JSON.parse(raw)
				if (!chat || String(chat.chatId) !== String(this.groupId)) return
				chat.chatName = name
				chat.avatar = avatar
				uni.setStorageSync('currentChat', JSON.stringify(chat))
			} catch (error) {
				console.warn('同步当前会话群资料失败:', error)
			}
		},
		async toggleMuteAll(event) {
			if (!this.canManage || this.mutePending) return
			const next = event.detail.value ? 1 : 0
			this.mutePending = true
			try {
				const res = await updateMuteAll({ groupId: String(this.groupId), muteAll: next })
				if (res && res.code === 200) {
					this.detail.muteAll = next
					uni.showToast({ title: next ? '已开启全员禁言' : '已关闭全员禁言', icon: 'none' })
				} else {
					this.detail.muteAll = next ? 0 : 1
					uni.showToast({ title: res.msg || '设置失败', icon: 'none' })
				}
			} catch (error) {
				this.detail.muteAll = next ? 0 : 1
				console.error('设置全员禁言失败:', error)
				uni.showToast({ title: '设置失败，请稍后重试', icon: 'none' })
			} finally {
				this.mutePending = false
			}
		},
		async loadFriends() {
			if (this.friends.length) return
			try {
				const res = await firstLetterFriends()
				if (res && res.code === 200 && Array.isArray(res.data)) {
					const flattened = []
					res.data.forEach(group => {
						(group.list || []).forEach(friend => {
							flattened.push({
								userId: friend.userId || friend.id,
								username: friend.username || friend.name || friend.nickname || '好友',
								avatar: friend.avatar || ''
							})
						})
					})
					this.friends = flattened
				}
			} catch (error) {
				console.error('加载好友失败:', error)
			}
		},
		async openInviteSheet() {
			this.showInviteSheet = true
			this.inviteSelected = []
			await this.loadFriends()
		},
		toggleInvite(userId) {
			const id = String(userId)
			const index = this.inviteSelected.indexOf(id)
			if (index === -1) this.inviteSelected.push(id)
			else this.inviteSelected.splice(index, 1)
		},
		async confirmInvite() {
			if (this.inviting || !this.inviteSelected.length) return
			this.inviting = true
			try {
				const res = await inviteMembers(this.groupId, this.inviteSelected)
				if (res && res.code === 200) {
					uni.showToast({ title: '邀请成功', icon: 'none' })
					this.closeSheets()
					this.inviteSelected = []
					await this.loadDetail()
				} else {
					uni.showToast({ title: res.msg || '邀请失败', icon: 'none' })
				}
			} catch (error) {
				console.error('邀请成员失败:', error)
				uni.showToast({ title: '邀请失败，请稍后重试', icon: 'none' })
			} finally {
				this.inviting = false
			}
		},
		async openGroupFiles() {
			this.showFilesSheet = true
			this.fileKeyword = ''
			this.fileTypeFilter = 'all'
			await this.loadFiles(true)
		},
		closeFilesSheet() {
			if (this.fileUploading) {
				uni.showToast({ title: '请先取消正在上传的文件', icon: 'none' })
				return
			}
			this.showFilesSheet = false
		},
		async loadFiles(reset = true) {
			if (!this.groupId) return
			const page = reset ? 1 : this.filePage
			const seq = ++this.fileLoadSeq
			if (reset) {
				this.filePage = 1
				this.fileHasMore = false
				this.filesError = ''
			}
			this.filesLoading = true
			try {
				const res = await getGroupFiles(this.groupId, this.fileKeyword.trim(), page, this.filePageSize, this.fileTypeFilter, { silent: true })
				if (seq !== this.fileLoadSeq) return
				const data = res && res.data
				if (!res || res.code !== 200 || !data) {
					throw new Error((res && (res.msg || res.message)) || '群文件加载失败')
				}
				const rows = Array.isArray(data)
					? data
					: ((data && (data.records || data.rows || data.list)) || [])
				this.groupFiles = reset ? rows : this.groupFiles.concat(rows)
				this.filePage = page
				const totalValue = data && !Array.isArray(data)
					? (data.total !== undefined ? data.total : (data.totalCount !== undefined ? data.totalCount : data.count))
					: undefined
				this.fileTotal = totalValue !== undefined && totalValue !== null
					? Number(totalValue)
					: this.groupFiles.length
				this.fileHasMore = this.groupFiles.length < this.fileTotal || rows.length >= this.filePageSize
				this.filesError = ''
			} catch (error) {
				if (seq !== this.fileLoadSeq) return
				console.error('加载群文件失败:', error)
				this.filesError = '群文件加载失败，请稍后重试'
				if (reset) {
					this.groupFiles = []
					this.fileTotal = 0
					this.fileHasMore = false
				}
			} finally {
				if (seq === this.fileLoadSeq) this.filesLoading = false
			}
		},
		loadMoreFiles() {
			if (this.filesLoading || !this.fileHasMore) return
			this.filePage += 1
			this.loadFiles(false)
		},
		onFileKeywordInput() {
			if (this.fileSearchTimer) clearTimeout(this.fileSearchTimer)
			this.fileSearchTimer = setTimeout(() => {
				this.loadFiles(true)
			}, 350)
		},
		clearFileKeyword() {
			this.fileKeyword = ''
			this.loadFiles(true)
		},
		changeFileType(value) {
			if (this.fileTypeFilter === value) return
			this.fileTypeFilter = value
			this.loadFiles(true)
		},
		pickFile() {
			if (this.fileUploading) return
			// #ifdef H5
			if (typeof document !== 'undefined') {
				const el = document.createElement('input')
				el.type = 'file'
				el.style.position = 'fixed'
				el.style.left = '-9999px'
				el.style.width = '1px'
				el.style.height = '1px'
				el.style.opacity = '0'
				const cleanup = () => {
					setTimeout(() => {
						if (el.parentNode) el.parentNode.removeChild(el)
					}, 100)
				}
				el.addEventListener('change', (event) => {
					this.onH5FileChange(event)
					cleanup()
				})
				if (typeof window !== 'undefined') {
					window.addEventListener('focus', cleanup, { once: true })
				}
				document.body.appendChild(el)
				el.click()
			}
			return
			// #endif
			// #ifndef H5
			if (typeof uni.chooseMessageFile === 'function') {
				uni.chooseMessageFile({
					count: 1,
					type: 'all',
					success: (res) => {
						const picked = (res.tempFiles && res.tempFiles[0]) || null
						if (picked) this.startFileUpload(picked)
					}
				})
			} else {
				uni.showToast({ title: '当前版本暂不支持选择文件', icon: 'none' })
			}
			// #endif
		},
		onH5FileChange(event) {
			const picked = event && event.target && event.target.files && event.target.files[0]
			if (event && event.target) event.target.value = ''
			if (picked) this.startFileUpload(picked)
		},
		async startFileUpload(file) {
			if (!file || this.fileUploading) return
			const name = file.name || file.fileName || (file.path ? String(file.path).split('/').pop() : '') || '文件'
			const size = Number(file.size || file.fileSize || 0)
			if (size > 200 * 1024 * 1024) {
				uni.showToast({ title: '文件不能超过 200MB', icon: 'none' })
				return
			}
			this.fileUploading = true
			this.fileUploadPhase = 'uploading'
			this.fileUploadProgress = 0
			this.fileUploadCancelRequested = false
			this.uploadingFileName = name
			this.fileUploadTask = null
			try {
				const uploaded = await uploadFile(file, {
					onProgress: (event) => {
						const percent = event && event.total
							? Math.round((Number(event.loaded) || 0) * 100 / Number(event.total))
							: (event && event.percent) || 0
						this.fileUploadProgress = Math.max(1, Math.min(99, percent))
					},
					onTask: (task) => {
						this.fileUploadTask = task
						if (this.fileUploadCancelRequested && task && typeof task.abort === 'function') {
							task.abort()
						}
					}
				})
				const payload = uploaded && uploaded.data
				const fileUrl = typeof payload === 'string'
					? payload
					: (payload && (payload.url || payload.fileUrl || payload.path))
				if (!fileUrl) throw new Error('上传结果缺少文件地址')
				this.fileUploadPhase = 'saving'
				this.fileUploadProgress = 100
				const saved = await saveGroupFile({
					groupId: this.groupId,
					fileName: name,
					fileUrl,
					fileSize: size,
					mimeType: file.type || file.mimeType || ''
				})
				if (!saved || saved.code !== 200) throw new Error((saved && saved.msg) || '保存到群文件失败')
				uni.showToast({ title: '文件已上传', icon: 'success' })
				await this.loadFiles(true)
			} catch (error) {
				const canceled = !!error && (
					error.name === 'AbortError' ||
					error.code === 'ERR_CANCELED' ||
					/abort|cancel|取消/i.test(String(error.message || error.msg || ''))
				)
				if (canceled) {
					uni.showToast({ title: '已取消上传', icon: 'none' })
				} else {
					console.error('上传群文件失败:', error)
					uni.showToast({ title: (error && (error.msg || error.message)) || '上传失败，请稍后重试', icon: 'none' })
				}
			} finally {
				this.fileUploading = false
				this.fileUploadPhase = ''
				this.fileUploadProgress = 0
				this.uploadingFileName = ''
				this.fileUploadTask = null
				this.fileUploadCancelRequested = false
			}
		},
		cancelFileUpload() {
			if (!this.fileUploading) return
			if (this.fileUploadPhase === 'saving') {
				uni.showToast({ title: '文件已上传，正在保存', icon: 'none' })
				return
			}
			this.fileUploadCancelRequested = true
			if (this.fileUploadTask && typeof this.fileUploadTask.abort === 'function') {
				this.fileUploadTask.abort()
			} else {
				uni.showToast({ title: '正在取消上传…', icon: 'none' })
			}
		},
		fileExtension(file) {
			const name = String((file && (file.fileName || file.name)) || '')
			const ext = name.indexOf('.') !== -1 ? name.split('.').pop() : ''
			return String((file && file.fileExt) || ext || '').toLowerCase()
		},
		fileTypeLabel(file) {
			const ext = this.fileExtension(file)
			if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg'].includes(ext)) return '图片'
			if (ext === 'pdf') return 'PDF'
			if (['doc', 'docx', 'txt', 'md', 'rtf'].includes(ext)) return '文档'
			if (['xls', 'xlsx', 'csv'].includes(ext)) return '表格'
			if (['zip', 'rar', '7z', 'tar', 'gz'].includes(ext)) return '压缩包'
			if (['mp4', 'mov', 'avi', 'mkv', 'webm'].includes(ext)) return '视频'
			if (['mp3', 'wav', 'm4a', 'aac', 'flac'].includes(ext)) return '音频'
			return '其他'
		},
		fileIconText(file) {
			const ext = this.fileExtension(file)
			return ext ? ext.slice(0, 4).toUpperCase() : 'FILE'
		},
		fileIconClass(file) {
			const label = this.fileTypeLabel(file)
			if (label === '图片') return 'type-image'
			if (label === 'PDF') return 'type-pdf'
			if (label === '文档') return 'type-doc'
			if (label === '表格') return 'type-sheet'
			if (label === '压缩包') return 'type-zip'
			if (label === '视频') return 'type-video'
			if (label === '音频') return 'type-audio'
			return 'type-other'
		},
		openGroupFile(file) {
			const url = file.fileUrl || file.url || file.path
			if (!url) {
				uni.showToast({ title: '文件地址不可用', icon: 'none' })
				return
			}
			// #ifdef H5
			window.open(url, '_blank')
			// #endif
			// #ifndef H5
			uni.downloadFile({ url, success: () => uni.showToast({ title: '已开始下载', icon: 'none' }) })
			// #endif
		},
		canDeleteFile(file) {
			if (!file) return false
			if (this.canManage) return true
			const uploaderId = file.uploaderId || file.userId || file.creatorId || file.createBy
			return Boolean(this.myUserId && uploaderId && String(uploaderId) === this.myUserId)
		},
		openGroupFileActions(file) {
			if (!this.canDeleteFile(file)) return
			uni.showActionSheet({
				itemList: ['删除文件'],
				success: (res) => {
					if (Number(res.tapIndex) === 0) this.confirmDeleteFile(file)
				}
			})
		},
		confirmDeleteFile(file) {
			if (!file) return
			const fileId = file.fileId || file.id
			if (!fileId) {
				uni.showToast({ title: '文件信息已失效', icon: 'none' })
				return
			}
			uni.showModal({
				title: '删除群文件',
				content: `「${file.fileName || file.name || '该文件'}」将从群文件列表移除，聊天里已发送的文件消息不受影响。`,
				confirmText: '删除',
				confirmColor: '#d92d20',
				success: async (res) => {
					if (!res.confirm) return
					try {
						const result = await deleteGroupFile(fileId)
						if (!result || result.code !== 200) {
							throw new Error((result && (result.msg || result.message)) || '删除文件失败')
						}
						this.groupFiles = this.groupFiles.filter(item => String(item.fileId || item.id) !== String(fileId))
						this.fileTotal = Math.max(0, Number(this.fileTotal || 0) - 1)
						uni.showToast({ title: '群文件已删除', icon: 'none' })
						await this.loadFiles(true)
					} catch (error) {
						console.error('删除群文件失败:', error)
						uni.showToast({ title: (error && (error.msg || error.message)) || '删除失败，请稍后重试', icon: 'none' })
					}
				}
			})
		},
		openAiSummary() {
			if (!this.canManage) {
				uni.showToast({ title: '仅群主或管理员可生成群摘要', icon: 'none' })
				return
			}
			this.showAiSheet = true
			if (this.aiLoading || this.aiSummary || this.aiTodos.length || this.aiError) return
			this.generateAiSummary()
		},
		onAiAction() {
			if (this.aiLoading) return
			if (this.aiCooldownLeft > 0) {
				uni.showToast({ title: `生成冷却中，请 ${this.aiCooldownLeft} 秒后再试`, icon: 'none' })
				return
			}
			this.generateAiSummary()
		},
		startAiCooldown(seconds = 8) {
			if (this.aiCooldownTimer) clearInterval(this.aiCooldownTimer)
			this.aiCooldownLeft = seconds
			this.aiCooldownTimer = setInterval(() => {
				this.aiCooldownLeft = Math.max(0, this.aiCooldownLeft - 1)
				if (this.aiCooldownLeft <= 0 && this.aiCooldownTimer) {
					clearInterval(this.aiCooldownTimer)
					this.aiCooldownTimer = null
				}
			}, 1000)
		},
		async generateAiSummary() {
			if (!this.canManage) {
				uni.showToast({ title: '仅群主或管理员可生成群摘要', icon: 'none' })
				return
			}
			if (this.aiLoading) return
			if (this.aiCooldownLeft > 0) {
				uni.showToast({ title: `生成冷却中，请 ${this.aiCooldownLeft} 秒后再试`, icon: 'none' })
				return
			}
			this.aiLoading = true
			this.aiError = ''
			try {
				const res = await getGroupAiSummary(this.groupId, { silent: true })
				if (!res || res.code !== 200) throw new Error((res && res.msg) || '摘要生成失败')
				const data = res.data || {}
				this.aiSummary = data.summary || data.aiSummary || ''
				this.aiTodos = Array.isArray(data.todos) ? data.todos : (Array.isArray(data.todoList) ? data.todoList : [])
				this.aiMessageCount = Number(data.messageCount) || 0
				this.aiGeneratedAt = data.generatedAt || ''
				this.startAiCooldown(8)
			} catch (error) {
				const message = String((error && (error.msg || error.message || (error.data && error.data.msg))) || '')
				if (/频繁/.test(message)) {
					const wait = message.match(/请\s*(\d+)\s*秒后/)
					this.aiError = wait ? `生成太频繁，请 ${wait[1]} 秒后再试` : '生成太频繁，请稍后再试'
					if (wait) this.startAiCooldown(Math.max(1, Number(wait[1]) || 8))
				}
				else if (/群主|管理员/.test(message)) this.aiError = '仅群主或管理员可生成群摘要'
				else this.aiError = message || '生成失败，请稍后重试'
				this.aiSummary = ''
				this.aiTodos = []
				this.aiMessageCount = 0
				this.aiGeneratedAt = ''
			} finally {
				this.aiLoading = false
			}
		},
		onMemberTap(member) {
			if (!member) return
			if (this.memberBatchMode) {
				this.toggleMemberSelection(member)
				return
			}
			this.openMemberActions(member)
		},
		toggleMemberBatch() {
			this.memberBatchMode = !this.memberBatchMode
			if (!this.memberBatchMode) this.selectedMemberIds = []
		},
		canSelectMember(member) {
			if (!member || !this.canManage) return false
			if (String(member.userId) === this.myUserId) return false
			if (Number(member.role) === 1 || member.owner === true) return false
			if (this.isOwner) return true
			return Number(member.role) === 3
		},
		isMemberSelected(member) {
			if (!member) return false
			return this.selectedMemberIds.includes(String(member.userId))
		},
		toggleMemberSelection(member) {
			if (!this.canSelectMember(member)) {
				if (String(member.userId) === this.myUserId) uni.showToast({ title: '不能选择自己', icon: 'none' })
				else if (Number(member.role) === 1 || member.owner === true) uni.showToast({ title: '群主不可被操作', icon: 'none' })
				else uni.showToast({ title: '当前身份不能操作该成员', icon: 'none' })
				return
			}
			const id = String(member.userId)
			const index = this.selectedMemberIds.indexOf(id)
			if (index === -1) this.selectedMemberIds.push(id)
			else this.selectedMemberIds.splice(index, 1)
		},
		selectAllVisibleMembers() {
			const ids = this.visibleMembers.filter(member => this.canSelectMember(member)).map(member => String(member.userId))
			this.selectedMemberIds = Array.from(new Set(this.selectedMemberIds.concat(ids)))
		},
		clearMemberSelection() {
			this.selectedMemberIds = []
		},
		canSetRoleFor(member, role) {
			if (!member || !this.isOwner) return false
			if (String(member.userId) === this.myUserId) return false
			if (member.owner === true || Number(member.role) === 1) return false
			if (Number(role) === 2) return Number(member.role) !== 2
			if (Number(role) === 3) return Number(member.role) === 2
			return false
		},
		canKickMember(member) {
			if (!member || String(member.userId) === this.myUserId) return false
			if (this.isOwner) return member.owner !== true && Number(member.role) !== 1
			if (this.canManage) return Number(member.role) === 3
			return false
		},
		batchChangeRole(role) {
			if (this.batchBusy) return
			if (!this.isOwner) {
				uni.showToast({ title: '只有群主可以设置管理员', icon: 'none' })
				return
			}
			const targets = Number(role) === 2 ? this.batchAdminTargets : this.batchRemoveAdminTargets
			if (!targets.length) {
				uni.showToast({ title: '选中的成员里没有可操作对象', icon: 'none' })
				return
			}
			const action = Number(role) === 2 ? '设为管理员' : '取消管理员'
			uni.showModal({
				title: `批量${action}`,
				content: Number(role) === 2
					? `将对选中的 ${targets.length} 位成员开放管理员权限，可撤回消息、移出普通成员。`
					: `将收回选中的 ${targets.length} 位成员的管理员权限，恢复为普通成员。`,
				confirmText: action,
				success: async (res) => {
					if (!res.confirm || this.batchBusy) return
					this.batchBusy = true
					try {
						const results = await Promise.allSettled(targets.map(member =>
							setMemberRole(this.groupId, member.userId, role, { silent: true, loading: false })
						))
						const failed = []
						let succeeded = 0
						results.forEach((result, index) => {
							if (result.status === 'fulfilled' && result.value && result.value.code === 200) {
								succeeded += 1
							} else {
								failed.push(targets[index])
							}
						})
						this.selectedMemberIds = failed.map(member => String(member.userId))
						if (failed.length) {
							uni.showToast({ title: `已${action} ${succeeded} 人，${failed.length} 人失败，请重试`, icon: 'none' })
						} else {
							uni.showToast({ title: `已${action} ${succeeded} 人`, icon: 'none' })
						}
						await this.loadDetail()
					} finally {
						this.batchBusy = false
					}
				}
			})
		},
		confirmBatchKick() {
			if (this.batchBusy) return
			const targets = this.batchKickTargets
			if (!targets.length) {
				uni.showToast({ title: '选中的成员里没有可移出的对象', icon: 'none' })
				return
			}
			uni.showModal({
				title: '批量移出成员',
				content: `将把选中的 ${targets.length} 位成员移出群聊，历史消息仍会保留。`,
				confirmText: '移出群聊',
				confirmColor: '#d92d20',
				success: async (res) => {
					if (!res.confirm || this.batchBusy) return
					this.batchBusy = true
					try {
						const results = await Promise.allSettled(targets.map(member =>
							kickMember(this.groupId, member.userId, { silent: true, loading: false })
						))
						const failed = []
						let succeeded = 0
						results.forEach((result, index) => {
							if (result.status === 'fulfilled' && result.value && result.value.code === 200) {
								succeeded += 1
							} else {
								failed.push(targets[index])
							}
						})
						this.selectedMemberIds = failed.map(member => String(member.userId))
						if (failed.length) {
							uni.showToast({ title: `已移出 ${succeeded} 人，${failed.length} 人失败，请重试`, icon: 'none' })
						} else {
							uni.showToast({ title: `已移出 ${succeeded} 人`, icon: 'none' })
						}
						await this.loadDetail()
					} finally {
						this.batchBusy = false
					}
				}
			})
		},
		openMemberActions(member) {
			if (!member || !this.canManage) return
			const isSelf = String(member.userId) === this.myUserId
			const isGroupOwner = Number(member.role) === 1 || member.owner === true
			if (isSelf || isGroupOwner) return
			const actions = []
			if (this.isOwner) {
				actions.push({ key: 'role', label: Number(member.role) === 2 ? '取消管理员' : '设为管理员' })
				actions.push({ key: 'transfer', label: '转让群主' })
			}
			const canKick = this.isOwner || Number(member.role) === 3
			if (canKick) actions.push({ key: 'kick', label: '移出群聊' })
			// 群主可禁言管理员与成员，管理员仅可禁言普通成员
			const canMute = this.isOwner || Number(member.role) === 3
			if (canMute) actions.push({ key: 'mute', label: member.muted ? '解除 / 调整禁言' : '禁言成员' })
			if (!actions.length) return
			uni.showActionSheet({
				itemList: actions.map(action => action.label),
				success: (res) => {
					const action = actions[res.tapIndex]
					if (!action) return
					if (action.key === 'role') this.changeMemberRole(member)
					else if (action.key === 'kick') this.confirmKick(member)
					else if (action.key === 'transfer') this.confirmTransfer(member)
					else if (action.key === 'mute') this.chooseMuteDuration(member)
				}
			})
		},
		chooseMuteDuration(member) {
			const options = member.muted
				? [{ key: 0, label: '解除禁言' }, { key: 60, label: '再禁言 1 小时' }, { key: 1440, label: '再禁言 1 天' }]
				: [{ key: 10, label: '10 分钟' }, { key: 60, label: '1 小时' }, { key: 1440, label: '1 天' }, { key: 10080, label: '7 天' }, { key: 43200, label: '30 天' }]
			uni.showActionSheet({
				itemList: options.map(option => option.label),
				success: (res) => {
					const option = options[res.tapIndex]
					if (!option) return
					this.confirmMute(member, option.key)
				}
			})
		},
		confirmMute(member, minutes) {
			const isUnmute = minutes <= 0
			uni.showModal({
				title: isUnmute ? '解除禁言' : '禁言成员',
				content: isUnmute
					? `确定解除“${member.username || '该成员'}”的禁言吗？`
					: `确定禁言“${member.username || '该成员'}”${minutes % 1440 === 0 ? minutes / 1440 + ' 天' : minutes % 60 === 0 ? minutes / 60 + ' 小时' : minutes + ' 分钟'}吗？期间该成员无法在群内发言。`,
				confirmText: isUnmute ? '解除' : '禁言',
				confirmColor: isUnmute ? '#0a84ff' : '#d92d20',
				success: async (res) => {
					if (!res.confirm) return
					try {
						const result = await muteMember({
							groupId: String(this.groupId),
							targetUserId: member.userId,
							durationMinutes: minutes,
						})
						if (result && result.code === 200) {
							uni.showToast({ title: isUnmute ? '已解除禁言' : '已禁言该成员', icon: 'none' })
							await this.loadDetail()
						} else {
							uni.showToast({ title: (result && result.msg) || '设置失败', icon: 'none' })
						}
					} catch (error) {
						console.error('禁言设置失败:', error)
						uni.showToast({ title: '设置失败，请稍后重试', icon: 'none' })
					}
				}
			})
		},
		changeMemberRole(member) {
			const role = Number(member.role) === 2 ? 3 : 2
			uni.showModal({
				title: role === 2 ? '设为管理员' : '取消管理员',
				content: `确定${role === 2 ? '把' + (member.username || '该成员') + '设为管理员' : '取消' + (member.username || '该成员') + '的管理员身份'}吗？`,
				confirmText: '确定',
				success: async (res) => {
					if (!res.confirm) return
					try {
						const result = await setMemberRole(this.groupId, member.userId, role)
						if (result && result.code === 200) {
							uni.showToast({ title: '设置成功', icon: 'none' })
							await this.loadDetail()
						} else {
							uni.showToast({ title: result.msg || '设置失败', icon: 'none' })
						}
					} catch (error) {
						console.error('设置成员角色失败:', error)
					}
				}
			})
		},
		confirmKick(member) {
			uni.showModal({
				title: '移出群聊',
				content: `确定把“${member.username || '该成员'}”移出群聊吗？`,
				confirmText: '移出',
				confirmColor: '#d92d20',
				success: async (res) => {
					if (!res.confirm) return
					try {
						const result = await kickMember(this.groupId, member.userId)
						if (result && result.code === 200) {
							uni.showToast({ title: '已移出群聊', icon: 'none' })
							await this.loadDetail()
						} else {
							uni.showToast({ title: result.msg || '移出失败', icon: 'none' })
						}
					} catch (error) {
						console.error('移出成员失败:', error)
					}
				}
			})
		},
		confirmTransfer(member) {
			uni.showModal({
				title: '转让群主',
				content: `确定把群主转让给“${member.username || '该成员'}”吗？转让后你将成为普通成员。`,
				confirmText: '转让',
				confirmColor: '#d92d20',
				success: async (res) => {
					if (!res.confirm) return
					try {
						const result = await transferOwner(this.groupId, member.userId)
						if (result && result.code === 200) {
							uni.showToast({ title: '群主已转让', icon: 'none' })
							await this.loadDetail()
						} else {
							uni.showToast({ title: result.msg || '转让失败', icon: 'none' })
						}
					} catch (error) {
						console.error('转让群主失败:', error)
					}
				}
			})
		}
	}
}
</script>

<style lang="scss" scoped>
.page {
	display: block;
	min-height: 100vh;
	background-color: #f2f2f7;
}

.top-bar {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 50;
	background: rgba(255, 255, 255, 0.86);
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.12);
	backdrop-filter: saturate(180%) blur(20px);
	-webkit-backdrop-filter: saturate(180%) blur(20px);
}

.top-bar-inner {
	height: 96rpx;
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 0 24rpx;
}

.back-btn {
	width: 64rpx;
	height: 64rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}

.back-icon {
	width: 40rpx;
	height: 40rpx;
	opacity: 0.75;
}

.page-title {
	font-size: 34rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.top-bar-placeholder {
	width: 64rpx;
}

.main-content {
	height: 100vh;
	padding-top: 0;
	padding-bottom: 0;
}

.content {
	display: block;
	padding: calc(var(--status-bar-height) + 124rpx) 24rpx 64rpx;
	max-width: 720rpx;
	margin: 0 auto;
}

.hero-card {
	position: relative;
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
	padding: 44rpx 32rpx 36rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.hero-edit {
	position: absolute;
	top: 18rpx;
	right: 20rpx;
	padding: 8rpx 16rpx;
	font-size: 24rpx;
	color: #0a84ff;
}

.hero-desc {
	margin-top: 14rpx;
	padding: 0 20rpx;
	font-size: 23rpx;
	line-height: 1.55;
	color: rgba(60, 60, 67, 0.55);
	text-align: center;
}

.hero-name {
	margin-top: 24rpx;
	font-size: 38rpx;
	font-weight: 600;
	color: #1c1c1e;
	letter-spacing: -0.02em;
}

.hero-meta {
	margin-top: 8rpx;
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.6);
}

.info-card {
	display: block;
	margin-top: 24rpx;
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
	overflow: hidden;
}

.section-row {
	display: flex;
	align-items: flex-start;
	gap: 20rpx;
	padding: 28rpx 28rpx 24rpx;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.08);
}

.section-row-action {
	background: rgba(10, 132, 255, 0.04);
}

.section-label {
	font-size: 26rpx;
	font-weight: 600;
	color: #1c1c1e;
	white-space: nowrap;
}

.section-value {
	flex: none;
	font-size: 26rpx;
	line-height: 1.5;
	color: rgba(60, 60, 67, 0.65);
	word-break: break-all;
}

.section-main {
	display: flex;
	flex: 1;
	flex-direction: column;
	min-width: 0;
	gap: 6rpx;
}

.section-meta {
	font-size: 21rpx;
	line-height: 1.4;
	color: rgba(60, 60, 67, 0.4);
}

.section-arrow {
	font-size: 32rpx;
	line-height: 1;
	color: rgba(60, 60, 67, 0.35);
}

.info-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 26rpx 28rpx;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.08);
}

.info-row:last-child {
	border-bottom: 0;
}

.info-label {
	font-size: 26rpx;
	color: #1c1c1e;
}

.info-value {
	font-size: 26rpx;
	color: rgba(60, 60, 67, 0.6);
}

.info-copy {
	display: flex;
	align-items: center;
	gap: 16rpx;
	min-width: 0;
}

.info-mono {
	font-family: 'SF Mono', ui-monospace, Menlo, Consolas, monospace;
	font-size: 24rpx;
	letter-spacing: 0.2rpx;
}

.copy-link {
	flex-shrink: 0;
	font-size: 24rpx;
	color: #0a84ff;
	padding: 6rpx 4rpx;
}

.info-switch {
	transform: scale(0.85);
}

.manage-card {
	margin-top: 24rpx;
}

.manage-row {
	display: flex;
	align-items: center;
	gap: 16rpx;
	padding: 26rpx 28rpx;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.08);
}

.manage-row:last-child {
	border-bottom: 0;
}

.manage-label {
	font-size: 26rpx;
	color: #1c1c1e;
}

.manage-value {
	flex: 1;
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.55);
	text-align: right;
}

.manage-arrow {
	font-size: 32rpx;
	line-height: 1;
	color: rgba(60, 60, 67, 0.35);
}

.join-requests-card { padding-bottom: 16rpx; }
.join-request-row {
	display: flex;
	align-items: center;
	gap: 16rpx;
	padding: 16rpx 28rpx;
}
.join-request-avatar {
	width: 64rpx;
	height: 64rpx;
	border-radius: 50%;
	background: #111;
	color: #fff;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 26rpx;
	flex: 0 0 auto;
}
.join-request-info { flex: 1; min-width: 0; }
.join-request-name { font-size: 28rpx; font-weight: 600; color: #111; }
.join-request-meta { display: block; font-size: 22rpx; color: #8e8e93; margin-top: 4rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.join-request-actions { display: flex; gap: 12rpx; }
.join-btn {
	padding: 8rpx 20rpx;
	border-radius: 10rpx;
	font-size: 24rpx;
}
.join-btn.approve { background: #111; color: #fff; }
.join-btn.reject { background: rgba(0,0,0,.06); color: #666; }

.members-card {
	display: block;
	margin-top: 24rpx;	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
	padding: 28rpx 0 24rpx;
}

.members-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 0 28rpx 20rpx;
}

.members-count {
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.5);
}

.members-scroll {
	width: 100%;
	white-space: nowrap;
}

.members-row {
	display: inline-flex;
	align-items: flex-start;
	gap: 24rpx;
	padding: 0 28rpx;
}

.member-item {
	width: 108rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 10rpx;
}

.member-name {
	max-width: 108rpx;
	font-size: 22rpx;
	color: #1c1c1e;
	text-align: center;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}

.member-role {
	font-size: 20rpx;
	color: #0a84ff;
}

.member-invite {
	width: 96rpx;
	height: 96rpx;
	margin-top: 4rpx;
	border-radius: 50%;
	border: 2rpx dashed rgba(60, 60, 67, 0.25);
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 2rpx;
}

.member-invite-icon {
	font-size: 36rpx;
	line-height: 1;
	color: rgba(60, 60, 67, 0.55);
}

.member-invite-text {
	font-size: 20rpx;
	color: rgba(60, 60, 67, 0.45);
}

.members-empty {
	display: flex;
	align-items: center;
	padding: 0 28rpx;
}

.members-empty-text {
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.45);
}

.action-group {
	margin-top: 24rpx;
	display: flex;
	flex-direction: column;
	gap: 20rpx;
}

.primary-btn,
.danger-btn,
.secondary-btn {
	height: 92rpx;
	border-radius: 18rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 30rpx;
	font-weight: 500;
}

.primary-btn {
	background: #111111;
	color: #ffffff;
}

.primary-btn::after {
	border: 0;
}

.primary-btn-hover {
	opacity: 0.82;
}

.danger-btn {
	background: rgba(217, 45, 32, 0.08);
	color: #d92d20;
}

.danger-btn::after {
	border: 0;
}

.danger-btn-hover {
	background: rgba(217, 45, 32, 0.14);
}

.secondary-btn {
	background: #ffffff;
	color: #1c1c1e;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
}

.secondary-btn::after {
	border: 0;
}

.secondary-btn-hover {
	background: #f2f2f7;
}

.page-hint {
	display: block;
	margin-top: 20rpx;
	font-size: 22rpx;
	line-height: 1.5;
	color: rgba(60, 60, 67, 0.45);
	text-align: center;
}

/* 弹层 */
.sheet-mask {
	position: fixed;
	inset: 0;
	z-index: 100;
	background: rgba(0, 0, 0, 0.32);
	display: flex;
	align-items: flex-end;
	justify-content: center;
}

.sheet {
	width: 100%;
	max-width: 750rpx;
	background: #ffffff;
	border-radius: 28rpx 28rpx 0 0;
	padding: 28rpx 28rpx calc(28rpx + env(safe-area-inset-bottom));
	box-sizing: border-box;
}

.sheet-tall {
	max-height: 78vh;
	display: flex;
	flex-direction: column;
}

.sheet-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding-bottom: 20rpx;
}

.sheet-title {
	font-size: 32rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.sheet-close {
	font-size: 40rpx;
	line-height: 1;
	color: rgba(60, 60, 67, 0.45);
	padding: 4rpx 12rpx;
}

.sheet-textarea {
	width: 100%;
	height: 260rpx;
	padding: 20rpx;
	box-sizing: border-box;
	border-radius: 16rpx;
	background: #f2f2f7;
	font-size: 26rpx;
	line-height: 1.6;
}

.sheet-counter {
	display: block;
	margin-top: 8rpx;
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.45);
	text-align: right;
}

.sheet-search {
	height: 76rpx;
	padding: 0 20rpx;
	border-radius: 14rpx;
	background: #f2f2f7;
	font-size: 26rpx;
}

.vote-option-row {
	display: flex;
	align-items: center;
	gap: 12rpx;
	margin-top: 14rpx;
}

.vote-option-input {
	flex: 1;
	height: 72rpx;
	padding: 0 18rpx;
	border-radius: 14rpx;
	background: #f2f2f7;
	font-size: 26rpx;
}

.vote-option-remove {
	width: 48rpx;
	text-align: center;
	font-size: 34rpx;
	color: rgba(60, 60, 67, 0.45);
}

.vote-tools {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-top: 20rpx;
	padding: 8rpx 4rpx;
}

.vote-add {
	font-size: 26rpx;
	color: #0a84ff;
}

.vote-multi {
	display: flex;
	align-items: center;
	gap: 8rpx;
}

.vote-multi-label {
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.6);
}

.sheet-list {
	flex: 1;
	max-height: 460rpx;
	margin-top: 16rpx;
}

.friend-row,
.file-row {
	display: flex;
	align-items: center;
	gap: 20rpx;
	padding: 20rpx 8rpx;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.08);
}

.friend-name,
.file-name {
	flex: 1;
	font-size: 26rpx;
	color: #1c1c1e;
	word-break: break-all;
}

.file-size {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.45);
}

.friend-check {
	width: 44rpx;
	height: 44rpx;
	border-radius: 50%;
	border: 2rpx solid rgba(60, 60, 67, 0.2);
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 24rpx;
	color: transparent;
}

.friend-check.checked {
	background: #0a84ff;
	border-color: #0a84ff;
	color: #ffffff;
}

.sheet-empty {
	display: block;
	padding: 40rpx 0;
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.45);
	text-align: center;
}

.ai-summary {
	display: block;
	padding: 16rpx 8rpx;
	font-size: 26rpx;
	line-height: 1.7;
	color: #1c1c1e;
	white-space: pre-wrap;
}

.ai-todo {
	display: flex;
	align-items: flex-start;
	gap: 14rpx;
	padding: 14rpx 8rpx;
}

.ai-todo-index {
	flex-shrink: 0;
	width: 34rpx;
	height: 34rpx;
	line-height: 34rpx;
	border-radius: 50%;
	background: rgba(10, 132, 255, 0.1);
	color: #0a84ff;
	font-size: 20rpx;
	font-weight: 600;
	text-align: center;
	margin-top: 2rpx;
}

.ai-todo-main {
	display: flex;
	flex: 1;
	flex-direction: column;
	min-width: 0;
	gap: 6rpx;
}

.ai-todo-text {
	font-size: 24rpx;
	line-height: 1.6;
	color: rgba(60, 60, 67, 0.7);
}

.ai-todo-meta {
	display: flex;
	flex-wrap: wrap;
	gap: 18rpx;
	font-size: 21rpx;
	color: rgba(60, 60, 67, 0.45);
}

.ai-empty-todo {
	padding: 18rpx 8rpx;
	font-size: 23rpx;
	color: rgba(60, 60, 67, 0.45);
}

.ai-meta {
	display: block;
	padding: 22rpx 8rpx 8rpx;
	font-size: 21rpx;
	color: rgba(60, 60, 67, 0.4);
}

.sheet-actions {
	display: flex;
	gap: 20rpx;
	margin-top: 20rpx;
}

.sheet-btn {
	flex: 1;
	height: 84rpx;
	border-radius: 16rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 28rpx;
}

.sheet-btn.ghost {
	background: #f2f2f7;
	color: #1c1c1e;
}

.sheet-btn.primary {
	background: #0a84ff;
	color: #ffffff;
}

.sheet-btn.disabled {
	opacity: 0.5;
}

/* ===== 群成员搜索、筛选与批量管理 ===== */
.members-header-actions {
	display: flex;
	align-items: center;
	gap: 16rpx;
}

.members-batch-toggle {
	font-size: 24rpx;
	color: #0a84ff;
	padding: 6rpx 4rpx;
}

.member-tools {
	padding: 0 28rpx 18rpx;
}

.member-search-wrap,
.file-search-wrap {
	display: flex;
	align-items: center;
	gap: 10rpx;
	height: 72rpx;
	padding: 0 20rpx;
	border-radius: 14rpx;
	background: #f2f2f7;
}

.member-search-icon,
.file-search-icon {
	font-size: 32rpx;
	line-height: 1;
	color: rgba(60, 60, 67, 0.45);
}

.member-search,
.file-search {
	flex: 1;
	height: 72rpx;
	font-size: 26rpx;
	color: #1c1c1e;
}

.member-search-clear,
.file-search-clear {
	font-size: 32rpx;
	line-height: 1;
	color: rgba(60, 60, 67, 0.35);
	padding: 6rpx 4rpx;
}

.member-filter-scroll,
.file-type-scroll {
	width: 100%;
	margin-top: 14rpx;
	white-space: nowrap;
}

.member-filter-row,
.file-type-row {
	display: inline-flex;
	align-items: center;
	gap: 12rpx;
	padding: 2rpx 0;
}

.member-filter-chip,
.file-type-chip {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	height: 54rpx;
	padding: 0 22rpx;
	border-radius: 27rpx;
	background: #f2f2f7;
	color: rgba(60, 60, 67, 0.7);
	font-size: 22rpx;
	flex-shrink: 0;
}

.member-filter-chip.active,
.file-type-chip.active {
	background: #1c1c1e;
	color: #ffffff;
}

.member-batch-info {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin: 0 28rpx 18rpx;
	padding: 16rpx 20rpx;
	border-radius: 14rpx;
	background: #f2f2f7;
}

.member-batch-count {
	font-size: 24rpx;
	color: #1c1c1e;
}

.member-batch-tools {
	display: flex;
	align-items: center;
	gap: 28rpx;
}

.member-batch-link {
	font-size: 24rpx;
	color: #0a84ff;
}

.member-avatar-wrap {
	position: relative;
}

.member-check {
	position: absolute;
	right: -6rpx;
	top: -6rpx;
	width: 34rpx;
	height: 34rpx;
	border-radius: 50%;
	border: 2rpx solid #ffffff;
	background: #d1d1d6;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.12);
}

.member-check.checked {
	background: #0a84ff;
}

.member-check.disabled {
	opacity: 0.45;
}

.member-check-icon {
	font-size: 20rpx;
	line-height: 1;
	color: #ffffff;
}

.member-item.selected .member-name {
	color: #0a84ff;
}

.member-batch-actions {
	display: flex;
	gap: 12rpx;
	padding: 6rpx 28rpx 4rpx;
}

.member-batch-btn {
	flex: 1;
	height: 72rpx;
	border-radius: 14rpx;
	background: #f2f2f7;
	color: #1c1c1e;
	font-size: 24rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}

.member-batch-btn.danger {
	background: #fff1f0;
	color: #d92d20;
}

.member-batch-btn.disabled {
	opacity: 0.45;
}

/* ===== 群文件搜索、筛选与上传 ===== */
.sheet-head-actions {
	display: flex;
	align-items: center;
	gap: 18rpx;
}

.file-total {
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.45);
}

.file-tools {
	padding-bottom: 12rpx;
}

.file-upload-card {
	margin-top: 14rpx;
	padding: 18rpx 20rpx;
	border-radius: 16rpx;
	background: #f2f2f7;
}

.file-upload-line {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 20rpx;
}

.file-upload-name {
	flex: 1;
	font-size: 24rpx;
	color: #1c1c1e;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.file-upload-phase {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.55);
}

.file-upload-track {
	height: 8rpx;
	margin-top: 14rpx;
	border-radius: 4rpx;
	background: rgba(60, 60, 67, 0.12);
	overflow: hidden;
}

.file-upload-fill {
	height: 100%;
	border-radius: 4rpx;
	background: #0a84ff;
	transition: width 0.2s ease;
}

.file-upload-actions {
	display: flex;
	justify-content: flex-end;
	margin-top: 10rpx;
}

.file-upload-cancel {
	font-size: 22rpx;
	color: #d92d20;
}

.file-upload-entry {
	display: flex;
	align-items: center;
	gap: 14rpx;
	height: 76rpx;
	margin-top: 14rpx;
	padding: 0 20rpx;
	border-radius: 16rpx;
	background: #f2f2f7;
}

.file-upload-plus {
	font-size: 34rpx;
	line-height: 1;
	color: #0a84ff;
}

.file-upload-label {
	font-size: 26rpx;
	color: #1c1c1e;
}

.file-upload-hint {
	margin-left: auto;
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.45);
}

.file-input {
	display: none;
}

.file-list {
	display: flex;
	flex-direction: column;
}

.file-icon {
	width: 68rpx;
	height: 68rpx;
	border-radius: 14rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 20rpx;
	font-weight: 600;
	color: #ffffff;
	flex-shrink: 0;
	background: #8e8e93;
}

.file-icon.type-image { background: #34c759; }
.file-icon.type-pdf { background: #ff3b30; }
.file-icon.type-doc { background: #0a84ff; }
.file-icon.type-sheet { background: #ff9500; }
.file-icon.type-zip { background: #af52de; }
.file-icon.type-video { background: #5856d6; }
.file-icon.type-audio { background: #ff2d55; }
.file-icon.type-other { background: #8e8e93; }

.file-main {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 6rpx;
}

.file-meta {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.45);
}

.file-sub-meta {
	font-size: 21rpx;
	line-height: 1.4;
	color: rgba(60, 60, 67, 0.38);
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.file-arrow {
	font-size: 30rpx;
	color: rgba(60, 60, 67, 0.35);
}

.file-list-end {
	padding: 24rpx 0 10rpx;
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.4);
	text-align: center;
}

.file-size {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.45);
	flex-shrink: 0;
}

/* ===== 公告防丢与 AI 错误 ===== */
.sheet-head-title-wrap {
	display: flex;
	align-items: center;
	gap: 14rpx;
}

.sheet-dirty {
	font-size: 20rpx;
	color: #ff9500;
	background: rgba(255, 149, 0, 0.12);
	padding: 4rpx 12rpx;
	border-radius: 10rpx;
}

.ai-error {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 44rpx 20rpx;
	gap: 12rpx;
}

.ai-error-title {
	font-size: 28rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.ai-error-text {
	font-size: 24rpx;
	line-height: 1.6;
	color: rgba(60, 60, 67, 0.6);
	text-align: center;
}

.ai-retry {
	margin-top: 16rpx;
	padding: 14rpx 44rpx;
	border-radius: 14rpx;
	background: #1c1c1e;
	color: #ffffff;
	font-size: 24rpx;
}

.manage-value.muted {
	color: rgba(60, 60, 67, 0.45);
}

/* ===== 群资料编辑、公告删除与文件删除 ===== */
.announcement-delete {
	padding: 8rpx 14rpx;
	font-size: 24rpx;
	color: #d92d20;
}

.announcement-delete:active {
	opacity: 0.6;
}

.profile-form {
	max-height: 52vh;
}

.profile-avatar-row {
	display: flex;
	align-items: center;
	gap: 22rpx;
	padding: 12rpx 8rpx 26rpx;
	border-bottom: 1rpx solid rgba(60, 60, 67, 0.08);
}

.profile-avatar-copy {
	display: flex;
	flex-direction: column;
	gap: 6rpx;
}

.profile-avatar-title {
	font-size: 26rpx;
	color: #0a84ff;
}

.profile-avatar-hint {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.45);
}

.profile-field {
	margin-top: 24rpx;
}

.profile-field-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 10rpx;
}

.profile-field-label {
	font-size: 24rpx;
	color: rgba(60, 60, 67, 0.6);
}

.profile-field-counter {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.4);
}

.profile-input {
	width: 100%;
	height: 80rpx;
	padding: 0 20rpx;
	box-sizing: border-box;
	border-radius: 14rpx;
	background: #f2f2f7;
	font-size: 26rpx;
}

.profile-textarea {
	width: 100%;
	height: 150rpx;
	padding: 18rpx 20rpx;
	box-sizing: border-box;
	border-radius: 14rpx;
	background: #f2f2f7;
	font-size: 25rpx;
	line-height: 1.55;
}

.file-more {
	padding: 8rpx 10rpx;
	font-size: 30rpx;
	line-height: 1;
	letter-spacing: 2rpx;
	color: rgba(60, 60, 67, 0.45);
	flex-shrink: 0;
}

.file-more:active {
	color: #d92d20;
}

/* 首屏加载 / 失败 / 后台降级 */
.detail-skeleton {
	display: flex;
	flex-direction: column;
	gap: 24rpx;
}

.detail-skeleton-hero,
.detail-skeleton-card {
	background: #ffffff;
	border: 1rpx solid rgba(60, 60, 67, 0.12);
	border-radius: 20rpx;
	padding: 32rpx;
}

.detail-skeleton-hero {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 26rpx;
}

.skeleton-avatar {
	width: 160rpx;
	height: 160rpx;
	border-radius: 18rpx;
	background: #f2f2f7;
	animation: skeleton-pulse 1.4s ease-in-out infinite;
}

.skeleton-lines {
	width: 100%;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 14rpx;
}

.skeleton-line {
	height: 20rpx;
	border-radius: 10rpx;
	background: #f2f2f7;
	animation: skeleton-pulse 1.4s ease-in-out infinite;
}

.skeleton-line.w30 { width: 30%; align-self: flex-start; }
.skeleton-line.w45 { width: 45%; }
.skeleton-line.w55 { width: 55%; }
.skeleton-line.w70 { width: 70%; }

.skeleton-row {
	height: 44rpx;
	margin-top: 22rpx;
	border-radius: 10rpx;
	background: #f2f2f7;
	animation: skeleton-pulse 1.4s ease-in-out infinite;
}

.detail-skeleton-card.short .skeleton-line {
	height: 34rpx;
}

@keyframes skeleton-pulse {
	0%, 100% { opacity: 1; }
	50% { opacity: 0.55; }
}

.detail-error-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 120rpx 48rpx 80rpx;
	text-align: center;
}

.detail-error-icon {
	width: 88rpx;
	height: 88rpx;
	line-height: 88rpx;
	border-radius: 50%;
	background: rgba(217, 45, 32, 0.08);
	color: #d92d20;
	font-size: 46rpx;
	font-weight: 600;
}

.detail-error-title {
	margin-top: 26rpx;
	font-size: 32rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.detail-error-text {
	margin-top: 12rpx;
	font-size: 24rpx;
	line-height: 1.6;
	color: rgba(60, 60, 67, 0.55);
}

.detail-error-retry {
	margin-top: 32rpx;
	padding: 18rpx 48rpx;
	border-radius: 14rpx;
	background: #1c1c1e;
	color: #ffffff;
	font-size: 26rpx;
}

.detail-warning {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
	margin-bottom: 20rpx;
	padding: 18rpx 22rpx;
	border-radius: 14rpx;
	background: rgba(255, 149, 0, 0.1);
	border: 1rpx solid rgba(255, 149, 0, 0.22);
}

.detail-warning-text {
	flex: 1;
	font-size: 23rpx;
	line-height: 1.5;
	color: #8a5300;
}

.detail-warning-retry {
	flex-shrink: 0;
	font-size: 23rpx;
	font-weight: 600;
	color: #0a84ff;
}

.hero-name-row {
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 12rpx;
	margin-top: 24rpx;
	max-width: 100%;
}

.hero-name-row .hero-name {
	margin-top: 0;
	min-width: 0;
}

.hero-role {
	flex-shrink: 0;
	padding: 5rpx 12rpx;
	border-radius: 8rpx;
	background: rgba(60, 60, 67, 0.08);
	color: rgba(60, 60, 67, 0.7);
	font-size: 21rpx;
	line-height: 1.4;
}

.hero-role.owner {
	background: rgba(255, 149, 0, 0.12);
	color: #9a5b00;
}

.hero-role.admin {
	background: rgba(10, 132, 255, 0.1);
	color: #0a5fbf;
}

.info-label-wrap {
	display: flex;
	flex-direction: column;
	gap: 4rpx;
	min-width: 0;
}

.info-sub {
	font-size: 20rpx;
	line-height: 1.4;
	color: rgba(60, 60, 67, 0.42);
}

.member-filter-count {
	margin-left: 6rpx;
	font-size: 20rpx;
	opacity: 0.62;
}

.permission-note {
	display: flex;
	align-items: flex-start;
	gap: 12rpx;
	margin-top: 24rpx;
	padding: 20rpx 22rpx;
	border-radius: 14rpx;
	background: rgba(10, 132, 255, 0.06);
}

.permission-note-icon {
	flex-shrink: 0;
	width: 28rpx;
	height: 28rpx;
	line-height: 28rpx;
	border-radius: 50%;
	background: rgba(10, 132, 255, 0.16);
	color: #0a84ff;
	font-size: 20rpx;
	text-align: center;
}

.permission-note-text {
	flex: 1;
	font-size: 21rpx;
	line-height: 1.55;
	color: rgba(10, 95, 191, 0.78);
}

.ai-regen {
	padding: 6rpx 12rpx;
	font-size: 24rpx;
	color: #0a84ff;
}

.ai-regen.disabled {
	color: rgba(60, 60, 67, 0.35);
}

.file-error-card {
	display: flex;
	flex-direction: column;
	align-items: flex-start;
	gap: 10rpx;
	margin: 18rpx 0;
	padding: 28rpx;
	border-radius: 16rpx;
	background: rgba(217, 45, 32, 0.06);
	border: 1rpx solid rgba(217, 45, 32, 0.14);
}

.file-error-title {
	font-size: 26rpx;
	font-weight: 600;
	color: #1c1c1e;
}

.file-error-text {
	font-size: 22rpx;
	color: rgba(60, 60, 67, 0.55);
}

.file-error-retry {
	margin-top: 6rpx;
	font-size: 24rpx;
	font-weight: 600;
	color: #0a84ff;
}

.qr-mask {
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 0 40rpx;
}

.qr-dialog {
	width: 100%;
	max-width: 560rpx;
	padding: 40rpx 32rpx 28rpx;
	box-sizing: border-box;
	background: #ffffff;
	border-radius: 28rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.qr-title {
	font-size: 34rpx;
	font-weight: 600;
	color: #111111;
}

.qr-subtitle {
	margin-top: 10rpx;
	font-size: 24rpx;
	line-height: 1.5;
	color: #888888;
	text-align: center;
}

.qr-frame {
	width: 420rpx;
	height: 420rpx;
	margin-top: 28rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background: #f7f7f8;
	border-radius: 20rpx;
}

.qr-image {
	width: 380rpx;
	height: 380rpx;
}

.qr-loading {
	font-size: 26rpx;
	color: #999999;
}

.qr-group {
	margin-top: 22rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 6rpx;
}

.qr-group-name {
	font-size: 30rpx;
	font-weight: 600;
	color: #111111;
}

.qr-group-no {
	font-size: 23rpx;
	color: #999999;
}

.qr-actions {
	width: 100%;
	margin-top: 30rpx;
	display: flex;
	gap: 20rpx;
}

.qr-action {
	flex: 1;
	height: 78rpx;
	line-height: 78rpx;
	text-align: center;
	border-radius: 14rpx;
	font-size: 27rpx;
	font-weight: 600;
}

.qr-action.ghost {
	color: #555555;
	background: #f2f2f4;
}

.qr-action.primary {
	color: #ffffff;
	background: #111111;
}
</style>
