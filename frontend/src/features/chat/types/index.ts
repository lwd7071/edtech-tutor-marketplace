export interface ConversationView {
  id: string;
  participantId: string; // The ID of the other user
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
  senderName: string;
  senderAvatar: string | null;
  content: string;
  createdAt: string;
  isOwnMessage: boolean; // Computed on frontend or provided by backend
  status?: 'SENDING' | 'SENT' | 'FAILED'; // Local state
}

export interface SendMessageRequest {
  conversationId?: string; // If null, we might be starting a new conversation
  receiverId?: string; // Needed if creating a new conversation
  content: string;
}
