import { mergeIncomingMessage } from './messageReducer';
import { parseIncomingMessage } from './messageSchema';
import type { MessageView } from '../types';

describe('chat message model', () => {
  it('rejects malformed websocket payloads', () => {
    expect(parseIncomingMessage('{bad-json')).toBeNull();
    expect(parseIncomingMessage(JSON.stringify({ id: '1', content: 'missing routing fields' }))).toBeNull();
  });

  it('replaces an optimistic message and keeps chronological order', () => {
    const optimistic: MessageView = {
      id: 'client-1', conversationId: 'conversation-1', senderId: 'student-1', content: 'Xin chào',
      createdAt: '2026-09-09T10:00:00Z', isOwnMessage: true, status: 'SENDING',
    };
    const result = mergeIncomingMessage([optimistic], {
      id: 'server-1', clientMessageId: 'client-1', conversationId: 'conversation-1', senderId: 'student-1',
      content: 'Xin chào', sentAt: '2026-09-09T10:00:01Z',
    }, 'student-1');
    expect(result).toHaveLength(1);
    expect(result[0]).toMatchObject({ id: 'server-1', status: 'SENT', isOwnMessage: true });
  });
});
