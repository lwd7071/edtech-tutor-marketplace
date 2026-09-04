export type StudentPackageStatus =
  | 'PENDING_PAYMENT'
  | 'ACTIVE'
  | 'COMPLETED'
  | 'LOCKED_EXPIRED'
  | 'REFUND_PENDING'
  | 'REFUNDED';

export interface StudentPackageView {
  id: string;
  studentId: string;
  teacherId: string;
  teacherName: string;
  subjectName: string;
  totalSessions: number;
  remainingSessions: number;
  heldSessions: number;
  completedSessions: number;
  price: number;
  status: StudentPackageStatus;
  expiresAt: string;
  createdAt: string;
  version: number;
}
