'use client';

import React, { useEffect, useState, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Spin } from 'antd';
import { ChatLayout } from './ChatLayout';
import { ChatThread } from './ChatThread';
import { chatApi } from '../api/chatApi';
import { ConversationView, MessageView } from '../types';
import { useChatStomp } from '../hooks/useChatStomp';
import { useAuthStore } from '@/features/auth';

function ChatPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const activeConversationId = searchParams.get('id') || undefined;
  
  const [conversations, setConversations] = useState<ConversationView[]>([]);
  const [messages, setMessages] = useState<MessageView[]>([]);
  const [loadingConv, setLoadingConv] = useState(true);
  const [loadingMsg, setLoadingMsg] = useState(false);
  
  const { user } = useAuthStore();
  const { lastMessage, sendMessage, isConnected } = useChatStomp();

  const fetchConversations = async () => {
    try {
      setLoadingConv(true);
      const res = await chatApi.getConversations();
      setConversations(res.data || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoadingConv(false);
    }
  };

  const fetchMessages = async (convId: string) => {
    try {
      setLoadingMsg(true);
      const res = await chatApi.getMessages(convId);
      setMessages(res.data?.map(m => ({ ...m, isOwnMessage: m.senderId === user?.id })) || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoadingMsg(false);
    }
  };

  useEffect(() => {
    fetchConversations();
  }, []);

  useEffect(() => {
    if (activeConversationId) {
      fetchMessages(activeConversationId);
    } else {
      setMessages([]);
    }
  }, [activeConversationId]);

  // Handle incoming STOMP messages
  useEffect(() => {
    if (lastMessage) {
      if (lastMessage.conversationId === activeConversationId) {
        setMessages(prev => [...prev, { ...lastMessage, isOwnMessage: lastMessage.senderId === user?.id }]);
      }
      
      // Update last message in conversation list
      setConversations(prev => prev.map(c => 
        c.id === lastMessage.conversationId 
          ? { ...c, lastMessagePreview: lastMessage.content, unreadCount: c.id === activeConversationId ? 0 : c.unreadCount + 1 }
          : c
      ));
    }
  }, [lastMessage, activeConversationId, user?.id]);


  const handleSelectConversation = (id: string) => {
    router.push(`?id=${id}`);
  };

  const handleSendMessage = (content: string) => {
    if (!activeConversationId || !user) return;
    
    // Optimistic update
    const tempId = `temp-${Date.now()}`;
    const newMsg: MessageView = {
      id: tempId,
      conversationId: activeConversationId,
      senderId: user.id,
      senderName: user.fullName || 'Me',
      senderAvatar: user.avatarUrl || null,
      content,
      createdAt: new Date().toISOString(),
      isOwnMessage: true,
      status: 'SENDING'
    };
    setMessages(prev => [...prev, newMsg]);

    const destination = `/app/chat.send`; // Standard Spring STOMP prefix
    const success = sendMessage(destination, {
      conversationId: activeConversationId,
      content
    });

    if (success) {
      setMessages(prev => prev.map(m => m.id === tempId ? { ...m, status: 'SENT' } : m));
    } else {
      setMessages(prev => prev.map(m => m.id === tempId ? { ...m, status: 'FAILED' } : m));
    }
  };

  const activeConv = conversations.find(c => c.id === activeConversationId);

  return (
    <ChatLayout
      conversations={conversations}
      activeConversationId={activeConversationId}
      loadingConversations={loadingConv}
      onSelectConversation={handleSelectConversation}
    >
      {activeConversationId && activeConv ? (
        <ChatThread
          conversationId={activeConversationId}
          participantName={activeConv.participantName}
          messages={messages}
          loading={loadingMsg}
          onSendMessage={handleSendMessage}
          onBack={() => router.push('?')}
        />
      ) : (
        <div className="flex flex-col items-center justify-center h-full text-text-secondary p-8 text-center">
          <div className="text-6xl mb-4 opacity-20">💬</div>
          <h3 className="text-xl font-medium text-text-primary mb-2">Chưa chọn cuộc trò chuyện</h3>
          <p>Hãy chọn một cuộc trò chuyện từ danh sách bên trái để bắt đầu nhắn tin.</p>
        </div>
      )}
    </ChatLayout>
  );
}

export function ChatPage() {
  return (
    <div className="bg-surface rounded-xl overflow-hidden shadow-sm">
      <Suspense fallback={<div className="p-8 text-center"><Spin size="large" /></div>}>
        <ChatPageContent />
      </Suspense>
    </div>
  );
}
