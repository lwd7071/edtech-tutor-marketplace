(() => {
  const thread = document.querySelector('.messages-thread__active[data-current-user-id]');
  const list = thread?.querySelector('[data-chat-message-list]');
  const form = thread?.querySelector('.message-composer');
  const conversationId = form?.querySelector('[name="conversationId"]')?.value;
  if (!thread || !list || !conversationId || !window.StompJs?.Client) return;

  const status = document.createElement('p');
  status.className = 'sr-only';
  status.setAttribute('role', 'status');
  status.setAttribute('aria-live', 'polite');
  status.textContent = 'Đang kết nối để nhận tin nhắn mới.';
  thread.querySelector('.thread-heading')?.append(status);

  const client = new window.StompJs.Client({
    brokerURL: `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws`,
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  });

  client.onConnect = () => {
    status.textContent = 'Đang nhận tin nhắn mới.';
    client.subscribe('/user/queue/messages', (frame) => {
      let message;
      try {
        message = JSON.parse(frame.body);
      } catch {
        return;
      }
      if (!message || message.conversationId !== conversationId) return;
      const duplicate = [...list.children].some((item) =>
        (message.id && item.dataset.messageId === message.id)
        || (message.clientMessageId && item.dataset.clientMessageId === message.clientMessageId));
      if (duplicate) return;

      const wasNearBottom = list.parentElement.scrollHeight - list.parentElement.scrollTop
        <= list.parentElement.clientHeight + 80;
      const item = document.createElement('li');
      if (message.id) item.dataset.messageId = message.id;
      if (message.clientMessageId) item.dataset.clientMessageId = message.clientMessageId;
      const article = document.createElement('article');
      article.className = `chat-message${message.senderId === thread.dataset.currentUserId ? ' chat-message--own' : ''}`;
      const bubble = document.createElement('div');
      bubble.className = 'chat-message__bubble';
      if (typeof message.content === 'string' && message.content) {
        const content = document.createElement('p');
        content.textContent = message.content;
        bubble.append(content);
      }
      if (message.attachment?.originalFilename) {
        const filename = document.createElement('span');
        filename.className = 'chat-message__attachment';
        filename.textContent = message.attachment.originalFilename;
        bubble.append(filename);
      }
      const footer = document.createElement('footer');
      const time = document.createElement('time');
      const sentAt = message.sentAt || message.createdAt;
      if (sentAt && !Number.isNaN(Date.parse(sentAt))) {
        time.dateTime = sentAt;
        time.textContent = new Intl.DateTimeFormat('vi-VN', {
          dateStyle: 'short', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh',
        }).format(new Date(sentAt));
      } else {
        time.textContent = 'Vừa xong';
      }
      footer.append(time);
      bubble.append(footer);
      article.append(bubble);
      item.append(article);
      list.append(item);
      thread.querySelector('.message-history__empty')?.remove();
      if (wasNearBottom) list.parentElement.scrollTop = list.parentElement.scrollHeight;
    });
  };

  client.onWebSocketClose = () => {
    status.textContent = 'Mất kết nối tin nhắn thời gian thực; bạn vẫn có thể gửi bằng biểu mẫu.';
  };
  client.onStompError = () => {
    status.textContent = 'Không thể kết nối tin nhắn thời gian thực; bạn vẫn có thể gửi bằng biểu mẫu.';
  };
  client.activate();
})();
