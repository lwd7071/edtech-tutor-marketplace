'use client';

import React, { useCallback, useEffect, useState, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Alert, Button, Spin } from 'antd';
import { ChatLayout } from './ChatLayout';
import { ChatThread } from './ChatThread';
import { chatApi } from '../api/chatApi';
import { ConversationView, MessageView } from '../types';
import { useChatStomp } from '../hooks/useChatStomp';
import { useAuthStore } from '@/features/auth';
import { mergeIncomingMessage } from '../model/messageReducer';

function ChatPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const activeConversationId = searchParams.get('id') || undefined;
  
  const [conversations, setConversations] = useState<ConversationView[]>([]);
  const [messages, setMessages] = useState<MessageView[]>([]);
  const [loadingConv, setLoadingConv] = useState(true);
  const [loadingMsg, setLoadingMsg] = useState(false);
  const [conversationError, setConversationError] = useState(false);
  const [messageError, setMessageError] = useState(false);
  const [conversationPage, setConversationPage] = useState(0);
  const [hasMoreConversations, setHasMoreConversations] = useState(false);
  const [messagePage, setMessagePage] = useState(0);
  const [hasOlderMessages, setHasOlderMessages] = useState(false);
  
  const { user } = useAuthStore();
  const { lastMessage, sendMessage, reconnecting, isConnected } = useChatStomp();

  const fetchConversations = async (page = 0) => {
    try {
      setLoadingConv(true);
      setConversationError(false);
      const res = await chatApi.getConversations(page, 20);
      setConversations(previous => page === 0 ? (res.data || []) : [...previous, ...(res.data || []).filter(item => !previous.some(old => old.id === item.id))]);
      setConversationPage(page);
      setHasMoreConversations((res.meta?.page ?? page) + 1 < (res.meta?.totalPages ?? 0));
    } catch (e) {
      console.error(e);
      setConversationError(true);
    } finally {
      setLoadingConv(false);
    }
  };

  const fetchMessages = useCallback(async (convId: string, page = 0) => {
    try {
      setLoadingMsg(true);
      setMessageError(false);
      const res = await chatApi.getMessages(convId, page, 20);
      const loaded = (res.data ?? []).map(m => ({
        ...m,
        createdAt: m.createdAt || m.sentAt || new Date().toISOString(),
        isOwnMessage: m.senderId === user?.id,
      })).reverse();
      setMessages(previous => page === 0 ? loaded : [...loaded, ...previous]);
      setMessagePage(page);
      setHasOlderMessages((res.meta?.page ?? page) + 1 < (res.meta?.totalPages ?? 0));
    } catch (e) {
      console.error(e);
      setMessageError(true);
    } finally {
      setLoadingMsg(false);
    }
  }, [user?.id]);

  useEffect(() => {
    fetchConversations();
  }, []);

  useEffect(() => {
    if (activeConversationId) {
      fetchMessages(activeConversationId, 0);
    } else {
      setMessages([]);
    }
  }, [activeConversationId, fetchMessages]);

  useEffect(() => {
    if (activeConversationId && isConnected) {
      sendMessage('/app/chat.read', { conversationId: activeConversationId });
      setConversations(previous => previous.map(conversation => conversation.id === activeConversationId ? { ...conversation, unreadCount: 0 } : conversation));
    }
  }, [activeConversationId, isConnected, sendMessage]);

  // Handle incoming STOMP messages
  useEffect(() => {
    if (lastMessage) {
      if (lastMessage.conversationId === activeConversationId) {
        setMessages(prev => mergeIncomingMessage(prev, lastMessage, user?.id));
      }
      
      // Update last message in conversation list
      setConversations(prev => {
        if (!prev.some(c => c.id === lastMessage.conversationId)) { void fetchConversations(0); return prev; }
        return prev.map(c =>
        c.id === lastMessage.conversationId 
          ? { ...c, lastMessagePreview: lastMessage.content, unreadCount: c.id === activeConversationId ? 0 : c.unreadCount + 1 }
          : c
        );
      });
    }
  }, [lastMessage, activeConversationId, user?.id]);


  const handleSelectConversation = (id: string) => {
    router.push(`?id=${id}`);
  };

  const handleSendMessage = (content: string) => {
    if (!activeConversationId || !user) return;
    
    // Optimistic update
    const clientMessageId = crypto.randomUUID();
    const newMsg: MessageView = {
      id: clientMessageId,
      conversationId: activeConversationId,
      senderId: user.id,
      senderName: user.fullName || 'Bạn',
      senderAvatar: user.avatarUrl || null,
      content,
      createdAt: new Date().toISOString(),
      isOwnMessage: true,
      status: 'SENDING'
    };
    setMessages(prev => [...prev, newMsg]);

    const destination = `/app/chat.send`; // Standard Spring STOMP prefix
    const success = sendMessage(destination, {
      clientMessageId,
      conversationId: activeConversationId,
      messageType: 'TEXT',
      content
    });

    if (success) {
      setMessages(prev => prev.map(m => m.id === clientMessageId ? { ...m, status: 'SENT' } : m));
    } else {
      setMessages(prev => prev.map(m => m.id === clientMessageId ? { ...m, status: 'FAILED' } : m));
    }
  };

  const activeConv = conversations.find(c => c.id === activeConversationId);

  return (
    <ChatLayout
      conversations={conversations}
      activeConversationId={activeConversationId}
      loadingConversations={loadingConv}
      error={conversationError}
      onRetry={fetchConversations}
      reconnecting={reconnecting}
      onSelectConversation={handleSelectConversation}
      hasMoreConversations={hasMoreConversations}
      onLoadMoreConversations={() => void fetchConversations(conversationPage + 1)}
    >
      {activeConversationId && activeConv ? (
        <ChatThread
          conversationId={activeConversationId}
          participantName={activeConv.participantName}
          messages={messages}
          loading={loadingMsg}
          onSendMessage={handleSendMessage}
          onBack={() => router.push('?')}
          error={messageError}
          onRetry={() => fetchMessages(activeConversationId)}
          reconnecting={reconnecting}
          hasOlderMessages={hasOlderMessages}
          onLoadOlder={() => void fetchMessages(activeConversationId, messagePage + 1)}
        />
      ) : (
        <div style={{display:'flex',flexDirection:'column',alignItems:'center',justifyContent:'center',height:'100%',color:'var(--color-text-secondary)',padding:32,textAlign:'center'}}>
          <div style={{fontSize:56,marginBottom:16,opacity:.25}}>💬</div>
          <h3 style={{fontSize:20,color:'var(--color-text-primary)',margin:'0 0 8px'}}>Chưa chọn cuộc trò chuyện</h3>
          <p>Hãy chọn một cuộc trò chuyện từ danh sách bên trái để bắt đầu nhắn tin.</p>
        </div>
      )}
    </ChatLayout>
  );
}
export function ChatPage() {
  return (
    <div style={{background:'var(--color-surface)',borderRadius:12,overflow:'hidden',border:'1px solid var(--color-border)'}}>
      <Suspense fallback={<div style={{padding:32,textAlign:'center'}}><Spin size="large" /></div>}>
        <ChatPageContent />
      </Suspense>
    </div>
  );
}
