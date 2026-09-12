import axiosClient from './axiosClient';
import { AvailabilityView, PricingPackageView } from './public';

export interface TeacherProfile {
  id?: string;
  bio?: string;
  yearsOfExperience?: number;
  languages?: string[];
  supportsOnline?: boolean;
  supportsOffline?: boolean;
  locationAddress?: string;
  introductionVideoUrl?: string;
  profileStatus?: 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED';
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
  proposedName: string;
  educationLevel: 'ELEMENTARY' | 'MIDDLE_SCHOOL' | 'HIGH_SCHOOL' | 'UNIVERSITY' | 'OTHER';
  description: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  reviewNote?: string | null;
  reviewedAt?: string | null;
  createdSubjectId?: string | null;
}


export interface ReplaceAvailabilityRequest {
  items: {
    dayOfWeek: string;
    startTime: string;
    endTime: string;
    timezone: string;
    isActive: boolean;
  }[];
}

export interface CreatePackageRequest {
  name: string;
  subjectId: string;
  description: string;
  priceVnd: number;
  totalSessions: number;
  durationDays: number;
  sessionDurationMinutes: number;
  status: string;
  version: number;
}

export interface UpdatePackageRequest {
  subjectId?: string;
  name: string;
  description: string;
  priceVnd: number;
  totalSessions: number;
  durationDays: number;
  sessionDurationMinutes: number;
  status: string;
  version: number;
}

type SubjectWire = {id:string;subject:{id:string;name:string;educationLevel:string}};
const subjectView = (s:SubjectWire):TeacherSubject => ({id:s.subject.id,subjectId:s.subject.id,name:s.subject.name,category:s.subject.educationLevel});
type DocumentWire = {id:string;title:string;secureUrl:string;documentType:string;verificationStatus:TeacherDocument['status'];verifiedAt:string};
const documentView = (d:DocumentWire):TeacherDocument => ({id:d.id,name:d.title||d.documentType,url:d.secureUrl,type:d.documentType,status:d.verificationStatus,uploadedAt:d.verifiedAt});

export const teacherApi = {
  getProfile: () => axiosClient.get<{data: TeacherProfile}>('/api/teacher/profile').then(res => ({...res.data.data, approvalStatus: res.data.data.profileStatus})),
  updateProfile: (data: TeacherProfile) => axiosClient.put<{data: TeacherProfile}>('/api/teacher/profile', data).then(res => res.data.data),
  submitProfile: () => axiosClient.post('/api/teacher/profile/submit').then(res => res.data),
  
  getDocuments: () => axiosClient.get<{data:DocumentWire[]}>('/api/teacher/documents').then(res => res.data.data.map(documentView)),
  uploadDocument: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('documentType', 'OTHER');
    formData.append('title', file.name);
    return axiosClient.post<{data:DocumentWire}>('/api/teacher/documents', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }).then(res => documentView(res.data.data));
  },
  deleteDocument: (id: string) => axiosClient.delete(`/api/teacher/documents/${id}`).then(res => res.data),

  getSubjects: () => axiosClient.get<{data:SubjectWire[]}>('/api/teacher/subjects').then(res => res.data.data.map(subjectView)),
  addSubject: (subjectId: string) => axiosClient.post<{data:SubjectWire}>(`/api/teacher/subjects/${subjectId}`, {}).then(res => subjectView(res.data.data)),
  deleteSubject: (id: string) => axiosClient.delete(`/api/teacher/subjects/${id}`).then(res => res.data),
  searchPublicSubjects: (query: string) => axiosClient.get<{data:{id: string, name: string, educationLevel: string}[]}>('/api/public/subjects', { params: { keyword:query,size:100 } }).then(res => res.data.data.map(s=>({...s,category:s.educationLevel}))),

  getSubjectProposals: () => axiosClient.get<{data:TeacherSubjectProposal[]}>('/api/teacher/subject-proposals', {params:{size:100}}).then(res => res.data.data),
  createSubjectProposal: (data: { proposedName: string, educationLevel: TeacherSubjectProposal['educationLevel'], description?: string }) => axiosClient.post<{data:TeacherSubjectProposal}>('/api/teacher/subject-proposals', data).then(res => res.data.data),

  // Availability
  getAvailabilities: () => axiosClient.get<{data:AvailabilityView[]}>('/api/teacher/availability').then(res => res.data.data),
  replaceAvailabilities: (data: ReplaceAvailabilityRequest) => axiosClient.put('/api/teacher/availability', data).then(res => res.data),

  // Packages
  getPackages: () => axiosClient.get<{data:PricingPackageView[]}>('/api/teacher/packages', {params:{size:100}}).then(res => res.data.data),
  getPackage: (id:string) => axiosClient.get<{data:PricingPackageView}>(`/api/teacher/packages/${id}`).then(res=>res.data.data),
  createPackage: (data: CreatePackageRequest) => axiosClient.post<{data:PricingPackageView}>('/api/teacher/packages', data).then(res => res.data.data),
  updatePackage: (id: string, data: UpdatePackageRequest) => axiosClient.put<{data:PricingPackageView}>(`/api/teacher/packages/${id}`, data).then(res => res.data.data),
  updatePackageStatus: (id: string, status: string, version: number) => axiosClient.patch<void>(`/api/teacher/packages/${id}/status`, { status, version }).then(res => res.data),
};
