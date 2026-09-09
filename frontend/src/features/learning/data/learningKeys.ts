export const learningKeys = {
  all: ['learning'] as const,
  assignments: (role: 'teacher' | 'student', page: number) =>
    [...learningKeys.all, 'assignments', role, page] as const,
  studentAssignment: (assignmentId: string) =>
    [...learningKeys.all, 'student-assignment', assignmentId] as const,
};
