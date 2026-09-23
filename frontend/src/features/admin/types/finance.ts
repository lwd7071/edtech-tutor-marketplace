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
  studentId: string;
  studentName: string;
  teacherId: string;
  teacherName: string;
  bookingStatus: string;
  startTime: string;
  endTime: string;
  status: string;
  teacherConfirmedAt?: string | null;
  studentConfirmedAt?: string | null;
  confirmationDeadline: string;
  reopenDeadline?: string | null;
  netAmountVnd?: number | null;
  disputeReason?: string | null;
  disputedAt?: string | null;
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
  | 'TEACHER_APPROVED' | 'TEACHER_REJECTED'
  | 'SUBJECT_PROPOSAL_APPROVED' | 'SUBJECT_PROPOSAL_REJECTED'
  | 'USER_LOCKED' | 'USER_UNLOCKED'
  | 'REFUND_APPROVED' | 'REFUND_REJECTED' | 'REFUND_COMPLETED'
  | 'PAYOUT_PROCESSING' | 'PAYOUT_REJECTED' | 'PAYOUT_COMPLETED'
  | 'EXTENSION_APPROVED' | 'EXTENSION_REJECTED' | 'PLATFORM_SETTINGS_UPDATED'
  | 'TEACHER_RESIDENCE_UPDATED'
  | 'TEACHER_CREDENTIAL_CREATED' | 'TEACHER_CREDENTIAL_UPDATED' | 'TEACHER_CREDENTIAL_DELETED'
  | 'TEACHER_CREDENTIAL_APPROVED' | 'TEACHER_CREDENTIAL_REJECTED'
  | 'BOOKING_SETTLEMENT_REOPENED' | 'BOOKING_SETTLEMENT_RELEASED' | 'BOOKING_SETTLEMENT_RETAINED';

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
