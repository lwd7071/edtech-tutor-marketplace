export interface AdminTeacherApprovalView {
  teacherId: string;
  fullName: string;
  email: string;
  bio: string;
  education: string;
  experienceYears: number;
  certificates: Array<{ name: string; url: string }>;
  submittedAt: string;
  status: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED';
}

export interface AdminSubjectProposalView {
  id: string;
  teacherId: string;
  teacherName: string;
  subjectName: string;
  description: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdAt: string;
}

export interface AdminDashboardView {
  totalUsers: number;
  totalTeachers: number;
  totalStudents: number;
  totalRevenue: number;
  platformProfit: number;
  activeBookings: number;
}
