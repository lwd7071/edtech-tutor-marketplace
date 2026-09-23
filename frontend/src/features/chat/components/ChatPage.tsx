'use client';

import React, { useCallback, useEffect, useRef, useState, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Alert, Button, Spin } from 'antd';
import { ChatLayout } from './ChatLayout';
import { ChatThread } from './ChatThread';
import { chatApi } from '../api/chatApi';
import { ConversationView, MessageView } from '../types';
import { useChatStomp } from '../hooks/useChatStomp';
import { useAuthStore } from '@/features/auth';
import { mergeIncomingMessage } from '../model/messageReducer';
import { attachmentApi } from '@/shared/api/attachmentApi';
import type { AttachmentView } from '@/shared/api/attachmentApi';

type OutgoingCommand = {
  clientMessageId: string;
  conversationId: string;
  messageType: 'TEXT' | 'IMAGE' | 'FILE';
  content: string;
  attachmentId: string | null;
};

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
  const pendingCommands = useRef(new Map<string, OutgoingCommand>());
  const pendingTimers = useRef(new Map<string, ReturnType<typeof setTimeout>>());
  const activeConversationRef = useRef(activeConversationId);
  useEffect(() => { activeConversationRef.current = activeConversationId; }, [activeConversationId]);
  
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

  const fetchMessages = useCallback(async (convId: string, page = 0, preservePending = false) => {
    try {
      setLoadingMsg(true);
      setMessageError(false);
      const res = await chatApi.getMessages(convId, page, 20);
      const loaded = (res.data ?? []).map(m => ({
        ...m,
        content: m.content ?? '',
        createdAt: m.createdAt || m.sentAt || new Date().toISOString(),
        isOwnMessage: m.senderId === user?.id,
      })).reverse();
      if (activeConversationRef.current !== convId) return;
      for (const item of loaded) {
        if (!item.clientMessageId) continue;
        pendingCommands.current.delete(item.clientMessageId);
        const timer = pendingTimers.current.get(item.clientMessageId);
        if (timer) clearTimeout(timer);
        pendingTimers.current.delete(item.clientMessageId);
      }
      setMessages(previous => {
        const current = page === 0 ? (preservePending ? previous : []) : previous;
        return loaded.reduce<MessageView[]>((items, item) => mergeIncomingMessage(items, item, user?.id), current);
      });
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
      setMessages([]);
      void fetchMessages(activeConversationId, 0);
    } else {
      setMessages([]);
    }
  }, [activeConversationId, fetchMessages]);

  useEffect(() => () => { pendingTimers.current.forEach(clearTimeout); }, []);

  useEffect(() => {
    if (activeConversationId && isConnected) void fetchMessages(activeConversationId, 0, true);
  }, [activeConversationId, isConnected, fetchMessages]);

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
      if (lastMessage.clientMessageId) {
        pendingCommands.current.delete(lastMessage.clientMessageId);
        const timer = pendingTimers.current.get(lastMessage.clientMessageId);
        if (timer) clearTimeout(timer);
        pendingTimers.current.delete(lastMessage.clientMessageId);
      }
      
      // Update last message in conversation list
      setConversations(prev => {
        if (!prev.some(c => c.id === lastMessage.conversationId)) { void fetchConversations(0); return prev; }
        return prev.map(c =>
        c.id === lastMessage.conversationId 
          ? { ...c, lastMessagePreview: lastMessage.content ?? (lastMessage.attachment ? `Tệp: ${lastMessage.attachment.originalFilename}` : ''), unreadCount: c.id === activeConversationId ? 0 : c.unreadCount + 1 }
          : c
        );
      });
    }
  }, [lastMessage, activeConversationId, user?.id]);


  const handleSelectConversation = (id: string) => {
    router.push(`?id=${id}`);
  };

  const publishCommand = (command: OutgoingCommand) => {
    const sent = sendMessage('/app/chat.send', command);
    if (!sent) {
      setMessages(previous => previous.map(item => item.clientMessageId === command.clientMessageId ? { ...item, status: 'FAILED' } : item));
      return;
    }
    const oldTimer = pendingTimers.current.get(command.clientMessageId);
    if (oldTimer) clearTimeout(oldTimer);
    const timer = setTimeout(() => {
      setMessages(previous => previous.map(item => item.clientMessageId === command.clientMessageId && item.status === 'SENDING' ? { ...item, status: 'FAILED' } : item));
      void fetchMessages(command.conversationId, 0, true);
    }, 15_000);
    pendingTimers.current.set(command.clientMessageId, timer);
  };

  const queueMessage = (content: string, attachment?: AttachmentView, messageType: OutgoingCommand['messageType'] = 'TEXT') => {
    if (!activeConversationId || !user) return;
    const clientMessageId = crypto.randomUUID();
    const command: OutgoingCommand = { clientMessageId, conversationId: activeConversationId, messageType, content, attachmentId: attachment?.id ?? null };
    pendingCommands.current.set(clientMessageId, command);
    const newMsg: MessageView = {
      id: clientMessageId,
      conversationId: activeConversationId,
      senderId: user.id,
      senderName: user.fullName || 'Bạn',
      senderAvatar: user.avatarUrl || null,
      content,
      createdAt: new Date().toISOString(),
      isOwnMessage: true,
      clientMessageId,
      messageType,
      attachmentId: attachment?.id,
      attachment: attachment ?? null,
      status: 'SENDING',
    };
    setMessages(prev => [...prev, newMsg]);
    publishCommand(command);
  };

  const handleSendAttachment = async (file: File, content: string) => {
    if (!activeConversationId || !user) return false;
    const response = await attachmentApi.uploadAttachment(file, 'MESSAGE');
    if (!response.data) return false;
    queueMessage(content, response.data, file.type.startsWith('image/') ? 'IMAGE' : 'FILE');
    return true;
  };

  const retryMessage = (clientMessageId: string) => {
    const command = pendingCommands.current.get(clientMessageId);
    if (!command || command.conversationId !== activeConversationId) return;
    setMessages(previous => previous.map(item => item.clientMessageId === clientMessageId ? { ...item, status: 'SENDING' } : item));
    publishCommand(command);
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
          onSendMessage={content => queueMessage(content)}
          onSendAttachment={handleSendAttachment}
          onRetryMessage={retryMessage}
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
