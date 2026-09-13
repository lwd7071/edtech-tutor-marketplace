/**
 * Kiểu dữ liệu và DTO của phân hệ Quản trị (Admin)
 * Khớp hoàn toàn với Backend AdminApprovalController & Facade DTOs
 */

export interface TeacherDocumentSnapshot {
  id: string;
  type: string;
  title: string;
  secureUrl: string;
  mimeType: string;
  fileSize: number;
  verificationStatus: string;
}

export interface TeacherApprovalSnapshot {
  teacherProfileId: string;
  userId: string;
  fullName?: string;
  email?: string;
  bio?: string;
  yearsOfExperience?: number;
  languages?: string[];
  supportsOnline?: boolean;
  supportsOffline?: boolean;
  locationAddress?: string | null;
  introductionVideoUrl?: string | null;
  status: 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED';
  rejectionReason?: string | null;
  approvedBy?: string | null;
  approvedAt?: string | null;
  documents: TeacherDocumentSnapshot[];
}

export interface SubjectProposalSnapshot {
  proposalId: string;
  id?: string;
  teacherId: string;
  teacherName?: string;
  proposedName: string;
  proposedSubjectName?: string;
  educationLevel: string;
  proposedCategory?: string;
  description: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  reviewNote?: string | null;
  reviewedBy?: string | null;
  reviewedAt?: string | null;
  subjectId?: string | null;
  createdAt?: string;
  version: number;
}

export interface IdentitySnapshot {
  id: string;
  email: string;
  fullName: string;
  roleName: 'STUDENT' | 'TEACHER' | 'ADMIN';
  statusName: 'ACTIVE' | 'LOCKED' | 'PENDING' | 'APPROVED';
  avatarUrl?: string | null;
  notifyParent?: boolean;
  parentEmail?: string | null;
}

// Request Payload DTOs
export interface ApproveTeacherRequest {
  note?: string;
}

export interface RejectRequest {
  reason: string;
}

export interface SubjectProposalRejectRequest {
  reason: string;
  version: number;
}

export interface ApproveSubjectProposalRequest {
  resolution: 'CREATE_NEW' | 'LINK_EXISTING';
  existingSubjectId?: string;
  code?: string;
  name?: string;
  educationLevel?: 'ELEMENTARY' | 'MIDDLE_SCHOOL' | 'HIGH_SCHOOL' | 'UNIVERSITY' | 'OTHER';
  description?: string;
  note?: string;
  version: number;
}

export interface ChangeUserStatusRequest {
  status: 'ACTIVE' | 'LOCKED';
  reason: string;
}

export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: string;
}

export * from './finance';
