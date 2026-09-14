import { useQuery } from '@tanstack/react-query';
import { studentDashboardApi } from '../api/studentDashboardApi';
import { studentDashboardKeys } from '../data/studentDashboardKeys';

export function useStudentDashboard() {
  return useQuery({
    queryKey: studentDashboardKeys.summary(),
    queryFn: studentDashboardApi.getSummary,
    staleTime: 30_000,
  });
}
