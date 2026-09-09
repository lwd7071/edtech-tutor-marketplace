export const learningKeys = {
  all: ['learning'] as const,
  assignments: (role: 'teacher' | 'student', page: number) =>
    [...learningKeys.all, 'assignments', role, page] as const,
  studentAssignment: (assignmentId: string) =>
    [...learningKeys.all, 'student-assignment', assignmentId] as const,
  teacherAssignment: (assignmentId: string) =>
    [...learningKeys.all, 'teacher-assignment', assignmentId] as const,
  submission: (submissionId: string) =>
    [...learningKeys.all, 'submission', submissionId] as const,
  assignmentLearners: () => [...learningKeys.all, 'assignment-learners'] as const,
};
