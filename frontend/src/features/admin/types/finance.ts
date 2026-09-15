/**
 * Admin Finance & System Types for Member B
 * Khớp 100% với Admin Controllers trong Backend
 */


export interface AdminDashboardView {
  totalGmvVnd: number;
  totalCommissionVnd: number;
  totalTeachers: number;
  totalStudents: number;
  totalBookings: number;
  completedBookings: number;
  scheduledBookings: number;
  cancelledBookings: number;
  pendingPayoutsCount: number;
  pendingPayoutsAmountVnd: number;
  pendingRefundsCount: number;
}

export interface BookingSettlementAdminView {
  bookingId: string;
  studentId?: string;
  teacherId?: string;
  status: string;
  teacherConfirmedAt?: string | null;
  studentConfirmedAt?: string | null;
  confirmationDeadline?: string | null;
  reopenDeadline?: string | null;
  netAmountVnd?: number | null;
  disputeReason?: string | null;
  version: number;
}

export interface PlatformSettingsView {
  id: string;
  commissionRate: number;
  bayesianMinimumReviews: number;
  bookingReminderHours: number;
  bookingExpirationHours: number;
  updatedAt: string;
}

export interface UpdatePlatformSettingsRequest {
  commissionRate: number;
  bayesianMinimumReviews: number;
  bookingReminderHours: number;
  bookingExpirationHours: number;
}

export type AuditAction =
  | 'CREATE'
  | 'UPDATE'
  | 'DELETE'
  | 'APPROVE'
  | 'REJECT'
  | 'LOCK'
  | 'UNLOCK'
  | 'LOGIN'
  | 'LOGOUT'
  | 'REFUND_APPROVED' | 'REFUND_REJECTED' | 'REFUND_COMPLETED'
  | 'PAYOUT_PROCESSING' | 'PAYOUT_REJECTED' | 'PAYOUT_COMPLETED'
  | 'EXTENSION_APPROVED' | 'EXTENSION_REJECTED' | 'PLATFORM_SETTINGS_UPDATED';

export interface AuditLogView {
  id: string;
  actorId: string;
  action: AuditAction;
  targetType: string;
  targetId: string;
  beforeData?: Record<string, unknown> | null;
  afterData?: Record<string, unknown> | null;
  ipAddress?: string | null;
  userAgent?: string | null;
  createdAt: string;
}

export interface ProcessPayoutRequest {
  version: number;
}

export interface CompleteTransferRequest {
  bankReference: string;
  transferredAt: string;
  proof: File;
  version: number;
}

export interface ApproveRefundRequest {
  approvedSessions: number;
  adminNote?: string;
  version: number;
}

export interface RejectFinanceRequest {
  reason: string;
  version: number;
}

export interface ApproveExtensionRequest {
  approvedExpiryDate: string;
  adminNote?: string;
}
