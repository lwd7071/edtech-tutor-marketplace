import {axiosClient} from '@/shared/api/axiosClient';
import { requireApiData } from '@/shared/backend';
import type {ApiResponse, ApiResponseWithData} from '@/shared/backend';
import type {StudentPackageSummary,StudentPackageDetail,StudentPackageStatus} from '../types';

export const studentPackageApi={
 async getStudentPackages(status?:StudentPackageStatus,page=0,size=20,sort='createdAt,desc'):Promise<ApiResponseWithData<StudentPackageSummary[]>>{
  return requireApiData((await axiosClient.get<ApiResponse<StudentPackageSummary[]>>('/api/student/packages',{params:{status,page,size,sort}})).data);
 },
 async getStudentPackageDetail(id:string):Promise<ApiResponseWithData<StudentPackageDetail>>{
  return requireApiData((await axiosClient.get<ApiResponse<StudentPackageDetail>>('/api/student/packages/'+id)).data);
 }
};
