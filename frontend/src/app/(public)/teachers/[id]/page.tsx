import {notFound} from 'next/navigation';
import {getTeacherDetailServer,getTeacherPackagesServer,getTeacherAvailabilityServer,getTeacherReviewsServer,getPublicSubjectsServer,PublicApiError} from '@/shared/api/public.server';
import {TeacherProfileHeader} from '@/features/marketplace/components/TeacherProfileHeader';
import TeacherDetailClient from './TeacherDetailClient';
import {BackLink} from '@/shared/components/navigation/NavigationLinks';
export const revalidate = 60;
export async function generateStaticParams(){ return []; }
export default async function Page({params}:{params:Promise<{id:string}>}){
 const {id}=await params;
 if(!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id))notFound();
 const [teacherResult,packages,availability,reviews,subjects]=await Promise.allSettled([getTeacherDetailServer(id),getTeacherPackagesServer(id,0,6),getTeacherAvailabilityServer(id),getTeacherReviewsServer(id,0,10),getPublicSubjectsServer({size:100})]);
 if(teacherResult.status==='rejected') { if(teacherResult.reason instanceof PublicApiError && teacherResult.reason.status===404) notFound(); throw teacherResult.reason; }
 const teacher=teacherResult.value;
 const p=packages.status==='fulfilled'?packages.value:{data:[],meta:{}};
 const subjectOptions=subjects.status==='fulfilled'?subjects.value.data.filter(s=>teacher.subjects.includes(s.name)):[];
 const errors=[packages.status==='rejected'?'packages':'',availability.status==='rejected'?'availability':'',reviews.status==='rejected'?'reviews':''].filter(Boolean);
 return <div className="tm-container tm-page"><BackLink href="/teachers">Danh sách gia sư</BackLink><TeacherProfileHeader teacher={teacher}/><TeacherDetailClient teacher={teacher} packages={p} availability={availability.status==='fulfilled'?availability.value:[]} reviews={reviews.status==='fulfilled'?reviews.value:{data:[],meta:{}}} subjectOptions={subjectOptions} errors={errors}/></div>;
}

