import type { StudentPackageStatus } from '../types';

export const studentPackageKeys = {
  all: ['student-packages'] as const,
  lists: () => [...studentPackageKeys.all, 'list'] as const,
  list: (status?: StudentPackageStatus, page?: number, size?: number, sort?: string) =>
    [...studentPackageKeys.lists(), { status, page, size, sort }] as const,
  details: () => [...studentPackageKeys.all, 'detail'] as const,
  detail: (id: string) => [...studentPackageKeys.details(), id] as const,
};
