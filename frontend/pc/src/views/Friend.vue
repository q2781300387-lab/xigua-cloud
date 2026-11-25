<template>
  <div class="main-container">

    <!-- 左侧联系人 -->
    <div class="left-container">
      <div class="left-content">
        <h1>联系人</h1>
        <p class="left-subtitle">好友、群聊与机器人 · 点击条目查看资料</p>

        <div class="contact-search">
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <circle cx="10.5" cy="10.5" r="6.5" stroke="currentColor" stroke-width="2"/>
            <path d="M15.5 15.5L20 20" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <input
            v-model="contactSearch"
            type="search"
            placeholder="搜索好友、群聊或机器人"
            aria-label="搜索联系人"
            @keydown.esc="contactSearch = ''"
          >
          <button v-if="contactSearch" type="button" class="search-clear" aria-label="清空搜索" @click="contactSearch = ''">×</button>
        </div>
        <div v-if="isSearching" class="search-summary">
          找到 {{ searchResultCount }} 个结果
        </div>

        <!-- 好友列表 -->
        <div class="dropdown-section">
          <div class="dropdown-header" @click="toggleFriendDropdown('friends')">
            <span>好友 ({{ isSearching ? filteredFriends.length : showCount(friendCount) }})</span>
            <span v-if="onlineFriendCount" class="header-tint">{{ onlineFriendCount }} 在线</span>
            <span class="arrow">{{ dropdowns.friends || isSearching ? '▼' : '▶' }}</span>
          </div>
          <div class="dropdown-content" v-show="dropdowns.friends || isSearching">
            <!-- 好友空态插画 -->
            <div v-if="friends.length === 0" class="friend-empty">
              <svg class="empty-art" viewBox="0 0 72 72" fill="none" aria-hidden="true">
                <defs>
                  <linearGradient id="friendGrad" x1="0" y1="0" x2="1" y2="1">
                    <stop offset="0" stop-color="#34c759" stop-opacity="0.16"/>
                    <stop offset="1" stop-color="#007aff" stop-opacity="0.08"/>
                  </linearGradient>
                </defs>
                <circle cx="36" cy="36" r="30" fill="url(#friendGrad)"/>
                <circle cx="36" cy="27" r="9.5" style="fill: var(--card)" stroke="currentColor" stroke-width="2.4"/>
                <path d="M21 55c0-8.4 6.7-13.5 15-13.5S51 46.6 51 55" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>
                <circle cx="58" cy="15" r="5.5" fill="#ff9500"/>
                <path d="M58 12v6M55 15h6" stroke="#fff" stroke-width="1.6" stroke-linecap="round"/>
              </svg>
              <p class="empty-tip">还没有好友</p>
              <p class="empty-sub">搜索添加你的第一位好友吧</p>
            </div>
            <div v-else-if="filteredFriends.length === 0" class="list-empty">
              没有匹配的好友
            </div>

            <div
              class="contact-subgroup"
              v-for="bucket in groupedFriends"
              :key="bucket.key"
            >
              <div class="subgroup-header">
                <i class="subgroup-dot" :class="bucket.online ? 'is-online' : 'is-offline'"></i>
                <span>{{ bucket.label }}</span>
                <span class="subgroup-count">{{ bucket.list.length }}</span>
              </div>
              <div
                class="contact-item is-clickable"
                :class="{ 'is-selected': selectedKey === 'friend-' + friend.userId }"
                v-for="friend in bucket.list"
                :key="friend.userId"
                role="button"
                tabindex="0"
                :aria-label="`查看 ${friend.username} 的个人资料`"
                @click="openFriendDetail(friend.userId)"
                @keydown.enter.prevent="openFriendDetail(friend.userId)"
                @keydown.space.prevent="openFriendDetail(friend.userId)"
                @contextmenu.prevent="showFriendClickMenu($event, friend.userId)"
              >
                <div class="avatar-placeholder" v-if="!friend.avatar">
                  {{ friend.username && friend.username.charAt(0) }}
                </div>
                <img v-else :src="friend.avatar" class="avatar" alt="好友头像">
                <div class="contact-body">
                  <span class="name">{{ friend.username }}</span>
                  <span class="contact-sub" :title="friend.signature || '暂无个性签名'">
                    {{ friend.signature || '暂无个性签名' }}
                  </span>
                </div>
                <span class="presence" :class="friend.isOnline ? 'is-online' : 'is-offline'">
                  <i class="presence-dot"></i>{{ friend.isOnline ? '在线' : '离线' }}
                </span>
                <span class="row-chevron" aria-hidden="true">›</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 群聊列表 -->
        <div class="dropdown-section">
          <div class="dropdown-header" @click="toggleGroupDropdown('groups')">
            <span>群聊 ({{ isSearching ? filteredGroups.length : showCount(groupCount) }})</span>
            <span class="arrow">{{ dropdowns.groups || isSearching ? '▼' : '▶' }}</span>
          </div>
          <div class="dropdown-content" v-show="dropdowns.groups || isSearching">
            <div v-if="groups.length === 0" class="list-empty">还没有加入群聊</div>
            <div v-else-if="filteredGroups.length === 0" class="list-empty">没有匹配的群聊</div>
            <div
              class="contact-item is-clickable"
              :class="{ 'is-selected': selectedKey === 'group-' + group.groupId }"
              v-for="group in filteredGroups"
              :key="group.groupId"
              role="button"
              tabindex="0"
              :aria-label="`查看群聊 ${group.groupName} 的群信息`"
              @click="openGroupDetail(group.groupId)"
              @keydown.enter.prevent="openGroupDetail(group.groupId)"
              @keydown.space.prevent="openGroupDetail(group.groupId)"
              @contextmenu.prevent="showGroupClickMenu($event, group.groupId)"
            >
              <div class="avatar-placeholder group" v-if="!group.groupAvatar">
                {{ group.groupName && group.groupName.charAt(0) }}
              </div>
              <img v-else :src="group.groupAvatar" class="avatar" alt="群聊头像">
              <div class="contact-body">
                <span class="name">{{ group.groupName }}</span>
                <span class="contact-sub">群聊</span>
              </div>
              <span class="member-count">{{ group.currentMember }} 人</span>
              <span class="row-chevron" aria-hidden="true">›</span>
            </div>
          </div>
        </div>

        <!-- 机器人列表 -->
        <div class="dropdown-section">
          <div class="dropdown-header" @click="toggleBotDropdown('bots')">
            <span>机器人 ({{ isSearching ? filteredBots.length : showCount(botCount) }})</span>
            <span class="arrow">{{ dropdowns.bots || isSearching ? '▼' : '▶' }}</span>
          </div>
          <div class="dropdown-content" v-show="dropdowns.bots || isSearching">
            <div v-if="bots.length === 0" class="list-empty">还没有机器人</div>
            <div v-else-if="filteredBots.length === 0" class="list-empty">没有匹配的机器人</div>
            <div
              class="contact-item is-clickable"
              :class="{ 'is-selected': selectedKey === 'bot-' + bot.id }"
              v-for="bot in filteredBots"
              :key="bot.id"
              role="button"
              tabindex="0"
              :aria-label="`查看机器人 ${bot.name} 的资料`"
              @click="openBotDetail(bot.id)"
              @keydown.enter.prevent="openBotDetail(bot.id)"
              @keydown.space.prevent="openBotDetail(bot.id)"
              @contextmenu.prevent="showBotClickMenu($event, bot.id)"
            >
              <div class="avatar-placeholder" v-if="!bot.avatar">
                {{ bot.name && bot.name.charAt(0) }}
              </div>
              <img v-else :src="bot.avatar" class="avatar" alt="机器人头像">
              <div class="contact-body">
                <span class="name">{{ bot.name }}</span>
                <span class="contact-sub">AI 机器人</span>
              </div>
              <span class="row-chevron" aria-hidden="true">›</span>
            </div>
          </div>
        </div>

        <!-- 发送的好友申请列表 -->
        <div class="dropdown-section">
          <div class="dropdown-header" @click="toggleSendFriendRequestDropdown('sendFriendRequest')">
            <span>发送申请 ({{ showCount(sendCount) }})</span>
            <span v-if="pendingSendCount" class="badge">{{ pendingSendCount }}</span>
            <span class="arrow">{{ dropdowns.sendFriendRequest ? '▼' : '▶' }}</span>
          </div>
          <div class="dropdown-content" v-show="dropdowns.sendFriendRequest">
            <div v-if="sendFriendRequests.length === 0" class="list-empty">没有发送中的申请</div>
            <div class="request-item" v-for="request in sendFriendRequests" :key="request.requestId">
              <div class="avatar-placeholder" v-if="!request.avatar">
                  {{ request.username && request.username.charAt(0) }}
              </div>
              <img v-else :src="request.avatar" class="avatar" alt="用户头像">
              <div class="request-content">
                <div class="request-top-line">
                  <span class="name">{{ request.username }}</span>
                  <span class="request-time">{{ formatTime(request.createTime) }}</span>
                </div>
                <div class="request-meta-line">
                  <span class="request-message" :title="request.applyMsg">{{ request.applyMsg }}</span>
                  <div class="request-right">
                    <button v-if="request.flowStatus === 0"
                            @click="withdrawRequest(request)"
                            class="withdraw-btn"
                            :disabled="!!requestActioning[request.requestId]">
                      {{ requestActioning[request.requestId] ? '撤回中…' : '撤回' }}
                    </button>
                    <span class="request-status"
                          :class="request.flowStatus === 0
                          ? 'request-status-pending'
                          : request.flowStatus === 1
                          ? 'request-status-accepted'
                          : request.flowStatus === 2
                          ? 'request-status-rejected'
                          : ''">
                      {{ getStatusText(request.flowStatus) }}
                    </span>
                    <button v-if="request.flowStatus === 2"
                            @click="handleRequest(request, 0)"
                            class="resend-btn"
                            :disabled="!!requestActioning[request.requestId]">
                      重新发送
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 接收的好友申请列表 -->
        <div class="dropdown-section">
          <div class="dropdown-header" @click="toggleReceiveFriendRequestDropdown('receiveFriendRequest')">
            <span>接收申请 ({{ showCount(receiveCount) }})</span>
            <span v-if="pendingReceiveCount" class="badge">{{ pendingReceiveCount }}</span>
            <span class="arrow">{{ dropdowns.receiveFriendRequest ? '▼' : '▶' }}</span>
          </div>
          <div class="dropdown-content" v-show="dropdowns.receiveFriendRequest">
            <div v-if="receiveFriendRequests.length === 0" class="list-empty">没有收到的申请</div>
            <div class="request-item" v-for="request in receiveFriendRequests" :key="request.requestId">
              <div class="avatar-placeholder" v-if="!request.avatar">
                {{ request.username && request.username.charAt(0) }}
              </div>
              <img v-else :src="request.avatar" class="avatar" alt="用户头像">
              <div class="request-content">
                <div class="request-top-line">
                  <span class="name">{{ request.username }}</span>
                  <span class="request-time">{{ formatTime(request.createTime) }}</span>
                </div>
                <div class="request-meta-line">
                  <span class="request-message" :title="request.applyMsg">{{ request.applyMsg }}</span>
                  <div class="request-actions" v-if="request.flowStatus == 0">
                    <button @click="handleRequest(request, 1)" class="accept-btn"
                            :disabled="!!requestActioning[request.requestId]">
                      {{ requestActioning[request.requestId] ? '处理中…' : '同意' }}
                    </button>
                    <button @click="handleRequest(request, 2)" class="reject-btn"
                            :disabled="!!requestActioning[request.requestId]">拒绝</button>
                  </div>
                  <div class="request-right" v-if="request.flowStatus == 1 || request.flowStatus == 2">
                    <span class="request-status"
                          :class="request.flowStatus === 1
                          ? 'request-status-accepted'
                          : request.flowStatus === 2
                          ? 'request-status-rejected'
                          : ''">
                      {{ getStatusText(request.flowStatus) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="right-container">
      <!-- 右侧好友详情 -->
      <div :class="{'detail-container': showFriendDetail}">
        <div :class="{'right-content': showFriendDetail}">
          <FriendDetail v-if="showFriendDetail" ref="friendDetail" @close="closeFriendDetail" />
        </div>
      </div>

      <!-- 右侧群组详情 -->
      <div :class="{'detail-container': showGroupDetail}">
        <div :class="{'right-content': showGroupDetail}">
          <GroupDetail v-if="showGroupDetail" ref="groupDetail" @close="closeGroupDetail" @refresh="getGroupList" />
        </div>
      </div>

      <!-- 右侧机器人详情 -->
      <div :class="{'detail-container': showBotDetail}">
        <div :class="{'right-content': showBotDetail}">
          <BotDetail v-if="showBotDetail" ref="botDetail" @close="closeBotDetail" />
        </div>
      </div>

      <!-- 欢迎 / 联系人概览 -->
      <div v-if="showFriendDetail == false && showGroupDetail == false && showBotDetail == false" class="contact-overview">
        <div class="overview-head">
          <div class="overview-heading">
            <div class="overview-eyebrow">联系人中心</div>
            <h2>所有联系，都在这</h2>
            <p>
              好友 {{ showCount(friendCount) }} 位，{{ onlineFriendCount }} 位在线 · 群聊 {{ showCount(groupCount) }} 个 · 机器人 {{ showCount(botCount) }} 个
            </p>
            <div class="overview-actions">
              <button type="button" class="overview-primary" @click="openAddFriend('friend')">
                <span class="plus">+</span> 添加好友
              </button>
              <button type="button" class="overview-secondary" @click="openAddFriend('group')">发起群聊</button>
            </div>
          </div>
          <svg class="overview-watermelon" viewBox="0 0 72 72" fill="none" aria-hidden="true">
            <circle cx="36" cy="36" r="30" fill="#2f8f4e"/>
            <circle cx="36" cy="36" r="25" fill="#ff5a5f"/>
            <ellipse cx="23" cy="30" rx="2" ry="3.2" fill="#2b2b2b"/>
            <ellipse cx="35" cy="41" rx="2" ry="3.2" fill="#2b2b2b"/>
            <ellipse cx="46" cy="27" rx="2" ry="3.2" fill="#2b2b2b"/>
            <ellipse cx="47" cy="43" rx="2" ry="3.2" fill="#2b2b2b"/>
            <ellipse cx="26" cy="47" rx="2" ry="3.2" fill="#2b2b2b"/>
            <path d="M36 6c1.6 3.4 1.6 5.6 0 8" stroke="#2f8f4e" stroke-width="3" stroke-linecap="round"/>
          </svg>
        </div>

        <div class="overview-stats">
          <button type="button" class="overview-stat" @click="focusSection('friends')">
            <strong>{{ showCount(friendCount) }}</strong>
            <span>好友</span>
          </button>
          <button type="button" class="overview-stat" @click="focusSection('friends')">
            <strong class="is-green">{{ onlineFriendCount }}</strong>
            <span>在线好友</span>
          </button>
          <button type="button" class="overview-stat" @click="focusSection('groups')">
            <strong>{{ showCount(groupCount) }}</strong>
            <span>群聊</span>
          </button>
          <button type="button" class="overview-stat" @click="focusSection('receiveFriendRequest')">
            <strong :class="{ 'is-red': pendingReceiveCount > 0 }">{{ pendingReceiveCount }}</strong>
            <span>待处理</span>
          </button>
        </div>

        <div class="overview-columns">
          <section class="overview-section">
            <div class="overview-section-head">
              <h3>在线好友</h3>
              <button type="button" @click="focusSection('friends')">全部 ›</button>
            </div>
            <div v-if="onlineFriends.length" class="overview-list">
              <div
                class="overview-contact"
                v-for="friend in onlineFriends.slice(0, 4)"
                :key="friend.userId"
                role="button"
                tabindex="0"
                @click="openFriendDetail(friend.userId)"
                @keydown.enter.prevent="openFriendDetail(friend.userId)"
              >
                <div class="avatar-placeholder" v-if="!friend.avatar">{{ friend.username.charAt(0) }}</div>
                <img v-else :src="friend.avatar" class="avatar" alt="好友头像">
                <div class="contact-body">
                  <span class="name">{{ friend.username }}</span>
                  <span class="contact-sub">{{ friend.signature || '暂无个性签名' }}</span>
                </div>
                <span class="presence is-online"><i class="presence-dot"></i>在线</span>
              </div>
            </div>
            <div v-else class="overview-empty">暂时没有在线好友</div>
          </section>

          <section class="overview-section">
            <div class="overview-section-head">
              <h3>待处理申请</h3>
              <span v-if="pendingReceiveCount" class="badge">{{ pendingReceiveCount }}</span>
              <button type="button" class="overview-head-action" @click="focusSection('receiveFriendRequest')">全部 ›</button>
            </div>
            <div v-if="pendingReceiveRequests.length" class="overview-list">
              <div
                class="overview-request"
                v-for="request in pendingReceiveRequests.slice(0, 3)"
                :key="request.requestId"
              >
                <div class="avatar-placeholder" v-if="!request.avatar">{{ request.username.charAt(0) }}</div>
                <img v-else :src="request.avatar" class="avatar" alt="申请头像">
                <div class="contact-body">
                  <span class="name">{{ request.username }}</span>
                  <span class="contact-sub" :title="request.applyMsg">{{ request.applyMsg || '请求添加你为好友' }}</span>
                </div>
                <div class="overview-request-actions">
                  <button type="button" class="accept-btn" :disabled="!!requestActioning[request.requestId]" @click="handleRequest(request, 1)">同意</button>
                  <button type="button" class="reject-btn" :disabled="!!requestActioning[request.requestId]" @click="handleRequest(request, 2)">拒绝</button>
                </div>
              </div>
            </div>
            <div v-else class="overview-empty">没有待处理的申请</div>
          </section>
        </div>

        <div class="overview-groups" v-if="groups.length">
          <div class="overview-section-head">
            <h3>群聊速览</h3>
            <button type="button" @click="focusSection('groups')">全部 ›</button>
          </div>
          <div class="group-chips">
            <button
              type="button"
              class="group-chip"
              v-for="group in groups.slice(0, 5)"
              :key="group.groupId"
              @click="openGroupDetail(group.groupId)"
            >
              <span class="chip-avatar" v-if="!group.groupAvatar">{{ group.groupName.charAt(0) }}</span>
              <img v-else :src="group.groupAvatar" class="chip-avatar" alt="群头像">
              <span class="chip-name">{{ group.groupName }}</span>
              <span class="chip-count">{{ group.currentMember }}人</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 好友右键菜单 -->
    <div class="right-click-menu"
         v-show="friendRightClickMenu.visible"
         :style="{ top: friendRightClickMenu.top + 'px', left: friendRightClickMenu.left + 'px' }"
         @mouseleave="hideFriendRightClickMenu">
      <div class="menu-item" @click="handleFriendRightClickMenu('chat')">
        <i class="icon-message"></i> 发消息
      </div>
      <div class="menu-item" @click="handleFriendRightClickMenu('detail')">
        <i class="icon-friend"></i> 查看详情
      </div>
      <div class="menu-divider"></div>
<!--      <div class="menu-item" @click="handleFriendRightClickMenu('delete')">-->
<!--        <i class="icon-delete"></i> 删除好友-->
<!--      </div>-->
    </div>

    <!-- 群组右键菜单 -->
    <div class="right-click-menu"
         v-show="groupRightClickMenu.visible"
         :style="{ top: groupRightClickMenu.top + 'px', left: groupRightClickMenu.left + 'px' }"
         @mouseleave="hideGroupRightClickMenu">
      <div class="menu-item" @click="handleGroupRightClickMenu('chat')">
        <i class="icon-message"></i> 发消息
      </div>
      <div class="menu-item" @click="handleGroupRightClickMenu('detail')">
        <i class="icon-group"></i> 查看详情
      </div>
      <div class="menu-divider"></div>
<!--      <div class="menu-item" @click="handleGroupRightClickMenu('delete')">-->
<!--        <i class="icon-delete"></i> 删除群组-->
<!--      </div>-->
    </div>

    <!-- 机器人右键菜单 -->
    <div class="right-click-menu"
         v-show="botRightClickMenu.visible"
         :style="{ top: botRightClickMenu.top + 'px', left: botRightClickMenu.left + 'px' }"
         @mouseleave="hideBotRightClickMenu">
      <div class="menu-item" @click="handleBotRightClickMenu('chat')">
        <i class="icon-message"></i> 发消息
      </div>
      <div class="menu-item" @click="handleBotRightClickMenu('detail')">
        <i class="icon-group"></i> 查看详情
      </div>
      <div class="menu-divider"></div>
    </div>

    <AddFriend
      v-if="showAddFriend"
      :initial-tab="addFriendInitialTab"
      @close="closeAddFriend"
      @group-created="handleGroupCreated"
      @robot-created="handleBotCreated"
    />

  </div>
</template>

<script>
import {
  getBotList,
  getContactCount,
  getFriendList,
  getGroupList,
  getReceiveFriendRequestList,
  getSendFriendRequestList,
  withdrawFriendRequest
} from "@/api/contact.js";
import { friendVerify } from '@/api/friendRelation.js'
import FriendDetail from "@/views/FriendDetail.vue";
import GroupDetail from "@/views/GroupDetail.vue";
import BotDetail from "@/views/BotDetail.vue";
import AddFriend from "@/views/AddFriend.vue";
import {getFriendDetail} from "@/api/friendRelation.js";
import {set, setObject} from "@/utils/localStorage.js";
import {openChat} from "@/api/chat.js";

export default {
  data() {
    return {
      dropdowns: {
        friends: false,
        groups: false,
        bots: false,
        sendFriendRequest: false,
        receiveFriendRequest: false,
      },
      friendCount: 0,
      groupCount: 0,
      botCount: 0,
      // 计数接口失败时显示「—」，不要伪装成真实的 0
      countLoadFailed: false,
      sendCount: 0,
      receiveCount: 0,
      friends: [],
      groups: [],
      bots: [],
      sendFriendRequests: [],
      receiveFriendRequests: [],
      // 搜索关键词和列表加载标记：计数以真实列表为准，避免标题与列表不一致
      contactSearch: '',
      friendsLoaded: false,
      groupsLoaded: false,
      botsLoaded: false,
      sendLoaded: false,
      receiveLoaded: false,
      // 申请操作中的 requestId，防止重复点击
      requestActioning: {},
      showAddFriend: false,
      addFriendInitialTab: 'friend',
      unreadRequests: 0,
      // 当前在右侧打开的条目，用于左侧列表选中态
      selectedKey: null,
      showFriendDetail: false, // 好友详情
      friendRightClickMenu: {
        visible: false,
        top: 0,
        left: 0,
        friendId:null
      },
      showGroupDetail: false, // 群组详情
      groupRightClickMenu: {
        visible: false,
        top: 0,
        left: 0,
        groupId:null
      },
      showBotDetail: false, // 机器人详情
      botRightClickMenu: {
        visible: false,
        top: 0,
        left: 0,
        botId:null
      },
    }
  },
  created() {
    // 首屏就把概览需要的数据拉全，列表和计数共用同一份结果
    this.getContactCount();
    this.getFriendList();
    this.getGroupList();
    this.getBotList();
    this.getSendFriendRequestList();
    this.getReceiveFriendRequestList();
  },
  computed: {
    isSearching() {
      return this.contactSearch.trim().length > 0;
    },
    searchResultCount() {
      return this.filteredFriends.length + this.filteredGroups.length + this.filteredBots.length;
    },
    filteredFriends() {
      const keyword = this.contactSearch.trim().toLowerCase();
      if (!keyword) return this.friends;
      return this.friends.filter(friend =>
        String(friend.username || '').toLowerCase().includes(keyword) ||
        String(friend.signature || '').toLowerCase().includes(keyword)
      );
    },
    filteredGroups() {
      const keyword = this.contactSearch.trim().toLowerCase();
      if (!keyword) return this.groups;
      return this.groups.filter(group =>
        String(group.groupName || '').toLowerCase().includes(keyword)
      );
    },
    filteredBots() {
      const keyword = this.contactSearch.trim().toLowerCase();
      if (!keyword) return this.bots;
      return this.bots.filter(bot =>
        String(bot.name || '').toLowerCase().includes(keyword)
      );
    },
    groupedFriends() {
      const sorted = this.filteredFriends.slice().sort((a, b) => {
        if (!!a.isOnline !== !!b.isOnline) return a.isOnline ? -1 : 1;
        return String(a.username || '').localeCompare(String(b.username || ''), 'zh-Hans-CN');
      });
      const online = sorted.filter(friend => friend.isOnline);
      const offline = sorted.filter(friend => !friend.isOnline);
      return [
        { key: 'online', label: '在线', online: true, list: online },
        { key: 'offline', label: '离线', online: false, list: offline }
      ].filter(bucket => bucket.list.length > 0);
    },
    onlineFriendCount() {
      return this.friends.filter(friend => friend.isOnline).length;
    },
    onlineFriends() {
      return this.friends
        .filter(friend => friend.isOnline)
        .sort((a, b) => String(a.username || '').localeCompare(String(b.username || ''), 'zh-Hans-CN'));
    },
    pendingSendCount() {
      return this.sendFriendRequests.filter(request => request.flowStatus === 0).length;
    },
    pendingReceiveCount() {
      return this.receiveFriendRequests.filter(request => request.flowStatus === 0).length;
    },
    pendingReceiveRequests() {
      return this.receiveFriendRequests.filter(request => request.flowStatus === 0);
    }
  },
  components: {
    FriendDetail,
    GroupDetail,
    BotDetail,
    AddFriend
  },
  methods: {
    // 打开/关闭 下拉列表
    toggleDropdown(type) {
      this.dropdowns[type] = !this.dropdowns[type];
    },

    // 获取联系人数量
    async getContactCount() {
      try {
        const res = await getContactCount();
        if (res.code === 200) {
          // 列表已经加载成功时以列表长度为准，避免接口统计与可见条目不一致
          if (!this.friendsLoaded) this.friendCount = res.data.friendCount;
          if (!this.groupsLoaded) this.groupCount = res.data.groupCount;
          if (!this.botsLoaded) this.botCount = res.data.botCount;
          if (!this.sendLoaded) this.sendCount = res.data.sendCount;
          if (!this.receiveLoaded) this.receiveCount = res.data.receiveCount;
          this.countLoadFailed = false;
        } else {
          this.countLoadFailed = true;
        }
      } catch (e) {
        this.countLoadFailed = true;
        console.error("获取联系人数量失败:", e);
      }
    },

    // 计数展示：失败时用「—」而不是 0
    showCount(n) {
      return this.countLoadFailed ? "—" : n;
    },

    // 获取好友列表
    async getFriendList() {
      try {
        const res = await getFriendList();
        if (res.code === 200) {
          this.friends = res.data || [];
          this.friendsLoaded = true;
          this.friendCount = this.friends.length;
          this.countLoadFailed = false;
        }
      } catch (e) {
        console.error("获取好友列表失败:", e);
      }
    },

    // 获取群聊列表
    async getGroupList() {
      try {
        const res = await getGroupList();
        if (res.code === 200) {
          this.groups = res.data || [];
          this.groupsLoaded = true;
          this.groupCount = this.groups.length;
          this.countLoadFailed = false;
        }
      } catch (e) {
        console.error("获取群聊列表失败:", e);
      }
    },

    // 获取机器人列表
    async getBotList() {
      try {
        const res = await getBotList();
        if (res.code === 200) {
          this.bots = res.data || [];
          this.botsLoaded = true;
          this.botCount = this.bots.length;
          this.countLoadFailed = false;
        }
      } catch (e) {
        console.error("获取机器人列表失败:", e);
      }
    },

    // 展开好友下拉列表
    toggleFriendDropdown(type) {
      this.toggleDropdown(type);

      // 展开好友下拉表表时，获取好友列表数据
      if(this.dropdowns.friends == true){
        this.getFriendList();
      }
    },

    // 展开群聊下拉列表
    toggleGroupDropdown(type) {
      this.toggleDropdown(type);

      // 展开群聊下拉表表时，获取群聊列表数据
      if(this.dropdowns.groups == true){
        this.getGroupList();
      }
    },

    // 展开群聊下拉列表
    toggleBotDropdown(type) {
      this.toggleDropdown(type);

      // 展开群聊下拉表表时，获取群聊列表数据
      if(this.dropdowns.bots == true){
        this.getBotList();
      }
    },

    async startChat(chatId, chatType) {
      let rank = 0;
      const res = await this.openChat(chatId, chatType);
      if(res.rank != null){
        rank = res.rank;
        setObject('currentChatWindow', res);
      }
      set('activeMenu', 'chat');

      // 打开窗口参数
      const openChat = {
        "chatId": chatId,
        "chatType": chatType,
        "rank": rank
      }
      set("openChat", JSON.stringify(openChat))
      this.$router.push({
        path: 'Chat',
      })
    },

    // 打开好友详情
    openFriendDetail(friendId) {
      if(this.showGroupDetail){
        this.showGroupDetail = false;
      }
      if(this.showBotDetail){
        this.showBotDetail = false;
      }
      this.selectedKey = `friend-${friendId}`;
      this.showFriendDetail = true;

      this.$nextTick(() => {
        if (this.$refs.friendDetail) {
          // 调用子组件方法
          this.$refs.friendDetail.getFriendDetail(friendId);
        }
      });
    },

    // 关闭好友详情
    closeFriendDetail() {
      this.showFriendDetail = false;
      this.selectedKey = null;
    },

    // 打开好友右键菜单
    showFriendClickMenu(event, friendId) {
      this.friendRightClickMenu = {
        visible: true,
        top: event.clientY,
        left: event.clientX,
        friendId: friendId
      }
      // 点击其他地方关闭菜单
      document.addEventListener('click', this.hideFriendRightClickMenu)
    },

    hideFriendRightClickMenu() {
      this.friendRightClickMenu.visible = false
      document.removeEventListener('click', this.hideFriendRightClickMenu)
    },

    handleFriendRightClickMenu(action) {
      this.hideFriendRightClickMenu()
      if (!this.friendRightClickMenu.friendId) return

      const friendId = this.friendRightClickMenu.friendId
      switch(action) {
        case 'chat':
          this.startChat(friendId, 1)
          break
        case 'detail':
          this.openFriendDetail(friendId)
          break
        case 'delete':
          this.deleteFriend(friendId)
          break
      }
    },

    showGroupClickMenu(event, groupId) {
      this.groupRightClickMenu = {
        visible: true,
        top: event.clientY,
        left: event.clientX,
        groupId: groupId
      }
      // 点击其他地方关闭菜单
      document.addEventListener('click', this.hideGroupRightClickMenu)
    },

    hideGroupRightClickMenu() {
      this.groupRightClickMenu.visible = false
      document.removeEventListener('click', this.hideGroupRightClickMenu)
    },

    handleGroupRightClickMenu(action) {
      this.hideGroupRightClickMenu()
      if (!this.groupRightClickMenu.groupId) return

      const groupId = this.groupRightClickMenu.groupId
      switch(action) {
        case 'chat':
          this.startChat(groupId, 2)
          break
        case 'detail':
          this.openGroupDetail(groupId)
          break
        case 'delete':
          this.deleteGroup(groupId)
          break
      }
    },

    // 打开群组详情
    openGroupDetail(groupId) {
      if(this.showFriendDetail){
        this.showFriendDetail = false;
      }
      if(this.showBotDetail){
        this.showBotDetail = false;
      }
      this.selectedKey = `group-${groupId}`;
      this.showGroupDetail = true;

      this.$nextTick(() => {
        if (this.$refs.groupDetail) {
          // 调用子组件方法
          this.$refs.groupDetail.getGroupDetail(groupId);
        }
      });
    },

    // 关闭群组详情
    closeGroupDetail() {
      this.showGroupDetail = false;
      this.selectedKey = null;
    },


    showBotClickMenu(event, botId) {
      this.botRightClickMenu = {
        visible: true,
        top: event.clientY,
        left: event.clientX,
        botId: botId
      }
      // 点击其他地方关闭菜单
      document.addEventListener('click', this.hideBotRightClickMenu)
    },

    hideBotRightClickMenu() {
      this.botRightClickMenu.visible = false
      document.removeEventListener('click', this.hideBotRightClickMenu)
    },

    handleBotRightClickMenu(action) {
      this.hideBotRightClickMenu()
      if (!this.botRightClickMenu.botId) return

      const botId = this.botRightClickMenu.botId
      switch(action) {
        case 'chat':
          this.startChat(botId, 3)
          break
        case 'detail':
          this.openBotDetail(botId)
          break
      }
    },

    // 打开群组详情
    openBotDetail(botId) {
      if(this.showFriendDetail){
        this.showFriendDetail = false;
      }
      if(this.showGroupDetail){
        this.showGroupDetail = false;
      }
      this.selectedKey = `bot-${botId}`;
      this.showBotDetail = true;

      this.$nextTick(() => {
        if (this.$refs.botDetail) {
          // 调用子组件方法
          this.$refs.botDetail.getBotDetail(botId);
        }
      });
    },

    // 关闭群组详情
    closeBotDetail() {
      this.showBotDetail = false;
      this.selectedKey = null;
    },

    // 展开发送好友申请下拉列表
    toggleSendFriendRequestDropdown(type) {
      this.toggleDropdown(type);

      // 展开送好友申请下拉表表时，获取送好友申请列表数据
      if(this.dropdowns.sendFriendRequest == true){
        this.getSendFriendRequestList();
      }
    },

    // 获取发送好友申请列表
    async getSendFriendRequestList() {
      try {
        const res = await getSendFriendRequestList();
        if (res.code === 200) {
          this.sendFriendRequests = res.data || [];
          this.sendLoaded = true;
          this.sendCount = this.sendFriendRequests.length;
          this.countLoadFailed = false;
        }
      } catch (e) {
        console.error("获取发送好友申请列表失败:", e);
      }
    },

    // 展开接收好友申请下拉列表
    toggleReceiveFriendRequestDropdown(type) {
      this.toggleDropdown(type);

      // 展开接收好友申请下拉表表时，获取接收好友申请列表数据
      if(this.dropdowns.receiveFriendRequest == true){
        this.getReceiveFriendRequestList();
      }
    },

    // 获取接收好友申请列表
    async getReceiveFriendRequestList() {
      try {
        const res = await getReceiveFriendRequestList();
        if (res.code === 200) {
          this.receiveFriendRequests = res.data || [];
          this.receiveLoaded = true;
          this.receiveCount = this.receiveFriendRequests.length;
          this.countLoadFailed = false;
        }
      } catch (e) {
        console.error("获取接收好友申请列表失败:", e);
      }
    },

    // 刷新两类申请和计数
    refreshRequestLists() {
      this.getContactCount();
      this.getSendFriendRequestList();
      this.getReceiveFriendRequestList();
    },

    getStatusText(status) {
      const statusMap = {
        0: '等待验证',
        1: '已通过',
        2: '已拒绝'
      }
      return statusMap[status] || status
    },

    formatTime(datetimeStr) {
      if (!datetimeStr) return '';

      const date = new Date(datetimeStr); // 直接解析
      const now = new Date();

      // 获取今天、昨天和输入日期的00:00:00时间戳
      const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
      const yesterdayStart = todayStart - 86400000;
      const inputDateStart = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();

      // 今天 - 显示时间（去掉小时前导零）
      if (inputDateStart >= todayStart) {
        const hours = date.getHours();
        const minutes = date.getMinutes().toString().padStart(2, '0');
        return `${hours}:${minutes}`;
      } else if (inputDateStart >= yesterdayStart) {
        return '昨天';
      } else if (this.isInCurrentWeek(date, now)) {
        const weekday = date.getDay();
        if (weekday >= 1 && weekday <= 5) {
          const weekdays = ['星期一', '星期二', '星期三', '星期四', '星期五'];
          return weekdays[weekday - 1];
        }
        return this.formatDate(date);
      } else {
        return this.formatDate(date);
      }
    },

    /**
     * 判断日期是否在本周
     */
    isInCurrentWeek(date, now) {
      const nowStartOfWeek = new Date(now.getFullYear(), now.getMonth(), now.getDate() - (now.getDay() || 7) + 1)
      const dateStartOfWeek = new Date(date.getFullYear(), date.getMonth(), date.getDate() - (date.getDay() || 7) + 1)
      return nowStartOfWeek.getTime() === dateStartOfWeek.getTime()
    },

    /**
     * 格式化日期为YY/MM/DD
     */
    formatDate(date) {
      const year = date.getFullYear().toString().slice(-2).padStart(2, '0')
      const month = (date.getMonth() + 1).toString().padStart(2, '0')
      const day = date.getDate().toString().padStart(2, '0')
      return `${year}/${month}/${day}`
    },

    // 处理好友验证请求
    async handleRequest(request, flowStatus) {
      const requestId = request.requestId
      if (this.requestActioning[requestId]) return
      this.requestActioning = { ...this.requestActioning, [requestId]: true }

      const reqData = {
        requestId: requestId,
        friendId: request.userId,
        flowStatus: flowStatus
      }

      try {
        const response = await friendVerify(reqData)
        if (response.code != 200) {
          this.$message.error(response.msg || '操作失败')
          return
        }

        this.$message.success(
          flowStatus === 0 ? '已重新发送申请' : flowStatus === 1 ? '已同意好友申请' : '已拒绝好友申请'
        )

        if (flowStatus == 0) {
          await this.getSendFriendRequestList()
        } else {
          await this.getReceiveFriendRequestList()
        }
        this.getContactCount()
        if (flowStatus == 1) {
          await this.getFriendList()
        }
      } catch (e) {
        this.$message.error(e && e.message ? e.message : '操作失败，请稍后重试')
      } finally {
        const nextActioning = { ...this.requestActioning }
        delete nextActioning[requestId]
        this.requestActioning = nextActioning
      }
    },

    // 撤回自己发送且仍在等待验证的申请
    async withdrawRequest(request) {
      const requestId = request.requestId
      if (this.requestActioning[requestId]) return
      try {
        await this.$confirm('撤回后对方将看不到这条好友申请，确定撤回吗？', '撤回申请', {
          confirmButtonText: '撤回',
          cancelButtonText: '取消',
          type: 'warning'
        })
      } catch (e) {
        return
      }

      this.requestActioning = { ...this.requestActioning, [requestId]: true }
      try {
        const response = await withdrawFriendRequest(requestId)
        if (response.code !== 200) {
          this.$message.error(response.msg || '撤回失败')
          return
        }
        this.$message.success('已撤回好友申请')
        await this.getSendFriendRequestList()
        this.getContactCount()
      } catch (e) {
        this.$message.error(e && e.message ? e.message : '撤回失败，请稍后重试')
      } finally {
        const nextActioning = { ...this.requestActioning }
        delete nextActioning[requestId]
        this.requestActioning = nextActioning
      }
    },

    // 概览卡片点击：展开左侧对应分组
    focusSection(type) {
      this.dropdowns[type] = true
      if (type === 'friends' && this.friends.length === 0) this.getFriendList()
      if (type === 'groups' && this.groups.length === 0) this.getGroupList()
      if (type === 'receiveFriendRequest' && this.receiveFriendRequests.length === 0) this.getReceiveFriendRequestList()
    },

    // 联系人页快捷入口：添加好友 / 发起群聊
    openAddFriend(tab) {
      this.addFriendInitialTab = tab || 'friend'
      this.showAddFriend = true
    },

    closeAddFriend() {
      this.showAddFriend = false
      // 弹框里可能刚发出好友申请，关闭时刷新申请列表和计数
      this.refreshRequestLists()
    },

    handleGroupCreated() {
      this.showAddFriend = false
      this.getGroupList()
      this.getContactCount()
    },

    handleBotCreated() {
      this.showAddFriend = false
      this.getBotList()
      this.getContactCount()
    },

    // 打开聊天窗口
    async openChat(chatId, chatType){
      const req = {
        chatId: chatId,
        chatType: chatType
      };
      const res = await openChat(req);
      if(res.code == 200){
        return res.data;
      }
    }
  }
}
</script>

<style scoped>
/* ===== 联系人页 · iOS 设计语言 ===== */
.main-container {
  display: flex;
  flex: 1;
  width: 100%;
  height: 100%;
  background: var(--bg);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

.left-container {
  width: 30%;
  min-width: 260px;
  height: 100%;
  background: var(--bg-blur-strong);
  border-right: 1px solid var(--separator);
  display: flex;
  flex-direction: column;
}

.right-container {
  width: 70%;
  height: 100%;
}

.left-content {
  padding: 14px 10px;
  flex: 1;
  overflow-y: auto;
}

.left-content::-webkit-scrollbar {
  display: none;
  width: 0;
}

/* 右侧详情 */
.detail-container {
  height: 100%;
  flex: 1;
  background-color: var(--bg);
  display: flex;
  flex-direction: column;
}

.right-content {
  flex: 1;
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder {
  text-align: center;
  color: rgba(0, 0, 0, 0.38);
  font-size: 15px;
  font-weight: 500;
}

/* 分组标题 · iOS group header */
h1 {
  font-size: 13px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: var(--label-secondary);
  padding: 0 12px;
  letter-spacing: 0.2px;
}

.left-subtitle {
  margin: 0 0 14px;
  padding: 0 12px;
  font-size: 11.5px;
  line-height: 1.55;
  color: var(--label-tertiary);
}

/* iOS 折叠分组 */
.dropdown-section {
  margin-bottom: 14px;
}

.dropdown-header {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 12px;
  cursor: pointer;
  user-select: none;
  font-weight: 600;
  font-size: 14px;
  color: var(--label);
  transition: background-color 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}

.dropdown-header span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.dropdown-header:hover {
  background: var(--fill);
}

.arrow {
  margin-left: auto;
  font-size: 10px;
  color: var(--label-tertiary);
}

.badge {
  background-color: #ff3b30;
  color: white;
  border-radius: 999px;
  padding: 1px 7px;
  font-size: 11px;
  font-weight: 600;
  margin-left: 6px;
  box-shadow: 0 1px 3px rgba(255, 59, 48, 0.35);
}

.dropdown-content {
  margin-top: 4px;
  padding: 4px;
}

/* 好友空态插画 */
.friend-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  text-align: center;
}

.friend-empty .empty-art {
  width: 64px;
  height: 64px;
  color: var(--accent);
  filter: drop-shadow(0 8px 18px rgba(0, 122, 255, 0.15));
  margin-bottom: 10px;
}

.friend-empty .empty-tip {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: var(--label);
}

.friend-empty .empty-sub {
  margin: 0;
  font-size: 12px;
  color: var(--label-secondary);
}

/* 联系人行 · iOS inset grouped */
.contact-item,
.request-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 12px;
  transition: background-color 0.25s cubic-bezier(0.32, 0.72, 0, 1);
  cursor: default;
}

.contact-item:hover,
.request-item:hover {
  background: var(--fill);
}

.contact-item.is-clickable {
  cursor: pointer;
}

.contact-item.is-clickable:focus-visible {
  outline: 3px solid rgba(0, 122, 255, 0.24);
  outline-offset: -1px;
}

.contact-item.is-selected {
  background: var(--fill);
  box-shadow: inset 3px 0 0 var(--label);
}

.avatar-placeholder {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: linear-gradient(145deg, #4da3ff, #007aff);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-weight: 600;
  font-size: 15px;
  box-shadow: 0 2px 6px rgba(0, 122, 255, 0.2);
  flex-shrink: 0;
}

.avatar-placeholder.group {
  background: linear-gradient(145deg, #58d68d, #34c759);
  box-shadow: 0 2px 6px rgba(52, 199, 89, 0.2);
}

.avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  margin-right: 12px;
  object-fit: cover;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
  flex-shrink: 0;
}

.name {
  font-weight: 500;
  font-size: 15px;
  color: var(--label);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  margin-left: 8px;
  flex-shrink: 0;
}

.status.online {
  background-color: #34c759;
  box-shadow: 0 0 5px rgba(52, 199, 89, 0.5);
}

.status.offline {
  background-color: #c7c7cc;
}

.member-count {
  font-size: 12px;
  color: var(--label-secondary);
  margin-left: 8px;
  flex-shrink: 0;
}

.row-chevron {
  margin-left: 6px;
  font-size: 17px;
  line-height: 1;
  color: var(--label-tertiary);
  flex-shrink: 0;
}

.request-message {
  font-size: 12px;
  color: var(--label-secondary);
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 右键菜单 · iOS 菜单 */
.right-click-menu {
  position: fixed;
  z-index: 9999;
  background: rgba(255, 255, 255, 0.94);
  -webkit-backdrop-filter: saturate(180%) blur(24px);
  backdrop-filter: saturate(180%) blur(24px);
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
  padding: 5px;
  min-width: 170px;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.menu-item {
  padding: 10px 14px;
  font-size: 14px;
  color: var(--label);
  display: flex;
  align-items: center;
  border-radius: 10px;
  transition: background-color 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}

.menu-item:hover {
  background: rgba(0, 122, 255, 0.1);
}

.menu-item i {
  margin-right: 8px;
  font-size: 16px;
}

.menu-divider {
  height: 1px;
  background: var(--separator-strong);
  margin: 5px 6px;
}

.icon-message:before {
  content: "💬";
}

.icon-friend:before {
  content: "👥";
}

.icon-group:before {
  content: "👪";
}

.icon-delete:before {
  content: "🗑️";
}

.request-item {
  display: flex;
  padding: 10px 12px;
  align-items: center;
  min-height: 56px;
}

.request-content {
  flex: 1;
  min-width: 0;
}

.request-top-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.request-meta-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.request-time {
  font-size: 11px;
  color: var(--label-tertiary);
}

.request-message {
  font-size: 12px;
  color: var(--label-secondary);
  flex: 1;
  margin-right: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.request-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 请求状态 · iOS tint 徽章 */
.request-status {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 999px;
  white-space: nowrap;
  font-weight: 600;
}

.request-status-pending {
  background-color: rgba(0, 122, 255, 0.12);
  color: #007aff;
}

.request-status-accepted {
  background-color: rgba(52, 199, 89, 0.12);
  color: #34c759;
}

.request-status-rejected {
  background-color: rgba(255, 59, 48, 0.12);
  color: #ff3b30;
}

.request-actions {
  display: flex;
  gap: 8px;
}

/* iOS 按钮 */
.accept-btn,
.reject-btn,
.resend-btn {
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  white-space: nowrap;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}

.accept-btn {
  background-color: #007aff;
  color: white;
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.3);
}

.accept-btn:hover {
  background-color: #3395ff;
}

.reject-btn {
  background-color: var(--card);
  color: var(--label-secondary);
  border: 1px solid rgba(60, 60, 67, 0.15);
}

.resend-btn {
  background-color: var(--card);
  color: #007aff;
  border: 1px solid rgba(0, 122, 255, 0.4);
}

.reject-btn:hover,
.resend-btn:hover {
  background-color: var(--fill);
}

/* ===== 联系人搜索 ===== */
.contact-search {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0 6px 8px;
  padding: 0 11px;
  height: 36px;
  border-radius: 11px;
  background: var(--fill);
  border: 1px solid transparent;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.contact-search:focus-within {
  background: var(--card);
  border-color: var(--separator-strong);
}

.search-icon {
  width: 15px;
  height: 15px;
  color: var(--label-tertiary);
  flex-shrink: 0;
}

.contact-search input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  color: var(--label);
  font-size: 13px;
  font-family: inherit;
  appearance: none;
  -webkit-appearance: none;
}

.contact-search input::-webkit-search-cancel-button {
  display: none;
  -webkit-appearance: none;
}

.contact-search input::placeholder {
  color: var(--label-tertiary);
}

.search-clear {
  width: 20px;
  height: 20px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: var(--separator);
  color: var(--label-secondary);
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
  flex-shrink: 0;
}

.search-clear:hover {
  background: var(--separator-strong);
}

.search-summary {
  margin: -2px 12px 8px;
  font-size: 11px;
  color: var(--label-tertiary);
}

.header-tint {
  margin-left: 8px;
  font-size: 11px;
  font-weight: 500;
  color: #34c759;
  white-space: nowrap;
}

/* ===== 好友在线/离线分组 ===== */
.contact-subgroup + .contact-subgroup {
  margin-top: 6px;
}

.subgroup-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px 3px;
  font-size: 11px;
  font-weight: 600;
  color: var(--label-tertiary);
  letter-spacing: 0.2px;
}

.subgroup-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.subgroup-dot.is-online {
  background: #34c759;
  box-shadow: 0 0 4px rgba(52, 199, 89, 0.5);
}

.subgroup-dot.is-offline {
  background: #c7c7cc;
}

.subgroup-count {
  min-width: 16px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--fill);
  color: var(--label-secondary);
  font-size: 10px;
  line-height: 15px;
  text-align: center;
}

/* ===== 联系人两行信息 ===== */
.contact-item {
  min-height: 54px;
}

.contact-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.contact-sub {
  font-size: 11.5px;
  line-height: 1.25;
  color: var(--label-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.presence {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 8px;
  font-size: 10.5px;
  font-weight: 500;
  white-space: nowrap;
  flex-shrink: 0;
}

.presence.is-online {
  color: #34c759;
}

.presence.is-offline {
  color: var(--label-tertiary);
}

.presence-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  flex-shrink: 0;
}

.list-empty {
  padding: 18px 12px;
  text-align: center;
  font-size: 12px;
  color: var(--label-secondary);
}

/* ===== 申请中心新增操作 ===== */
.withdraw-btn {
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid var(--separator-strong);
  background: transparent;
  color: var(--label-secondary);
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: color 0.2s ease, border-color 0.2s ease, background-color 0.2s ease;
}

.withdraw-btn:hover:not(:disabled) {
  color: #ff3b30;
  border-color: rgba(255, 59, 48, 0.35);
  background: rgba(255, 59, 48, 0.06);
}

.accept-btn:disabled,
.reject-btn:disabled,
.resend-btn:disabled,
.withdraw-btn:disabled {
  opacity: 0.55;
  cursor: default;
  box-shadow: none;
}

/* ===== 右侧联系人概览 ===== */
.contact-overview {
  height: 100%;
  padding: 26px 28px 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.contact-overview::-webkit-scrollbar {
  width: 6px;
}

.contact-overview::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: var(--separator-strong);
}

.overview-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 26px 28px;
  border-radius: 22px;
  border: 1px solid var(--separator);
  background: linear-gradient(155deg, var(--card) 0%, var(--bg-blur-strong) 100%);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.overview-heading {
  min-width: 0;
}

.overview-eyebrow {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.6px;
  text-transform: uppercase;
  color: var(--label-tertiary);
  margin-bottom: 8px;
}

.overview-heading h2 {
  margin: 0 0 8px;
  font-size: 27px;
  line-height: 1.2;
  letter-spacing: -0.4px;
  font-weight: 700;
  color: var(--label);
}

.overview-heading p {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--label-secondary);
}

.overview-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 18px;
  flex-wrap: wrap;
}

.overview-primary,
.overview-secondary {
  height: 34px;
  padding: 0 16px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s ease, background-color 0.2s ease, transform 0.2s ease;
}

.overview-primary {
  border: 1px solid transparent;
  background: var(--label);
  color: var(--card);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.overview-primary:hover {
  opacity: 0.86;
  transform: translateY(-1px);
}

.overview-primary .plus {
  font-size: 14px;
  font-weight: 500;
  margin-right: 3px;
}

.overview-secondary {
  border: 1px solid var(--separator-strong);
  background: var(--card);
  color: var(--label);
}

.overview-secondary:hover {
  background: var(--fill);
}

.overview-watermelon {
  width: 68px;
  height: 68px;
  flex-shrink: 0;
  filter: drop-shadow(0 8px 18px rgba(47, 143, 78, 0.18));
}

.overview-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 9px;
}

.overview-stat {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
  padding: 13px 15px 12px;
  border-radius: 16px;
  border: 1px solid var(--separator);
  background: var(--card);
  text-align: left;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.2s ease;
}

.overview-stat:hover {
  background: var(--fill);
  transform: translateY(-1px);
}

.overview-stat strong {
  font-size: 23px;
  line-height: 1.1;
  font-weight: 700;
  color: var(--label);
  font-variant-numeric: tabular-nums;
}

.overview-stat strong.is-green {
  color: #34c759;
}

.overview-stat strong.is-red {
  color: #ff3b30;
}

.overview-stat span {
  font-size: 11px;
  color: var(--label-secondary);
}

.overview-columns {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
}

.overview-section {
  min-width: 0;
  padding: 15px 16px 12px;
  border-radius: 18px;
  border: 1px solid var(--separator);
  background: var(--card);
}

.overview-section-head {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 0 2px 8px;
}

.overview-section-head h3 {
  margin: 0;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--label);
}

.overview-section-head > button,
.overview-head-action {
  margin-left: auto;
  padding: 2px 0;
  border: none;
  background: none;
  color: var(--label-tertiary);
  font-size: 11px;
  cursor: pointer;
}

.overview-section-head > button:hover,
.overview-head-action:hover {
  color: var(--label);
}

.overview-list {
  display: flex;
  flex-direction: column;
}

.overview-contact,
.overview-request {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 7px 8px;
  border-radius: 10px;
  transition: background-color 0.2s ease;
}

.overview-contact {
  cursor: pointer;
}

.overview-contact:hover,
.overview-request:hover {
  background: var(--fill);
}

.overview-contact:focus-visible {
  outline: 2px solid var(--separator-strong);
  outline-offset: -2px;
}

.overview-contact .avatar,
.overview-request .avatar,
.overview-contact .avatar-placeholder,
.overview-request .avatar-placeholder {
  width: 32px;
  height: 32px;
  margin-right: 10px;
  font-size: 13px;
}

.overview-contact .name,
.overview-request .name {
  font-size: 13.5px;
}

.overview-contact .contact-sub,
.overview-request .contact-sub {
  font-size: 11px;
}

.overview-contact .presence {
  font-size: 10px;
  margin-left: 6px;
}

.overview-request-actions {
  display: flex;
  gap: 5px;
  margin-left: 6px;
  flex-shrink: 0;
}

.overview-request-actions .accept-btn,
.overview-request-actions .reject-btn {
  padding: 4px 9px;
  font-size: 10.5px;
  border-radius: 999px;
}

.overview-empty {
  padding: 18px 6px;
  font-size: 11.5px;
  color: var(--label-tertiary);
}

.overview-groups {
  padding: 15px 16px 16px;
  border-radius: 18px;
  border: 1px solid var(--separator);
  background: var(--card);
}

.group-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.group-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  max-width: 190px;
  padding: 6px 10px 6px 6px;
  border-radius: 999px;
  border: 1px solid var(--separator);
  background: var(--fill);
  color: var(--label);
  font-size: 11.5px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.group-chip:hover {
  background: var(--separator);
}

.chip-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
  background: #34c759;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.chip-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chip-count {
  color: var(--label-tertiary);
  font-size: 10px;
  flex-shrink: 0;
}

/* 窄屏重排 */
@media (max-width: 1180px) {
  .overview-columns {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 980px) {
  .contact-overview {
    padding: 20px;
  }
  .overview-head {
    padding: 22px;
  }
  .overview-heading h2 {
    font-size: 23px;
  }
  .overview-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .overview-watermelon {
    width: 52px;
    height: 52px;
  }
}
</style>