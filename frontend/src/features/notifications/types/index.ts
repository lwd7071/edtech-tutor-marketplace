export interface NotificationView {
  id: string;
  userId: string;
  type: string;
  title: string;
  content: string;
  isRead: boolean;
  referenceId: string | null;
  referenceType: 'BOOKING' | 'TRIAL_REQUEST' | 'ASSIGNMENT' | 'SUBMISSION' | 'INVOICE' | 'PAYMENT' | 'REFUND' | 'EXTENSION' | 'PROFILE' | 'SYSTEM' | string | null;
  referenceUrl: string | null;
  createdAt: string;
}

export interface NotificationReadAllResponse {
  count: number;
}
