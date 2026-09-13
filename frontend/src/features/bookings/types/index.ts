/**
 * Kiểu dữ liệu và DTO cho phân hệ Đặt lịch & Buổi học (Bookings & Session Report)
 * Khớp chuẩn API Contract và SPEC-FE
 */

export type BookingStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'EXPIRED';

export type DeliveryMode = 'ONLINE' | 'OFFLINE';

export interface SessionReport {
  recordLink?: string;
  content: string;
  feedback: string;
  followUpNote?: string;
  teacherSelfRating: number; // 1 - 5
}

export interface BookingDetail {
  id: string;
  teacher: {
    id: string;
    fullName: string;
    avatarUrl?: string;
  };
  student: {
    id: string;
    fullName: string;
    avatarUrl?: string;
  };
  studentPackageId: string;
  subject: {
    id: string;
    name: string;
  };
  startTime: string;
  endTime: string;
  deliveryMode: DeliveryMode;
  meetingLink?: string | null;
  locationAddress?: string | null;
  status: BookingStatus;
  trial: boolean;
  outsideAvailabilityWarning: boolean;
  sessionReport?: SessionReport | null;
  version: number;
}

export interface CreateBookingRequest {
  studentPackageId: string;
  startTime: string;
  endTime: string;
  deliveryMode: DeliveryMode;
  meetingLink?: string | null;
  locationAddress?: string | null;
}

export interface CompleteBookingRequest {
  version: number;
  report: SessionReport;
}

export interface CancelBookingRequest {
  version: number;
  reason: string;
  initiatedBy: 'STUDENT_REQUEST' | 'TEACHER_EMERGENCY';
}

export interface TrialRequestView {
  id: string;
  teacherId: string;
  studentId: string;
  subjectId: string;
  preferredStartTime: string;
  note?: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  bookingId?: string | null;
  rejectionReason?: string | null;
  respondedAt?: string | null;
  createdAt: string;
  version: number;
}

export interface ReviewView {
  id: string;
  rating: number;
  comment?: string | null;
  createdAt: string;
}

export interface SessionReportView extends SessionReport {
  id: string;
  bookingId: string;
  teacherId: string;
  subjectId: string;
  startTime: string;
  endTime: string;
  deliveryMode: DeliveryMode;
  submittedAt: string;
}

export interface CreateTrialRequest {
  teacherId: string;
  subjectId: string;
  preferredStartTime: string;
  note?: string;
}

export interface AcceptTrialRequest {
  startTime: string;
  endTime: string;
  deliveryMode: DeliveryMode;
  meetingLink?: string | null;
  locationAddress?: string | null;
  version: number;
}

export interface RejectTrialRequest {
  reason: string;
  version: number;
}

export interface BookingFilterParams {
  status?: BookingStatus;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
  sort?: string;
}
