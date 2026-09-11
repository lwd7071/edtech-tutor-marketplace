export interface ContentBlock {
  id?: string;
  type: 'TEXT' | 'IMAGE' | 'FILE';
  content?: string;
  attachmentId?: string;
}

export interface AssignmentDetail {
  id: string;
  teacherId: string;
  studentId: string;
  subjectId: string;
  title: string;
  assignmentType: 'FREEFORM' | 'SYSTEM_QUIZ';
  contentBlocks: ContentBlock[];
  quizSchema?: any;
  dueAt: string;
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED';
}

export interface SubmissionDetail {
  id: string;
  assignmentId: string;
  studentId: string;
  contentBlocks: ContentBlock[];
  submittedAt: string | null;
  status: 'DRAFT' | 'SUBMITTED' | 'GRADED';
  score: number | null;
  feedbackText: string | null;
  gradedAt: string | null;
}

export interface CreateAssignmentRequest {
  studentId: string;
  subjectId: string;
  title: string;
  assignmentType: 'FREEFORM' | 'SYSTEM_QUIZ';
  contentBlocks: ContentBlock[];
  quizSchema?: any;
  dueAt: string;
  status: 'DRAFT' | 'PUBLISHED';
}

export interface CreateSubmissionRequest {
  contentBlocks: ContentBlock[];
}

export interface GradeSubmissionRequest {
  score: number;
  feedbackText?: string;
}

export interface TeacherAssignmentListItem {
  id: string;
  title: string;
  studentName: string;
  subjectName: string;
  dueAt: string;
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED';
  submittedCount: number;
  totalStudents: number;
}

export interface SubmissionListItem {
  id: string;
  studentId: string;
  studentName: string;
  submittedAt: string | null;
  status: 'DRAFT' | 'SUBMITTED' | 'GRADED';
  score: number | null;
}

export interface TeacherAssignmentDetail extends AssignmentDetail {
  submissions: SubmissionDetail[];
}

export interface AttachmentView {
  id: string;
  secureUrl: string;
  originalFilename: string;
  mimeType: string;
  fileSize: number;
}

export interface StudentAssignmentDetail extends AssignmentDetail {
  submission?: SubmissionDetail | null;
  assignmentAttachments: AttachmentView[];
  submissionAttachments: AttachmentView[];
}
