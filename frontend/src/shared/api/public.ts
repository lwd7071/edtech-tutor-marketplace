import axiosClient from './axiosClient';
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
  thumbnailUrl?: string;
  totalTeachers?: number;
}

export interface TeacherCard {
  id: string;
  user: {
    fullName: string;
    avatarUrl?: string;
  };
  headline?: string;
  rating?: number;
  reviewCount?: number;
  minPrice?: number;
  subjects?: { id: string; name: string }[];
  isVerified?: boolean;
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
  sessionCount: number;
  durationMinutes: number;
  status: string;
}

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
}

export interface ApiResponse<T> {
  data: T;
  meta: PageMeta;
}

export const getPublicSubjects = async (params?: GetPublicSubjectsParams): Promise<ApiResponse<SubjectSummary[]>> => {
  const response = await axiosClient.get('/api/public/subjects', { params });
  return response.data;
};

export const getPublicTeachers = async (params?: TeacherSearchParams): Promise<ApiResponse<TeacherCard[]>> => {
  const response = await axiosClient.get('/api/public/teachers', { params });
  return response.data;
};

export const getTeacherDetail = async (id: string): Promise<TeacherPublicDetail> => {
  const response = await axiosClient.get(`/api/public/teachers/${id}`);
  return response.data.data; // Note: ApiResponse.ok usually wraps in { data: ... }
};

export const getTeacherPackages = async (id: string, page: number = 0, size: number = 20): Promise<ApiResponse<PricingPackageView[]>> => {
  const response = await axiosClient.get(`/api/public/teachers/${id}/packages`, { params: { page, size } });
  return response.data;
};

export const getTeacherAvailability = async (id: string): Promise<AvailabilityView[]> => {
  const response = await axiosClient.get(`/api/public/teachers/${id}/availability`);
  return response.data.data;
};

export const getTeacherReviews = async (id: string, page: number = 0, size: number = 10): Promise<ApiResponse<Review[]>> => {
  const response = await axiosClient.get(`/api/public/teachers/${id}/reviews`, { params: { page, size } });
  return response.data;
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

export const getGlobalRanking = async (subjectId?: string, page: number = 0, size: number = 10): Promise<ApiResponse<TeacherRankingItem[]>> => {
  const response = await axiosClient.get('/api/public/teachers/ranking', { params: { subjectId, page, size } });
  return response.data;
};
