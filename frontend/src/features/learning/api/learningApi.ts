import { axiosClient } from '@/shared/api/axiosClient';
import { ApiResponse } from '@/shared/api/types';
import {
  CreateAssignmentRequest,
  AssignmentDetail,
  GradeSubmissionRequest,
  SubmissionDetail,
  TeacherAssignmentListItem,
  TeacherAssignmentDetail,
  CreateSubmissionRequest,
} from '../types';

export const learningApi = {
  /**
   * Tạo bài tập mới
   */
  createAssignment: async (
    data: CreateAssignmentRequest
  ): Promise<ApiResponse<AssignmentDetail>> => {
    const response = await axiosClient.post<ApiResponse<AssignmentDetail>>(
      '/api/teacher/assignments',
      data
    );
    return response.data;
  },

  /**
   * Chấm điểm bài tập
   */
  gradeSubmission: async (
    submissionId: string,
    data: GradeSubmissionRequest
  ): Promise<ApiResponse<SubmissionDetail>> => {
    const response = await axiosClient.post<ApiResponse<SubmissionDetail>>(
      `/api/teacher/submissions/${submissionId}/grade`,
      data
    );
    return response.data;
  },

  /**
   * Lấy danh sách bài tập của giáo viên (Mock cho đến khi backend có API GET)
   */
  getTeacherAssignments: async (): Promise<ApiResponse<TeacherAssignmentListItem[]>> => {
    try {
      const response = await axiosClient.get<ApiResponse<TeacherAssignmentListItem[]>>(
        '/api/teacher/assignments'
      );
      return response.data;
    } catch (e: any) {
      if (e.response?.status === 404) {
        // Return empty array if backend doesn't have this API yet
        return { data: [], message: 'Mock data', success: true, errors: [] };
      }
      throw e;
    }
  },

  /**
   * Lấy chi tiết bài tập kèm danh sách nộp bài (Mock cho đến khi backend có API GET)
   */
  getTeacherAssignmentDetail: async (id: string): Promise<ApiResponse<TeacherAssignmentDetail>> => {
    try {
      const response = await axiosClient.get<ApiResponse<TeacherAssignmentDetail>>(
        `/api/teacher/assignments/${id}`
      );
      return response.data;
    } catch (e: any) {
      if (e.response?.status === 404) {
        // Mock response
        return { 
          data: {
            id,
            teacherId: '',
            studentId: '',
            subjectId: '',
            title: 'Mock Assignment',
            assignmentType: 'HOMEWORK',
            contentBlocks: [],
            dueAt: new Date().toISOString(),
            status: 'PUBLISHED',
            submissions: []
          } as TeacherAssignmentDetail, 
          message: 'Mock data', 
          success: true,
          errors: []
        };
      }
      throw e;
    }
  },
  
  /**
   * Lấy chi tiết bài nộp để chấm (Mock cho đến khi backend có API GET)
   */
  getSubmissionDetail: async (id: string): Promise<ApiResponse<SubmissionDetail>> => {
    try {
      const response = await axiosClient.get<ApiResponse<SubmissionDetail>>(
        `/api/teacher/submissions/${id}`
      );
      return response.data;
    } catch (e: any) {
      if (e.response?.status === 404) {
        // Mock response
        return { 
          data: {
            id,
            assignmentId: '',
            studentId: '',
            contentBlocks: [],
            submittedAt: new Date().toISOString(),
            status: 'SUBMITTED',
            score: null,
            feedbackText: null,
            gradedAt: null
          } as SubmissionDetail, 
          message: 'Mock data', 
          success: true,
          errors: []
        };
      }
      throw e;
    }
  },

  /**
   * (STUDENT) Lấy danh sách bài tập của học viên
   */
  getStudentAssignments: async (
    page: number = 0,
    size: number = 20,
    status?: string
  ): Promise<ApiResponse<TeacherAssignmentListItem[]>> => { // Currently backend returns same AssignmentDetail
    const params: any = { page, size };
    if (status) params.status = status;
    const response = await axiosClient.get<ApiResponse<TeacherAssignmentListItem[]>>('/api/student/assignments', { params });
    return response.data;
  },

  /**
   * (STUDENT) Lấy chi tiết bài tập của học viên
   * Mock endpoint because backend API might not have this specifically, but it's needed for UI
   */
  getStudentAssignmentDetail: async (id: string): Promise<ApiResponse<TeacherAssignmentDetail>> => {
    try {
      const response = await axiosClient.get<ApiResponse<TeacherAssignmentDetail>>(`/api/student/assignments/${id}`);
      return response.data;
    } catch (e: any) {
      if (e.response?.status === 404) {
        // Fallback or mock
        return { 
          data: {
            id,
            teacherId: 'Teacher 1',
            studentId: 'Student 1',
            subjectId: 'SUB1',
            title: 'Mock Student Assignment',
            assignmentType: 'HOMEWORK',
            contentBlocks: [],
            dueAt: new Date(Date.now() + 86400000).toISOString(),
            status: 'PUBLISHED',
            submissions: []
          } as TeacherAssignmentDetail, 
          message: 'Mock data', 
          success: true,
          errors: []
        };
      }
      throw e;
    }
  },

  /**
   * (STUDENT) Nộp bài
   */
  submitAssignment: async (
    id: string,
    data: CreateSubmissionRequest
  ): Promise<ApiResponse<SubmissionDetail>> => {
    const response = await axiosClient.post<ApiResponse<SubmissionDetail>>(
      `/api/student/assignments/${id}/submissions`,
      data
    );
    return response.data;
  },
};
