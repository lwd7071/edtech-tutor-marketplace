import {axiosClient} from '@/shared/api/axiosClient';
import type {ApiResponse} from '@/shared/backend';
import {getTeacherDetail,getPublicSubjects} from '@/shared/api/public';
import type {StudentPackageSummary,StudentPackageDetail,StudentPackageStatus} from '../types';
type Wire=Omit<StudentPackageSummary,'teacher'|'subject'>&{teacherId:string;subjectId:string};
async function enrich(items:Wire[]):Promise<StudentPackageSummary[]>{
 const ids=[...new Set(items.map(p=>p.teacherId).filter(Boolean))];
 const teachers=await Promise.all(ids.map(async id=>[id,await getTeacherDetail(id).catch(()=>null)] as const));
 const byId=new Map(teachers);
 const subjects=items.length?await getPublicSubjects({size:100}).catch(()=>null):null;
 return items.map(p=>({...p,teacher:{id:p.teacherId,fullName:byId.get(p.teacherId)?.fullName||'Gia sư',avatarUrl:byId.get(p.teacherId)?.avatarUrl},subject:{id:p.subjectId,name:subjects?.data.find(s=>s.id===p.subjectId)?.name||'Môn học'}}));
}
export const studentPackageApi={
 async getStudentPackages(status?:StudentPackageStatus,page=0,size=20,sort='createdAt,desc'):Promise<ApiResponse<StudentPackageSummary[]>>{
  const {data}=await axiosClient.get<{content:Wire[];number:number;size:number;totalElements:number;totalPages:number}>('/api/student/packages',{params:{status,page,size,sort}});
  return {data:await enrich(data.content),success:true,message:'',errors:[],meta:{page:data.number,size:data.size,totalElements:data.totalElements,totalPages:data.totalPages}};
 },
 async getStudentPackageDetail(id:string):Promise<ApiResponse<StudentPackageDetail>>{
  const {data}=await axiosClient.get<Wire>('/api/student/packages/'+id);
  return {data:(await enrich([data]))[0],success:true,message:'',errors:[]};
 }
};

