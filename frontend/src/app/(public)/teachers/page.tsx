import {getPublicTeachers,getPublicSubjects,type TeacherSearchParams} from '@/shared/api/public';
import TeacherSearchClient from './TeacherSearchClient';
export default async function Page({searchParams}:{searchParams:Promise<Record<string,string|string[]|undefined>>}){
 const q=await searchParams;const value=(key:string)=>typeof q[key]==='string'?q[key] as string:undefined;
 const number=(key:string)=>{const v=value(key);return v!==undefined&&v.trim()!==''&&Number.isFinite(Number(v))&&Number(v)>=0?Number(v):undefined;};
 const params:TeacherSearchParams={keyword:value('keyword'),subjectId:value('subjectId'),minPrice:number('minPrice'),maxPrice:number('maxPrice'),minRating:number('minRating'),deliveryMode:value('deliveryMode'),sort:value('sort'),page:Math.max(0,Math.floor(number('page')||1)-1),size:12};
 if(value('dayOfWeek')&&value('startTime')&&value('endTime'))Object.assign(params,{dayOfWeek:value('dayOfWeek'),startTime:value('startTime'),endTime:value('endTime')});
 const [teachers,subjects]=await Promise.allSettled([getPublicTeachers(params),getPublicSubjects({size:100})]);
 return <div className="tm-container tm-page"><header className="tm-page-heading"><p className="tm-eyebrow">Tìm người đồng hành</p><h1>Gia sư cho mục tiêu của bạn</h1><p>Chọn môn học, hình thức và ngân sách. Khám phá từng hồ sơ trước khi bắt đầu.</p></header><TeacherSearchClient initialFilters={params} initialTeachers={teachers.status==='fulfilled'?teachers.value:{data:[],meta:{}}} subjects={subjects.status==='fulfilled'?subjects.value.data:[]} isError={teachers.status==='rejected'}/></div>;
}
