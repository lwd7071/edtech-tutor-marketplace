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
  
  const { user } = useAuthStore();
  const { lastMessage, sendMessage, reconnecting, isConnected } = useChatStomp();

  const fetchConversations = async () => {
    try {
      setLoadingConv(true);
      setConversationError(false);
      const res = await chatApi.getConversations();
      setConversations(res.data || []);
    } catch (e) {
      console.error(e);
      setConversationError(true);
    } finally {
      setLoadingConv(false);
    }
  };

  const fetchMessages = useCallback(async (convId: string) => {
    try {
      setLoadingMsg(true);
      setMessageError(false);
      const res = await chatApi.getMessages(convId);
      setMessages(res.data?.map(m => ({
        ...m,
        createdAt: m.createdAt || m.sentAt || new Date().toISOString(),
        isOwnMessage: m.senderId === user?.id,
      })) || []);
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
      fetchMessages(activeConversationId);
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
        setMessages(prev => {
          const received = { ...lastMessage, createdAt: lastMessage.sentAt || new Date().toISOString(), isOwnMessage: lastMessage.senderId === user?.id, status: 'SENT' as const };
          const optimisticIndex = prev.findIndex(message => message.id === lastMessage.clientMessageId);
          if (optimisticIndex < 0) return [...prev, received];
          return prev.map((message, index) => index === optimisticIndex ? received : message);
        });
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
