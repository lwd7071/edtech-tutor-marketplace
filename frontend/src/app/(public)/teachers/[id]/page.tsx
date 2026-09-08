import {notFound} from 'next/navigation';
import {getTeacherDetail,getTeacherPackages,getTeacherAvailability,getTeacherReviews,getPublicSubjects} from '@/shared/api/public';
import {TeacherProfileHeader} from '@/features/marketplace/components/TeacherProfileHeader';
import TeacherDetailClient from './TeacherDetailClient';
import {BackLink} from '@/shared/components/navigation/NavigationLinks';
export default async function Page({params,searchParams}:{params:Promise<{id:string}>;searchParams:Promise<Record<string,string|string[]|undefined>>}){
 const {id}=await params; const q=await searchParams;
 if(!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id))notFound();
 const index=(v:unknown)=>typeof v==='string'&&/^\d+$/.test(v)?Math.max(0,Number(v)-1):0;
 const teacher=await getTeacherDetail(id).catch((error)=>{if(error.response?.status===404)notFound();throw error;});
 const [packages,availability,reviews,subjects]=await Promise.allSettled([getTeacherPackages(id,index(q.packagesPage),6),getTeacherAvailability(id),getTeacherReviews(id,index(q.reviewsPage),10),getPublicSubjects({size:100})]);
 const p=packages.status==='fulfilled'?packages.value:{data:[],meta:{}};
 const subjectOptions=subjects.status==='fulfilled'?subjects.value.data.filter(s=>teacher.subjects.includes(s.name)):[];
 const errors=[packages.status==='rejected'?'packages':'',availability.status==='rejected'?'availability':'',reviews.status==='rejected'?'reviews':''].filter(Boolean);
 return <div className="tm-container tm-page"><BackLink href="/teachers">Danh sách gia sư</BackLink><TeacherProfileHeader teacher={teacher}/><TeacherDetailClient teacher={teacher} packages={p} availability={availability.status==='fulfilled'?availability.value:[]} reviews={reviews.status==='fulfilled'?reviews.value:{data:[],meta:{}}} subjectOptions={subjectOptions} errors={errors}/></div>;
}

