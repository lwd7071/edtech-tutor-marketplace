'use client';

import React from 'react';
import { Alert, Button, Spin } from 'antd';
import { ConversationView } from '../types';
import { formatVietnamTime } from '@/shared/lib/vietnamTime';

interface ChatLayoutProps {
  conversations: ConversationView[];
  participantLabel?: 'gia sư' | 'học viên';
  activeConversationId?: string;
  loadingConversations?: boolean;
  error?: boolean;
  onRetry?: () => void;
  reconnecting?: boolean;
  onSelectConversation: (id: string) => void;
  hasMoreConversations?: boolean;
  onLoadMoreConversations?: () => void;
  children: React.ReactNode;
}

export const ChatLayout: React.FC<ChatLayoutProps> = ({
  conversations, participantLabel = 'gia sư', activeConversationId, loadingConversations = false,
  error = false, onRetry, reconnecting = false, onSelectConversation,
  hasMoreConversations = false, onLoadMoreConversations, children,
}) => (
  <section className={`tm-chat ${activeConversationId ? 'tm-chat--active' : ''}`} aria-label={`Tin nhắn với ${participantLabel}`}>
    {reconnecting && <p className="tm-chat__connection" role="status">Mất kết nối. Đang thử kết nối lại…</p>}
    <div className="tm-chat__workspace">
      <aside className="tm-chat__inbox" aria-label="Danh sách cuộc trò chuyện">
        <header className="tm-chat__inbox-heading"><h1>Tin nhắn</h1><p>Trao đổi với {participantLabel} của bạn</p></header>
        <div className="tm-chat__conversation-list">
          {loadingConversations ? (
            <div className="tm-chat__state" role="status" aria-label="Đang tải cuộc trò chuyện"><Spin /><span>Đang tải tin nhắn…</span></div>
          ) : error ? (
            <div className="tm-chat__state"><Alert type="error" showIcon title="Chưa tải được danh sách tin nhắn" /><Button onClick={onRetry}>Thử lại</Button></div>
          ) : conversations.length === 0 ? (
            <p className="tm-chat__empty">Chưa có cuộc trò chuyện với {participantLabel} nào.</p>
          ) : conversations.map((conversation) => (
            <button key={conversation.id} type="button" onClick={() => onSelectConversation(conversation.id)}
              className={`tm-chat__conversation ${activeConversationId === conversation.id ? 'is-active' : ''}`}
              aria-current={activeConversationId === conversation.id ? 'page' : undefined}>
              <span className="tm-chat__avatar" aria-hidden="true">
                <span>{(conversation.participantName || 'Người dùng').split(/\s+/).slice(-1)[0]?.slice(0, 1).toLocaleUpperCase('vi-VN')}</span>
                {conversation.participantAvatar && <img src={conversation.participantAvatar} alt="" onError={event => { event.currentTarget.hidden = true; }} />}
              </span>
              <span className="tm-chat__conversation-text">
                <span className="tm-chat__conversation-name">{conversation.participantName || 'Người dùng'}</span>
                <span className="tm-chat__conversation-meta">
                  <span className="tm-chat__conversation-preview">{conversation.lastMessagePreview || 'Bắt đầu cuộc trò chuyện'}</span>
                  {conversation.lastMessageAt && <time dateTime={conversation.lastMessageAt}>{formatVietnamTime(conversation.lastMessageAt)}</time>}
                </span>
              </span>
              {conversation.unreadCount > 0 && <span className="tm-chat__unread" aria-label={`${conversation.unreadCount} tin nhắn chưa đọc`}>{conversation.unreadCount}</span>}
            </button>
          ))}
          {!loadingConversations && !error && hasMoreConversations && <Button className="tm-chat__load-more" onClick={onLoadMoreConversations}>Tải thêm cuộc trò chuyện</Button>}
        </div>
      </aside>
      <div className="tm-chat__thread" role="region" aria-label="Nội dung cuộc trò chuyện">{children}</div>
    </div>
    <style>{`
      .tm-chat{--chat-line:#dedbd1;--chat-muted:#625f57;--chat-paper:#fffefa;--chat-wash:#f5f2e9;--chat-ink:#171714;display:flex;flex-direction:column;height:min(760px,calc(100dvh - 176px));min-height:500px;overflow:hidden;background:var(--chat-paper);color:var(--chat-ink)}
      .tm-chat__workspace{display:grid;grid-template-columns:minmax(250px,320px) minmax(0,1fr);flex:1;min-height:0}
      .tm-chat__connection{margin:0;padding:9px 18px;background:#fff4cf;color:#534100;font-size:13px;text-align:center}
      .tm-chat__inbox{display:flex;min-height:0;flex-direction:column;border-right:1px solid var(--chat-line);background:var(--chat-wash)}
      .tm-chat__inbox-heading{padding:22px 20px 17px;border-bottom:1px solid var(--chat-line);background:var(--chat-paper)}
      .tm-chat__inbox-heading h1{margin:0;font-size:24px;line-height:1.2;letter-spacing:-.025em}
      .tm-chat__inbox-heading p{margin:6px 0 0;color:var(--chat-muted);font-size:14px}
      .tm-chat__conversation-list{overflow:auto;padding:8px 10px 18px}
      .tm-chat__conversation{display:grid;width:100%;grid-template-columns:44px minmax(0,1fr) auto;align-items:center;gap:12px;padding:12px 10px;border:0;border-bottom:1px solid var(--chat-line);background:transparent;color:inherit;text-align:left;cursor:pointer}
      .tm-chat__conversation:hover,.tm-chat__conversation.is-active{background:#fff}
      .tm-chat__conversation:focus-visible,.tm-chat__load-more:focus-visible{outline:3px solid #8d7000;outline-offset:2px}
      .tm-chat__avatar{position:relative;display:grid;width:44px;height:44px;place-items:center;overflow:hidden;border-radius:50%;background:#e9e4d8;color:#514a39;font-size:15px;font-weight:650}
      .tm-chat__avatar img{position:absolute;inset:0;width:100%;height:100%;object-fit:cover}
      .tm-chat__conversation-text{display:grid;min-width:0;gap:4px}
      .tm-chat__conversation-name,.tm-chat__conversation-preview{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
      .tm-chat__conversation-name{font-size:15px;font-weight:650}
      .tm-chat__conversation-meta{display:flex;min-width:0;align-items:center;gap:8px;color:var(--chat-muted);font-size:12px}
      .tm-chat__conversation-preview{min-width:0;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:13px}
      .tm-chat__conversation-meta time{flex:none;font-variant-numeric:tabular-nums}
      .tm-chat__unread{min-width:22px;padding:2px 6px;border-radius:999px;background:#ffd43b;color:#171714;font-size:12px;font-weight:700;text-align:center}
      .tm-chat__load-more{display:block;margin:14px auto 0}
      .tm-chat__state,.tm-chat__empty{display:flex;flex-direction:column;align-items:center;gap:12px;padding:34px 16px;color:var(--chat-muted);text-align:center}
      .tm-chat__thread{display:flex;min-width:0;min-height:0;background:var(--chat-paper)}
      @media(max-width:767px){.tm-chat{height:calc(100dvh - 178px);min-height:420px}.tm-chat__workspace{display:block}.tm-chat__inbox{height:100%;border-right:0}.tm-chat__thread{display:none;height:100%}.tm-chat--active .tm-chat__inbox{display:none}.tm-chat--active .tm-chat__thread{display:flex}}
      @media(min-width:768px) and (max-width:1023px){.tm-chat__workspace{grid-template-columns:minmax(220px,280px) minmax(0,1fr)}.tm-chat__inbox-heading{padding-inline:16px}}
      @media(prefers-reduced-motion:reduce){.tm-chat *{scroll-behavior:auto!important;transition:none!important}}
    `}</style>
  </section>
);
