'use client';
import {useState} from 'react';
import {useQuery} from '@tanstack/react-query';
import Link from 'next/link';
import {Alert,Button,Pagination,Skeleton} from 'antd';
import {teacherDashboardApi,type LearnerPackage} from '../api/teacherDashboardApi';
import {CreateBookingModal} from '@/features/bookings';
import {teacherDashboardKeys} from '../data/teacherDashboardKeys';
export async function getLearners(page=0,size=12,studentId?:string){return teacherDashboardApi.getLearners(page,size,studentId);}
export default function LearnerList({studentId}:{studentId?:string}){
 const [page,setPage]=useState(0);const [pkg,setPkg]=useState<string|null>(null);
 const q=useQuery({queryKey:teacherDashboardKeys.learners(page,studentId),queryFn:()=>getLearners(page,12,studentId)});
 return <div className="tm-stack"><header className="tm-page-heading"><h1>{studentId?'Gói học của học viên':'Học viên và gói đang học'}</h1><p>Chọn gói thuộc học viên để tạo buổi học hoặc giao bài.</p></header>{q.isLoading?<Skeleton active/>:q.isError?<Alert type="error" title="Chưa tải được học viên" action={<Button onClick={()=>q.refetch()}>Thử lại</Button>}/>:<><div className="tm-package-grid">{q.data?.data.map(r=><article className="tm-panel" key={r.studentPackage.id}><h2><Link href={'/teacher/students/'+r.studentId}>{r.studentName}</Link></h2><p>{r.subjectName} · {r.studentPackage.packageName}</p><p>Còn {r.studentPackage.remainingSessions} buổi · Hết hạn {new Date(r.studentPackage.expiresAt).toLocaleDateString('vi-VN')}</p><div className="tm-inline"><Button type="primary" disabled={r.studentPackage.status!=='ACTIVE'||r.studentPackage.remainingSessions<1} onClick={()=>setPkg(r.studentPackage.id)}>Tạo buổi học</Button><Link href={'/teacher/assignments/new?studentId='+r.studentId+'&subjectId='+r.studentPackage.subjectId}>Giao bài tập →</Link></div></article>)}</div>{!q.data?.data.length&&<div className="tm-panel">Chưa có gói học của học viên trong tài khoản.</div>}<Pagination current={page+1} total={q.data?.meta?.totalElements||0} pageSize={12} showSizeChanger={false} onChange={n=>setPage(n-1)}/></>}<CreateBookingModal open={!!pkg} studentPackageId={pkg||''} onClose={()=>{setPkg(null);q.refetch();}}/></div>;
}
