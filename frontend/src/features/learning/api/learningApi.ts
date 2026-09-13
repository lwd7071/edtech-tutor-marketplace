import {axiosClient} from '@/shared/api/axiosClient';
import { ApiResponse, ApiResponseWithData, requireApiData } from '@/shared/backend';
import type {CreateAssignmentRequest,AssignmentDetail,GradeSubmissionRequest,SubmissionDetail,TeacherAssignmentListItem,TeacherAssignmentDetail,StudentAssignmentDetail,CreateSubmissionRequest} from '../types';
export const learningApi={
 async updateDraft(id:string,data:CreateAssignmentRequest): Promise<ApiResponseWithData<AssignmentDetail>> {return requireApiData((await axiosClient.put<ApiResponse<AssignmentDetail>>('/api/teacher/assignments/'+id,data)).data);},
 async publishAssignment(id:string,version:number): Promise<ApiResponseWithData<AssignmentDetail>> {return requireApiData((await axiosClient.post<ApiResponse<AssignmentDetail>>('/api/teacher/assignments/'+id+'/publish',{version})).data);},
 async closeAssignment(id:string,version:number): Promise<ApiResponseWithData<AssignmentDetail>> {return requireApiData((await axiosClient.post<ApiResponse<AssignmentDetail>>('/api/teacher/assignments/'+id+'/close',{version})).data);},
 async createAssignment(data:CreateAssignmentRequest): Promise<ApiResponseWithData<AssignmentDetail>> {return requireApiData((await axiosClient.post<ApiResponse<AssignmentDetail>>('/api/teacher/assignments',data)).data);},
 async gradeSubmission(id:string,data:GradeSubmissionRequest): Promise<ApiResponseWithData<SubmissionDetail>> {return requireApiData((await axiosClient.post<ApiResponse<SubmissionDetail>>('/api/teacher/submissions/'+id+'/grade',data)).data);},
 async getTeacherAssignments(page=0,size=20): Promise<ApiResponseWithData<TeacherAssignmentListItem[]>> {return requireApiData((await axiosClient.get<ApiResponse<TeacherAssignmentListItem[]>>('/api/teacher/assignments',{params:{page,size}})).data);},
 async getTeacherAssignmentDetail(id:string): Promise<ApiResponseWithData<TeacherAssignmentDetail>> {return requireApiData((await axiosClient.get<ApiResponse<TeacherAssignmentDetail>>('/api/teacher/assignments/'+id)).data);},
 async getSubmissionDetail(id:string): Promise<ApiResponseWithData<SubmissionDetail>> {return requireApiData((await axiosClient.get<ApiResponse<SubmissionDetail>>('/api/teacher/submissions/'+id)).data);},
 async getStudentAssignments(page=0,size=20,progress?:'TODO'|'SUBMITTED'|'GRADED'): Promise<ApiResponseWithData<TeacherAssignmentListItem[]>> {return requireApiData((await axiosClient.get<ApiResponse<TeacherAssignmentListItem[]>>('/api/student/assignments',{params:{page,size,progress}})).data);},
 async getStudentAssignmentDetail(id:string): Promise<ApiResponseWithData<StudentAssignmentDetail>> {return requireApiData((await axiosClient.get<ApiResponse<StudentAssignmentDetail>>('/api/student/assignments/'+id)).data);},
 async submitAssignment(id:string,data:CreateSubmissionRequest): Promise<ApiResponseWithData<SubmissionDetail>> {return requireApiData((await axiosClient.post<ApiResponse<SubmissionDetail>>('/api/student/assignments/'+id+'/submissions',{...data,status:'SUBMITTED'})).data);}
};

