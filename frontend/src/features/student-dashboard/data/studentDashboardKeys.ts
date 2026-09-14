export const studentDashboardKeys = {
  all: ['student-dashboard'] as const,
  summary: () => [...studentDashboardKeys.all, 'summary'] as const,
};
