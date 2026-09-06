'use client';

import React, { useState } from 'react';
import { Badge, Spin } from 'antd';
import { useChatStomp } from '../hooks/useChatStomp';
import { ConversationView } from '../types';

interface ChatLayoutProps {
  conversations: ConversationView[];
  activeConversationId?: string;
  loadingConversations?: boolean;
  onSelectConversation: (id: string) => void;
  children: React.ReactNode;
}

export const ChatLayout: React.FC<ChatLayoutProps> = ({
  conversations,
  activeConversationId,
  loadingConversations = false,
  onSelectConversation,
  children
}) => {
  const { reconnecting } = useChatStomp();
  const [showSidebar, setShowSidebar] = useState(true);

  // In mobile view, if there is an active conversation, hide sidebar
  const isMobileDetailView = activeConversationId && window.innerWidth < 768;

  return (
    <div className="flex flex-col h-[calc(100vh-64px)] bg-surface border-t border-border overflow-hidden">
      {/* Reconnect Banner */}
      {reconnecting && (
        <div className="bg-warning-100 text-warning-800 px-4 py-2 text-center text-sm font-medium">
          Đang kết nối lại...
        </div>
      )}

      <div className="flex flex-1 overflow-hidden relative">
        {/* Conversation List Sidebar */}
        <div 
          className={`
            w-full md:w-[320px] shrink-0 border-r border-border bg-neutral-50 flex flex-col
            ${isMobileDetailView ? 'hidden md:flex' : 'flex'}
          `}
        >
          <div className="p-4 border-b border-border">
            <h2 className="text-lg font-bold m-0">Tin nhắn</h2>
          </div>
          
          <div className="flex-1 overflow-y-auto p-2">
            {loadingConversations ? (
              <div className="flex justify-center p-8"><Spin /></div>
            ) : conversations.length === 0 ? (
              <div className="text-center p-8 text-text-secondary text-sm">
                Chưa có tin nhắn nào
              </div>
            ) : (
              conversations.map(conv => (
                <div 
                  key={conv.id}
                  onClick={() => onSelectConversation(conv.id)}
                  className={`
                    p-3 flex gap-3 cursor-pointer rounded-lg mb-1 transition-colors
                    ${activeConversationId === conv.id ? 'bg-primary-50' : 'hover:bg-neutral-100'}
                  `}
                >
                  <Badge count={conv.unreadCount} size="small">
                    <img 
                      src={conv.participantAvatar || '/images/default-avatar.png'} 
                      alt={conv.participantName}
                      className="w-12 h-12 rounded-full object-cover border border-border"
                    />
                  </Badge>
                  <div className="flex-1 min-w-0">
                    <div className="flex justify-between items-baseline mb-1">
                      <h4 className="m-0 font-semibold text-text-primary truncate">{conv.participantName}</h4>
                      {/* Would use DateTimeText relative here */}
                    </div>
                    <p className={`m-0 text-sm truncate ${conv.unreadCount > 0 ? 'font-semibold text-text-primary' : 'text-text-secondary'}`}>
                      {conv.lastMessagePreview || 'Bắt đầu cuộc trò chuyện'}
                    </p>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Chat Thread Area */}
        <div 
          className={`
            flex-1 bg-surface flex flex-col
            ${!activeConversationId ? 'hidden md:flex' : 'flex'}
          `}
        >
          {children}
        </div>
      </div>
    </div>
  );
};
