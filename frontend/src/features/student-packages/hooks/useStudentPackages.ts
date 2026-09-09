import { useQuery } from '@tanstack/react-query';
import { studentPackageApi } from '../api/studentPackageApi';
import { StudentPackageStatus } from '../types';
import { studentPackageKeys } from '../data/studentPackageKeys';

export const STUDENT_PACKAGE_KEYS = studentPackageKeys;

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
    queryKey: studentPackageKeys.list(status, page, size, sort),
    queryFn: () => studentPackageApi.getStudentPackages(status, page, size, sort),
  });
}

/**
 * Hook lấy chi tiết gói học của học sinh
 */
export function useStudentPackageDetail(id: string) {
  return useQuery({
    queryKey: studentPackageKeys.detail(id),
    queryFn: () => studentPackageApi.getStudentPackageDetail(id),
    enabled: !!id,
  });
}
