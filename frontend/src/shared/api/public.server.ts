import 'server-only';

import type { ApiResponse } from '@/shared/backend';
import type {
  AvailabilityView,
  GetPublicSubjectsParams,
  PageMeta,
  PricingPackageView,
  Review,
  SubjectSummary,
  TeacherCard,
  TeacherPublicDetail,
  TeacherRankingItem,
  TeacherSearchParams,
} from './public';

type ServerPaginatedResponse<T> = {
  success: true;
  message: string | null;
  data: T[];
  errors: null;
  meta: PageMeta;
};

export class PublicApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = 'PublicApiError';
  }
}

const backendUrl = () => {
  if (process.env.NODE_ENV === 'production' && !process.env.BACKEND_API_URL) {
    throw new Error('BACKEND_API_URL is required for production server-side public fetches');
  }
  const configured = process.env.BACKEND_API_URL || process.env.NEXT_PUBLIC_API_URL;
  if (configured) return configured.replace(/\/$/, '');
  return 'http://localhost:8080';
};

const queryString = (query: object) => {
  const params = new URLSearchParams();
  Object.entries(query as Record<string, unknown>).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') params.set(key, String(value));
  });
  const result = params.toString();
  return result ? `?${result}` : '';
};

async function serverApiGet<T>(
  path: string,
  query: object,
  policy: { revalidate: number; tags: string[] },
): Promise<T> {
  const response = await fetch(`${backendUrl()}${path}${queryString(query)}`, {
    cache: 'force-cache',
    next: { revalidate: policy.revalidate, tags: policy.tags },
    headers: { Accept: 'application/json' },
  });
  const body = await response.json() as ApiResponse<T>;
  const error = body.errors?.[0];
  if (!response.ok || !body.success || body.data == null) {
    throw new PublicApiError(response.status, error?.code || 'PUBLIC_API_ERROR', error?.message || body.message || 'Public API request failed');
  }
  return body.data;
}

async function serverApiGetPage<T>(
  path: string,
  query: object,
  policy: { revalidate: number; tags: string[] },
): Promise<ServerPaginatedResponse<T>> {
  const response = await fetch(`${backendUrl()}${path}${queryString(query)}`, {
    cache: 'force-cache',
    next: { revalidate: policy.revalidate, tags: policy.tags },
    headers: { Accept: 'application/json' },
  });
  const body = await response.json() as ApiResponse<T[]>;
  const error = body.errors?.[0];
  if (!response.ok || !body.success || body.data == null || body.meta == null) {
    throw new PublicApiError(response.status, error?.code || 'PUBLIC_API_ERROR', error?.message || body.message || 'Public API request failed');
  }
  return body as ServerPaginatedResponse<T>;
}

export const getPublicSubjectsServer = (params: GetPublicSubjectsParams = {}) =>
  serverApiGetPage<SubjectSummary>('/api/public/subjects', params, { revalidate: 300, tags: ['public-subjects'] });

export const getPublicTeachersServer = (params: TeacherSearchParams = {}) =>
  serverApiGetPage<TeacherCard>('/api/public/teachers', params, { revalidate: 300, tags: ['public-teachers'] });

export const getTeacherDetailServer = (id: string) =>
  serverApiGet<TeacherPublicDetail>(`/api/public/teachers/${id}`, {}, { revalidate: 300, tags: ['public-teachers', `public-teacher:${id}`] });

export const getTeacherPackagesServer = (id: string, page = 0, size = 20) =>
  serverApiGetPage<PricingPackageView>(`/api/public/teachers/${id}/packages`, { page, size }, { revalidate: 300, tags: [`public-teacher:${id}`, `public-teacher-packages:${id}`] });

export const getTeacherAvailabilityServer = (id: string) =>
  serverApiGet<AvailabilityView[]>(`/api/public/teachers/${id}/availability`, {}, { revalidate: 60, tags: [`public-teacher-availability:${id}`] });

export const getTeacherReviewsServer = (id: string, page = 0, size = 10) =>
  serverApiGetPage<Review>(`/api/public/teachers/${id}/reviews`, { page, size }, { revalidate: 60, tags: [`public-teacher-reviews:${id}`] });

export const getGlobalRankingServer = (subjectId?: string, page = 0, size = 10) =>
  serverApiGetPage<TeacherRankingItem>('/api/public/teachers/ranking', { subjectId, page, size }, { revalidate: 60, tags: ['public-ranking', ...(subjectId ? [`public-ranking:${subjectId}`] : [])] });
