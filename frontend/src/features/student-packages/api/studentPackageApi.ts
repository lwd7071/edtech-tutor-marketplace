import {axiosClient} from '@/shared/api/axiosClient';
import type {ApiResponse} from '@/shared/backend';
import type {StudentPackageSummary,StudentPackageDetail,StudentPackageStatus} from '../types';

export const studentPackageApi={
 async getStudentPackages(status?:StudentPackageStatus,page=0,size=20,sort='createdAt,desc'):Promise<ApiResponse<StudentPackageSummary[]>>{
  return (await axiosClient.get<ApiResponse<StudentPackageSummary[]>>('/api/student/packages',{params:{status,page,size,sort}})).data;
 },
 async getStudentPackageDetail(id:string):Promise<ApiResponse<StudentPackageDetail>>{
  return (await axiosClient.get<ApiResponse<StudentPackageDetail>>('/api/student/packages/'+id)).data;
 }
};
