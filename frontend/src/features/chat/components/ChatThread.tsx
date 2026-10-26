'use client';

import { useLayoutEffect, useRef, useState } from 'react';
import { Alert, Button, Input, Spin } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import type { MessageView } from '../types';
import { formatVietnamTime } from '@/shared/lib/vietnamTime';

interface ChatThreadProps {
  conversationId: string;
  participantName: string;
  participantLabel?: 'gia sư' | 'học viên';
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

export function ChatThread({ conversationId, participantName, participantLabel = 'gia sư', messages, loading = false, error = false,
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

  return (
    <section className="tm-chat-thread" aria-label={`Cuộc trò chuyện với ${participantName || participantLabel}`}>
      <header className="tm-chat-thread__header">
        <Button className="tm-chat-thread__back" onClick={onBack} aria-label="Quay lại danh sách"><ArrowLeftOutlined /><span>Danh sách</span></Button>
        <div><h2>{participantName || 'Cuộc trò chuyện'}</h2><p>Tin nhắn riêng giữa bạn và {participantLabel}</p></div>
        {reconnecting && <span className="tm-chat-thread__status" role="status">Đang kết nối lại</span>}
      </header>
      <div className="tm-chat-thread__history" ref={messagesRef} aria-live="polite" aria-relevant="additions text"
        onScroll={event => {
          const node = event.currentTarget;
          nearBottomRef.current = node.scrollHeight - node.scrollTop - node.clientHeight < 96;
        }}>
        {hasOlderMessages && <div className="tm-chat-thread__older"><Button onClick={loadOlder}>Tải tin nhắn cũ hơn</Button></div>}
        {loading && <div className="tm-chat-thread__state" role="status"><Spin /><span>Đang tải tin nhắn…</span></div>}
        {error && <div className="tm-chat-thread__state"><Alert type="error" showIcon title="Chưa tải được tin nhắn" /><Button onClick={onRetry}>Thử lại</Button></div>}
        {!loading && !error && messages.length === 0 && <p className="tm-chat-thread__empty">Chưa có tin nhắn. Hãy gửi lời chào để bắt đầu.</p>}
        {messages.map(message => (
          <article key={message.id} className={`tm-chat-message ${message.isOwnMessage ? 'tm-chat-message--own' : ''}`}>
            <div className="tm-chat-message__content">
              {message.attachment && (message.attachment.secureUrl ? (
                <a className="tm-chat-message__attachment" href={message.attachment.secureUrl} target="_blank" rel="noopener noreferrer">
                  {message.messageType === 'IMAGE' ? <img src={message.attachment.secureUrl} alt={message.attachment.originalFilename} /> : <span>Tệp đính kèm · {message.attachment.originalFilename}</span>}
                </a>
              ) : <span className="tm-chat-message__attachment">{message.attachment.originalFilename}</span>)}
              {message.content && <p>{message.content}</p>}
              <footer><time dateTime={message.createdAt}>{formatVietnamTime(message.createdAt)}</time>
                {message.status === 'SENDING' && <span>Đang gửi…</span>}
                {message.status === 'FAILED' && <span className="tm-chat-message__retry">Chưa xác nhận đã gửi <Button size="small" onClick={() => onRetryMessage(message.clientMessageId || message.id)}>Thử lại</Button></span>}
              </footer>
            </div>
          </article>
        ))}
      </div>
      <form className="tm-chat-composer" onSubmit={event => { event.preventDefault(); void send(); }}>
        <input ref={fileInputRef} type="file" className="tm-chat-composer__file-input" aria-label="Chọn tệp đính kèm"
          onChange={event => { setFile(event.target.files?.[0] ?? null); setUploadError(''); }} />
        <div className="tm-chat-composer__tools">
          <Button disabled={reconnecting || uploading} onClick={() => fileInputRef.current?.click()}>Đính kèm tệp</Button>
          {file && <div className="tm-chat-composer__selected"><span>{file.name}</span><Button size="small" onClick={() => { setFile(null); if (fileInputRef.current) fileInputRef.current.value = ''; }}>Bỏ tệp</Button></div>}
          {uploadError && <Alert type="error" showIcon title={uploadError} />}
        </div>
        <label className="tm-chat-composer__label" htmlFor="tm-chat-message-input">Tin nhắn</label>
        <Input.TextArea id="tm-chat-message-input" aria-label="Nội dung tin nhắn" value={inputValue} onChange={event => setInputValue(event.target.value)}
          onPressEnter={event => { if (!event.shiftKey) { event.preventDefault(); void send(); } }}
          placeholder={reconnecting ? 'Đang kết nối lại…' : 'Nhập tin nhắn…'} autoSize={{ minRows: 1, maxRows: 4 }} disabled={reconnecting || uploading} />
        <Button type="primary" htmlType="submit" loading={uploading} disabled={(!inputValue.trim() && !file) || reconnecting}
          aria-label={file ? 'Gửi tệp đính kèm' : 'Gửi tin nhắn'}>{file ? 'Gửi tệp' : 'Gửi tin nhắn'}</Button>
      </form>
      <style>{`
        .tm-chat-thread{display:flex;flex-direction:column;width:100%;min-width:0;min-height:0}
        .tm-chat-thread__header{display:flex;align-items:center;gap:16px;padding:14px 22px;border-bottom:1px solid #dedbd1;min-height:78px}
        .tm-chat-thread__header h2{margin:0;font-size:18px;line-height:1.3}.tm-chat-thread__header p{margin:4px 0 0;color:#625f57;font-size:13px}
        .tm-chat-thread__back{display:none}.tm-chat-thread__status{margin-left:auto;color:#695200;font-size:13px}
        .tm-chat-thread__history{flex:1;min-height:0;overflow-y:auto;padding:24px clamp(16px,4vw,48px);background:#faf8f2;scrollbar-color:#aaa38f #eeeae0;scrollbar-width:thin}
        .tm-chat-thread__older,.tm-chat-thread__state{display:flex;justify-content:center;align-items:center;gap:12px;padding:16px}
        .tm-chat-thread__empty{margin:0;padding:48px 16px;color:#625f57;text-align:center}
        .tm-chat-message{display:flex;justify-content:flex-start;margin:0 0 14px}.tm-chat-message--own{justify-content:flex-end}
        .tm-chat-message__content{max-width:min(72%,620px);padding:10px 14px;background:#fff;border:1px solid #dedbd1;border-radius:12px 12px 12px 3px;overflow-wrap:anywhere}
        .tm-chat-message--own .tm-chat-message__content{background:#fff0ae;border-color:#e8cf68;border-radius:12px 12px 3px 12px}
        .tm-chat-message__content p{margin:0;white-space:pre-wrap}.tm-chat-message__content footer{display:flex;flex-wrap:wrap;justify-content:flex-end;align-items:center;gap:8px;margin-top:6px;color:#625f57;font-size:11px}
        .tm-chat-message__attachment{display:block;margin-bottom:6px;overflow-wrap:anywhere}.tm-chat-message__attachment img{display:block;max-width:min(280px,100%);max-height:240px;object-fit:contain}
        .tm-chat-message__retry{display:flex;align-items:center;gap:6px;color:#8b2a1c}
        .tm-chat-composer{display:grid;grid-template-columns:minmax(0,1fr) auto;align-items:end;gap:10px;padding:14px 18px;border-top:1px solid #dedbd1;background:#fffefa}
        .tm-chat-composer__tools{grid-column:1/-1;display:flex;align-items:center;gap:10px;min-width:0}.tm-chat-composer__selected{display:flex;min-width:0;align-items:center;gap:8px}.tm-chat-composer__selected span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
        .tm-chat-composer__label{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap}.tm-chat-composer__file-input{position:absolute;width:1px;height:1px;opacity:0;pointer-events:none}
        .tm-chat-composer :focus-visible{outline:3px solid #8d7000;outline-offset:2px}
        @media(max-width:767px){.tm-chat-thread__header{min-height:68px;padding:10px 12px;gap:10px}.tm-chat-thread__header p{display:none}.tm-chat-thread__back{display:inline-flex;align-items:center;gap:6px;padding-inline:5px}.tm-chat-thread__history{padding:16px 12px}.tm-chat-message__content{max-width:88%}.tm-chat-composer{grid-template-columns:minmax(0,1fr) auto;gap:8px;padding:10px 12px calc(12px + env(safe-area-inset-bottom))}.tm-chat-composer__tools{gap:8px}.tm-chat-composer__tools>.ant-btn{min-height:40px}.tm-chat-composer .ant-input{font-size:16px}}
        @media(prefers-reduced-motion:reduce){.tm-chat-thread__history{scroll-behavior:auto}}
      `}</style>
    </section>
  );
}
