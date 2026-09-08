'use client';
import Link from 'next/link';
import {BookOutlined} from '@ant-design/icons';
import {Skeleton} from 'antd';
import {EmptyState} from '@/shared/components/feedback/EmptyState';
import {ErrorState} from '@/shared/components/feedback/ErrorState';
import type {SubjectSummary} from '@/shared/api/public';
const levels:Record<string,string>={ELEMENTARY:'Tiểu học',MIDDLE_SCHOOL:'THCS',HIGH_SCHOOL:'THPT',UNIVERSITY:'Đại học',OTHER:'Khác'};
export default function SubjectGrid({subjects,isError,isLoading,onRetry}:{subjects:SubjectSummary[];isError?:boolean;isLoading?:boolean;onRetry?:()=>void}){
 if(isError)return <ErrorState title="Chưa tải được môn học" actionText="Tải lại" onRetry={onRetry}/>;
 if(isLoading)return <div className="tm-subject-grid">{[1,2,3,4].map(i=><div className="tm-panel" key={i}><Skeleton active/></div>)}</div>;
 if(!subjects.length)return <EmptyState title="Không tìm thấy môn học" description="Thử tìm bằng tên môn học khác."/>;
 return <div className="tm-subject-grid">{subjects.map(s=><Link key={s.id} className="tm-card-link" href={'/teachers?subjectId='+encodeURIComponent(s.id)}><article className="tm-subject-card"><span className="tm-subject-icon"><BookOutlined/></span><h3>{s.name}</h3>{s.educationLevel&&<small>{levels[s.educationLevel]||s.educationLevel}</small>}{s.description&&<p>{s.description}</p>}<small>Tìm gia sư →</small></article></Link>)}</div>;
}
