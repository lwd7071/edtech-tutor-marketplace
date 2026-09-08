'use client';

import React from 'react';
import { Alert, Badge, Button, Spin } from 'antd';
import { ConversationView } from '../types';

interface ChatLayoutProps {
  conversations: ConversationView[];
  activeConversationId?: string;
  loadingConversations?: boolean;
  error?: boolean;
  onRetry?: () => void;
  reconnecting?: boolean;
  onSelectConversation: (id: string) => void;
  children: React.ReactNode;
}

export const ChatLayout: React.FC<ChatLayoutProps> = ({
  conversations, activeConversationId, loadingConversations = false,
  error = false, onRetry, reconnecting = false, onSelectConversation, children,
}) => {
  return (
    <div className={`chat-shell ${activeConversationId ? 'has-active-chat' : ''}`}>
      {reconnecting && <div className="chat-reconnect">Mất kết nối. Hệ thống đang thử kết nối lại…</div>}
      <div className="chat-body">
        <aside className="chat-sidebar" aria-label="Danh sách cuộc trò chuyện">
          <header className="chat-sidebar-header"><h2>Tin nhắn</h2></header>
          <div className="chat-conversations">
            {loadingConversations ? <div className="chat-state"><Spin /></div> : error ? (
              <div className="chat-state"><Alert type="error" showIcon title="Chưa tải được danh sách tin nhắn" /><Button onClick={onRetry}>Thử lại</Button></div>
            ) : conversations.length === 0 ? <div className="chat-empty">Chưa có cuộc trò chuyện nào.</div> : conversations.map((conversation) => (
              <button key={conversation.id} type="button" onClick={() => onSelectConversation(conversation.id)} className={`chat-conversation ${activeConversationId === conversation.id ? 'active' : ''}`} aria-current={activeConversationId === conversation.id ? 'true' : undefined}>
                <Badge count={conversation.unreadCount} size="small">
                  <img src={conversation.participantAvatar || '/images/default-avatar.png'} alt="" />
                </Badge>
                <span className="chat-conversation-copy"><strong>{conversation.participantName || 'Người dùng'}</strong><span className={conversation.unreadCount > 0 ? 'unread' : ''}>{conversation.lastMessagePreview || 'Bắt đầu cuộc trò chuyện'}</span></span>
              </button>
            ))}
          </div>
        </aside>
        <main className="chat-thread-area">{children}</main>
      </div>
      <style>{`
        .chat-shell{height:calc(100vh - 136px);min-height:520px;background:var(--color-surface);overflow:hidden}.chat-reconnect{padding:8px 16px;text-align:center;font-size:13px;font-weight:600;color:var(--color-warning-600);background:var(--color-warning-bg)}.chat-body{display:flex;height:100%;min-height:0}.chat-sidebar{width:320px;flex:0 0 320px;display:flex;flex-direction:column;border-right:1px solid var(--color-border);background:var(--color-surface-sunken);min-height:0}.chat-sidebar-header{padding:18px 20px;border-bottom:1px solid var(--color-border);background:var(--color-surface)}.chat-sidebar-header h2{margin:0;font-size:20px}.chat-conversations{padding:8px;overflow-y:auto}.chat-state{display:flex;flex-direction:column;align-items:center;gap:12px;padding:32px 12px}.chat-empty{padding:32px 16px;text-align:center;color:var(--color-text-secondary);font-size:14px}.chat-conversation{width:100%;display:flex;gap:12px;align-items:center;padding:12px;border:0;border-radius:10px;background:transparent;text-align:left;cursor:pointer;color:inherit}.chat-conversation:hover{background:var(--color-surface-hover)}.chat-conversation.active{background:var(--color-primary-50)}.chat-conversation img{width:48px;height:48px;border-radius:50%;object-fit:cover;border:1px solid var(--color-border)}.chat-conversation-copy{display:flex;flex-direction:column;gap:4px;min-width:0;flex:1}.chat-conversation-copy strong,.chat-conversation-copy span{white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.chat-conversation-copy span{font-size:13px;color:var(--color-text-secondary)}.chat-conversation-copy .unread{font-weight:600;color:var(--color-text-primary)}.chat-thread-area{display:flex;flex:1;min-width:0;min-height:0;background:var(--color-surface)}
        @media(max-width:767px){.chat-shell{height:calc(100vh - 120px);min-height:480px}.chat-sidebar{width:100%;flex-basis:100%;border-right:0}.chat-thread-area{display:none}.chat-shell.has-active-chat .chat-sidebar{display:none}.chat-shell.has-active-chat .chat-thread-area{display:flex}}
      `}</style>
    </div>
  );
};
