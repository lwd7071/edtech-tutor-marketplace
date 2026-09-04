export type BookingStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'EXPIRED';

export interface BookingView {
  id: string;
  studentPackageId: string;
  teacherId: string;
  studentId: string;
  teacherName?: string;
  studentName?: string;
  subjectName?: string;
  startTime: string;
  endTime: string;
  status: BookingStatus;
  meetingUrl?: string | null;
  cancelledReason?: string | null;
  version: number;
}

export interface SessionReportView {
  id: string;
  bookingId: string;
  feedback: string;
  homeworkAssigned?: string | null;
  rating?: number | null;
  createdAt: string;
}

export type TrialStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

export interface TrialRequestView {
  id: string;
  studentId: string;
  teacherId: string;
  subjectId: string;
  preferredTime: string;
  note?: string;
  status: TrialStatus;
}
