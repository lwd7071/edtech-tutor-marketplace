export interface NotificationView {
  id: string;
  userId: string;
  type: 'BOOKING' | 'ASSIGNMENT' | 'FINANCE' | 'SYSTEM' | 'PROFILE';
  title: string;
  content: string;
  isRead: boolean;
  referenceId: string | null;
  referenceUrl: string | null;
  createdAt: string;
}

export interface NotificationReadAllResponse {
  count: number;
}
