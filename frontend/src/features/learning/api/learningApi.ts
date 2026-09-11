import {axiosClient} from '@/shared/api/axiosClient';
import type {ApiResponse} from '@/shared/backend';
import type {CreateAssignmentRequest,AssignmentDetail,GradeSubmissionRequest,SubmissionDetail,TeacherAssignmentListItem,TeacherAssignmentDetail,StudentAssignmentDetail,CreateSubmissionRequest} from '../types';
export const learningApi={
 async updateDraft(id:string,data:CreateAssignmentRequest){return (await axiosClient.put<ApiResponse<AssignmentDetail>>('/api/teacher/assignments/'+id,data)).data;},
 async publishAssignment(id:string){return (await axiosClient.post<ApiResponse<AssignmentDetail>>('/api/teacher/assignments/'+id+'/publish')).data;},
 async closeAssignment(id:string){return (await axiosClient.post<ApiResponse<AssignmentDetail>>('/api/teacher/assignments/'+id+'/close')).data;},
 async createAssignment(data:CreateAssignmentRequest){return (await axiosClient.post<ApiResponse<AssignmentDetail>>('/api/teacher/assignments',data)).data;},
 async gradeSubmission(id:string,data:GradeSubmissionRequest){return (await axiosClient.post<ApiResponse<SubmissionDetail>>('/api/teacher/submissions/'+id+'/grade',data)).data;},
 async getTeacherAssignments(page=0,size=20){return (await axiosClient.get<ApiResponse<TeacherAssignmentListItem[]>>('/api/teacher/assignments',{params:{page,size}})).data;},
 async getTeacherAssignmentDetail(id:string){return (await axiosClient.get<ApiResponse<TeacherAssignmentDetail>>('/api/teacher/assignments/'+id)).data;},
 async getSubmissionDetail(id:string){return (await axiosClient.get<ApiResponse<SubmissionDetail>>('/api/teacher/submissions/'+id)).data;},
 async getStudentAssignments(page=0,size=20,progress?:'TODO'|'SUBMITTED'|'GRADED'){return (await axiosClient.get<ApiResponse<TeacherAssignmentListItem[]>>('/api/student/assignments',{params:{page,size,progress}})).data;},
 async getStudentAssignmentDetail(id:string){return (await axiosClient.get<ApiResponse<StudentAssignmentDetail>>('/api/student/assignments/'+id)).data;},
 async submitAssignment(id:string,data:CreateSubmissionRequest){return (await axiosClient.post<ApiResponse<SubmissionDetail>>('/api/student/assignments/'+id+'/submissions',{...data,status:'SUBMITTED'})).data;}
};

