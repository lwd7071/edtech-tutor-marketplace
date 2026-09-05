import { useQuery } from '@tanstack/react-query';
import { studentPackageApi } from '../api/studentPackageApi';
import { StudentPackageStatus } from '../types';

export const STUDENT_PACKAGE_KEYS = {
  all: ['student-packages'] as const,
  lists: () => [...STUDENT_PACKAGE_KEYS.all, 'list'] as const,
  list: (status?: StudentPackageStatus, page?: number, size?: number) =>
    [...STUDENT_PACKAGE_KEYS.lists(), { status, page, size }] as const,
  details: () => [...STUDENT_PACKAGE_KEYS.all, 'detail'] as const,
  detail: (id: string) => [...STUDENT_PACKAGE_KEYS.details(), id] as const,
};

/**
 * Hook lấy danh sách gói học của học sinh
 */
export function useStudentPackages(
  status?: StudentPackageStatus,
  page: number = 0,
  size: number = 20,
  sort: string = 'createdAt,desc'
) {
  return useQuery({
    queryKey: STUDENT_PACKAGE_KEYS.list(status, page, size),
    queryFn: () => studentPackageApi.getStudentPackages(status, page, size, sort),
  });
}

/**
 * Hook lấy chi tiết gói học của học sinh
 */
export function useStudentPackageDetail(id: string) {
  return useQuery({
    queryKey: STUDENT_PACKAGE_KEYS.detail(id),
    queryFn: () => studentPackageApi.getStudentPackageDetail(id),
    enabled: !!id,
  });
}
