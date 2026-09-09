import type { MessageView } from '../types';
import type { IncomingMessage } from './messageSchema';

export function mergeIncomingMessage(
  current: MessageView[],
  incoming: IncomingMessage,
  currentUserId?: string,
): MessageView[] {
  const message: MessageView = {
    ...incoming,
    createdAt: incoming.createdAt ?? incoming.sentAt ?? new Date().toISOString(),
    isOwnMessage: incoming.senderId === currentUserId,
    status: 'SENT',
  };
  const matchingIndex = current.findIndex((item) =>
    item.id === incoming.id || Boolean(incoming.clientMessageId && (item.id === incoming.clientMessageId || item.clientMessageId === incoming.clientMessageId)),
  );
  const merged = matchingIndex < 0
    ? [...current, message]
    : current.map((item, index) => index === matchingIndex ? message : item);
  return merged.sort((left, right) => left.createdAt.localeCompare(right.createdAt));
}
