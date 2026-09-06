'use client';

import React, { useRef, useEffect, useState } from 'react';
import { Button, Input, Spin, Typography } from 'antd';
import { SendOutlined, ArrowLeftOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { MessageView } from '../types';
import { useChatStomp } from '../hooks/useChatStomp';

interface ChatThreadProps {
  conversationId: string;
  participantName: string;
  messages: MessageView[];
  loading?: boolean;
  onSendMessage: (content: string) => void;
  onBack?: () => void;
}

export const ChatThread: React.FC<ChatThreadProps> = ({
  conversationId,
  participantName,
  messages,
  loading = false,
  onSendMessage,
  onBack
}) => {
  const [inputValue, setInputValue] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const { reconnecting } = useChatStomp();

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSend = () => {
    if (!inputValue.trim() || reconnecting) return;
    onSendMessage(inputValue);
    setInputValue('');
  };

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="p-4 border-b border-border bg-surface flex items-center gap-3 shadow-sm z-10">
        <Button 
          type="text" 
          icon={<ArrowLeftOutlined />} 
          className="md:hidden"
          onClick={onBack}
        />
        <h3 className="m-0 font-bold text-lg">{participantName}</h3>
      </div>

      {/* Message List */}
      <div className="flex-1 overflow-y-auto p-4 bg-neutral-50 flex flex-col gap-4">
        {loading && <div className="text-center py-4"><Spin /></div>}
        
        {messages.map((msg) => {
          const isOwn = msg.isOwnMessage;
          return (
            <div 
              key={msg.id} 
              className={`flex ${isOwn ? 'justify-end' : 'justify-start'}`}
            >
              <div 
                className={`
                  max-w-[75%] md:max-w-[60%] rounded-2xl px-4 py-2 relative
                  ${isOwn 
                    ? 'bg-primary-600 text-white rounded-tr-sm' 
                    : 'bg-white border border-border text-text-primary rounded-tl-sm'
                  }
                `}
              >
                <div className="whitespace-pre-wrap break-words">{msg.content}</div>
                <div 
                  className={`text-[11px] mt-1 text-right ${isOwn ? 'text-primary-100' : 'text-text-secondary'}`}
                >
                  {new Date(msg.createdAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                  {msg.status === 'FAILED' && <ExclamationCircleOutlined className="text-error-500 ml-1" title="Gửi lỗi" />}
                </div>
              </div>
            </div>
          );
        })}
        <div ref={messagesEndRef} />
        
        {/* Screen Reader region for new messages */}
        <div aria-live="polite" className="sr-only">
          {messages.length > 0 && messages[messages.length - 1].content}
        </div>
      </div>

      {/* Composer */}
      <div className="p-4 bg-surface border-t border-border">
        <div className="flex gap-2">
          <Input.TextArea
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onPressEnter={(e) => {
              if (!e.shiftKey) {
                e.preventDefault();
                handleSend();
              }
            }}
            placeholder={reconnecting ? "Đang kết nối lại..." : "Nhập tin nhắn..."}
            autoSize={{ minRows: 1, maxRows: 4 }}
            disabled={reconnecting}
            className="flex-1 rounded-xl"
          />
          <Button 
            type="primary" 
            shape="circle" 
            icon={<SendOutlined />} 
            size="large"
            disabled={!inputValue.trim() || reconnecting}
            onClick={handleSend}
            className="shrink-0 self-end"
          />
        </div>
      </div>
    </div>
  );
};
