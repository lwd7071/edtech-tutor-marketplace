/**
 * Kiểu dữ liệu và DTO cho phân hệ Gói học sinh (Student Packages)
 * Khớp chuẩn API Contract và SPEC-FE
 */

export type StudentPackageStatus =
  | 'PENDING_PAYMENT'
  | 'ACTIVE'
  | 'COMPLETED'
  | 'LOCKED_EXPIRED'
  | 'REFUND_PENDING'
  | 'REFUNDED';

export interface TeacherReference {
  id: string;
  fullName: string;
  avatarUrl?: string;
}

export interface SubjectReference {
  id: string;
  name: string;
}

export interface StudentPackageSummary {
  id: string;
  teacher: TeacherReference;
  subject: SubjectReference;
  packageName: string;
  totalSessions: number;
  remainingSessions: number;
  reservedSessions: number;
  completedSessions: number;
  refundedSessions: number;
  purchasePriceVnd: number;
  startsAt?: string;
  expiresAt?: string;
  status: StudentPackageStatus;
  lockedReason?: string | null;
  version: number;
}

export interface StudentPackageDetail extends StudentPackageSummary {
  description?: string;
  sessionDurationMinutes?: number;
}

export interface StudentPackageFilterParams {
  status?: StudentPackageStatus;
  page?: number;
  size?: number;
  sort?: string;
}
