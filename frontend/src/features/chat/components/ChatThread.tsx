'use client';

import { useLayoutEffect, useRef, useState } from 'react';
import { Alert, Button, Input, Spin } from 'antd';
import { ArrowLeftOutlined, PaperClipOutlined, SendOutlined } from '@ant-design/icons';
import type { MessageView } from '../types';
import { formatVietnamTime } from '@/shared/lib/vietnamTime';

interface ChatThreadProps {
  conversationId: string;
  participantName: string;
  messages: MessageView[];
  loading?: boolean;
  error?: boolean;
  reconnecting?: boolean;
  hasOlderMessages?: boolean;
  onLoadOlder?: () => void;
  onRetry?: () => void;
  onSendMessage: (content: string) => void;
  onSendAttachment: (file: File, content: string) => Promise<boolean>;
  onRetryMessage: (clientMessageId: string) => void;
  onBack?: () => void;
}

export function ChatThread({ conversationId, participantName, messages, loading = false, error = false,
  reconnecting = false, hasOlderMessages = false, onLoadOlder, onRetry, onSendMessage,
  onSendAttachment, onRetryMessage, onBack }: ChatThreadProps) {
  const [inputValue, setInputValue] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);
  const messagesRef = useRef<HTMLDivElement>(null);
  const previousConversationRef = useRef(conversationId);
  const nearBottomRef = useRef(true);
  const prependPositionRef = useRef<{ height: number; top: number } | null>(null);

  useLayoutEffect(() => {
    const container = messagesRef.current;
    if (!container || loading) return;
    if (previousConversationRef.current !== conversationId) {
      previousConversationRef.current = conversationId;
      prependPositionRef.current = null;
      nearBottomRef.current = true;
      container.scrollTop = container.scrollHeight;
      return;
    }
    if (prependPositionRef.current) {
      const { height, top } = prependPositionRef.current;
      container.scrollTop = top + container.scrollHeight - height;
      prependPositionRef.current = null;
      return;
    }
    if (nearBottomRef.current) {
      const reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
      container.scrollTo({ top: container.scrollHeight, behavior: reducedMotion ? 'instant' : 'smooth' });
    }
  }, [conversationId, loading, messages]);

  const loadOlder = () => {
    const container = messagesRef.current;
    if (container) prependPositionRef.current = { height: container.scrollHeight, top: container.scrollTop };
    onLoadOlder?.();
  };
  const send = async () => {
    if (reconnecting || uploading) return;
    const content = inputValue.trim();
    if (file) {
      setUploading(true);
      setUploadError('');
      try {
        if (await onSendAttachment(file, content)) {
          setFile(null);
          setInputValue('');
          if (fileInputRef.current) fileInputRef.current.value = '';
        } else setUploadError('Chưa tải được tệp. Bạn có thể thử gửi lại.');
      } catch {
        setUploadError('Chưa tải được tệp. Bạn có thể thử gửi lại.');
      } finally { setUploading(false); }
    } else if (content) {
      onSendMessage(content);
      setInputValue('');
    }
  };

  return <section className="chat-thread">
    <header className="chat-thread-header">
      <Button type="text" icon={<ArrowLeftOutlined />} className="chat-back" onClick={onBack} aria-label="Quay lại danh sách" />
      <h3>{participantName || 'Cuộc trò chuyện'}</h3>
    </header>
    <div className="chat-messages" ref={messagesRef} aria-live="polite"
      onScroll={event => {
        const node = event.currentTarget;
        nearBottomRef.current = node.scrollHeight - node.scrollTop - node.clientHeight < 96;
      }}>
      {loading && <div className="chat-loading"><Spin /></div>}
      {error && <div className="chat-loading"><Alert type="error" showIcon title="Chưa tải được tin nhắn" /><Button onClick={onRetry}>Thử lại</Button></div>}
      {hasOlderMessages && <div className="chat-loading"><Button onClick={loadOlder}>Tải tin nhắn cũ hơn</Button></div>}
      {!loading && !error && messages.length === 0 && <div className="chat-no-messages">Chưa có tin nhắn. Hãy gửi lời chào để bắt đầu.</div>}
      {messages.map(message => <div key={message.id} className={`chat-message-row ${message.isOwnMessage ? 'own' : ''}`}>
        <div className={`chat-bubble ${message.isOwnMessage ? 'own' : ''}`}>
          {message.attachment && (message.attachment.secureUrl ?
            <a href={message.attachment.secureUrl} target="_blank" rel="noopener noreferrer">
              {message.messageType === 'IMAGE' ? <img src={message.attachment.secureUrl} alt={message.attachment.originalFilename} className="chat-image" /> : `📎 ${message.attachment.originalFilename}`}
            </a> : <span>{message.attachment.originalFilename}</span>)}
          {message.content && <div className="chat-content">{message.content}</div>}
          <time dateTime={message.createdAt}>{formatVietnamTime(message.createdAt)}</time>
          {message.status === 'SENDING' && <span className="chat-message-state">Đang gửi…</span>}
          {message.status === 'FAILED' && <span className="chat-message-state">Chưa xác nhận đã gửi <Button size="small" onClick={() => onRetryMessage(message.clientMessageId || message.id)}>Thử lại</Button></span>}
        </div>
      </div>)}
    </div>
    <footer className="chat-composer">
      <input ref={fileInputRef} type="file" className="chat-file-input" aria-label="Chọn tệp đính kèm"
        onChange={event => { setFile(event.target.files?.[0] ?? null); setUploadError(''); }} />
      <Button icon={<PaperClipOutlined />} aria-label="Đính kèm tệp" title="Đính kèm tệp"
        disabled={reconnecting || uploading} onClick={() => fileInputRef.current?.click()} />
      <div className="chat-composer-content">
        {file && <div className="chat-selected-file"><span>{file.name}</span><Button size="small" aria-label="Bỏ tệp đính kèm" onClick={() => { setFile(null); if (fileInputRef.current) fileInputRef.current.value = ''; }}>Bỏ tệp</Button></div>}
        {uploadError && <Alert type="error" title={uploadError} />}
        <Input.TextArea aria-label="Nội dung tin nhắn" value={inputValue} onChange={event => setInputValue(event.target.value)}
          onPressEnter={event => { if (!event.shiftKey) { event.preventDefault(); void send(); } }}
          placeholder={reconnecting ? 'Đang kết nối lại…' : 'Nhập tin nhắn…'} autoSize={{ minRows: 1, maxRows: 4 }} disabled={reconnecting || uploading} />
      </div>
      <Button type="primary" icon={<SendOutlined />} size="large" loading={uploading}
        disabled={(!inputValue.trim() && !file) || reconnecting} onClick={() => void send()} aria-label={file ? 'Gửi tệp đính kèm' : 'Gửi tin nhắn'} />
    </footer>
    <style>{`
      .chat-thread{display:flex;flex-direction:column;width:100%;min-height:0}.chat-thread-header{display:flex;align-items:center;gap:10px;padding:14px 18px;border-bottom:1px solid var(--color-border);min-height:64px}.chat-thread-header h3{margin:0;font-size:18px}.chat-back{display:none}.chat-messages{flex:1;overflow-y:auto;padding:20px;background:var(--color-surface-sunken)}.chat-loading{display:flex;flex-direction:column;align-items:center;gap:12px;padding:24px}.chat-no-messages{text-align:center;color:var(--color-text-secondary);padding:40px 12px}.chat-message-row{display:flex;justify-content:flex-start;margin-bottom:12px}.chat-message-row.own{justify-content:flex-end}.chat-bubble{max-width:68%;padding:9px 13px;border:1px solid var(--color-border);border-radius:16px 16px 16px 4px;background:white;overflow-wrap:anywhere}.chat-bubble.own{color:white;background:var(--color-primary-600);border-color:var(--color-primary-600);border-radius:16px 16px 4px 16px}.chat-content{white-space:pre-wrap}.chat-image{max-width:260px;max-height:220px;border-radius:8px}.chat-bubble time{display:block;margin-top:4px;text-align:right;font-size:11px;color:var(--color-text-tertiary)}.chat-bubble.own time{color:#ccfbf1}.chat-message-state{display:flex;align-items:center;gap:6px;font-size:12px}.chat-composer{display:flex;align-items:flex-end;gap:10px;padding:14px 18px;border-top:1px solid var(--color-border)}.chat-composer-content{flex:1;min-width:0}.chat-file-input{position:absolute;width:1px;height:1px;opacity:0;pointer-events:none}.chat-selected-file{display:flex;justify-content:space-between;align-items:center;gap:8px;padding-bottom:8px;overflow-wrap:anywhere}
      @media(max-width:767px){.chat-back{display:inline-flex}.chat-bubble{max-width:85%}.chat-messages{padding:14px}.chat-composer{padding:12px}}
      @media(prefers-reduced-motion:reduce){.chat-messages{scroll-behavior:auto}}
    `}</style>
  </section>;
}
