'use client';

import React, { useEffect, useRef, useState } from 'react';
import { Alert, Button, Input, Spin } from 'antd';
import { ArrowLeftOutlined, ExclamationCircleOutlined, SendOutlined } from '@ant-design/icons';
import { MessageView } from '../types';

interface ChatThreadProps { conversationId: string; participantName: string; messages: MessageView[]; loading?: boolean; error?: boolean; reconnecting?: boolean; onRetry?: () => void; onSendMessage: (content: string) => void; onBack?: () => void; }

export const ChatThread: React.FC<ChatThreadProps> = ({ participantName, messages, loading = false, error = false, reconnecting = false, onRetry, onSendMessage, onBack }) => {
  const [inputValue, setInputValue] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  useEffect(() => { messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages]);
  const handleSend = () => { const content = inputValue.trim(); if (!content || reconnecting) return; onSendMessage(content); setInputValue(''); };
  return (
    <section className="chat-thread">
      <header className="chat-thread-header"><Button type="text" icon={<ArrowLeftOutlined />} className="chat-back" onClick={onBack} aria-label="Quay lại danh sách" /><h3>{participantName || 'Cuộc trò chuyện'}</h3></header>
      <div className="chat-messages" aria-live="polite">
        {loading && <div className="chat-loading"><Spin /></div>}
        {error && <div className="chat-loading"><Alert type="error" showIcon title="Chưa tải được tin nhắn" /><Button onClick={onRetry}>Thử lại</Button></div>}
        {!loading && !error && messages.length === 0 && <div className="chat-no-messages">Chưa có tin nhắn. Hãy gửi lời chào để bắt đầu.</div>}
        {messages.map((message) => <div key={message.id} className={`chat-message-row ${message.isOwnMessage ? 'own' : ''}`}><div className={`chat-bubble ${message.isOwnMessage ? 'own' : ''}`}><div className="chat-content">{message.content}</div><time dateTime={message.createdAt}>{new Date(message.createdAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}{message.status === 'FAILED' && <ExclamationCircleOutlined className="chat-failed" title="Gửi thất bại" />}</time></div></div>)}
        <div ref={messagesEndRef} />
      </div>
      <footer className="chat-composer"><Input.TextArea value={inputValue} onChange={(event) => setInputValue(event.target.value)} onPressEnter={(event) => { if (!event.shiftKey) { event.preventDefault(); handleSend(); } }} placeholder={reconnecting ? 'Đang kết nối lại…' : 'Nhập tin nhắn…'} autoSize={{ minRows: 1, maxRows: 4 }} disabled={reconnecting} /><Button type="primary" shape="circle" icon={<SendOutlined />} size="large" disabled={!inputValue.trim() || reconnecting} onClick={handleSend} aria-label="Gửi tin nhắn" /></footer>
      <style>{`
        .chat-thread{display:flex;flex-direction:column;width:100%;min-height:0}.chat-thread-header{display:flex;align-items:center;gap:10px;padding:14px 18px;border-bottom:1px solid var(--color-border);min-height:64px}.chat-thread-header h3{margin:0;font-size:18px}.chat-back{display:none}.chat-messages{flex:1;overflow-y:auto;padding:20px;background:var(--color-surface-sunken)}.chat-loading{display:flex;flex-direction:column;align-items:center;gap:12px;padding:24px}.chat-no-messages{text-align:center;color:var(--color-text-secondary);padding:40px 12px}.chat-message-row{display:flex;justify-content:flex-start;margin-bottom:12px}.chat-message-row.own{justify-content:flex-end}.chat-bubble{max-width:68%;padding:9px 13px;border:1px solid var(--color-border);border-radius:16px 16px 16px 4px;background:white;overflow-wrap:anywhere}.chat-bubble.own{color:white;background:var(--color-primary-600);border-color:var(--color-primary-600);border-radius:16px 16px 4px 16px}.chat-content{white-space:pre-wrap}.chat-bubble time{display:block;margin-top:4px;text-align:right;font-size:11px;color:var(--color-text-tertiary)}.chat-bubble.own time{color:#ccfbf1}.chat-failed{margin-left:5px;color:#fecaca}.chat-composer{display:flex;align-items:flex-end;gap:10px;padding:14px 18px;border-top:1px solid var(--color-border)}
        @media(max-width:767px){.chat-back{display:inline-flex}.chat-bubble{max-width:85%}.chat-messages{padding:14px}.chat-composer{padding:12px}}
      `}</style>
    </section>
  );
};
