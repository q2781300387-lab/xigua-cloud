<template>
  <div ref="gmRoot" class="group-manage" :class="{ 'is-sheet-open': !!sheet }" role="dialog" aria-modal="true" aria-labelledby="gm-title" tabindex="-1">
    <!-- 顶部导航：与聊天页的 52px 导航栏同高，返回后回到原会话 -->
    <header class="gm-nav">
      <button class="gm-back" type="button" aria-label="返回聊天" @click="requestClose">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 4L7 12l8 8"/>
        </svg>
      </button>
      <div class="gm-nav-title" id="gm-title">群管理</div>
      <div class="gm-nav-side"></div>
    </header>

    <!-- 加载骨架 -->
    <div v-if="loading" class="gm-scroll" aria-busy="true">
      <div class="gm-skeleton gm-skeleton-hero">
        <div class="gm-skeleton-avatar"></div>
        <div class="gm-skeleton-lines">
          <div class="gm-skeleton-line w45"></div>
          <div class="gm-skeleton-line w65"></div>
        </div>
      </div>
      <div class="gm-skeleton gm-skeleton-card">
        <div class="gm-skeleton-line w30"></div>
        <div class="gm-skeleton-grid">
          <div v-for="i in 6" :key="i" class="gm-skeleton-cell"></div>
        </div>
      </div>
      <div class="gm-skeleton gm-skeleton-card short">
        <div class="gm-skeleton-line w55"></div>
      </div>
    </div>

    <!-- 失败态：给出重试，不伪装成空群 -->
    <div v-else-if="error" class="gm-state">
      <div class="gm-state-icon">
        <svg viewBox="0 0 48 48" width="44" height="44" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <circle cx="24" cy="24" r="18" opacity=".35"/>
          <path d="M24 13v12"/>
          <circle cx="24" cy="33" r="1.4" fill="currentColor" stroke="none"/>
        </svg>
      </div>
      <div class="gm-state-title">群资料加载失败</div>
      <div class="gm-state-desc">{{ error }}</div>
      <button class="gm-retry" type="button" @click="load">重新加载</button>
    </div>

    <div v-else class="gm-scroll" ref="scroll" :class="{ 'sheet-open': !!sheet }">
      <!-- 后台刷新失败：保留上次可用数据，不让整页变错误页 -->
      <div v-if="detailWarning" class="gm-detail-warning" role="alert">
        <span>群资料同步失败：{{ detailWarning }}</span>
        <button type="button" class="gm-inline-retry" @click="load">重试</button>
      </div>

      <!-- 群身份卡 -->
      <section class="gm-hero">
        <div class="gm-hero-avatar" :style="avatarStyle(group.groupId)">
          <img v-if="group.groupAvatar" :src="group.groupAvatar" :alt="group.groupName" @error="group.groupAvatar = ''">
          <span v-else class="gm-hero-avatar-text">{{ initial(group.groupName) }}</span>
        </div>
        <div class="gm-hero-main">
          <div class="gm-hero-name-row">
            <h1 class="gm-hero-name">{{ group.groupName || '未命名群聊' }}</h1>
            <span v-if="myRoleText" class="gm-role-pill" :class="rolePillClass">{{ myRoleText }}</span>
            <button v-if="canManage" type="button" class="gm-hero-edit" aria-label="编辑群资料" @click="openProfile">
              编辑
            </button>
            <button type="button" class="gm-hero-edit gm-hero-qr" aria-label="查看群二维码" @click="openGroupQr">
              群二维码
            </button>
          </div>
          <p class="gm-hero-meta">{{ memberCount }} 位成员 · 群主 {{ group.groupOwner || '未知' }}<span v-if="Number(group.muteAll) === 1" class="gm-mute-inline">全员禁言中</span></p>
          <p class="gm-hero-desc">{{ group.description || '暂无群介绍' }}</p>
        </div>
      </section>

      <nav class="gm-mobile-nav" aria-label="群管理快捷导航">
        <button
          v-for="item in mobileNavItems"
          :key="item.id"
          type="button"
          class="gm-mobile-nav-chip"
          :class="{ active: activeMobileSection === item.id }"
          :aria-current="activeMobileSection === item.id ? 'true' : undefined"
          @click="jumpTo(item.id)"
        >{{ item.label }}</button>
      </nav>

      <div class="gm-layout">
      <div class="gm-main">
      <!-- 群公告：QQ 式置顶公告卡，有内容直接展示，没内容给发布入口 -->
      <section id="gm-announce" class="gm-card gm-announce">
        <div class="gm-card-head">
          <h2 class="gm-card-title">
            群公告
            <span v-if="hasAnnouncement" class="gm-announce-dot" aria-hidden="true"></span>
          </h2>
          <button v-if="canManage" class="gm-text-btn" type="button" @click="openAnnouncement">
            {{ hasAnnouncement ? '编辑' : '发布' }}
          </button>
        </div>
        <div v-if="hasAnnouncement" class="gm-announce-body">{{ group.announcement }}</div>
        <div v-else class="gm-announce-empty" :class="{ 'is-readonly': !canManage }">
          {{ canManage ? '还没有群公告，发布后所有成员都能看到。' : '群主还没有发布公告。' }}
        </div>
        <div v-if="hasAnnouncement" class="gm-announce-meta">
          {{ group.announcementEditor || '群主' }} · {{ formatDate(group.announcementTime) }}
        </div>
      </section>

      <!-- 群 AI 摘要与待办：服务端拉最近 50 条消息，不依赖本地已加载的部分 -->
      <section class="gm-card gm-ai-card">
        <div class="gm-card-head">
          <h2 class="gm-card-title">
            AI 摘要与待办
            <span class="gm-ai-spark" aria-hidden="true">✦</span>
          </h2>
          <button v-if="canManage" class="gm-text-btn" type="button" :disabled="aiLoading || aiCooldownLeft > 0" @click="loadAiSummary(true)">
            {{ aiLoading ? '生成中…' : (aiCooldownLeft > 0 ? `冷却 ${aiCooldownLeft}s` : (aiSummary ? '重新生成' : '生成摘要')) }}
          </button>
          <span v-else class="gm-ai-permission">仅群主/管理员可生成</span>
        </div>

        <div v-if="aiLoading" class="gm-ai-loading" aria-live="polite">
          <span class="gm-spinner" aria-hidden="true"></span>
          <span>正在读取最近 50 条群消息并提炼…</span>
        </div>

        <div v-else-if="aiError" class="gm-ai-error" role="alert">
          <p>{{ aiError }}</p>
          <button v-if="canManage" type="button" class="gm-mini-btn" @click="loadAiSummary(true)">重试</button>
        </div>

        <template v-else-if="aiSummary">
          <p class="gm-ai-summary">{{ aiSummary.summary }}</p>
          <div v-if="aiTodos.length" class="gm-ai-todos">
            <div class="gm-ai-todos-title">待办事项 <span>{{ aiTodos.length }}</span></div>
            <div v-for="(todo, index) in aiTodos" :key="index" class="gm-ai-todo">
              <span class="gm-ai-todo-index">{{ index + 1 }}</span>
              <span class="gm-ai-todo-main">{{ todo.content }}</span>
              <span class="gm-ai-todo-meta">
                <span v-if="todo.owner">{{ todo.owner }}</span>
                <span v-if="todo.due" class="gm-ai-due">{{ todo.due }}</span>
              </span>
            </div>
          </div>
          <p v-else class="gm-ai-empty-todo">最近消息里没有明确的待办事项</p>
          <p class="gm-ai-meta">基于最近 {{ aiSummary.messageCount || 0 }} 条消息 · {{ formatDate(aiSummary.generatedAt) }}</p>
        </template>

        <div v-else class="gm-ai-idle">
          <p>把最近 50 条群消息压成一段摘要，并提取明确的待办、负责人和时间。生成结果会显示在这里。</p>
        </div>
      </section>

      <!-- 群文件中心：搜索、类型筛选、拖拽上传、进度取消，全部走后端真实接口 -->
      <section
        id="gm-files"
        class="gm-card gm-file-card"
        :class="{ 'is-file-dragging': isFileDrag }"
        @dragover.prevent="onFileDragOver"
        @dragleave="onFileDragLeave"
        @drop.prevent="onFileDrop"
      >
        <div class="gm-card-head">
          <h2 class="gm-card-title">群文件 <span class="gm-card-count">{{ fileTotal }}</span></h2>
          <button class="gm-text-btn" type="button" :disabled="fileUploading" @click="pickFile">
            {{ fileUploading ? '上传中…' : '上传文件' }}
          </button>
        </div>
        <input ref="fileInput" type="file" class="gm-file-input" @change="handleFileInput" />

        <div class="gm-file-toolbar">
          <label class="gm-file-search">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" aria-hidden="true">
              <circle cx="11" cy="11" r="6.4"/><path d="M16 16l4 4"/>
            </svg>
            <input v-model="fileKeyword" type="search" placeholder="搜索文件名" aria-label="搜索群文件">
            <button v-if="fileKeyword" type="button" class="gm-file-search-clear" aria-label="清除文件搜索" @click="fileKeyword = ''">×</button>
          </label>
          <select v-model="fileTypeFilter" class="gm-file-type" aria-label="按文件类型筛选">
            <option v-for="opt in fileTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>

        <div v-if="isFileDrag" class="gm-file-drop-hint" aria-live="polite">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" aria-hidden="true">
            <path d="M12 16V5"/><path d="M7.5 9.5L12 5l4.5 4.5"/><path d="M4 19h16"/>
          </svg>
          <span>松开鼠标，把文件上传到群文件</span>
        </div>

        <div v-if="fileUploading" class="gm-file-uploading" aria-live="polite">
          <span class="gm-spinner" aria-hidden="true"></span>
          <div class="gm-file-upload-main">
            <div class="gm-file-upload-line">
              <span>正在上传 {{ uploadingFileName }}…</span>
              <strong>{{ fileUploadProgress }}%</strong>
            </div>
            <div class="gm-file-progress" role="progressbar" :aria-valuenow="fileUploadProgress" aria-valuemin="0" aria-valuemax="100">
              <i :style="{ width: fileUploadProgress + '%' }"></i>
            </div>
          </div>
          <button
            type="button"
            class="gm-file-cancel"
            :disabled="fileUploadPhase !== 'uploading'"
            @click="cancelFileUpload"
          >{{ fileUploadPhase === 'uploading' ? '取消' : '保存中' }}</button>
        </div>

        <div v-if="filesLoading && files.length === 0" class="gm-file-loading">
          <span class="gm-spinner" aria-hidden="true"></span> 加载群文件…
        </div>

        <div v-else-if="filesError" class="gm-ai-error" role="alert">
          <p>{{ filesError }}</p>
          <button type="button" class="gm-mini-btn" @click="loadFiles">重试</button>
        </div>

        <div v-else-if="visibleFiles.length === 0" class="gm-file-empty">
          <div class="gm-file-empty-art" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3.5 7.5a1.5 1.5 0 0 1 1.5-1.5h5l2 2h7a1.5 1.5 0 0 1 1.5 1.5v9A1.5 1.5 0 0 1 19 20H5A1.5 1.5 0 0 1 3.5 18.5z"/>
            </svg>
          </div>
          <p>{{ fileFilterActive ? '没有匹配的群文件，换个关键词或类型试试。' : '还没有群文件，上传后群成员都能下载。' }}</p>
        </div>

        <div v-else class="gm-file-list">
          <div v-for="file in visibleFiles" :key="file.id" class="gm-file-row">
            <div class="gm-file-icon" :class="'ext-' + fileExtClass(file)">{{ fileExtLabel(file) }}</div>
            <div class="gm-file-main">
              <button type="button" class="gm-file-name" :title="file.fileName" @click="downloadFile(file)">{{ file.fileName }}</button>
              <div class="gm-file-meta">
                <span>{{ file.uploaderName || '群成员' }}</span>
                <span>{{ formatSize(file.fileSize) }}</span>
                <span>{{ formatDate(file.createTime) }}</span>
              </div>
            </div>
            <button type="button" class="gm-file-action" title="下载" aria-label="下载文件" @click="downloadFile(file)">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <path d="M12 4v11"/><path d="M7.5 11.5L12 16l4.5-4.5"/><path d="M5 20h14"/>
              </svg>
            </button>
            <button
              v-if="canDeleteFile(file)"
              type="button"
              class="gm-file-action danger"
              title="删除文件"
              aria-label="删除文件"
              @click="openDeleteFile(file)"
            >
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <path d="M4 7h16"/><path d="M9 7V4h6v3"/><path d="M6 7l1 13h10l1-13"/>
              </svg>
            </button>
          </div>
          <button v-if="fileHasMore" type="button" class="gm-file-more" :disabled="filesLoading" @click="loadMoreFiles">
            {{ filesLoading ? '加载中…' : '加载更多' }}
          </button>
          <p v-else class="gm-file-end">共 {{ fileTotal }} 个文件</p>
        </div>
      </section>

      <!-- 入群申请：群主 / 管理员审批 -->
      <section v-if="canManage && joinRequests.length" class="gm-card gm-join-card">
        <div class="gm-card-head">
          <h2 class="gm-card-title">入群申请 <span class="gm-card-count">{{ joinRequests.length }}</span></h2>
          <button class="gm-text-btn" type="button" @click="loadJoinRequests">刷新</button>
        </div>
        <div class="gm-join-list">
          <div v-for="req in joinRequests" :key="req.id" class="gm-join-row">
            <div class="gm-join-avatar">{{ (req.username || '用')[0] }}</div>
            <div class="gm-join-info">
              <div class="gm-join-name">{{ req.username || req.userId }}</div>
              <div class="gm-join-meta">{{ req.reason || '申请加入群聊' }} · {{ formatDate(req.createTime) }}</div>
            </div>
            <div class="gm-join-actions">
              <button class="gm-mini-btn" type="button" :disabled="joinPendingId === req.id" @click="handleJoin(req, true)">同意</button>
              <button class="gm-mini-btn ghost" type="button" :disabled="joinPendingId === req.id" @click="handleJoin(req, false)">拒绝</button>
            </div>
          </div>
        </div>
      </section>

      <!-- 群成员：搜索、角色筛选、批量设管理员/移出 -->
      <section id="gm-members" class="gm-card gm-member-card">
        <div class="gm-card-head">
          <h2 class="gm-card-title">群成员 <span class="gm-card-count">{{ memberCount }}</span></h2>
          <div class="gm-card-actions">
            <button v-if="canManage" class="gm-text-btn" type="button" @click="toggleMemberSelectMode">
              {{ memberSelectMode ? '完成' : '批量管理' }}
            </button>
            <button v-if="canManage && !memberSelectMode" class="gm-text-btn" type="button" @click="openInvite">
              邀请成员
            </button>
          </div>
        </div>

        <div class="gm-member-toolbar">
          <label class="gm-member-search">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" aria-hidden="true">
              <circle cx="11" cy="11" r="6.4"/><path d="M16 16l4 4"/>
            </svg>
            <input v-model="memberKeyword" type="search" placeholder="搜索成员昵称" aria-label="搜索群成员">
            <button v-if="memberKeyword" type="button" class="gm-file-search-clear" aria-label="清除成员搜索" @click="memberKeyword = ''">×</button>
          </label>
          <div class="gm-member-filters" role="group" aria-label="按角色筛选成员">
            <button
              v-for="opt in memberRoleOptions"
              :key="opt.value"
              type="button"
              class="gm-filter-chip"
              :class="{ active: memberRoleFilter === opt.value }"
              :aria-pressed="memberRoleFilter === opt.value"
              @click="memberRoleFilter = opt.value"
            >{{ opt.label }}<span>{{ opt.count }}</span></button>
          </div>
        </div>

        <div v-if="memberSelectMode" class="gm-member-batchbar" aria-live="polite">
          <div class="gm-member-batch-info">
            已选 <strong>{{ selectedMemberIds.length }}</strong> 人
            <button type="button" class="gm-batch-link" @click="selectAllFiltered">全选当前</button>
            <button type="button" class="gm-batch-link" :disabled="!selectedMemberIds.length" @click="clearMemberSelection">清空</button>
          </div>
          <div class="gm-member-batch-actions">
            <button v-if="isOwner" type="button" class="gm-batch-btn" :disabled="!batchAdminTargets.length" @click="openBatchRole(2)">设为管理员</button>
            <button v-if="isOwner" type="button" class="gm-batch-btn" :disabled="!batchRemoveAdminTargets.length" @click="openBatchRole(3)">取消管理员</button>
            <button type="button" class="gm-batch-btn danger" :disabled="!batchKickTargets.length" @click="openBatchKick">移出群聊</button>
          </div>
        </div>

        <div v-if="filteredMembers.length" class="gm-member-grid">
          <button
              v-for="m in filteredMembers"
              :key="m.userId"
              type="button"
              class="gm-member"
              :class="{ selected: isMemberSelected(m.userId) }"
              :aria-pressed="memberSelectMode ? isMemberSelected(m.userId) : undefined"
              :aria-label="memberSelectMode ? (isMemberSelected(m.userId) ? '取消选择 ' : '选择 ') + (m.username || '成员') : '查看 ' + (m.username || '成员') + ' 的资料'"
              @click="handleMemberClick(m)"
          >
            <span v-if="memberSelectMode" class="gm-member-select" :class="{ checked: isMemberSelected(m.userId) }" aria-hidden="true">
              <svg v-if="isMemberSelected(m.userId)" viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                <path d="M5 12.5l4.5 4.5L19 7"/>
              </svg>
            </span>
            <div class="gm-member-avatar" :style="avatarStyle(m.userId)">
              <img v-if="m.avatar" :src="m.avatar" :alt="m.username" @error="m.avatar = ''">
              <span v-else class="gm-member-avatar-text">{{ initial(m.username) }}</span>
            </div>
            <span class="gm-member-name">{{ m.username || '成员' }}</span>
            <span class="gm-member-role">{{ roleText(m.role) }}</span>
          </button>
          <button v-if="canManage && !memberSelectMode && !memberFilterActive" type="button" class="gm-member gm-member-add" @click="openInvite">
            <span class="gm-add-circle">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <path d="M12 5v14M5 12h14"/>
              </svg>
            </span>
            <span class="gm-member-name">邀请</span>
            <span class="gm-member-role">添加成员</span>
          </button>
        </div>
        <div v-else class="gm-member-empty">没有匹配的成员，换个关键词或角色试试。</div>
      </section>

      </div>
      <aside class="gm-side">
      <!-- 群资料 -->
      <section id="gm-profile" class="gm-card">
        <div class="gm-card-head">
          <h2 class="gm-card-title">群资料</h2>
        </div>
        <div class="gm-rows">
          <div class="gm-row">
            <span class="gm-row-label">群号</span>
            <span class="gm-row-value gm-mono">{{ group.groupId }}</span>
            <button class="gm-copy" type="button" aria-label="复制群号" @click="copyGroupId">复制</button>
          </div>
          <div class="gm-row">
            <span class="gm-row-label">群主</span>
            <span class="gm-row-value">{{ group.groupOwner || '未知' }}</span>
          </div>
          <div class="gm-row">
            <span class="gm-row-label">创建时间</span>
            <span class="gm-row-value">{{ formatDate(group.createTime) || '未知' }}</span>
          </div>
          <div class="gm-row">
            <span class="gm-row-label">我的角色</span>
            <span class="gm-row-value">{{ myRoleText }}</span>
          </div>
          <div class="gm-row">
            <span class="gm-row-label">入群时间</span>
            <span class="gm-row-value">{{ formatDate(group.joinTime) || '未知' }}</span>
          </div>
        </div>
      </section>

      <!-- 群设置：复用 chatPrefs，和左侧会话列表共用同一份本地偏好 -->
      <section id="gm-settings" class="gm-card">
        <div class="gm-card-head">
          <h2 class="gm-card-title">群设置</h2>
        </div>
        <div class="gm-rows">
          <div class="gm-row gm-row-toggle">
            <span class="gm-row-label">消息免打扰</span>
            <button
                type="button"
                class="gm-switch"
                :class="{ on: prefOn('muted') }"
                role="switch"
                :aria-checked="prefOn('muted')"
                aria-label="消息免打扰"
                @click="toggleSetting('muted')"
            ><span class="gm-switch-knob"></span></button>
          </div>
          <div class="gm-row gm-row-toggle">
            <span class="gm-row-label">置顶聊天</span>
            <button
                type="button"
                class="gm-switch"
                :class="{ on: prefOn('pinned') }"
                role="switch"
                :aria-checked="prefOn('pinned')"
                aria-label="置顶聊天"
                @click="toggleSetting('pinned')"
            ><span class="gm-switch-knob"></span></button>
          </div>
          <div class="gm-row gm-row-toggle gm-row-mute">
            <div class="gm-row-main">
              <span class="gm-row-label">全员禁言</span>
              <span class="gm-row-sub">开启后仅群主和管理员可发言</span>
            </div>
            <button
                v-if="canManage"
                type="button"
                class="gm-switch"
                :class="{ on: Number(group.muteAll) === 1, pending: mutePending }"
                role="switch"
                :aria-checked="Number(group.muteAll) === 1"
                :aria-label="Number(group.muteAll) === 1 ? '关闭全员禁言' : '开启全员禁言'"
                :disabled="mutePending"
                @click="toggleMuteAll"
            ><span class="gm-switch-knob"></span></button>
            <span v-else class="gm-row-value">{{ Number(group.muteAll) === 1 ? '已开启' : '未开启' }}</span>
          </div>
        </div>
      </section>
      <section v-if="canManage" class="gm-note">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
          <circle cx="12" cy="12" r="9" opacity=".55"/>
          <path d="M12 11v6"/>
          <circle cx="12" cy="7.6" r=".9" fill="currentColor" stroke="none"/>
        </svg>
        <span>{{ isOwner ? '你是群主，可编辑群资料、发布公告、设置管理员、转让群主或解散群聊' : '你是管理员，可编辑群资料、发布公告、邀请成员、移出普通成员' }}</span>
      </section>

      <!-- 退出 / 解散 -->
      <section id="gm-danger" class="gm-card gm-danger-card">
        <button v-if="isOwner" class="gm-danger-btn" type="button" @click="askDisband">解散群聊</button>
        <button v-else class="gm-danger-btn" type="button" @click="askQuit">退出群聊</button>
      </section>
      </aside>
      </div>
      <p class="gm-foot">群管理操作会实时同步给其他成员</p>
    </div>

  </div>
    <!-- 底部面板留在群管理覆盖层内：截图与层叠都稳定，不会被裁切 -->
    <div
      v-if="sheet"
      class="gm-mask"
      @click.self="closeSheet"
    >
          <div class="gm-sheet" ref="sheetEl" tabindex="-1" :class="{ 'gm-sheet-tall': sheet === 'invite' || sheet === 'profile' || sheet === 'announce' }" @click.stop>
            <div class="gm-sheet-handle"></div>

            <!-- 成员资料 / 操作 -->
            <template v-if="sheet === 'member' && actionMember">
              <div class="gm-sheet-member">
                <div class="gm-sheet-avatar" :style="avatarStyle(actionMember.userId)">
                  <img v-if="actionMember.avatar" :src="actionMember.avatar" :alt="actionMember.username" @error="actionMember.avatar = ''">
                  <span v-else class="gm-sheet-avatar-text">{{ initial(actionMember.username) }}</span>
                </div>
                <div class="gm-sheet-member-info">
                  <div class="gm-sheet-name">{{ actionMember.username || '成员' }}</div>
                  <div class="gm-sheet-sub">
                    <span class="gm-sheet-role" :class="rolePillClassFor(actionMember.role)">{{ roleText(actionMember.role) || actionMember.roleName || '成员' }}</span>
                    <span>入群于 {{ formatDate(actionMember.joinTime) || '未知' }}</span>
                    <span v-if="actionMember.muted" class="gm-sheet-muted">禁言至 {{ formatDate(actionMember.muteUntil) }}</span>
                  </div>
                </div>
              </div>

              <div v-if="mutePickerOpen && canMuteAction" class="gm-mute-picker">
                <div class="gm-mute-picker-title">{{ actionMember.muted ? '延长或解除禁言' : '选择禁言时长' }}</div>
                <div class="gm-mute-chips">
                  <button
                      v-for="d in muteOptions"
                      :key="d.minutes"
                      type="button"
                      class="gm-mute-chip"
                      :class="{ active: muteMinutes === d.minutes }"
                      @click="muteMinutes = d.minutes"
                  >{{ d.label }}</button>
                </div>
                <div class="gm-mute-picker-actions">
                  <button type="button" class="gm-sheet-cancel gm-mute-picker-cancel" @click="mutePickerOpen = false">返回</button>
                  <button type="button" class="gm-primary-btn" :disabled="mutePending" @click="doMute">
                    {{ muteMinutes === 0 ? '解除禁言' : '确认禁言' }}
                  </button>
                </div>
              </div>

              <div v-else class="gm-sheet-actions">
                <button
                    v-if="canMuteAction"
                    type="button"
                    class="gm-sheet-action"
                    :disabled="mutePending"
                    @click="openMutePicker"
                >{{ actionMember.muted ? '调整 / 解除禁言' : '禁言成员' }}</button>
                <button
                    v-if="canSetAdmin"
                    type="button"
                    class="gm-sheet-action"
                    :disabled="rolePending"
                    @click="askSetRole(actionMember.role === 2 ? 3 : 2)"
                >{{ actionMember.role === 2 ? '取消管理员' : '设为管理员' }}</button>
                <button
                    v-if="canTransfer"
                    type="button"
                    class="gm-sheet-action warn"
                    @click="askTransfer"
                >转让群主</button>
                <button
                    v-if="canKickAction"
                    type="button"
                    class="gm-sheet-action danger"
                    @click="askKick"
                >移出群聊</button>
                <div v-if="!canSetAdmin && !canKickAction && !canTransfer" class="gm-sheet-tip">
                  {{ actionMember.userId === myUserId ? '这是你自己' : '你没有管理该成员的权限' }}
                </div>
              </div>
              <button type="button" class="gm-sheet-cancel" @click="closeSheet">取消</button>
            </template>

            <!-- 邀请成员 -->
            <template v-else-if="sheet === 'invite'">
              <div class="gm-sheet-head">
                <div>
                  <div class="gm-sheet-title">邀请成员</div>
                  <div class="gm-sheet-desc">已选 {{ selectedIds.length }} 人 · 入群后会收到系统提示</div>
                </div>
                <button type="button" class="gm-sheet-close" aria-label="关闭" @click="closeSheet">×</button>
              </div>

              <div class="gm-invite-search">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
                  <circle cx="11" cy="11" r="6.5"/>
                  <path d="M16 16l4 4"/>
                </svg>
                <input v-model="friendKeyword" type="text" placeholder="搜索好友">
              </div>

              <div class="gm-invite-list">
                <div v-if="friendsLoading" class="gm-invite-state">好友列表加载中…</div>
                <div v-else-if="friendsError" class="gm-invite-state">
                  <span>{{ friendsError }}</span>
                  <button type="button" class="gm-inline-retry" @click="loadFriends">重试</button>
                </div>
                <div v-else-if="invitableFriends.length === 0" class="gm-invite-state">
                  <span>{{ friendKeyword ? '没有匹配的好友' : '没有可邀请的好友' }}</span>
                  <span class="gm-invite-state-sub">已在本群的好友不会重复出现</span>
                </div>
                <template v-else>
                  <button
                      v-for="f in invitableFriends"
                      :key="f.userId"
                      type="button"
                      class="gm-invite-item"
                      :class="{ selected: selectedIds.includes(f.userId) }"
                      @click="toggleInvite(f.userId)"
                  >
                    <div class="gm-invite-avatar" :style="avatarStyle(f.userId)">
                      <img v-if="f.avatar" :src="f.avatar" :alt="f.username" @error="f.avatar = ''">
                      <span v-else class="gm-invite-avatar-text">{{ initial(f.username) }}</span>
                    </div>
                    <span class="gm-invite-name">{{ f.username || '好友' }}</span>
                    <span class="gm-check" :class="{ checked: selectedIds.includes(f.userId) }">
                      <svg v-if="selectedIds.includes(f.userId)" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                        <path d="M5 12.5l4.5 4.5L19 7"/>
                      </svg>
                    </span>
                  </button>
                </template>
              </div>

              <div class="gm-sheet-footer">
                <button
                    type="button"
                    class="gm-primary-btn"
                    :disabled="!selectedIds.length || inviting"
                    @click="doInvite"
                >{{ inviting ? '邀请中…' : '邀请' + (selectedIds.length ? '（' + selectedIds.length + '）' : '') }}</button>
              </div>
            </template>

            <!-- 编辑群资料 -->
            <template v-else-if="sheet === 'profile'">
              <div class="gm-sheet-head">
                <div>
                  <div class="gm-sheet-title">编辑群资料</div>
                  <div class="gm-sheet-desc">群名称、头像和简介会同步到所有成员的会话列表</div>
                </div>
                <button type="button" class="gm-sheet-close" aria-label="关闭" @click="closeSheet">×</button>
              </div>

              <div class="gm-form">
                <div class="gm-form-avatar-row">
                  <div class="gm-form-avatar" :style="avatarStyle(group.groupId)">
                    <img v-if="profileAvatar" :src="profileAvatar" alt="群头像预览" @error="profileAvatar = ''">
                    <span v-else>{{ initial(profileName) }}</span>
                  </div>
                  <div class="gm-form-avatar-info">
                    <button type="button" class="gm-form-avatar-btn" :disabled="profileSaving" @click="pickAvatar">
                      {{ profileAvatar ? '更换头像' : '上传头像' }}
                    </button>
                    <span class="gm-form-tip">建议正方形，JPG / PNG，5MB 以内</span>
                  </div>
                  <input ref="avatarInput" type="file" accept="image/*" class="gm-file-input" @change="onAvatarPick">
                </div>

                <label class="gm-field">
                  <span class="gm-field-label">群名称</span>
                  <input v-model="profileName" type="text" maxlength="30" class="gm-input" placeholder="给群起个名字">
                  <span class="gm-counter">{{ profileName.length }}/30</span>
                </label>

                <label class="gm-field">
                  <span class="gm-field-label">群简介</span>
                  <textarea v-model="profileDesc" maxlength="200" rows="3" class="gm-textarea" placeholder="介绍一下这个群"></textarea>
                  <span class="gm-counter">{{ profileDesc.length }}/200</span>
                </label>
              </div>

              <div class="gm-sheet-footer">
                <button type="button" class="gm-primary-btn" :disabled="profileSaving || !profileName.trim()" @click="saveProfile">
                  {{ profileSaving ? (profileUploading ? '头像上传中…' : '保存中…') : '保存' }}
                </button>
              </div>
            </template>

            <!-- 群公告 -->
            <template v-else-if="sheet === 'announce'">
              <div class="gm-sheet-head">
                <div>
                  <div class="gm-sheet-title">{{ hasAnnouncement ? '编辑群公告' : '发布群公告' }}</div>
                  <div class="gm-sheet-desc">发布后会向群内发送一条系统消息</div>
                </div>
                <button type="button" class="gm-sheet-close" aria-label="关闭" @click="closeSheet">×</button>
              </div>

              <div class="gm-form">
                <label class="gm-field">
                  <span class="gm-field-label">公告内容</span>
                  <textarea v-model="announcementDraft" maxlength="500" rows="7" class="gm-textarea gm-textarea-lg" placeholder="例如：本周五晚 8 点群内开黑，进群先看置顶"></textarea>
                  <span class="gm-counter">{{ announcementDraft.length }}/500</span>
                </label>
              </div>

              <div class="gm-sheet-footer gm-announce-footer">
                <button
                    v-if="hasAnnouncement"
                    type="button"
                    class="gm-secondary-btn"
                    :disabled="announcing"
                    @click="askDeleteAnnouncement"
                >删除公告</button>
                <button
                    type="button"
                    class="gm-primary-btn"
                    :disabled="announcing || !announcementDraft.trim()"
                    @click="saveAnnouncement(announcementDraft)"
                >{{ announcing ? '保存中…' : (hasAnnouncement ? '保存公告' : '发布公告') }}</button>
              </div>
            </template>

            <!-- 二次确认 -->
            <template v-else-if="sheet === 'confirm'">
              <div class="gm-confirm">
                <div class="gm-confirm-icon" :class="{ danger: confirmKind !== 'role' }">
                  <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                    <path d="M12 8v5"/>
                    <circle cx="12" cy="16.5" r="1.1" fill="currentColor" stroke="none"/>
                    <path d="M10.3 3.8L2.7 17a1.6 1.6 0 0 0 1.4 2.4h15.8a1.6 1.6 0 0 0 1.4-2.4L13.7 3.8a1.6 1.6 0 0 0-3.4 0z"/>
                  </svg>
                </div>
                <div class="gm-confirm-title">{{ confirmTitle }}</div>
                <div class="gm-confirm-desc">{{ confirmDesc }}</div>
                <button
                    type="button"
                    class="gm-confirm-ok"
                    :class="{ danger: confirmKind !== 'role' }"
                    :disabled="confirmBusy"
                    @click="runConfirm"
                >{{ confirmBusy ? '处理中…' : confirmOk }}</button>
                <button type="button" class="gm-sheet-cancel gm-confirm-cancel" :disabled="confirmBusy" @click="cancelConfirm">取消</button>
              </div>
            </template>
          </div>
        </div>

      <!-- 群二维码 -->
      <div v-if="groupQrOpen" class="gm-qr-mask" @click="closeGroupQr">
        <div class="gm-qr-dialog" role="dialog" aria-modal="true" aria-label="群二维码" @click.stop>
          <div class="gm-qr-head">
            <div>
              <div class="gm-qr-title">群二维码</div>
              <div class="gm-qr-subtitle">扫码打开入群页，提交申请后由群主或管理员审批</div>
            </div>
            <button type="button" class="gm-qr-close" aria-label="关闭" @click="closeGroupQr">×</button>
          </div>
          <div class="gm-qr-body">
            <div class="gm-qr-frame">
              <img v-if="groupQrUrl" :src="groupQrUrl" alt="群二维码" class="gm-qr-img">
              <span v-else class="gm-qr-loading">{{ groupQrLoading ? '生成中…' : '二维码不可用' }}</span>
            </div>
            <div class="gm-qr-info">
              <div class="gm-qr-group">{{ group.groupName || '未命名群聊' }}</div>
              <div class="gm-qr-no">群号 {{ group.groupId }} · {{ memberCount }} 人</div>
              <div class="gm-qr-tip">手机扫码，或复制链接发给好友；非群成员也能查看群资料并申请加入。</div>
              <div class="gm-qr-actions">
                <button type="button" class="gm-qr-btn ghost" @click="closeGroupQr">关闭</button>
                <button type="button" class="gm-qr-btn primary" @click="copyGroupQrLink">复制入群链接</button>
              </div>
            </div>
          </div>
        </div>
      </div>
</template>

<script>
import {
  getGroupDetail,
  inviteMembers,
  kickMember,
  setMemberRole,
  quitGroup,
  disbandGroup,
  updateGroupInfo,
  updateAnnouncement,
  updateMuteAll,
  muteMember,
  transferOwner,
  getGroupAiSummary,
  getGroupFiles,
  saveGroupFile,
  deleteGroupFile,
  getJoinRequests,
  handleJoinRequest,
} from '@/api/group.js'
import { uploadFile } from '@/api/file.js'
import { getFriendList } from '@/api/contact.js'
import { ElMessage } from '@/plugins/message'
import { getObject } from '@/utils/localStorage.js'
import { loadChatPrefs, togglePref } from '@/utils/chatPrefs.js'
import { makeGroupQrDataUrl, groupQrContent } from '@/utils/groupQr.js'

const ROLE_OWNER = 1
const ROLE_ADMIN = 2

export default {
  name: 'GroupManage',
  props: {
    groupId: {
      type: [String, Number],
      required: true,
    },
  },
  emits: ['close', 'changed', 'exit', 'prefs-change'],
  data() {
    return {
      loading: true,
      error: '',
      detailWarning: '',
      group: {},
      members: [],
      joinRequests: [],
      joinLoading: false,
      joinPendingId: '',
      // 本地会话偏好：置顶 / 免打扰
      prefs: {},
      sheet: '',
      sheetTop: 0,
      sheetHeight: 0,
      sheetReturnFocus: null,
      actionMember: null,
      groupQrOpen: false,
      groupQrUrl: '',
      groupQrLoading: false,
      rolePending: false,
      mutePickerOpen: false,
      muteMinutes: 60,
      mutePending: false,
      muteOptions: [
        { label: '10 分钟', minutes: 10 },
        { label: '1 小时', minutes: 60 },
        { label: '1 天', minutes: 1440 },
        { label: '7 天', minutes: 10080 },
        { label: '30 天', minutes: 43200 },
      ],
      // 邀请
      friends: [],
      friendsLoading: false,
      friendsError: '',
      friendKeyword: '',
      selectedIds: [],
      inviting: false,
      // 群资料编辑
      profileName: '',
      profileDesc: '',
      profileAvatar: '',
      profileFile: null,
      profileSaving: false,
      profileUploading: false,
      // 群公告
      announcementDraft: '',
      announcing: false,
      // 全员禁言
      mutePending: false,
      // 二次确认
      confirmKind: '',
      confirmTitle: '',
      confirmDesc: '',
      confirmOk: '',
      confirmBusy: false,
      // 群 AI 摘要
      aiLoading: false,
      aiError: '',
      aiSummary: null,
      // 群文件中心
      files: [],
      filesLoading: false,
      filesError: '',
      fileTotal: 0,
      filePage: 1,
      filePageSize: 20,
      fileHasMore: false,
      fileUploading: false,
      uploadingFileName: '',
      pendingDeleteFile: null,
      // 群文件搜索 / 类型筛选 / 拖拽 / 上传进度
      fileKeyword: '',
      fileTypeFilter: 'all',
      fileTypeOptions: [
        { value: 'all', label: '全部类型' },
        { value: 'image', label: '图片' },
        { value: 'pdf', label: 'PDF' },
        { value: 'doc', label: '文档' },
        { value: 'sheet', label: '表格' },
        { value: 'zip', label: '压缩包' },
        { value: 'video', label: '视频' },
        { value: 'audio', label: '音频' },
        { value: 'other', label: '其他' },
      ],
      isFileDrag: false,
      fileUploadProgress: 0,
      fileUploadPhase: '',
      fileUploadAbort: null,
      fileSearchPending: false,
      fileSearchTimer: null,
      // 成员搜索 / 角色筛选 / 批量管理
      memberKeyword: '',
      memberRoleFilter: 'all',
      memberSelectMode: false,
      selectedMemberIds: [],
      pendingBatchRole: 0,
      pendingBatchTargets: [],
      // 编辑防丢
      discardTargetSheet: '',
      // 移动端快捷导航
      activeMobileSection: 'gm-announce',
      mobileScrollRaf: 0,
      navJumpTimer: null,
      // 后台静默刷新与无障碍
      refreshTimer: null,
      detailRefreshing: false,
      aiCooldownLeft: 0,
      aiCooldownTimer: null,
      backgroundInertTargets: [],
      sheetInertTargets: [],
    }
  },
  computed: {
    myUserId() {
      const user = getObject('userInfo') || {}
      return user.id == null ? '' : String(user.id)
    },
    groupRole() {
      return this.group && this.group.role != null ? Number(this.group.role) : 0
    },
    isOwner() {
      return this.groupRole === ROLE_OWNER
    },
    canManage() {
      return this.groupRole === ROLE_OWNER || this.groupRole === ROLE_ADMIN
    },
    memberCount() {
      if (this.members.length) {
        return this.members.length
      }
      return this.group.currentMember || 0
    },
    hasAnnouncement() {
      return !!(this.group && this.group.announcement && String(this.group.announcement).trim())
    },
    canTransfer() {
      return this.groupRole === ROLE_OWNER &&
        !!this.actionMember &&
        String(this.actionMember.userId) !== this.myUserId &&
        Number(this.actionMember.role) !== ROLE_OWNER
    },
    rolePillClass() {
      return this.rolePillClassFor(this.groupRole)
    },
    myRoleText() {
      return this.roleText(this.groupRole) || (this.group && this.group.roleName) || ''
    },
    invitableFriends() {
      const joined = this.members.map((m) => String(m.userId))
      const keyword = this.friendKeyword.trim().toLowerCase()
      return (this.friends || []).filter((f) => {
        if (!f || !f.userId || joined.indexOf(String(f.userId)) !== -1) {
          return false
        }
        if (!keyword) {
          return true
        }
        return String(f.username || '').toLowerCase().indexOf(keyword) !== -1
      })
    },
    canSetAdmin() {
      // 仅群主可设/取消管理员；目标不能是群主自己
      return this.groupRole === ROLE_OWNER &&
        !!this.actionMember &&
        String(this.actionMember.userId) !== this.myUserId &&
        !this.actionMember.owner
    },
    canKickAction() {
      const target = this.actionMember
      if (!target || String(target.userId) === this.myUserId) {
        return false
      }
      if (this.groupRole === ROLE_OWNER) {
        return !target.owner && target.role !== ROLE_OWNER
      }
      if (this.groupRole === ROLE_ADMIN) {
        return target.role === 3
      }
      return false
    },
    canMuteAction() {
      const target = this.actionMember
      if (!target || String(target.userId) === this.myUserId) return false
      if (target.owner || Number(target.role) === ROLE_OWNER) return false
      if (this.groupRole === ROLE_OWNER) return true
      return this.groupRole === ROLE_ADMIN && Number(target.role) === 3
    },
    aiTodos() {
      if (!this.aiSummary || !Array.isArray(this.aiSummary.todos)) {
        return []
      }
      return this.aiSummary.todos.filter(todo => todo && todo.content)
    },
    filteredMembers() {
      const keyword = this.memberKeyword.trim().toLowerCase()
      const role = this.memberRoleFilter
      return (this.members || [])
        .filter((m) => {
          if (!m) return false
          if (role !== 'all' && String(m.role) !== String(role)) return false
          if (!keyword) return true
          return String(m.username || '').toLowerCase().indexOf(keyword) !== -1
        })
        .slice()
        .sort((a, b) => {
          const rank = (m) => (Number(m.role) === ROLE_OWNER ? 0 : Number(m.role) === ROLE_ADMIN ? 1 : 2)
          const diff = rank(a) - rank(b)
          if (diff) return diff
          return String(a.username || '').localeCompare(String(b.username || ''), 'zh-Hans-CN')
        })
    },
    memberRoleOptions() {
      const counts = { '1': 0, '2': 0, '3': 0 }
      ;(this.members || []).forEach((m) => {
        const role = String(m && m.role)
        if (counts[role] != null) counts[role] += 1
      })
      return [
        { value: 'all', label: '全部', count: (this.members || []).length },
        { value: '1', label: '群主', count: counts['1'] },
        { value: '2', label: '管理员', count: counts['2'] },
        { value: '3', label: '成员', count: counts['3'] },
      ]
    },
    memberFilterActive() {
      return !!this.memberKeyword.trim() || this.memberRoleFilter !== 'all'
    },
    selectedMembers() {
      const ids = this.selectedMemberIds
      return (this.members || []).filter((m) => ids.indexOf(String(m.userId)) !== -1)
    },
    batchAdminTargets() {
      return this.selectedMembers.filter((m) => this.canSetRoleFor(m, ROLE_ADMIN))
    },
    batchRemoveAdminTargets() {
      return this.selectedMembers.filter((m) => this.canSetRoleFor(m, 3) && Number(m.role) === ROLE_ADMIN)
    },
    batchKickTargets() {
      return this.selectedMembers.filter((m) => this.canKickMember(m))
    },
    visibleFiles() {
      if (this.fileTypeFilter === 'all') return this.files
      return this.files.filter((file) => this.fileExtClass(file) === this.fileTypeFilter)
    },
    fileFilterActive() {
      return !!this.fileKeyword.trim() || this.fileTypeFilter !== 'all'
    },
    profileDirty() {
      if (!this.canManage) return false
      const groupName = String(this.group.groupName || '').trim()
      const description = String(this.group.description || '').trim()
      return this.profileName.trim() !== groupName ||
        this.profileDesc.trim() !== description ||
        !!this.profileFile ||
        this.profileAvatar !== (this.group.groupAvatar || '')
    },
    announcementDirty() {
      if (!this.canManage) return false
      return this.announcementDraft.trim() !== String(this.group.announcement || '').trim()
    },
    mobileNavItems() {
      return [
        { id: 'gm-announce', label: '公告' },
        { id: 'gm-files', label: '文件' },
        { id: 'gm-members', label: '成员' },
        { id: 'gm-profile', label: '资料' },
        { id: 'gm-settings', label: '设置' },
        { id: 'gm-danger', label: this.isOwner ? '解散' : '退出' },
      ]
    }
  },
  watch: {
    sheet(value, oldValue) {
      this.setSheetInert(!!value)
      if (value) {
        if (!oldValue) {
          this.sheetReturnFocus = document.activeElement
        }
        this.$nextTick(() => this.focusSheet())
      } else if (oldValue) {
        this.$nextTick(() => setTimeout(() => this.restoreSheetFocus(), 0))
      }
    },
    fileKeyword() {
      if (this.fileSearchTimer) clearTimeout(this.fileSearchTimer)
      this.fileSearchTimer = setTimeout(() => {
        this.fileSearchTimer = null
        this.loadFiles(true)
      }, 350)
    },
    fileTypeFilter() {
      this.loadFiles(true)
    },
  },
  mounted() {
    this.prefs = loadChatPrefs()
    window.addEventListener('keydown', this.onWindowKeydown, true)
    document.addEventListener('visibilitychange', this.onVisibilityChange)
    const root = this.$refs.gmRoot
    if (root) root.addEventListener('scroll', this.onRootScroll, { passive: true })
    this.$nextTick(() => {
      this.focusDialog()
      this.lockBackground()
    })
    this.startRealtime()
    this.load()
    this.loadFiles()
  },
  beforeUnmount() {
    window.removeEventListener('keydown', this.onWindowKeydown, true)
    document.removeEventListener('visibilitychange', this.onVisibilityChange)
    const root = this.$refs.gmRoot
    if (root) root.removeEventListener('scroll', this.onRootScroll)
    if (this.mobileScrollRaf) cancelAnimationFrame(this.mobileScrollRaf)
    this.mobileScrollRaf = 0
    if (this.navJumpTimer) clearTimeout(this.navJumpTimer)
    this.navJumpTimer = null
    if (this.refreshTimer) clearInterval(this.refreshTimer)
    if (this.fileSearchTimer) clearTimeout(this.fileSearchTimer)
    if (this.aiCooldownTimer) clearInterval(this.aiCooldownTimer)
    this.unlockBackground()
    this.setSheetInert(false)
    this.actionMember = null
    this.revokeAvatarPreview()
  },
  methods: {
    // ===== 焦点与背景可访问性 =====
    focusDialog() {
      const root = this.$refs.gmRoot
      if (!root) return
      const target = root.querySelector('.gm-back') || root
      if (target && target.focus) target.focus({ preventScroll: true })
    },
    lockBackground() {
      const root = this.$refs.gmRoot
      const parent = root && root.parentElement
      if (!root || !parent) return
      this.unlockBackground()
      this.backgroundInertTargets = Array.prototype.slice.call(parent.children)
        .filter((el) => el !== root)
        .map((el) => ({
          el,
          inert: !!el.inert,
          ariaHidden: el.getAttribute('aria-hidden'),
        }))
      this.backgroundInertTargets.forEach((item) => {
        item.el.inert = true
        item.el.setAttribute('aria-hidden', 'true')
      })
    },
    unlockBackground() {
      ;(this.backgroundInertTargets || []).forEach((item) => {
        if (!item || !item.el) return
        item.el.inert = item.inert
        if (item.ariaHidden == null) {
          item.el.removeAttribute('aria-hidden')
        } else {
          item.el.setAttribute('aria-hidden', item.ariaHidden)
        }
      })
      this.backgroundInertTargets = []
    },
    setSheetInert(active) {
      const root = this.$refs.gmRoot
      if (!root) return
      if (active) {
        if (this.sheetInertTargets.length) return
        const nav = root.querySelector('.gm-nav')
        const scroll = this.$refs.scroll
        this.sheetInertTargets = [nav, scroll].filter(Boolean).map((el) => ({
          el,
          inert: !!el.inert,
          ariaHidden: el.getAttribute('aria-hidden'),
        }))
        this.sheetInertTargets.forEach((item) => {
          item.el.inert = true
          item.el.setAttribute('aria-hidden', 'true')
        })
      } else {
        ;(this.sheetInertTargets || []).forEach((item) => {
          if (!item || !item.el) return
          item.el.inert = item.inert
          if (item.ariaHidden == null) {
            item.el.removeAttribute('aria-hidden')
          } else {
            item.el.setAttribute('aria-hidden', item.ariaHidden)
          }
        })
        this.sheetInertTargets = []
      }
    },

    // ===== 后台静默刷新 =====
    startRealtime() {
      if (this.refreshTimer) clearInterval(this.refreshTimer)
      this.refreshTimer = setInterval(() => this.refreshSilently(), 30000)
    },
    onVisibilityChange() {
      if (!document.hidden) this.refreshSilently()
    },
    refreshSilently() {
      if (document.hidden || this.loading || this.detailRefreshing) return
      if (this.sheet || this.memberSelectMode || this.profileSaving || this.announcing) return
      this.load({ background: true })
    },

    // ===== 移动端快捷导航 =====
    onRootScroll() {
      if (this.navJumpTimer) return
      if (this.mobileScrollRaf) return
      this.mobileScrollRaf = window.requestAnimationFrame(() => {
        this.mobileScrollRaf = 0
        this.updateActiveMobileSection()
      })
    },
    updateActiveMobileSection() {
      const root = this.$refs.gmRoot
      if (!root) return
      let current = this.mobileNavItems[0].id
      const threshold = 112
      this.mobileNavItems.forEach((item) => {
        const target = root.querySelector('#' + item.id)
        if (target && target.getBoundingClientRect().top <= threshold) current = item.id
      })
      if (this.activeMobileSection !== current) this.activeMobileSection = current
    },
    jumpTo(id) {
      const root = this.$refs.gmRoot
      const target = root && root.querySelector('#' + id)
      if (!target) return
      this.activeMobileSection = id
      if (this.navJumpTimer) clearTimeout(this.navJumpTimer)
      this.navJumpTimer = setTimeout(() => {
        this.navJumpTimer = null
      }, 1000)
      const reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches
      target.scrollIntoView({ behavior: reduce ? 'auto' : 'smooth', block: 'start' })
    },

    // ===== 群 AI 摘要与待办 =====
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
    async loadAiSummary(force) {
      if (!this.canManage) {
        ElMessage.warning('仅群主和管理员可生成 AI 摘要')
        return
      }
      if (this.aiLoading) return
      if (this.aiCooldownLeft > 0) {
        ElMessage.warning('生成冷却中，请 ' + this.aiCooldownLeft + ' 秒后再试')
        return
      }
      if (this.aiSummary && !force) return
      this.aiLoading = true
      this.aiError = ''
      try {
        const res = await getGroupAiSummary(this.group.groupId || this.groupId)
        if (!res || res.code !== 200 || !res.data) {
          throw new Error((res && (res.msg || res.message)) || '摘要生成失败')
        }
        this.aiSummary = res.data
        this.startAiCooldown(8)
      } catch (e) {
        this.aiSummary = null
        this.aiError = (e && (e.message || e.msg)) || '摘要生成失败，请稍后重试'
      } finally {
        this.aiLoading = false
      }
    },

    // ===== 群文件中心 =====
    pickFile() {
      if (this.fileUploading) return
      if (this.$refs.fileInput) this.$refs.fileInput.click()
    },

    async handleFileInput(event) {
      const input = event && event.target
      const file = input && input.files && input.files[0]
      if (input) input.value = ''
      if (!file || this.fileUploading) return
      await this.uploadGroupFile(file)
    },

    async uploadGroupFile(file) {
      if (!file || this.fileUploading) return
      if (file.size > 200 * 1024 * 1024) {
        ElMessage.error('单个文件不能超过 200MB')
        return
      }
      const controller = typeof AbortController !== 'undefined' ? new AbortController() : null
      this.fileUploadAbort = controller
      this.fileUploading = true
      this.fileUploadPhase = 'uploading'
      this.fileUploadProgress = 0
      this.uploadingFileName = file.name
      try {
        const uploadRes = await uploadFile(file, {
          signal: controller ? controller.signal : undefined,
          onUploadProgress: (event) => {
            const total = Number(event && event.total)
            const loaded = Number(event && event.loaded)
            if (!total || !loaded) return
            this.fileUploadProgress = Math.max(1, Math.min(99, Math.round((loaded / total) * 100)))
          },
        })
        if (!uploadRes || uploadRes.code !== 200 || !uploadRes.data) {
          throw new Error((uploadRes && (uploadRes.msg || uploadRes.message)) || '文件上传失败')
        }
        this.fileUploadProgress = 100
        this.fileUploadPhase = 'saving'
        const saveRes = await saveGroupFile({
          groupId: String(this.group.groupId || this.groupId),
          fileName: file.name,
          fileUrl: uploadRes.data,
          fileSize: file.size,
          mimeType: file.type || 'application/octet-stream'
        })
        if (!saveRes || saveRes.code !== 200) {
          throw new Error((saveRes && (saveRes.msg || saveRes.message)) || '保存群文件失败')
        }
        ElMessage.success('已上传到群文件')
        await this.loadFiles(true)
      } catch (e) {
        const canceled = e && (
          e.name === 'CanceledError' ||
          e.code === 'ERR_CANCELED' ||
          String(e.message || '').toLowerCase().indexOf('canceled') !== -1
        )
        if (canceled) {
          ElMessage.info('已取消上传')
        } else {
          ElMessage.error((e && (e.message || e.msg)) || '上传失败，请稍后重试')
        }
      } finally {
        this.fileUploading = false
        this.fileUploadPhase = ''
        this.fileUploadAbort = null
        this.fileUploadProgress = 0
        this.uploadingFileName = ''
      }
    },

    cancelFileUpload() {
      if (!this.fileUploading) return
      if (this.fileUploadPhase !== 'uploading' || !this.fileUploadAbort) {
        ElMessage.info('文件已上传，正在保存到群文件')
        return
      }
      this.fileUploadAbort.abort()
    },

    onFileDragOver() {
      if (this.fileUploading) return
      this.isFileDrag = true
    },
    onFileDragLeave(event) {
      if (event && event.currentTarget && event.currentTarget.contains(event.relatedTarget)) return
      this.isFileDrag = false
    },
    onFileDrop(event) {
      this.isFileDrag = false
      if (this.fileUploading) return
      const files = event && event.dataTransfer && event.dataTransfer.files
      if (!files || !files.length) return
      if (files.length > 1) ElMessage.warning('一次只能上传一个文件，已选择第一个')
      this.uploadGroupFile(files[0])
    },

    async loadFiles(reset = true) {
      if (this.filesLoading) {
        if (reset) this.fileSearchPending = true
        return
      }
      this.fileSearchPending = false
      if (reset) {
        this.filePage = 1
        this.filesError = ''
      }
      this.filesLoading = true
      try {
        const res = await getGroupFiles(
          String(this.group.groupId || this.groupId),
          this.fileKeyword.trim(),
          this.filePage,
          this.filePageSize,
          this.fileTypeFilter
        )
        if (!res || res.code !== 200 || !res.data) {
          throw new Error((res && (res.msg || res.message)) || '群文件加载失败')
        }
        const page = res.data
        const rows = Array.isArray(page.rows) ? page.rows : []
        this.files = reset ? rows : this.files.concat(rows)
        this.fileTotal = Number(page.total) || 0
        this.fileHasMore = Number(page.pageNum) < Number(page.totalPage)
      } catch (e) {
        this.filesError = (e && (e.message || e.msg)) || '群文件加载失败'
        if (reset) {
          this.files = []
          this.fileTotal = 0
          this.fileHasMore = false
        } else {
          this.filePage = Math.max(1, this.filePage - 1)
        }
      } finally {
        this.filesLoading = false
        if (this.fileSearchPending) {
          this.fileSearchPending = false
          this.loadFiles(true)
        }
      }
    },

    loadMoreFiles() {
      if (this.filesLoading || !this.fileHasMore) return
      this.filePage++
      this.loadFiles(false)
    },

    canDeleteFile(file) {
      if (!file) return false
      return this.canManage || String(file.uploaderId) === this.myUserId
    },

    downloadFile(file) {
      if (!file || !file.fileUrl) {
        ElMessage.error('文件地址无效')
        return
      }
      const link = document.createElement('a')
      link.href = file.fileUrl
      link.target = '_blank'
      link.rel = 'noopener noreferrer'
      link.download = file.fileName || ''
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    },

    openDeleteFile(file) {
      if (!file || this.confirmBusy) return
      this.pendingDeleteFile = file
      this.confirmKind = 'file-delete'
      this.confirmTitle = '删除群文件'
      this.confirmDesc = '「' + (file.fileName || '该文件') + '」将从群文件列表移除，聊天里已发送的文件消息不受影响。'
      this.confirmOk = '删除'
      this.confirmBusy = false
      this.syncSheetRect()
      this.sheet = 'confirm'
    },

    fileExtension(file) {
      const explicit = String((file && file.fileExt) || '').trim().toLowerCase()
      if (explicit) return explicit.replace(/^\./, '')
      const name = String((file && file.fileName) || '')
      const dot = name.lastIndexOf('.')
      return dot >= 0 ? name.slice(dot + 1).toLowerCase() : ''
    },
    fileExtLabel(file) {
      const ext = this.fileExtension(file)
      if (!ext) return '文件'
      return ext.length > 4 ? ext.slice(0, 4) : ext.toUpperCase()
    },

    fileExtClass(file) {
      const ext = this.fileExtension(file)
      if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg'].indexOf(ext) !== -1) return 'image'
      if (['pdf'].indexOf(ext) !== -1) return 'pdf'
      if (['doc', 'docx', 'txt', 'md', 'rtf'].indexOf(ext) !== -1) return 'doc'
      if (['xls', 'xlsx', 'csv'].indexOf(ext) !== -1) return 'sheet'
      if (['zip', 'rar', '7z', 'tar', 'gz'].indexOf(ext) !== -1) return 'zip'
      if (['mp4', 'mov', 'avi', 'mkv', 'webm'].indexOf(ext) !== -1) return 'video'
      if (['mp3', 'wav', 'm4a', 'aac', 'flac'].indexOf(ext) !== -1) return 'audio'
      return 'other'
    },

    formatSize(bytes) {
      const size = Number(bytes)
      if (!size || size < 0) return '未知大小'
      if (size < 1024) return size + ' B'
      if (size < 1024 * 1024) return (size / 1024).toFixed(size < 1024 * 10 ? 1 : 0) + ' KB'
      if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(1) + ' MB'
      return (size / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
    },

    async load(options = {}) {
      // 已有群数据时做后台刷新，不整页切回骨架屏，避免保存后页面闪空、焦点丢失
      const background = !!(options && options.background)
      if (background && this.detailRefreshing) return
      const hadGroup = !!(this.group && this.group.groupId)
      if (!hadGroup) this.loading = true
      if (background) this.detailRefreshing = true
      this.error = ''
      if (!background) this.detailWarning = ''
      try {
        // 本页自带整页错误态 / 后台降级提示，不再叠加全局 500 弹窗
        const res = await getGroupDetail(this.groupId, { silent: true })
        if (!res || res.code !== 200 || !res.data) {
          throw new Error((res && (res.msg || res.message)) || '接口未返回群资料')
        }
        if (background && (this.sheet || this.memberSelectMode)) return
        this.group = res.data
        this.detailWarning = ''
        // 批量选择期间不替换成员数组，避免选择状态被后台刷新打断
        if (!this.memberSelectMode) {
          this.members = Array.isArray(res.data.members) ? res.data.members : []
        }
        if (this.canManage && !this.memberSelectMode) {
          this.loadJoinRequests()
        }
      } catch (e) {
        // 不把 Axios 的英文原始报错直接抛给用户
        const status = e && e.response && e.response.status
        const message = status
          ? `服务暂时不可用（${status}），请稍后重试`
          : ((e && (e.msg || e.message)) || '网络异常，请稍后重试')
        // 已有数据时只做降级提示，避免一次后台刷新失败把整页清空
        if (this.group && this.group.groupId) {
          this.detailWarning = message
        } else {
          this.group = {}
          this.members = []
          this.error = message
        }
      } finally {
        this.loading = false
        this.detailRefreshing = false
      }
    },
    async loadJoinRequests() {
      this.joinLoading = true
      try {
        const res = await getJoinRequests(this.groupId)
        this.joinRequests = (res && res.data) || []
      } catch (e) {
        this.joinRequests = []
      } finally {
        this.joinLoading = false
      }
    },
    async handleJoin(request, approve) {
      if (this.joinPendingId) return
      this.joinPendingId = request.id
      try {
        const res = await handleJoinRequest({ requestId: request.id, approve: !!approve })
        if (res && res.code === 200) {
          ElMessage.success(approve ? '已同意入群' : '已拒绝申请')
          this.joinRequests = this.joinRequests.filter(i => i.id !== request.id)
          if (approve) await this.load({ background: true })
        }
      } catch (e) {
        /* request 层已提示 */
      } finally {
        this.joinPendingId = ''
      }
    },
    initial(name) {
      const text = String(name || '群')
      return text.slice(0, 1).toUpperCase()
    },
    avatarStyle(seed) {
      const palettes = [
        'linear-gradient(135deg, #5ac8fa, #007aff)',
        'linear-gradient(135deg, #ff9f0a, #ff6b22)',
        'linear-gradient(135deg, #bf5af2, #7d3cff)',
        'linear-gradient(135deg, #30d158, #0aa84a)',
        'linear-gradient(135deg, #ff6482, #ff2d55)',
        'linear-gradient(135deg, #64d2ff, #0a84ff)'
      ]
      const text = String(seed || '')
      let hash = 0
      for (let i = 0; i < text.length; i += 1) {
        hash = (hash * 31 + text.charCodeAt(i)) % 100000
      }
      return { background: palettes[hash % palettes.length] }
    },
    roleText(role) {
      const value = Number(role)
      if (value === ROLE_OWNER) return '群主'
      if (value === ROLE_ADMIN) return '管理员'
      if (value === 3) return '成员'
      return ''
    },
    rolePillClassFor(role) {
      const value = Number(role)
      if (value === ROLE_OWNER) return 'owner'
      if (value === ROLE_ADMIN) return 'admin'
      return ''
    },
    formatDate(value) {
      if (!value) return ''
      return String(value).replace('T', ' ').slice(0, 16)
    },
    prefOn(field) {
      const item = this.prefs && this.prefs[String(this.groupId)]
      return !!(item && item[field])
    },
    toggleSetting(field) {
      this.prefs = togglePref(this.prefs, String(this.groupId), field)
      this.$emit('prefs-change', this.prefs)
      ElMessage.success(field === 'muted' ? (this.prefOn('muted') ? '已开启消息免打扰' : '已关闭消息免打扰') : (this.prefOn('pinned') ? '已置顶该聊天' : '已取消置顶'))
    },
    // 全员禁言：真实写库 + 给全群发系统消息；失败回滚开关并明确报错
    async toggleMuteAll() {
      if (!this.canManage || this.mutePending) return
      const next = Number(this.group.muteAll) === 1 ? 0 : 1
      this.mutePending = true
      try {
        const res = await updateMuteAll({ groupId: String(this.groupId), muteAll: next })
        if (!res || res.code !== 200 || res.data === false) {
          throw new Error((res && (res.msg || res.message)) || '设置失败')
        }
        this.group.muteAll = next
        this.$emit('changed', { groupId: String(this.groupId), muteAll: next })
        ElMessage.success(next === 1 ? '已开启全员禁言，仅群主和管理员可发言' : '已关闭全员禁言，全体成员可发言')
      } catch (e) {
        ElMessage.error((e && (e.message || e.msg)) || '全员禁言设置失败，请稍后重试')
      } finally {
        this.mutePending = false
      }
    },
    legacyCopy(text) {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      let ok = false
      try {
        ok = document.execCommand('copy')
      } finally {
        document.body.removeChild(textarea)
      }
      return ok
    },
    async copyGroupId() {
      const text = String(this.group.groupId || '')
      if (!text) return
      // 先走同步 execCommand：在点击手势内完成，不受剪贴板授权弹窗阻塞。
      let copied = this.legacyCopy(text)
      if (!copied) {
        try {
          if (navigator.clipboard && window.isSecureContext) {
            await Promise.race([
              navigator.clipboard.writeText(text),
              new Promise((resolve, reject) => setTimeout(() => reject(new Error('clipboard-timeout')), 500))
            ])
            copied = true
          }
        } catch (e) {
          copied = false
        }
      }
      if (copied) {
        ElMessage.success('群号已复制')
      } else {
        ElMessage.warning('复制失败，请手动选择群号')
      }
    },

    async openGroupQr() {
      const id = String(this.group.groupId || this.groupId || '')
      if (!id) {
        ElMessage.warning('群号暂不可用')
        return
      }
      this.groupQrOpen = true
      if (this.groupQrUrl) return
      this.groupQrLoading = true
      try {
        this.groupQrUrl = await makeGroupQrDataUrl(id, 640)
      } catch (e) {
        this.groupQrOpen = false
        ElMessage.error('二维码生成失败，请重试')
      } finally {
        this.groupQrLoading = false
      }
    },
    closeGroupQr() {
      this.groupQrOpen = false
    },
    copyGroupQrLink() {
      const id = String(this.group.groupId || this.groupId || '')
      if (!id) return
      const link = groupQrContent(id)
      let copied = this.legacyCopy(link)
      if (!copied) {
        try {
          if (navigator.clipboard && window.isSecureContext) {
            navigator.clipboard.writeText(link)
            copied = true
          }
        } catch (e) {
          copied = false
        }
      }
      copied ? ElMessage.success('入群链接已复制') : ElMessage.warning('复制失败，请手动复制')
    },

    // ===== 群资料编辑 =====
    openProfile() {
      if (!this.canManage) return
      this.revokeAvatarPreview()
      this.profileName = this.group.groupName || ''
      this.profileDesc = this.group.description || ''
      this.profileAvatar = this.group.groupAvatar || ''
      this.profileFile = null
      this.sheet = 'profile'
    },
    revokeAvatarPreview() {
      if (this.profileAvatar && String(this.profileAvatar).startsWith('blob:')) {
        URL.revokeObjectURL(this.profileAvatar)
      }
    },
    pickAvatar() {
      if (this.profileSaving) return
      const input = this.$refs.avatarInput
      if (input) input.click()
    },
    onAvatarPick(event) {
      const file = event && event.target && event.target.files && event.target.files[0]
      if (event && event.target) event.target.value = ''
      if (!file) return
      if (!/^image\//.test(file.type)) {
        ElMessage.warning('请选择图片文件')
        return
      }
      if (file.size > 5 * 1024 * 1024) {
        ElMessage.warning('头像不能超过 5MB')
        return
      }
      this.revokeAvatarPreview()
      this.profileFile = file
      this.profileAvatar = URL.createObjectURL(file)
    },
    async saveProfile() {
      if (this.profileSaving) return
      const name = this.profileName.trim()
      if (!name) {
        ElMessage.warning('群名称不能为空')
        return
      }
      this.profileSaving = true
      try {
        let avatar = this.group.groupAvatar || ''
        if (this.profileFile) {
          this.profileUploading = true
          const upload = await uploadFile(this.profileFile)
          if (!upload || upload.code !== 200 || !upload.data) {
            throw new Error((upload && (upload.msg || upload.message)) || '头像上传失败')
          }
          avatar = upload.data
          this.profileUploading = false
        }
        const res = await updateGroupInfo({
          groupId: this.groupId,
          groupName: name,
          groupAvatar: avatar,
          description: this.profileDesc.trim(),
        })
        if (!res || res.code !== 200) {
          throw new Error((res && (res.msg || res.message)) || '群资料保存失败')
        }
        ElMessage.success('群资料已更新')
        this.revokeAvatarPreview()
        this.profileFile = null
        this.group = Object.assign({}, this.group, {
          groupName: name,
          groupAvatar: avatar,
          description: this.profileDesc.trim(),
        })
        this.profileAvatar = avatar
        this.confirmBusy = false
        this.forceCloseSheet()
        this.$emit('changed')
      } catch (e) {
        ElMessage.error((e && (e.message || e.msg)) || '群资料保存失败')
      } finally {
        this.profileSaving = false
        this.profileUploading = false
      }
    },

    // ===== 群公告 =====
    openAnnouncement() {
      if (!this.canManage) return
      this.announcementDraft = this.group.announcement || ''
      this.sheet = 'announce'
    },
    askDeleteAnnouncement() {
      if (!this.hasAnnouncement || this.announcing) return
      this.confirmKind = 'announce'
      this.confirmTitle = '删除群公告？'
      this.confirmDesc = '删除后群成员将看不到这条公告，且无法恢复。'
      this.confirmOk = '删除公告'
      this.syncSheetRect()
      this.sheet = 'confirm'
    },
    async saveAnnouncement(content) {
      if (this.announcing) return
      const text = (content == null ? '' : String(content)).trim()
      if (!text) {
        ElMessage.warning('公告内容不能为空，如需删除请用「删除公告」')
        return
      }
      this.announcing = true
      try {
        const wasPublished = this.hasAnnouncement
        const res = await updateAnnouncement(this.groupId, text)
        if (!res || res.code !== 200) {
          throw new Error((res && (res.msg || res.message)) || '群公告保存失败')
        }
        ElMessage.success(wasPublished ? '群公告已更新' : '群公告已发布')
        this.group = Object.assign({}, this.group, { announcement: text })
        this.announcementDraft = text
        this.forceCloseSheet()
        this.$emit('changed')
      } catch (e) {
        ElMessage.error((e && (e.message || e.msg)) || '群公告保存失败')
      } finally {
        this.announcing = false
      }
    },

    syncSheetRect() {
      const el = this.$refs.scroll
      if (!el) return
      const padTop = parseFloat(getComputedStyle(el).paddingTop) || 0
      this.sheetTop = el.scrollTop - padTop
      this.sheetHeight = el.clientHeight
    },

    // ===== 成员 =====
    openMember(member) {
      this.actionMember = member
      this.syncSheetRect()
      this.sheet = 'member'
    },
    handleMemberClick(member) {
      if (this.memberSelectMode) {
        this.toggleMemberSelection(member)
        return
      }
      this.openMember(member)
    },
    toggleMemberSelection(member) {
      if (!member) return
      const id = String(member.userId)
      const index = this.selectedMemberIds.indexOf(id)
      if (index === -1) {
        this.selectedMemberIds.push(id)
      } else {
        this.selectedMemberIds.splice(index, 1)
      }
    },
    isMemberSelected(userId) {
      return this.selectedMemberIds.indexOf(String(userId)) !== -1
    },
    toggleMemberSelectMode() {
      this.memberSelectMode = !this.memberSelectMode
      if (!this.memberSelectMode) this.selectedMemberIds = []
    },
    selectAllFiltered() {
      const ids = this.filteredMembers.map((m) => String(m.userId))
      this.selectedMemberIds = Array.from(new Set(this.selectedMemberIds.concat(ids)))
    },
    clearMemberSelection() {
      this.selectedMemberIds = []
    },
    canSetRoleFor(member, role) {
      if (!member) return false
      if (this.groupRole !== ROLE_OWNER) return false
      if (String(member.userId) === this.myUserId) return false
      if (member.owner || Number(member.role) === ROLE_OWNER) return false
      if (role === ROLE_ADMIN) return Number(member.role) !== ROLE_ADMIN
      if (role === 3) return Number(member.role) === ROLE_ADMIN
      return false
    },
    canKickMember(member) {
      if (!member || String(member.userId) === this.myUserId) return false
      if (this.groupRole === ROLE_OWNER) {
        return !member.owner && Number(member.role) !== ROLE_OWNER
      }
      if (this.groupRole === ROLE_ADMIN) {
        return Number(member.role) === 3
      }
      return false
    },
    openBatchRole(role) {
      if (!this.isOwner) {
        ElMessage.warning('只有群主可以设置管理员')
        return
      }
      const targets = role === ROLE_ADMIN ? this.batchAdminTargets : this.batchRemoveAdminTargets
      if (!targets.length) {
        ElMessage.warning('选中的成员里没有可操作对象')
        return
      }
      this.pendingBatchRole = role
      this.pendingBatchTargets = targets.slice()
      this.confirmKind = 'batch-role'
      this.confirmTitle = role === ROLE_ADMIN ? '批量设为管理员' : '批量取消管理员'
      this.confirmDesc = '将对选中的 ' + targets.length + ' 位成员' +
        (role === ROLE_ADMIN ? '开放管理员权限，可撤回消息、移出普通成员。' : '收回管理员权限，恢复为普通成员。')
      this.confirmOk = role === ROLE_ADMIN ? '设为管理员' : '取消管理员'
      this.confirmBusy = false
      this.syncSheetRect()
      this.sheet = 'confirm'
    },
    openBatchKick() {
      const targets = this.batchKickTargets
      if (!targets.length) {
        ElMessage.warning('选中的成员里没有可移出的对象')
        return
      }
      this.pendingBatchTargets = targets.slice()
      this.confirmKind = 'batch-kick'
      this.confirmTitle = '批量移出成员'
      this.confirmDesc = '将把选中的 ' + targets.length + ' 位成员移出群聊，历史消息仍会保留。'
      this.confirmOk = '移出群聊'
      this.confirmBusy = false
      this.syncSheetRect()
      this.sheet = 'confirm'
    },
    requestClose() {
      if (this.sheet) {
        this.closeSheet()
        return
      }
      this.$emit('close')
    },
    closeSheet() {
      if (this.confirmBusy) return
      this.mutePickerOpen = false
      if (this.sheet === 'profile' && (this.profileSaving || this.profileUploading)) return
      if (this.sheet === 'announce' && this.announcing) return
      if (this.sheet === 'confirm' && this.confirmKind === 'discard') {
        this.sheet = this.discardTargetSheet
        this.confirmKind = ''
        this.discardTargetSheet = ''
        return
      }
      if (this.sheet === 'profile' && this.profileDirty) {
        this.askDiscard('profile')
        return
      }
      if (this.sheet === 'announce' && this.announcementDirty) {
        this.askDiscard('announce')
        return
      }
      this.forceCloseSheet()
    },
    askDiscard(targetSheet) {
      this.discardTargetSheet = targetSheet
      this.confirmKind = 'discard'
      this.confirmTitle = targetSheet === 'profile' ? '放弃群资料修改？' : '放弃群公告修改？'
      this.confirmDesc = '当前有未保存的内容，关闭后会丢失。'
      this.confirmOk = '放弃修改'
      this.confirmBusy = false
      this.syncSheetRect()
      this.sheet = 'confirm'
    },
    forceCloseSheet() {
      if (this.sheet === 'profile') this.revokeAvatarPreview()
      this.sheet = ''
      this.actionMember = null
      this.confirmKind = ''
      this.pendingBatchRole = 0
      this.pendingBatchTargets = []
      this.discardTargetSheet = ''
    },
    cancelConfirm() {
      if (this.confirmBusy) return
      if (this.sheet === 'confirm' && this.confirmKind === 'discard') {
        this.sheet = this.discardTargetSheet
        this.confirmKind = ''
        this.discardTargetSheet = ''
        return
      }
      this.closeSheet()
    },
    sheetFocusables() {
      const sheet = this.$refs.sheetEl
      if (!sheet) return []
      const selector = [
        'a[href]',
        'button:not([disabled])',
        'input:not([disabled])',
        'textarea:not([disabled])',
        'select:not([disabled])',
        '[tabindex]:not([tabindex="-1"])',
      ].join(',')
      return Array.prototype.slice.call(sheet.querySelectorAll(selector)).filter((el) => {
        return el.offsetParent !== null || el === document.activeElement
      })
    },
    focusSheet() {
      const sheet = this.$refs.sheetEl
      if (!sheet) return
      const first = this.sheetFocusables()[0]
      const target = first || sheet
      if (target && target.focus) {
        target.focus({ preventScroll: true })
      }
    },
    restoreSheetFocus() {
      const previous = this.sheetReturnFocus
      this.sheetReturnFocus = null
      const root = this.$refs.gmRoot
      const fallback = (root && root.querySelector('.gm-hero-edit')) || document.querySelector('.gm-back')
      const target = previous && document.contains(previous) ? previous : fallback
      if (target && target.focus) {
        target.focus({ preventScroll: true })
      }
    },
    trapSheetFocus(event) {
      const sheet = this.$refs.sheetEl
      if (!sheet) return
      const focusables = this.sheetFocusables()
      if (!focusables.length) {
        event.preventDefault()
        sheet.focus({ preventScroll: true })
        return
      }
      const first = focusables[0]
      const last = focusables[focusables.length - 1]
      const active = document.activeElement
      if (!sheet.contains(active)) {
        event.preventDefault()
        first.focus({ preventScroll: true })
        return
      }
      if (event.shiftKey && active === first) {
        event.preventDefault()
        last.focus({ preventScroll: true })
      } else if (!event.shiftKey && active === last) {
        event.preventDefault()
        first.focus({ preventScroll: true })
      }
    },
    onWindowKeydown(event) {
      if (this.groupQrOpen && event.key === 'Escape') {
        event.preventDefault()
        event.stopPropagation()
        this.closeGroupQr()
        return
      }
      if (!this.sheet) return
      if (event.key === 'Escape') {
        event.preventDefault()
        event.stopPropagation()
        this.closeSheet()
        return
      }
      if (event.key === 'Tab') {
        this.trapSheetFocus(event)
      }
    },
    askSetRole(role) {
      if (!this.canSetAdmin) return
      if (role === ROLE_ADMIN) {
        this.doSetRole(ROLE_ADMIN)
        return
      }
      const target = this.actionMember
      if (!target) return
      this.confirmKind = 'role'
      this.confirmTitle = '取消管理员身份？'
      this.confirmDesc = '将收回「' + (target.username || '该成员') + '」的管理员权限，恢复为普通成员。'
      this.confirmOk = '取消管理员'
      this.confirmBusy = false
      this.syncSheetRect()
      this.sheet = 'confirm'
    },
    async doSetRole(role) {
      if (!this.actionMember || this.rolePending) return
      const target = this.actionMember
      this.rolePending = true
      try {
        const res = await setMemberRole(this.groupId, target.userId, role)
        if (!res || res.code !== 200) {
          throw new Error((res && (res.msg || res.message)) || '设置角色失败')
        }
        ElMessage.success(role === ROLE_ADMIN ? '已设为管理员' : '已取消管理员')
        target.role = role
        target.roleName = this.roleText(role)
        this.confirmBusy = false
        this.forceCloseSheet()
        this.$emit('changed')
      } catch (e) {
        ElMessage.error((e && (e.message || e.msg)) || '设置角色失败')
      } finally {
        this.rolePending = false
      }
    },

    // ===== 单人禁言 =====
    openMutePicker() {
      if (!this.canMuteAction || this.mutePending) return
      this.muteMinutes = this.actionMember && this.actionMember.muted ? 0 : 60
      this.mutePickerOpen = true
    },
    async doMute() {
      if (!this.actionMember || this.mutePending || !this.canMuteAction) return
      const target = this.actionMember
      this.mutePending = true
      try {
        const res = await muteMember({
          groupId: this.groupId,
          targetUserId: target.userId,
          durationMinutes: this.muteMinutes,
        })
        if (!res || res.code !== 200) {
          throw new Error((res && (res.msg || res.message)) || '禁言设置失败')
        }
        ElMessage.success(this.muteMinutes === 0 ? '已解除禁言' : '已禁言该成员')
        if (this.muteMinutes === 0) {
          target.muted = false
          target.muteUntil = ''
        } else {
          const until = new Date(Date.now() + this.muteMinutes * 60 * 1000)
          const pad = (n) => String(n).padStart(2, '0')
          target.muted = true
          target.muteUntil = until.getFullYear() + '-' + pad(until.getMonth() + 1) + '-' + pad(until.getDate())
              + ' ' + pad(until.getHours()) + ':' + pad(until.getMinutes()) + ':' + pad(until.getSeconds())
        }
        this.mutePickerOpen = false
        this.forceCloseSheet()
        this.$emit('changed')
      } catch (e) {
        ElMessage.error((e && (e.message || e.msg)) || '禁言设置失败')
      } finally {
        this.mutePending = false
      }
    },

    // ===== 邀请 =====
    async openInvite() {
      if (!this.canManage) return
      this.syncSheetRect()
      this.sheet = 'invite'
      this.friendKeyword = ''
      this.selectedIds = []
      await this.loadFriends()
    },
    async loadFriends() {
      this.friendsLoading = true
      this.friendsError = ''
      try {
        const res = await getFriendList()
        if (!res || res.code !== 200) {
          throw new Error((res && (res.msg || res.message)) || '好友列表加载失败')
        }
        this.friends = Array.isArray(res.data) ? res.data : []
      } catch (e) {
        this.friends = []
        this.friendsError = (e && (e.message || e.msg)) || '好友列表加载失败'
      } finally {
        this.friendsLoading = false
      }
    },
    toggleInvite(userId) {
      const id = String(userId)
      if (this.selectedIds.includes(id)) {
        this.selectedIds = this.selectedIds.filter((item) => item !== id)
      } else {
        this.selectedIds = this.selectedIds.concat(id)
      }
    },
    async doInvite() {
      if (!this.selectedIds.length || this.inviting) return
      this.inviting = true
      try {
        const res = await inviteMembers(this.groupId, this.selectedIds)
        if (!res || res.code !== 200) {
          throw new Error((res && (res.msg || res.message)) || '邀请失败')
        }
        const count = res.data == null ? this.selectedIds.length : res.data
        ElMessage.success('已邀请 ' + count + ' 人加入群聊')
        this.sheet = ''
        this.selectedIds = []
        await this.load()
        this.$emit('changed')
      } catch (e) {
        ElMessage.error((e && (e.message || e.msg)) || '邀请失败')
      } finally {
        this.inviting = false
      }
    },

    // ===== 破坏性操作 =====
    askKick() {
      const target = this.actionMember
      if (!target || !this.canKickAction) return
      this.confirmKind = 'kick'
      this.confirmTitle = '移出「' + (target.username || '该成员') + '」？'
      this.confirmDesc = '该成员将被移出群聊，历史消息仍保留。'
      this.confirmOk = '移出群聊'
      this.confirmBusy = false
      this.syncSheetRect()
      this.sheet = 'confirm'
    },
    askTransfer() {
      const target = this.actionMember
      if (!target || !this.canTransfer) return
      this.confirmKind = 'transfer'
      this.confirmTitle = '把群主转让给「' + (target.username || '该成员') + '」？'
      this.confirmDesc = '转让后你将成为普通成员，新群主可以设置管理员、移出成员或解散群聊。'
      this.confirmOk = '转让群主'
      this.confirmBusy = false
      this.syncSheetRect()
      this.sheet = 'confirm'
    },
    askQuit() {
      this.confirmKind = 'quit'
      this.confirmTitle = '退出「' + (this.group.groupName || '该群聊') + '」？'
      this.confirmDesc = '退出后你不再接收该群消息，群主和管理员可重新邀请你。'
      this.confirmOk = '退出群聊'
      this.confirmBusy = false
      this.syncSheetRect()
      this.sheet = 'confirm'
    },
    askDisband() {
      this.confirmKind = 'disband'
      this.confirmTitle = '解散「' + (this.group.groupName || '该群聊') + '」？'
      this.confirmDesc = '群聊将被解散，全部成员移出，且无法恢复。'
      this.confirmOk = '确认解散'
      this.confirmBusy = false
      this.syncSheetRect()
      this.sheet = 'confirm'
    },
    async runConfirm() {
      if (this.confirmBusy) return
      if (this.confirmKind === 'discard') {
        this.forceCloseSheet()
        return
      }
      this.confirmBusy = true
      try {
        if (this.confirmKind === 'batch-role') {
          await this.runBatchRole()
          this.confirmBusy = false
          return
        }
        if (this.confirmKind === 'batch-kick') {
          await this.runBatchKick()
          this.confirmBusy = false
          return
        }

        let res
        if (this.confirmKind === 'kick') {
          if (!this.actionMember) throw new Error('成员信息已失效')
          res = await kickMember(this.groupId, this.actionMember.userId)
        } else if (this.confirmKind === 'role') {
          if (!this.actionMember) throw new Error('成员信息已失效')
          res = await setMemberRole(this.groupId, this.actionMember.userId, 3)
        } else if (this.confirmKind === 'transfer') {
          if (!this.actionMember) throw new Error('成员信息已失效')
          res = await transferOwner(this.groupId, this.actionMember.userId)
        } else if (this.confirmKind === 'announce') {
          res = await updateAnnouncement(this.groupId, '')
        } else if (this.confirmKind === 'quit') {
          res = await quitGroup(this.groupId)
        } else if (this.confirmKind === 'disband') {
          res = await disbandGroup(this.groupId)
        } else if (this.confirmKind === 'file-delete') {
          if (!this.pendingDeleteFile) throw new Error('文件信息已失效')
          res = await deleteGroupFile(this.pendingDeleteFile.id)
        } else {
          throw new Error('未知操作')
        }
        if (!res || res.code !== 200) {
          throw new Error((res && (res.msg || res.message)) || '操作失败')
        }

        if (this.confirmKind === 'kick') {
          const target = this.actionMember
          const id = String(target.userId)
          ElMessage.success('已移出群聊')
          this.members = this.members.filter((m) => String(m.userId) !== id)
          this.selectedMemberIds = this.selectedMemberIds.filter((item) => item !== id)
          this.group = Object.assign({}, this.group, { currentMember: this.members.length })
          // closeSheet 在 confirmBusy 时会拒绝关闭，必须先释放锁。
          this.confirmBusy = false
          this.forceCloseSheet()
          this.$emit('changed')
          return
        }

        if (this.confirmKind === 'role') {
          const target = this.actionMember
          ElMessage.success('已取消管理员')
          target.role = 3
          target.roleName = this.roleText(3)
          this.confirmBusy = false
          this.forceCloseSheet()
          this.$emit('changed')
          return
        }

        if (this.confirmKind === 'transfer') {
          ElMessage.success('群主已转让')
          this.confirmBusy = false
          this.forceCloseSheet()
          await this.load()
          this.$emit('changed')
          return
        }

        if (this.confirmKind === 'announce') {
          ElMessage.success('群公告已删除')
          this.group = Object.assign({}, this.group, { announcement: '' })
          this.announcementDraft = ''
          this.confirmBusy = false
          this.forceCloseSheet()
          this.$emit('changed')
          return
        }

        if (this.confirmKind === 'file-delete') {
          ElMessage.success('群文件已删除')
          this.confirmBusy = false
          this.pendingDeleteFile = null
          this.forceCloseSheet()
          await this.loadFiles(true)
          return
        }

        ElMessage.success(this.confirmKind === 'quit' ? '已退出群聊' : '群聊已解散')
        this.confirmBusy = false
        this.sheet = ''
        this.$emit('exit')
      } catch (e) {
        this.confirmBusy = false
        ElMessage.error((e && (e.message || e.msg)) || '操作失败')
      }
    },
    async runBatchRole() {
      const role = this.pendingBatchRole
      const targets = (this.pendingBatchTargets || []).slice()
      if (!targets.length) throw new Error('没有可操作的成员')
      const results = await Promise.allSettled(targets.map((member) =>
        setMemberRole(this.groupId, member.userId, role)
      ))
      const succeeded = []
      const failed = []
      results.forEach((result, index) => {
        const target = targets[index]
        if (result.status === 'fulfilled' && result.value && result.value.code === 200) {
          target.role = role
          target.roleName = this.roleText(role)
          succeeded.push(target)
        } else {
          failed.push(target)
        }
      })
      const action = role === ROLE_ADMIN ? '设为管理员' : '取消管理员'
      if (failed.length) {
        ElMessage.warning(`已${action} ${succeeded.length} 人，${failed.length} 人失败，请重试`)
      } else {
        ElMessage.success(`已${action} ${succeeded.length} 人`)
      }
      const failedIds = failed.map((member) => String(member.userId))
      this.selectedMemberIds = this.selectedMemberIds.filter((id) => failedIds.indexOf(id) !== -1)
      this.forceCloseSheet()
      this.$emit('changed')
    },
    async runBatchKick() {
      const targets = (this.pendingBatchTargets || []).slice()
      if (!targets.length) throw new Error('没有可操作的成员')
      // 批量场景屏蔽单条接口的全局报错，避免 N 条失败弹 N 个提示；只保留末尾汇总
      const results = await Promise.allSettled(targets.map((member) =>
        kickMember(this.groupId, member.userId, { silent: true })
      ))
      const okIds = []
      const failed = []
      results.forEach((result, index) => {
        const target = targets[index]
        if (result.status === 'fulfilled' && result.value && result.value.code === 200) {
          okIds.push(String(target.userId))
        } else {
          failed.push(target)
        }
      })
      this.members = this.members.filter((m) => okIds.indexOf(String(m.userId)) === -1)
      this.group = Object.assign({}, this.group, { currentMember: this.members.length })
      const failedIds = failed.map((member) => String(member.userId))
      this.selectedMemberIds = this.selectedMemberIds.filter((id) => failedIds.indexOf(id) !== -1)
      if (failed.length) {
        ElMessage.warning(`已移出 ${okIds.length} 人，${failed.length} 人失败，请重试`)
      } else {
        ElMessage.success(`已移出 ${okIds.length} 人`)
      }
      this.forceCloseSheet()
      this.$emit('changed')
    },
  }
}
</script>

<style scoped>
/* ===== 群管理 · iOS 分组式信息架构 ===== */
.group-manage {
  /* 覆盖层不能继承聊天页的玻璃 token：--bg/--card 在玻璃主题下是半透明的，
     群管理需要自己的实色表面，否则会透出聊天内容。 */
  --bg: #f2f2f7;
  --bg-blur: rgba(249, 249, 249, .92);
  --bg-blur-strong: rgba(249, 249, 249, .96);
  --card: #ffffff;
  --fill: rgba(120, 120, 128, .14);
  --separator: rgba(60, 60, 67, .24);
  --label: #1c1c1e;
  --label-secondary: rgba(60, 60, 67, .8);
  --label-tertiary: rgba(60, 60, 67, .32);
  --accent: #007aff;
  --gm-content-width: 1120px;
  --gm-rail-width: 372px;
  --gm-surface-muted: #f7f7fa;

  position: fixed;
  inset: 0;
  z-index: 1000;
  display: block;
  overflow-y: auto;
  overscroll-behavior: contain;
  background:
    radial-gradient(1100px 620px at 88% -8%, rgba(0, 122, 255, .10), rgba(0, 122, 255, 0) 62%),
    radial-gradient(880px 520px at 6% 0%, rgba(88, 86, 214, .055), rgba(88, 86, 214, 0) 56%),
    var(--bg);
  color: var(--label);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

html[data-theme="dark"] .group-manage {
  --bg: #000000;
  --bg-blur: rgba(28, 28, 30, .92);
  --bg-blur-strong: rgba(28, 28, 30, .96);
  --card: #1c1c1e;
  --fill: rgba(120, 120, 128, .24);
  --separator: rgba(84, 84, 88, .6);
  --label: #ffffff;
  --label-secondary: rgba(235, 235, 245, .6);
  --label-tertiary: rgba(235, 235, 245, .3);
  --gm-surface-muted: #242427;
  background:
    radial-gradient(1100px 620px at 88% -8%, rgba(10, 132, 255, .16), rgba(10, 132, 255, 0) 62%),
    radial-gradient(880px 520px at 6% 0%, rgba(94, 92, 230, .10), rgba(94, 92, 230, 0) 56%),
    var(--bg);
}

.gm-nav {
  height: 52px;
  flex: 0 0 52px;
  display: flex;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 10;
  padding: 0 max(8px, calc((100% - var(--gm-content-width)) / 2 + 8px));
  background: var(--bg-blur);
  -webkit-backdrop-filter: saturate(180%) blur(28px);
  backdrop-filter: saturate(180%) blur(28px);
  border-bottom: 1px solid var(--separator);
}

.gm-back {
  width: 40px;
  height: 40px;
  border: 0;
  background: transparent;
  color: var(--accent, #007aff);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  cursor: pointer;
  transition: background .16s ease, transform .16s ease;
}
.gm-back:active { background: var(--fill); transform: scale(.94); }

.gm-nav-title {
  flex: 1;
  text-align: center;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -.3px;
}
.gm-nav-side { width: 40px; flex: 0 0 40px; }

.gm-scroll {
  flex: none;
  width: 100%;
  max-width: var(--gm-content-width);
  box-sizing: border-box;
  min-height: 0;
  margin: 0 auto;
  overflow: visible;
  overscroll-behavior: contain;
  padding: 22px 20px calc(30px + env(safe-area-inset-bottom));
  scrollbar-width: thin;
}
.gm-stage { position: relative; }
.group-manage.is-sheet-open { overflow: hidden; }
.gm-scroll.sheet-open { overflow: hidden; }

.gm-detail-warning {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 0 0 14px;
  padding: 10px 14px;
  border: 1px solid rgba(255, 159, 10, .34);
  border-radius: 12px;
  background: rgba(255, 159, 10, .10);
  color: #8a5100;
  font-size: 12px;
  line-height: 1.45;
}
html[data-theme="dark"] .gm-detail-warning {
  border-color: rgba(255, 179, 64, .28);
  background: rgba(255, 159, 10, .12);
  color: #ffb340;
}
.group-manage :focus-visible,
.gm-mask :focus-visible {
  outline: 3px solid rgba(0, 122, 255, .38);
  outline-offset: 2px;
}

/* 桌面双栏：左侧主内容，右侧资料与设置；窄屏自动退化为单列 */
.gm-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) var(--gm-rail-width);
  align-items: start;
  gap: 18px;
  margin-top: 18px;
}
.gm-main,
.gm-side {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.gm-main > .gm-card,
.gm-side > .gm-card { margin-top: 0; }
.gm-main > .gm-card:first-child { margin-top: 0; }

/* ===== 群身份卡 ===== */
.gm-hero {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 24px;
  background:
    radial-gradient(circle at 0 0, rgba(0, 122, 255, .12), rgba(0, 122, 255, 0) 52%),
    linear-gradient(135deg, rgba(255, 255, 255, .72), rgba(255, 255, 255, 0) 48%),
    var(--card);
  border: 1px solid var(--separator);
  border-radius: 22px;
  box-shadow: var(--shadow-sm);
  box-sizing: border-box;
}
html[data-theme="dark"] .gm-hero {
  background:
    radial-gradient(circle at 0 0, rgba(10, 132, 255, .18), rgba(10, 132, 255, 0) 52%),
    linear-gradient(135deg, rgba(255, 255, 255, .04), rgba(255, 255, 255, 0) 48%),
    var(--card);
}

.gm-hero-avatar,
.gm-member-avatar,
.gm-sheet-avatar,
.gm-invite-avatar {
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  color: #fff;
}

.gm-hero-avatar {
  width: 76px;
  height: 76px;
  border-radius: 22px;
  box-shadow: 0 8px 20px rgba(0, 122, 255, .22);
}
.gm-hero-avatar-text { font-size: 28px; font-weight: 700; }
.gm-hero-avatar img,
.gm-member-avatar img,
.gm-sheet-avatar img,
.gm-invite-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  border: 0;
}

.gm-hero-main { min-width: 0; flex: 1; }
.gm-hero-name-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}
.gm-hero-name {
  margin: 0;
  font-size: 22px;
  line-height: 1.25;
  font-weight: 700;
  letter-spacing: -.5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.gm-role-pill {
  flex: 0 0 auto;
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 11px;
  line-height: 1.4;
  font-weight: 600;
  background: var(--fill);
  color: var(--label-secondary);
}
.gm-role-pill.owner {
  background: rgba(255, 159, 10, .14);
  color: #c26a00;
}
.gm-role-pill.admin {
  background: rgba(0, 122, 255, .12);
  color: var(--accent, #007aff);
}

.gm-hero-meta {
  margin: 7px 0 0;
  font-size: 13px;
  color: var(--label-secondary);
  line-height: 1.5;
}
/* 全员禁言胶囊：跟在群主名后面，不新起一行 */
.gm-mute-inline {
  display: inline-flex;
  align-items: center;
  height: 20px;
  margin-left: 7px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(255, 159, 10, .16);
  color: #a55a00;
  font-size: 11px;
  font-weight: 600;
  vertical-align: 1px;
}
html[data-theme="dark"] .gm-mute-inline {
  background: rgba(255, 159, 10, .22);
  color: #ffb340;
}
.gm-hero-desc {
  margin: 9px 0 0;
  font-size: 13px;
  line-height: 1.55;
  color: var(--label-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.gm-hero-edit {
  flex: 0 0 auto;
  margin-left: auto;
  border: 0;
  border-radius: 999px;
  padding: 6px 14px;
  background: rgba(0, 122, 255, .1);
  color: var(--accent, #007aff);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
  cursor: pointer;
  transition: background .16s ease, transform .16s ease;
}
.gm-hero-edit:hover { background: rgba(0, 122, 255, .16); }
.gm-hero-edit:active { opacity: .65; transform: scale(.97); }

/* ===== 群公告 ===== */
.gm-announce { margin-top: 0; }
.gm-announce-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin: 0 0 2px 5px;
  border-radius: 50%;
  background: #ff9f0a;
  vertical-align: middle;
}
.gm-announce-body {
  margin: 2px 18px 0;
  padding: 14px 16px;
  border: 1px solid rgba(255, 159, 10, .16);
  border-radius: 14px;
  background:
    linear-gradient(180deg, rgba(255, 159, 10, .09), rgba(255, 159, 10, .035));
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--label);
  white-space: pre-wrap;
  word-break: break-word;
  box-sizing: border-box;
}
html[data-theme="dark"] .gm-announce-body {
  border-color: rgba(255, 159, 10, .24);
  background: linear-gradient(180deg, rgba(255, 159, 10, .14), rgba(255, 159, 10, .05));
}
.gm-announce-empty {
  margin: 2px 18px 0;
  padding: 16px;
  border-radius: 14px;
  background: var(--gm-surface-muted);
  font-size: 12.5px;
  line-height: 1.5;
  color: var(--label-secondary);
}
.gm-announce-empty.is-readonly { color: var(--label-secondary); }
.gm-announce-meta {
  padding: 10px 22px 16px;
  font-size: 11px;
  color: var(--label-secondary);
}

/* ===== 卡片 ===== */
.gm-card {
  margin-top: 0;
  background: var(--card);
  border: 1px solid var(--separator);
  border-radius: 18px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

/* ===== 入群申请 ===== */
.gm-join-list { padding: 0 18px 14px; display: flex; flex-direction: column; gap: 10px; }
.gm-join-row { display: flex; align-items: center; gap: 10px; padding: 10px; border-radius: 12px; background: rgba(0,0,0,.03); }
.gm-join-avatar { width: 34px; height: 34px; border-radius: 50%; background: #111; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 13px; flex: 0 0 auto; }
.gm-join-info { flex: 1; min-width: 0; }
.gm-join-name { font-size: 13px; font-weight: 600; color: var(--text); }
.gm-join-meta { font-size: 12px; color: var(--text-2); margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.gm-join-actions { display: flex; gap: 6px; }
.gm-mini-btn.ghost { background: transparent; color: var(--text-2); border: 1px solid var(--separator); }

.gm-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 48px;
  padding: 16px 18px 10px;
  box-sizing: border-box;
}
.gm-card-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--label);
  letter-spacing: -.2px;
}
.gm-card-count {
  margin-left: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--label-secondary);
}
.gm-text-btn {
  border: 0;
  background: rgba(0, 122, 255, .09);
  color: var(--accent, #007aff);
  font-size: 12px;
  font-weight: 600;
  padding: 6px 12px;
  cursor: pointer;
  border-radius: 999px;
  white-space: nowrap;
  transition: background .16s ease, transform .16s ease;
}
.gm-text-btn:hover { background: rgba(0, 122, 255, .15); }
.gm-text-btn:active { opacity: .65; transform: scale(.97); }
.gm-text-btn:disabled { opacity: .45; cursor: default; }
html[data-theme="dark"] .gm-text-btn {
  background: rgba(10, 132, 255, .18);
  color: #6cb2ff;
}

/* ===== 群 AI 摘要 ===== */
.gm-ai-card {
  background:
    radial-gradient(circle at 100% 0, rgba(0, 122, 255, .07), rgba(0, 122, 255, 0) 46%),
    var(--card);
}
html[data-theme="dark"] .gm-ai-card {
  background:
    radial-gradient(circle at 100% 0, rgba(10, 132, 255, .12), rgba(10, 132, 255, 0) 46%),
    var(--card);
}
.gm-ai-spark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  margin-left: 6px;
  border-radius: 50%;
  background: rgba(0, 122, 255, .12);
  color: #007aff;
  font-size: 11px;
  vertical-align: -1px;
}
.gm-ai-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px 18px;
  font-size: 13px;
  color: var(--label-secondary);
}
.gm-spinner {
  width: 16px;
  height: 16px;
  flex: none;
  border-radius: 50%;
  border: 2px solid rgba(0, 122, 255, .2);
  border-top-color: #007aff;
  animation: gm-spin 760ms linear infinite;
}
@keyframes gm-spin { to { transform: rotate(360deg); } }
.gm-ai-error {
  margin: 0 16px 16px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(255, 59, 48, .08);
  color: #c0392b;
  font-size: 12px;
}
.gm-ai-error p { margin: 0 0 8px; line-height: 1.5; }
html[data-theme="dark"] .gm-ai-error { color: #ff9f0a; }
.gm-mini-btn {
  border: 0;
  border-radius: 8px;
  padding: 5px 12px;
  background: rgba(0, 122, 255, .12);
  color: #007aff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.gm-ai-permission {
  color: var(--label-secondary);
  font-size: 12px;
  line-height: 1.4;
}
.gm-ai-summary {
  margin: 0;
  padding: 2px 18px 12px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--label);
  white-space: pre-wrap;
  word-break: break-word;
}
.gm-ai-todos {
  margin: 0 18px 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: var(--gm-surface-muted);
}
.gm-ai-todos-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--label-secondary);
  margin-bottom: 4px;
}
.gm-ai-todos-title span {
  color: var(--label-secondary);
  font-weight: 500;
  margin-left: 4px;
}
.gm-ai-todo {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 7px 0;
  border-top: 1px solid var(--separator);
}
.gm-ai-todo:first-of-type { border-top: 0; }
.gm-ai-todo-index {
  width: 18px;
  height: 18px;
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(0, 122, 255, .14);
  color: #007aff;
  font-size: 11px;
  font-weight: 700;
}
.gm-ai-todo-main {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--label);
  word-break: break-word;
}
.gm-ai-todo-meta {
  flex: none;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  max-width: 96px;
  font-size: 11px;
  color: var(--label-secondary);
}
.gm-ai-due { color: #007aff; }
.gm-ai-empty-todo {
  margin: 0 18px 10px;
  font-size: 12px;
  color: var(--label-secondary);
}
.gm-ai-meta {
  margin: 0;
  padding: 0 18px 16px;
  font-size: 11px;
  color: var(--label-secondary);
}
.gm-ai-idle {
  margin: 0 18px 18px;
  padding: 14px 16px;
  border: 1px dashed var(--separator);
  border-radius: 14px;
  background: var(--gm-surface-muted);
  font-size: 13px;
  color: var(--label-secondary);
  line-height: 1.65;
}
.gm-ai-idle p { margin: 0; }

/* ===== 群文件 ===== */
.gm-file-input { display: none; }
.gm-file-uploading {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 18px 10px;
  padding: 9px 12px;
  border-radius: 12px;
  background: rgba(0, 122, 255, .08);
  font-size: 12px;
  color: var(--label-secondary);
}
.gm-file-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 18px 18px;
  font-size: 13px;
  color: var(--label-secondary);
}
.gm-file-empty {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 18px 18px;
  padding: 16px;
  border: 1px dashed var(--separator);
  border-radius: 14px;
  background: var(--gm-surface-muted);
  text-align: left;
  color: var(--label-secondary);
}
.gm-file-empty-art {
  width: 44px;
  height: 44px;
  flex: none;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--card);
  box-shadow: var(--shadow-sm);
  color: var(--label-secondary);
}
.gm-file-empty p { margin: 0; font-size: 12.5px; line-height: 1.55; }
.gm-file-list { padding: 0 18px 12px; }
.gm-file-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 2px;
  border-top: 1px solid var(--separator);
}
.gm-file-row:first-child { border-top: 0; }
.gm-file-icon {
  width: 40px;
  height: 40px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: rgba(0, 122, 255, .1);
  color: #007aff;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: .2px;
  text-transform: uppercase;
}
.gm-file-icon.ext-image { background: rgba(52, 199, 89, .12); color: #2a9d4a; }
.gm-file-icon.ext-pdf { background: rgba(255, 59, 48, .1); color: #d70015; }
.gm-file-icon.ext-doc { background: rgba(0, 122, 255, .1); color: #0066cc; }
.gm-file-icon.ext-sheet { background: rgba(52, 199, 89, .14); color: #248a3d; }
.gm-file-icon.ext-zip { background: rgba(255, 149, 0, .13); color: #c93400; }
.gm-file-icon.ext-video { background: rgba(88, 86, 214, .12); color: #5856d6; }
.gm-file-icon.ext-audio { background: rgba(255, 45, 85, .1); color: #d70038; }
.gm-file-icon.ext-other { background: var(--fill); color: var(--label-secondary); }
.gm-file-main { flex: 1; min-width: 0; }
.gm-file-name {
  display: block;
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
  font-size: 14px;
  font-weight: 600;
  color: var(--label);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}
.gm-file-name:hover { color: #007aff; }
.gm-file-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 10px;
  margin-top: 3px;
  font-size: 11px;
  color: var(--label-secondary);
}
.gm-file-action {
  width: 34px;
  height: 34px;
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--label-secondary);
  cursor: pointer;
}
.gm-file-action:hover { background: var(--fill); color: #007aff; }
.gm-file-action.danger:hover { background: rgba(255, 59, 48, .1); color: #d70015; }
.gm-file-more {
  width: 100%;
  margin-top: 4px;
  padding: 10px;
  border: 0;
  border-top: 1px solid var(--separator);
  background: transparent;
  color: #007aff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.gm-file-more:disabled { opacity: .5; cursor: default; }
.gm-file-end {
  margin: 6px 0 0;
  padding-top: 8px;
  border-top: 1px solid var(--separator);
  text-align: center;
  font-size: 11px;
  color: var(--label-secondary);
}
@media (prefers-reduced-motion: reduce) {
  .gm-spinner { animation: none; }
}

/* 成员网格：4 列，头像 + 名字 + 角色，触碰区不小于 72px */
.gm-member-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  padding: 2px 18px 18px;
}
.gm-member {
  min-width: 0;
  border: 0;
  background: var(--gm-surface-muted);
  padding: 14px 8px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  border-radius: 14px;
  cursor: pointer;
  color: inherit;
  transition: background .16s ease, transform .16s ease, box-shadow .16s ease;
}
.gm-member:hover {
  background: var(--card-highlight);
  box-shadow: var(--shadow-sm);
}
.gm-member:active { transform: scale(.97); }
.gm-member-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  font-size: 19px;
  font-weight: 600;
  box-shadow: inset 0 0 0 1px rgba(60, 60, 67, .14);
}
.gm-member-avatar-text { font-size: 19px; font-weight: 600; }
.gm-avatar-badge {
  position: absolute;
  right: 0;
  bottom: 0;
  min-width: 17px;
  height: 17px;
  padding: 0 3px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 9px;
  font-weight: 700;
  color: #fff;
  border: 1.5px solid var(--card);
  box-sizing: border-box;
  z-index: 2;
}
.gm-avatar-badge.owner { background: #ff9f0a; }
.gm-avatar-badge.admin { background: #007aff; }

.gm-member-name {
  width: 100%;
  margin-top: 2px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.4;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--label);
}
.gm-member-role {
  font-size: 10px;
  line-height: 1.2;
  color: var(--label-secondary);
  white-space: nowrap;
}
.gm-member-add {
  border: 1px dashed var(--separator);
  background: transparent;
}
.gm-member-add:hover {
  background: rgba(0, 122, 255, .05);
  box-shadow: none;
}
.gm-member-add .gm-add-circle {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  color: var(--label-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--fill);
  box-sizing: border-box;
}

/* ===== 资料行 ===== */
.gm-rows { padding: 0 18px 10px; }
.gm-row {
  min-height: 46px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-top: 1px solid var(--separator);
  font-size: 13px;
}
.gm-row:first-child { border-top: 0; }
.gm-row-label {
  flex: 0 0 auto;
  color: var(--label-secondary);
}
.gm-row-value {
  flex: 1;
  min-width: 0;
  text-align: right;
  color: var(--label);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.gm-mono {
  font-family: 'SF Mono', 'Menlo', 'Consolas', monospace;
  font-size: 11.5px;
  color: var(--label-secondary);
}
.gm-copy {
  flex: 0 0 auto;
  border: 0;
  background: var(--fill);
  color: var(--accent, #007aff);
  font-size: 11px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 7px;
  cursor: pointer;
}
.gm-copy:active { opacity: .55; }
.gm-row-toggle { justify-content: space-between; }

/* 全员禁言：主标题 + 一句规则说明；开关在右，窄屏也不换行 */
.gm-row-mute {
  padding: 8px 0;
  align-items: center;
}
.gm-row-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.gm-row-sub {
  font-size: 11px;
  line-height: 1.35;
  color: var(--label-secondary);
}
.gm-switch.pending {
  opacity: .6;
  cursor: default;
}

/* iOS 开关 */
.gm-switch {
  width: 46px;
  height: 28px;
  flex: 0 0 46px;
  border: 0;
  border-radius: 14px;
  background: rgba(120, 120, 128, .18);
  cursor: pointer;
  transition: background .2s ease;
  padding: 3px;
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
}
.gm-switch.on { background: #34c759; }
.gm-switch-knob {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, .22);
  transition: margin-left .2s cubic-bezier(.32, .72, 0, 1);
  margin-left: 0;
}
.gm-switch.on .gm-switch-knob { margin-left: 18px; }

.gm-note {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  padding: 2px 6px 0;
  color: var(--label-secondary);
  font-size: 11.5px;
  line-height: 1.55;
}
.gm-note svg { flex: 0 0 auto; margin-top: 2px; }

.gm-danger-card {
  margin-top: 0;
  border-color: rgba(255, 59, 48, .18);
  background:
    linear-gradient(180deg, rgba(255, 59, 48, .055), rgba(255, 59, 48, .02)),
    var(--card);
}
html[data-theme="dark"] .gm-danger-card {
  border-color: rgba(255, 69, 58, .22);
  background:
    linear-gradient(180deg, rgba(255, 69, 58, .11), rgba(255, 69, 58, .03)),
    var(--card);
}
.gm-danger-btn {
  width: 100%;
  height: 52px;
  border: 0;
  background: transparent;
  color: #ff3b30;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background .16s ease;
}
.gm-danger-btn:active { background: rgba(255, 59, 48, .08); }
.gm-foot {
  margin: 18px 0 0;
  text-align: center;
  font-size: 11px;
  color: var(--label-secondary);
}

/* ===== 骨架与错误态 ===== */
.gm-skeleton {
  background: var(--card);
  border-radius: 16px;
  padding: 16px 14px;
  margin-bottom: 12px;
}
.gm-skeleton-hero { display: flex; gap: 12px; align-items: center; }
.gm-skeleton-avatar {
  width: 62px; height: 62px; border-radius: 16px;
  background: var(--fill); flex: 0 0 auto;
  animation: gm-pulse 1.3s ease-in-out infinite;
}
.gm-skeleton-lines { flex: 1; }
.gm-skeleton-line {
  height: 12px; border-radius: 6px; background: var(--fill);
  animation: gm-pulse 1.3s ease-in-out infinite;
}
.gm-skeleton-line.w30 { width: 30%; }
.gm-skeleton-line.w45 { width: 45%; }
.gm-skeleton-line.w55 { width: 55%; }
.gm-skeleton-line.w65 { width: 65%; margin-top: 10px; }
.gm-skeleton-grid {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 12px; margin-top: 16px;
}
.gm-skeleton-cell {
  height: 64px; border-radius: 12px; background: var(--fill);
  animation: gm-pulse 1.3s ease-in-out infinite;
}
.gm-skeleton-card.short { height: 72px; }
@keyframes gm-pulse { 0%, 100% { opacity: .5; } 50% { opacity: 1; } }

.gm-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 24px;
  color: var(--label-secondary);
}
.gm-state-icon { color: var(--label-tertiary); margin-bottom: 10px; }
.gm-state-title { font-size: 15px; font-weight: 600; color: var(--label); }
.gm-state-desc { margin-top: 6px; font-size: 12px; line-height: 1.5; max-width: 240px; }
.gm-retry {
  margin-top: 16px;
  border: 0;
  background: var(--accent, #007aff);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  padding: 9px 18px;
  border-radius: 10px;
  cursor: pointer;
}

/* ===== v11：成员筛选 / 批量、文件工具条、移动端快捷导航、焦点 ===== */
.gm-card-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 0 0 auto;
}
.gm-mobile-nav { display: none; }

.gm-member-toolbar {
  display: flex;
  flex-direction: column;
  gap: 9px;
  padding: 0 18px 12px;
}
.gm-member-search,
.gm-file-search {
  display: flex;
  align-items: center;
  gap: 7px;
  height: 36px;
  padding: 0 10px;
  border-radius: 10px;
  background: var(--gm-surface-muted);
  color: var(--label-secondary);
  box-sizing: border-box;
}
.gm-member-search:focus-within,
.gm-file-search:focus-within {
  box-shadow: 0 0 0 2px rgba(0, 122, 255, .2);
  background: var(--card);
}
.gm-member-search input,
.gm-file-search input {
  flex: 1;
  min-width: 0;
  height: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--label);
  font-size: 13px;
  font-family: inherit;
}
.gm-member-search input::placeholder,
.gm-file-search input::placeholder { color: var(--label-secondary); }
.gm-file-search-clear {
  width: 22px;
  height: 22px;
  border: 0;
  border-radius: 50%;
  background: var(--fill);
  color: var(--label-secondary);
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  flex: none;
}
.gm-member-filters {
  display: flex;
  align-items: center;
  gap: 6px;
  overflow-x: auto;
  scrollbar-width: none;
}
.gm-member-filters::-webkit-scrollbar { display: none; }
.gm-filter-chip {
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 28px;
  padding: 0 10px;
  border: 0;
  border-radius: 999px;
  background: var(--gm-surface-muted);
  color: var(--label-secondary);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  transition: background .16s ease, color .16s ease;
}
.gm-filter-chip span {
  min-width: 16px;
  text-align: center;
  font-size: 10px;
  opacity: .72;
}
.gm-filter-chip.active {
  background: rgba(0, 122, 255, .12);
  color: var(--accent, #007aff);
}
html[data-theme="dark"] .gm-filter-chip.active {
  background: rgba(10, 132, 255, .2);
}
.gm-member-batchbar {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 0 18px 12px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(0, 122, 255, .08);
  font-size: 12px;
  color: var(--label-secondary);
}
.gm-member-batch-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.gm-member-batch-info strong { color: var(--accent, #007aff); font-size: 14px; }
.gm-batch-link {
  border: 0;
  background: transparent;
  color: var(--accent, #007aff);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  padding: 2px 0;
}
.gm-batch-link:disabled { opacity: .45; cursor: default; }
.gm-member-batch-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.gm-batch-btn {
  height: 30px;
  padding: 0 10px;
  border: 0;
  border-radius: 9px;
  background: var(--card);
  color: var(--accent, #007aff);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
}
.gm-batch-btn.danger { color: #ff3b30; }
.gm-batch-btn:disabled { opacity: .4; cursor: default; box-shadow: none; }
html[data-theme="dark"] .gm-batch-btn { background: rgba(255, 255, 255, .09); }
.gm-member-empty {
  padding: 8px 18px 22px;
  text-align: center;
  font-size: 12px;
  color: var(--label-secondary);
}
.gm-member { position: relative; }
.gm-member.selected {
  background: rgba(0, 122, 255, .1);
  box-shadow: inset 0 0 0 1.5px rgba(0, 122, 255, .5);
}
.gm-member-select {
  position: absolute;
  top: 7px;
  left: 7px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--card);
  box-shadow: 0 0 0 1px rgba(60, 60, 67, .22);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  z-index: 2;
}
.gm-member-select.checked { background: #34c759; box-shadow: 0 0 0 1.5px #34c759; }

.gm-file-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 18px 10px;
}
.gm-file-search { flex: 1; min-width: 0; }
.gm-file-type {
  height: 36px;
  max-width: 116px;
  border: 0;
  outline: 0;
  border-radius: 10px;
  padding: 0 8px;
  background: var(--gm-surface-muted);
  color: var(--label);
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
}
.gm-file-type:focus { box-shadow: 0 0 0 2px rgba(0, 122, 255, .2); }
.gm-file-card.is-file-dragging {
  box-shadow: inset 0 0 0 2px rgba(0, 122, 255, .45);
}
.gm-file-drop-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 18px 10px;
  padding: 12px;
  border: 1.5px dashed rgba(0, 122, 255, .5);
  border-radius: 12px;
  background: rgba(0, 122, 255, .07);
  color: var(--accent, #007aff);
  font-size: 12.5px;
  font-weight: 600;
}
.gm-file-upload-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6px; }
.gm-file-upload-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: var(--label);
  font-size: 12px;
}
.gm-file-upload-line span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.gm-file-upload-line strong { flex: none; color: var(--accent, #007aff); font-variant-numeric: tabular-nums; }
.gm-file-progress {
  height: 4px;
  border-radius: 2px;
  background: rgba(0, 122, 255, .16);
  overflow: hidden;
}
.gm-file-progress i {
  display: block;
  height: 100%;
  border-radius: 2px;
  background: var(--accent, #007aff);
  transition: width .18s linear;
}
.gm-file-cancel {
  flex: none;
  height: 28px;
  padding: 0 9px;
  border: 0;
  border-radius: 8px;
  background: var(--card);
  color: #ff3b30;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
}
.gm-file-cancel:disabled { color: var(--label-secondary); opacity: .55; cursor: default; }
html[data-theme="dark"] .gm-file-cancel { background: rgba(255, 255, 255, .1); }

#gm-announce,
#gm-files,
#gm-members,
#gm-profile,
#gm-settings,
#gm-danger { scroll-margin-top: 62px; }

@media (max-width: 1000px) {
  #gm-announce,
  #gm-files,
  #gm-members,
  #gm-profile,
  #gm-settings,
  #gm-danger { scroll-margin-top: 112px; }
  .gm-mobile-nav {
    display: flex;
    gap: 6px;
    overflow-x: auto;
    scrollbar-width: none;
    position: sticky;
    top: 52px;
    z-index: 6;
    margin: 10px 0 0;
    padding: 8px 0 8px;
    background: linear-gradient(180deg, var(--bg) 72%, rgba(242, 242, 247, 0));
  }
  .gm-mobile-nav::-webkit-scrollbar { display: none; }
  .gm-mobile-nav-chip {
    flex: none;
    height: 30px;
    padding: 0 12px;
    border: 0;
    border-radius: 999px;
    background: var(--card);
    color: var(--label-secondary);
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: var(--shadow-sm);
  }
  .gm-mobile-nav-chip.active {
    background: var(--accent, #007aff);
    color: #fff;
  }
}
html[data-theme="dark"] .gm-mobile-nav {
  background: linear-gradient(180deg, #101014 72%, rgba(16, 16, 20, 0));
}
html[data-theme="dark"] .gm-mobile-nav-chip { background: rgba(255, 255, 255, .1); }

@media (max-width: 720px) {
  .gm-member-toolbar,
  .gm-file-toolbar { padding-left: 14px; padding-right: 14px; }
  .gm-member-batchbar { margin-left: 14px; margin-right: 14px; }
  .gm-member-empty { padding-left: 14px; padding-right: 14px; }
  .gm-file-toolbar { flex-wrap: wrap; }
  .gm-file-search { flex-basis: 100%; }
  .gm-file-type { width: 100%; max-width: none; }
  .gm-file-drop-hint { margin-left: 14px; margin-right: 14px; }
  .gm-mobile-nav { margin-top: 8px; }
}

/* ===== 底部面板 ===== */
.gm-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(0, 0, 0, .36);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  --card: #ffffff;
  --fill: rgba(120, 120, 128, .14);
  --separator: rgba(60, 60, 67, .24);
  --label: #1c1c1e;
  --label-secondary: rgba(60, 60, 67, .8);
  --label-tertiary: rgba(60, 60, 67, .32);
  --accent: #007aff;
  color: var(--label);
}
html[data-theme="dark"] .gm-mask {
  --card: #1c1c1e;
  --fill: rgba(120, 120, 128, .24);
  --separator: rgba(84, 84, 88, .6);
  --label: #ffffff;
  --label-secondary: rgba(235, 235, 245, .6);
  --label-tertiary: rgba(235, 235, 245, .3);
}
.gm-sheet {
  width: 100%;
  max-width: 520px;
  max-height: 82vh;
  background: var(--card);
  border-radius: 20px 20px 0 0;
  padding: 6px 14px calc(12px + env(safe-area-inset-bottom));
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: none;
}
.gm-sheet-tall { max-height: 76vh; }
.gm-sheet-handle {
  width: 36px;
  height: 4px;
  border-radius: 2px;
  background: var(--separator);
  margin: 2px auto 8px;
  flex: 0 0 auto;
}

.gm-sheet-member {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 2px 16px;
}
.gm-sheet-avatar {
  width: 52px; height: 52px; border-radius: 14px;
}
.gm-sheet-avatar-text { font-size: 20px; font-weight: 600; }
.gm-sheet-member-info { min-width: 0; }
.gm-sheet-name {
  font-size: 16px; font-weight: 700;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.gm-sheet-sub {
  margin-top: 4px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 11px;
  color: var(--label-secondary);
}
.gm-sheet-role {
  padding: 2px 6px;
  border-radius: 6px;
  background: var(--fill);
  color: var(--label-secondary);
  font-weight: 600;
}
.gm-sheet-role.owner { background: rgba(255, 159, 10, .14); color: #c26a00; }
.gm-sheet-role.admin { background: rgba(0, 122, 255, .12); color: var(--accent, #007aff); }

.gm-sheet-actions {
  border-top: 1px solid var(--separator);
}
.gm-sheet-action {
  width: 100%;
  height: 50px;
  border: 0;
  border-bottom: 1px solid var(--separator);
  background: transparent;
  color: var(--accent, #007aff);
  font-size: 15px;
  cursor: pointer;
}
.gm-sheet-action.danger { color: #ff3b30; }
.gm-sheet-muted {
  color: #ff3b30;
  font-weight: 600;
}
.gm-mute-picker {
  margin-top: 18px;
  padding: 16px;
  border-radius: 14px;
  background: var(--fill);
}
.gm-mute-picker-title {
  margin-bottom: 12px;
  color: var(--label);
  font-size: 14px;
  font-weight: 600;
}
.gm-mute-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.gm-mute-chip {
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid var(--separator);
  border-radius: 10px;
  background: var(--card, #fff);
  color: var(--label);
  font-size: 14px;
  cursor: pointer;
}
.gm-mute-chip.active {
  border-color: var(--accent, #007aff);
  background: rgba(0, 122, 255, .1);
  color: var(--accent, #007aff);
  font-weight: 600;
}
.gm-mute-picker-actions {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}
.gm-mute-picker-cancel {
  margin-top: 0;
  flex: 0 0 96px;
}
.gm-mute-picker-actions .gm-primary-btn {
  flex: 1;
}
.gm-sheet-action.warn { color: #ff9f0a; }
.gm-sheet-action:active { background: var(--fill); }
.gm-sheet-action:disabled { opacity: .5; }
.gm-sheet-tip {
  padding: 14px 0;
  text-align: center;
  font-size: 12px;
  color: var(--label-secondary);
}
.gm-sheet-cancel {
  width: 100%;
  height: 50px;
  margin-top: 8px;
  border: 0;
  border-radius: 12px;
  background: var(--fill);
  color: var(--label);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  flex: 0 0 auto;
}
.gm-sheet-cancel:active { opacity: .6; }

/* 群资料 / 群公告表单 */
.gm-form {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  gap: 16px;
  padding: 2px 2px 0;
  overflow-y: auto;
}
.gm-form-avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 2px;
}
.gm-form-avatar {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 56px;
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  box-shadow: 0 6px 14px rgba(0, 122, 255, .18);
}
.gm-form-avatar img { width: 100%; height: 100%; object-fit: cover; display: block; }
.gm-form-avatar-info { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.gm-form-avatar-btn {
  align-self: flex-start;
  border: 0;
  border-radius: 8px;
  padding: 5px 10px;
  background: rgba(0, 122, 255, .1);
  color: var(--accent, #007aff);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.gm-form-avatar-btn:active { opacity: .6; }
.gm-form-avatar-btn:disabled { opacity: .45; }
.gm-form-tip { font-size: 10.5px; color: var(--label-secondary); }
.gm-file-input { display: none; }

.gm-field { display: block; position: relative; }
.gm-field-label {
  display: block;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--label-secondary);
  letter-spacing: .2px;
}
.gm-input,
.gm-textarea {
  width: 100%;
  margin-top: 7px;
  border: 0;
  border-radius: 10px;
  background: var(--fill);
  color: var(--label);
  font-size: 14px;
  font-family: inherit;
  box-sizing: border-box;
  outline: 0;
  transition: box-shadow .16s ease;
}
.gm-input { height: 42px; padding: 0 12px; }
.gm-textarea {
  padding: 10px 12px;
  line-height: 1.55;
  resize: none;
  display: block;
}
.gm-textarea-lg { min-height: 132px; }
.gm-input:focus,
.gm-textarea:focus { box-shadow: 0 0 0 3px rgba(0, 122, 255, .16); }
.gm-input::placeholder,
.gm-textarea::placeholder { color: var(--label-tertiary); }
.gm-counter {
  display: block;
  margin-top: 5px;
  text-align: right;
  font-size: 10px;
  color: var(--label-secondary);
}
.gm-announce-footer { display: flex; gap: 10px; }
.gm-secondary-btn {
  flex: 0 0 108px;
  height: 46px;
  border: 0;
  border-radius: 12px;
  background: var(--fill);
  color: #ff3b30;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.gm-secondary-btn:active:not(:disabled) { opacity: .7; }
.gm-secondary-btn:disabled { opacity: .4; }
.gm-announce-footer .gm-primary-btn { flex: 1; }

/* 邀请面板 */
.gm-sheet-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 2px 2px 10px;
}
.gm-sheet-title { font-size: 17px; font-weight: 700; letter-spacing: -.3px; }
.gm-sheet-desc { margin-top: 3px; font-size: 11px; color: var(--label-secondary); }
.gm-sheet-close {
  width: 30px; height: 30px;
  border: 0; border-radius: 50%;
  background: var(--fill);
  color: var(--label-secondary);
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  flex: 0 0 auto;
}

.gm-invite-search {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 10px;
  margin: 0 2px 8px;
  border-radius: 10px;
  background: var(--fill);
  color: var(--label-tertiary);
  flex: 0 0 auto;
}
.gm-invite-search input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--label);
  font-size: 13px;
}
.gm-invite-search input::placeholder { color: var(--label-tertiary); }

.gm-invite-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  margin: 0 -4px;
  padding: 0 4px;
}
.gm-invite-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 160px;
  text-align: center;
  font-size: 12px;
  color: var(--label-secondary);
}
.gm-invite-state-sub { font-size: 10.5px; color: var(--label-secondary); }
.gm-inline-retry {
  border: 0; background: transparent;
  color: var(--accent, #007aff);
  font-size: 12px; font-weight: 600;
  cursor: pointer;
}
.gm-invite-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 54px;
  padding: 6px 2px;
  border: 0;
  border-bottom: 1px solid var(--separator);
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
}
.gm-invite-item:last-child { border-bottom: 0; }
.gm-invite-item:active { background: var(--fill); }
.gm-invite-avatar {
  width: 38px; height: 38px; border-radius: 10px;
}
.gm-invite-avatar-text { font-size: 15px; font-weight: 600; }
.gm-invite-name {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.gm-check {
  width: 22px; height: 22px;
  border-radius: 50%;
  border: 1.5px solid var(--separator);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex: 0 0 auto;
  box-sizing: border-box;
}
.gm-check.checked { background: #34c759; border-color: #34c759; }

.gm-sheet-footer {
  flex: 0 0 auto;
  padding: 10px 2px 0;
}
.gm-primary-btn {
  width: 100%;
  height: 46px;
  border: 0;
  border-radius: 12px;
  background: var(--accent, #007aff);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}
.gm-primary-btn:disabled { opacity: .4; cursor: default; }
.gm-primary-btn:active:not(:disabled) { opacity: .8; }

/* 确认面板 */
.gm-confirm { padding: 2px 4px 0; text-align: center; }
.gm-confirm-icon {
  width: 44px; height: 44px;
  margin: 2px auto 10px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 122, 255, .1);
  color: var(--accent, #007aff);
}
.gm-confirm-icon.danger { background: rgba(255, 59, 48, .1); color: #ff3b30; }
.gm-confirm-title { font-size: 17px; font-weight: 700; letter-spacing: -.3px; }
.gm-confirm-desc {
  margin: 7px auto 16px;
  max-width: 300px;
  font-size: 12px;
  line-height: 1.55;
  color: var(--label-secondary);
}
.gm-confirm-ok {
  width: 100%;
  height: 46px;
  border: 0;
  border-radius: 12px;
  background: var(--fill);
  color: var(--accent, #007aff);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}
.gm-confirm-ok.danger { background: #ff3b30; color: #fff; }
.gm-confirm-ok:disabled { opacity: .6; cursor: default; }
.gm-confirm-cancel { margin-top: 8px; }

/* 面板进出场 */
.gm-sheet-fade-enter-active, .gm-sheet-fade-leave-active {
  transition: opacity .2s ease;
}
.gm-sheet-fade-enter-from, .gm-sheet-fade-leave-to { opacity: 0; }

/* 1200 以下收窄右栏，保持双栏，避免设置区被推到主栏之后 */
@media (max-width: 1200px) {
  .group-manage { --gm-rail-width: 340px; }
}

/* 1000 以下退化为单列：先公告/成员，再资料/设置 */
@media (max-width: 1000px) {
  .gm-layout {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
    margin-top: 16px;
  }
  .gm-main,
  .gm-side { gap: 16px; }
  .gm-side { margin-top: 2px; }
  .gm-note { padding: 2px 6px 0; }
}

@media (max-width: 720px) {
  .gm-scroll { padding: 12px 12px calc(20px + env(safe-area-inset-bottom)); }
  .gm-hero { padding: 16px; gap: 14px; border-radius: 18px; }
  .gm-hero-avatar { width: 64px; height: 64px; border-radius: 18px; }
  .gm-hero-avatar-text { font-size: 24px; }
  .gm-hero-name { font-size: 19px; }
  .gm-card { border-radius: 16px; }
  .gm-card-head { padding: 14px 14px 8px; }
  .gm-layout,
  .gm-main,
  .gm-side { gap: 12px; }
  .gm-announce-body,
  .gm-announce-empty,
  .gm-file-empty,
  .gm-ai-idle {
    margin-left: 14px;
    margin-right: 14px;
  }
  .gm-announce-body,
  .gm-announce-empty,
  .gm-file-empty { padding: 13px 14px; }
  .gm-ai-idle { padding: 12px 14px; margin-bottom: 14px; }
  .gm-announce-meta,
  .gm-ai-summary,
  .gm-ai-meta { padding-left: 18px; padding-right: 18px; }
  .gm-ai-error,
  .gm-ai-todos { margin-left: 14px; margin-right: 14px; }
  .gm-file-uploading { margin-left: 14px; margin-right: 14px; }
  .gm-file-loading { padding-left: 18px; padding-right: 18px; }
  .gm-file-list { padding-left: 14px; padding-right: 14px; }
  .gm-member-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
    padding-left: 14px;
    padding-right: 14px;
  }
  .gm-rows { padding-left: 14px; padding-right: 14px; }
}

@media (max-width: 420px) {
  .gm-member-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .gm-hero { position: relative; align-items: flex-start; }
  .gm-hero-main { padding-right: 46px; }
  .gm-hero-name { font-size: 18px; letter-spacing: -.3px; }
  .gm-hero-name-row { gap: 6px; }
  .gm-role-pill { padding: 2px 8px; font-size: 10px; }
  .gm-hero-edit {
    position: absolute;
    top: 14px;
    right: 14px;
    margin-left: 0;
    padding: 5px 10px;
    font-size: 11px;
  }
}

/* 深色模式：卡片阴影在深色下几乎不可见，用描边补层级 */
@media (prefers-color-scheme: dark) {
  .gm-hero, .gm-card {
    box-shadow: 0 0 0 1px rgba(255, 255, 255, .04) inset;
  }
}

/* 尊重系统的减少动态偏好 */
@media (prefers-reduced-motion: reduce) {
  .group-manage { animation: none; }
  .gm-sheet { animation: none; }
  .gm-switch-knob { transition: none; }
}

.gm-hero-qr {
  margin-left: 8px;
  padding: 0 12px;
  white-space: nowrap;
}

.gm-qr-mask {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.42);
  backdrop-filter: blur(6px);
}

.gm-qr-dialog {
  width: min(620px, 100%);
  background: var(--gm-surface, #ffffff);
  border: 1px solid var(--gm-border, rgba(0, 0, 0, 0.08));
  border-radius: 22px;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.22);
  overflow: hidden;
}

.gm-qr-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px 16px;
}
.gm-qr-title {
  font-size: 19px;
  font-weight: 650;
  color: var(--gm-text, #111111);
}
.gm-qr-subtitle {
  margin-top: 6px;
  font-size: 12.5px;
  line-height: 1.55;
  color: var(--gm-text-secondary, #777777);
}
.gm-qr-close {
  flex: none;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--gm-text-secondary, #777777);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}
.gm-qr-close:hover {
  background: rgba(0, 0, 0, 0.06);
}
.gm-qr-body {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 24px;
  align-items: center;
  padding: 8px 24px 24px;
}
.gm-qr-frame {
  width: 260px;
  height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: #f6f7f9;
  border: 1px solid rgba(0, 0, 0, 0.05);
}
.gm-qr-img {
  width: 236px;
  height: 236px;
  display: block;
  border-radius: 8px;
}
.gm-qr-loading {
  font-size: 13px;
  color: #888888;
}
.gm-qr-info {
  min-width: 0;
}
.gm-qr-group {
  font-size: 18px;
  font-weight: 650;
  color: var(--gm-text, #111111);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.gm-qr-no {
  margin-top: 6px;
  font-size: 12.5px;
  color: var(--gm-text-secondary, #888888);
}
.gm-qr-tip {
  margin-top: 14px;
  font-size: 12.5px;
  line-height: 1.65;
  color: var(--gm-text-secondary, #666666);
}
.gm-qr-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}
.gm-qr-btn {
  height: 38px;
  padding: 0 16px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
}
.gm-qr-btn.ghost {
  color: var(--gm-text, #444444);
  background: rgba(0, 0, 0, 0.04);
  border-color: rgba(0, 0, 0, 0.07);
}
.gm-qr-btn.primary {
  color: #ffffff;
  background: #111111;
}
@media (max-width: 640px) {
  .gm-qr-body {
    grid-template-columns: 1fr;
    justify-items: center;
    text-align: center;
  }
  .gm-qr-actions { justify-content: center; }
}
</style>
