import axiosClient from './axiosClient';
import { AvailabilityView, PricingPackageView } from './public';

export interface TeacherProfile {
  id?: string;
  bio?: string;
  experience?: string;
  education?: string;
  avatarUrl?: string;
  approvalStatus?: 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED';
  rejectionReason?: string;
}

export interface TeacherDocument {
  id: string;
  name: string;
  url: string;
  type: string;
  uploadedAt: string;
  status: 'VERIFIED' | 'PENDING' | 'REJECTED';
}

export interface TeacherSubject {
  id: string;
  subjectId: string;
  name: string;
  category: string;
}

export interface TeacherSubjectProposal {
  id: string;
  name: string;
  description: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  submittedAt: string;
}


export interface ReplaceAvailabilityRequest {
  timezone: string;
  items: {
    dayOfWeek: string;
    startTime: string;
    endTime: string;
  }[];
}

export interface CreatePackageRequest {
  name: string;
  subjectId: string;
  description: string;
  priceVnd: number;
  sessionCount: number;
  durationMonths: number;
  trialEnabled: boolean;
}

export interface UpdatePackageRequest {
  name: string;
  description: string;
  priceVnd: number;
  sessionCount: number;
  durationMonths: number;
  trialEnabled: boolean;
}

export const teacherApi = {
  getProfile: () => axiosClient.get<TeacherProfile>('/api/teacher/profile').then(res => res.data),
  updateProfile: (data: TeacherProfile) => axiosClient.put<TeacherProfile>('/api/teacher/profile', data).then(res => res.data),
  submitProfile: () => axiosClient.post('/api/teacher/profile/submit').then(res => res.data),
  
  getDocuments: () => axiosClient.get<TeacherDocument[]>('/api/teacher/documents').then(res => res.data),
  uploadDocument: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return axiosClient.post<TeacherDocument>('/api/teacher/documents', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }).then(res => res.data);
  },
  deleteDocument: (id: string) => axiosClient.delete(`/api/teacher/documents/${id}`).then(res => res.data),

  getSubjects: () => axiosClient.get<TeacherSubject[]>('/api/teacher/subjects').then(res => res.data),
  addSubject: (subjectId: string) => axiosClient.post<TeacherSubject>('/api/teacher/subjects', { subjectId }).then(res => res.data),
  deleteSubject: (id: string) => axiosClient.delete(`/api/teacher/subjects/${id}`).then(res => res.data),
  searchPublicSubjects: (query: string) => axiosClient.get<{id: string, name: string, category: string}[]>('/api/subjects', { params: { query } }).then(res => res.data),

  getSubjectProposals: () => axiosClient.get<TeacherSubjectProposal[]>('/api/teacher/subject-proposals').then(res => res.data),
  createSubjectProposal: (data: { name: string, description: string }) => axiosClient.post<TeacherSubjectProposal>('/api/teacher/subject-proposals', data).then(res => res.data),

  // Availability
  getAvailabilities: () => axiosClient.get<AvailabilityView[]>('/api/teacher/availability').then(res => res.data),
  replaceAvailabilities: (data: ReplaceAvailabilityRequest) => axiosClient.put<void>('/api/teacher/availability', data).then(res => res.data),

  // Packages
  getPackages: () => axiosClient.get<PricingPackageView[]>('/api/teacher/packages').then(res => res.data),
  createPackage: (data: CreatePackageRequest) => axiosClient.post<PricingPackageView>('/api/teacher/packages', data).then(res => res.data),
  updatePackage: (id: string, data: UpdatePackageRequest) => axiosClient.put<PricingPackageView>(`/api/teacher/packages/${id}`, data).then(res => res.data),
  updatePackageStatus: (id: string, status: string) => axiosClient.patch<void>(`/api/teacher/packages/${id}/status`, { status }).then(res => res.data),
};
