export interface ConversationView {
  id: string;
  participantId: string;
  participantName: string;
  participantAvatar: string | null;
  lastMessagePreview: string | null;
  lastMessageAt: string | null;
  unreadCount: number;
}

export interface MessageView {
  id: string;
  conversationId: string;
  senderId: string;
  senderName?: string;
  senderAvatar?: string | null;
  content: string;
  createdAt: string;
  sentAt?: string;
  clientMessageId?: string;
  isOwnMessage: boolean;
  status?: 'SENDING' | 'SENT' | 'FAILED';
}

export interface SendMessageRequest {
  conversationId?: string; // If null, we might be starting a new conversation
  receiverId?: string; // Needed if creating a new conversation
  content: string;
}
