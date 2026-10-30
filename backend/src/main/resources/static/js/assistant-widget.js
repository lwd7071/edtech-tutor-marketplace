(() => {
  if (document.getElementById('tm-assistant-root')) return;

  const stylesheet = document.createElement('link');
  stylesheet.rel = 'stylesheet';
  stylesheet.href = '/css/assistant-widget.css';
  document.head.append(stylesheet);

  const template = document.createElement('template');
  template.innerHTML = `
    <div class="tm-assistant" id="tm-assistant-root">
      <section class="tm-assistant__panel" id="tm-assistant-panel" role="dialog" aria-labelledby="tm-assistant-title" hidden>
        <header class="tm-assistant__header">
          <div class="tm-assistant__identity">
            <span class="tm-assistant__mark" aria-hidden="true">T</span>
            <div><h2 id="tm-assistant-title">Trợ lý Tutor Match</h2><p>Hướng dẫn dùng Tutor Match và tìm gia sư</p></div>
          </div>
          <button class="tm-assistant__close" type="button" aria-label="Đóng trợ lý">×</button>
        </header>
        <p class="tm-assistant__privacy">Nội dung bạn gửi sẽ được xử lý bởi Groq để tạo câu trả lời. Đừng nhập email, số điện thoại hoặc thông tin cá nhân.</p>
        <div class="tm-assistant__messages" role="log" aria-label="Tin nhắn với trợ lý" aria-live="polite" aria-relevant="additions text">
          <div class="tm-assistant__message tm-assistant__message--assistant"><p>Chào bạn! Mình có thể hướng dẫn sử dụng Tutor Match và tìm gia sư từ danh sách công khai.</p></div>
        </div>
        <div class="tm-assistant__suggestions" aria-label="Câu hỏi gợi ý">
          <button type="button">Hướng dẫn sử dụng website Tutor Match?</button>
          <button type="button">Có gia sư dạy Toán cấp 3 không?</button>
          <button type="button">Học thử và đặt lịch thế nào?</button>
        </div>
        <form class="tm-assistant__form">
          <label class="tm-assistant__sr-only" for="tm-assistant-input">Nhập câu hỏi</label>
          <textarea id="tm-assistant-input" rows="1" maxlength="1000" placeholder="Bạn muốn tìm hiểu điều gì?" required></textarea>
          <button class="tm-assistant__send" type="submit" aria-label="Gửi câu hỏi">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 12 20 4l-5 16-3.5-6.5L4 12Z"/><path d="M11.5 13.5 20 4"/></svg>
          </button>
        </form>
        <p class="tm-assistant__hint">Enter để gửi · Shift+Enter xuống dòng</p>
      </section>
      <button class="tm-assistant__launcher" type="button" aria-expanded="false" aria-controls="tm-assistant-panel" aria-label="Mở trợ lý Tutor Match">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 11.5a7.5 7.5 0 0 1-7.5 7.5 8.4 8.4 0 0 1-3.2-.6L4 20l1.6-4A7.5 7.5 0 1 1 20 11.5Z"/><path d="M8.5 11.5h.01m3.5 0h.01m3.5 0h.01"/></svg>
        <span>Hỏi trợ lý</span>
      </button>
    </div>`;
  document.body.append(template.content);

  const root = document.getElementById('tm-assistant-root');
  const panel = root.querySelector('.tm-assistant__panel');
  const launcher = root.querySelector('.tm-assistant__launcher');
  const close = root.querySelector('.tm-assistant__close');
  const form = root.querySelector('.tm-assistant__form');
  const input = root.querySelector('#tm-assistant-input');
  const send = root.querySelector('.tm-assistant__send');
  const log = root.querySelector('.tm-assistant__messages');
  const suggestions = root.querySelector('.tm-assistant__suggestions');
  const history = [];

  function setOpen(open) {
    panel.hidden = !open;
    launcher.setAttribute('aria-expanded', String(open));
    launcher.setAttribute('aria-label', open ? 'Đóng trợ lý Tutor Match' : 'Mở trợ lý Tutor Match');
    if (open) input.focus();
    else launcher.focus();
  }

  function scrollMessages() {
    log.scrollTop = log.scrollHeight;
  }

  function addMessage(role, text, outcome, sources, pending = false) {
    const article = document.createElement('article');
    article.className = `tm-assistant__message tm-assistant__message--${role}`;
    if (pending) article.classList.add('tm-assistant__message--pending');

    const paragraph = document.createElement('p');
    if (outcome === 'CONTACT_ADMIN') {
      const email = 'luongvietvidong@gmail.com';
      const emailIndex = text.indexOf(email);
      paragraph.append(document.createTextNode(text.slice(0, emailIndex)));
      const link = document.createElement('a');
      link.href = 'mailto:luongvietvidong@gmail.com';
      link.textContent = email;
      paragraph.append(link, document.createTextNode(text.slice(emailIndex + email.length)));
    } else {
      paragraph.textContent = text;
    }
    article.append(paragraph);

    if (Array.isArray(sources) && sources.length) {
      const list = document.createElement('ul');
      list.className = 'tm-assistant__sources';
      sources.forEach((source) => {
        if (typeof source.url !== 'string' || !source.url.startsWith('/') || source.url.startsWith('//')) return;
        const target = new URL(source.url, window.location.origin);
        if (target.origin !== window.location.origin) return;
        const item = document.createElement('li');
        const link = document.createElement('a');
        link.href = target.href;
        link.textContent = source.label;
        item.append(link);
        list.append(item);
      });
      if (list.childElementCount) article.append(list);
    }

    log.append(article);
    scrollMessages();
    return article;
  }

  async function sendMessage(message, renderUser = true) {
    const cleanMessage = message.trim();
    if (!cleanMessage || send.disabled) return;
    if (renderUser) addMessage('user', cleanMessage);
    suggestions.hidden = true;
    input.value = '';
    send.disabled = true;
    log.setAttribute('aria-busy', 'true');
    const pending = addMessage('assistant', 'Đang tìm thông tin phù hợp…', null, [], true);

    try {
      const response = await fetch('/api/public/assistant/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: cleanMessage, history: history.slice(-12) })
      });
      const envelope = await response.json();
      if (!response.ok || !envelope.success || !envelope.data) {
        const error = new Error(envelope.message || 'Trợ lý hiện chưa thể trả lời. Vui lòng thử lại.');
        error.status = response.status;
        throw error;
      }

      pending.remove();
      addMessage('assistant', envelope.data.reply, envelope.data.outcome, envelope.data.sources);
      history.push({ role: 'USER', content: cleanMessage }, { role: 'ASSISTANT', content: envelope.data.reply });
      if (history.length > 12) history.splice(0, history.length - 12);
    } catch (error) {
      pending.remove();
      const copy = error.status === 429
        ? 'Bạn gửi hơi nhanh. Vui lòng đợi một chút rồi thử lại.'
        : 'Trợ lý đang tạm thời không khả dụng. Bạn có thể thử lại sau.';
      const errorBubble = addMessage('assistant', copy);
      errorBubble.classList.add('tm-assistant__message--error');
      errorBubble.setAttribute('role', 'alert');
      const retry = document.createElement('button');
      retry.type = 'button';
      retry.className = 'tm-assistant__retry';
      retry.textContent = 'Thử lại';
      retry.addEventListener('click', () => {
        errorBubble.remove();
        sendMessage(cleanMessage, false);
      });
      errorBubble.append(retry);
    } finally {
      send.disabled = false;
      log.removeAttribute('aria-busy');
    }
  }

  launcher.addEventListener('click', () => setOpen(panel.hidden));
  close.addEventListener('click', () => setOpen(false));
  panel.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') setOpen(false);
  });
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    sendMessage(input.value);
  });
  input.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      form.requestSubmit();
    }
  });
  suggestions.addEventListener('click', (event) => {
    const button = event.target.closest('button');
    if (button) sendMessage(button.textContent);
  });
})();
