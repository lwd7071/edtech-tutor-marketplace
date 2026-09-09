export const teacherDashboardKeys = {
  all: ['teacher-dashboard'] as const,
  profile: () => [...teacherDashboardKeys.all, 'profile'] as const,
  subjects: () => [...teacherDashboardKeys.all, 'subjects'] as const,
  packages: (page?: number) => [...teacherDashboardKeys.all, 'packages', page] as const,
  package: (packageId: string) => [...teacherDashboardKeys.all, 'package', packageId] as const,
  learners: (page: number, studentId?: string) =>
    [...teacherDashboardKeys.all, 'learners', page, studentId] as const,
};
