'use client';
import {useState} from 'react';
import {useQuery} from '@tanstack/react-query';
import Link from 'next/link';
import {Alert,Button,Pagination,Skeleton,Tag} from 'antd';
import {learningApi} from '../api/learningApi';
import {learningKeys} from '../data/learningKeys';
export default function AssignmentList({role}:{role:'student'|'teacher'}){
 const [page,setPage]=useState(0);const q=useQuery({queryKey:learningKeys.assignments(role,page),queryFn:()=>role==='teacher'?learningApi.getTeacherAssignments(page,12):learningApi.getStudentAssignments(page,12,'TODO')});
 return <div className="tm-stack"><header className="tm-toolbar"><div className="tm-page-heading"><h1>{role==='teacher'?'Quản lý bài tập':'Bài tập của tôi'}</h1><p>Xem đề bài, thời hạn và kết quả trong trang chi tiết.</p></div>{role==='teacher'&&<Link className="tm-button" href="/teacher/assignments/new">Giao bài tập</Link>}</header>{q.isLoading?<Skeleton active/>:q.isError?<Alert type="error" title="Chưa tải được bài tập" action={<Button onClick={()=>q.refetch()}>Thử lại</Button>}/>:<><div className="tm-package-grid">{q.data?.data.map(a=><Link href={'/'+role+'/assignments/'+a.id} className="tm-panel tm-dashboard-link" key={a.id}><Tag>{{DRAFT:'Bản nháp',PUBLISHED:'Đang giao',CLOSED:'Đã đóng'}[a.status]}</Tag><h2>{a.title}</h2>{a.studentName&&<p>{a.studentName} · {a.subjectName}</p>}<p>{a.dueAt?'Hạn nộp: '+new Date(a.dueAt).toLocaleString('vi-VN'):'Chưa có hạn nộp'}</p><strong>Xem bài tập →</strong></Link>)}</div>{!q.data?.data.length&&<section className="tm-panel">Chưa có bài tập.</section>}<Pagination current={page+1} total={q.data?.meta?.totalElements||0} pageSize={12} showSizeChanger={false} onChange={n=>setPage(n-1)}/></>}</div>;
}
