import axiosClient from './axiosClient';
import { requireApiData, type ApiResponseWithData } from '@/shared/backend';
export interface PaginatedResponse<T> {
  data: T[];
  meta: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

// Define missing types locally if they don't exist in shared/types yet
export interface SubjectSummary {
  id: string;
  name: string;
  description?: string;
  educationLevel?: string;
  thumbnailUrl?: string;
  totalTeachers?: number;
}

export interface TeacherCard {
  id: string;
  fullName?: string;
  user?: {
    fullName: string;
    avatarUrl?: string;
  };
  name?: string;
  avatarUrl?: string;
  headline?: string;
  bioExcerpt?: string;
  yearsOfExperience?: number;
  rating?: number;
  averageRating?: number;
  bayesianRating?: number;
  reviewCount?: number;
  minPrice?: number;
  lowestPrice?: number;
  startingPriceVnd?: number;
  subjects?: ({ id: string; name: string } | string)[];
  isVerified?: boolean;
  verifiedBadge?: boolean;
  supportsOnline?: boolean;
  supportsOffline?: boolean;
}

export interface GetPublicSubjectsParams {
  keyword?: string;
  educationLevel?: string;
  page?: number;
  size?: number;
}

export interface TeacherSearchParams {
  keyword?: string;
  subjectId?: string;
  dayOfWeek?: string;
  startTime?: string;
  endTime?: string;
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
  deliveryMode?: string;
  sort?: string;
  page?: number;
  size?: number;
}

export interface PageMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface TeacherPublicDetail {
  id: string;
  fullName: string;
  avatarUrl?: string;
  bio?: string;
  yearsOfExperience: number;
  languages: string[];
  supportsOnline: boolean;
  supportsOffline: boolean;
  locationAddress?: string;
  introductionVideoUrl?: string;
  subjects: string[];
  averageRating: number;
  reviewCount: number;
}

export interface PricingPackageView {
  id: string;
  name: string;
  description?: string;
  priceVnd: number;
  totalSessions: number;
  sessionDurationMinutes: number;
  durationDays: number;
  subjectId: string;
  subjectName: string;
  /** @deprecated use totalSessions/sessionDurationMinutes */
  sessionCount?: number;
  /** @deprecated use sessionDurationMinutes */
  durationMinutes?: number;
  version: number;
  status: string;
}

type PaginatedApiResponse<T> = Omit<ApiResponseWithData<T[]>, 'meta'> & { meta: PageMeta };

const paginated = <T>(response: ApiResponseWithData<T[]>): PaginatedApiResponse<T> => response as PaginatedApiResponse<T>;

export interface AvailabilityView {
  id?: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
}

export interface Review {
  id: string;
  rating: number;
  comment?: string;
  createdAt: string;
  reviewerName: string;
  student?: { id: string; fullName: string; avatarUrl?: string };
}

export const getPublicSubjects = async (params?: GetPublicSubjectsParams): Promise<PaginatedApiResponse<SubjectSummary>> => {
  const response = await axiosClient.get<ApiResponseWithData<SubjectSummary[]>>('/api/public/subjects', { params });
  return paginated(requireApiData(response.data));
};

export const getPublicTeachers = async (params?: TeacherSearchParams): Promise<PaginatedApiResponse<TeacherCard>> => {
  const response = await axiosClient.get<ApiResponseWithData<TeacherCard[]>>('/api/public/teachers', { params });
  return paginated(requireApiData(response.data));
};

export const getTeacherDetail = async (id: string): Promise<TeacherPublicDetail> => {
  const response = await axiosClient.get<ApiResponseWithData<TeacherPublicDetail>>(`/api/public/teachers/${id}`);
  return requireApiData(response.data).data;
};

export const getTeacherPackages = async (id: string, page: number = 0, size: number = 20): Promise<PaginatedApiResponse<PricingPackageView>> => {
  const response = await axiosClient.get<ApiResponseWithData<PricingPackageView[]>>(`/api/public/teachers/${id}/packages`, { params: { page, size } });
  return paginated(requireApiData(response.data));
};

export const getTeacherAvailability = async (id: string): Promise<AvailabilityView[]> => {
  const response = await axiosClient.get<ApiResponseWithData<AvailabilityView[]>>(`/api/public/teachers/${id}/availability`);
  return requireApiData(response.data).data;
};

export const getTeacherReviews = async (id: string, page: number = 0, size: number = 10): Promise<PaginatedApiResponse<Review>> => {
  const response = await axiosClient.get<ApiResponseWithData<Review[]>>(`/api/public/teachers/${id}/reviews`, { params: { page, size } });
  return paginated(requireApiData(response.data));
};

export interface TeacherRankingItem {
  teacherId: string;
  fullName: string;
  avatarUrl?: string;
  bioExcerpt?: string;
  bayesianRating?: number;
  completedSessionCount?: number;
  globalRank?: number;
}

export const getGlobalRanking = async (subjectId?: string, page: number = 0, size: number = 10): Promise<PaginatedApiResponse<TeacherRankingItem>> => {
  const response = await axiosClient.get<ApiResponseWithData<TeacherRankingItem[]>>('/api/public/teachers/ranking', { params: { subjectId, page, size } });
  return paginated(requireApiData(response.data));
};
