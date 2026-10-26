'use client';
import Link from 'next/link';
import {BookOutlined,RightOutlined, CalculatorOutlined, ExperimentOutlined, GlobalOutlined, CodeOutlined, ReadOutlined, TranslationOutlined} from '@ant-design/icons';
import {Skeleton} from 'antd';
import {EmptyState} from '@/shared/components/feedback/EmptyState';
import {ErrorState} from '@/shared/components/feedback/ErrorState';
import type {SubjectSummary} from '@/shared/api/public';
const levels:Record<string,string>={ELEMENTARY:'Tiểu học',MIDDLE_SCHOOL:'THCS',HIGH_SCHOOL:'THPT',UNIVERSITY:'Đại học',OTHER:'Khác'};
function SubjectIcon({name}:{name:string}) { const value=name.toLocaleLowerCase('vi'); if(value.includes('toán'))return <CalculatorOutlined/>; if(value.includes('lý')||value.includes('hóa')||value.includes('sinh'))return <ExperimentOutlined/>; if(value.includes('anh')||value.includes('ngữ'))return <TranslationOutlined/>; if(value.includes('lập trình')||value.includes('máy tính'))return <CodeOutlined/>; if(value.includes('văn'))return <ReadOutlined/>; if(value.includes('sử')||value.includes('địa'))return <GlobalOutlined/>; return <BookOutlined/>; }
export default function SubjectGrid({subjects,isError,isLoading,onRetry}:{subjects:SubjectSummary[];isError?:boolean;isLoading?:boolean;onRetry?:()=>void}){
 if(isError)return <ErrorState title="Chưa tải được môn học" actionText="Tải lại" onRetry={onRetry}/>;
 if(isLoading)return <div className="tm-subject-grid">{[1,2,3,4].map(i=><div className="tm-panel" key={i}><Skeleton active/></div>)}</div>;
 if(!subjects.length)return <EmptyState title="Không tìm thấy môn học" description="Thử tìm bằng tên môn học khác."/>;
 return <div className="tm-subject-grid">{subjects.map(s=><Link key={s.id} className="tm-card-link" href={'/subjects?subjectId='+encodeURIComponent(s.id)}><article className="tm-subject-card"><span className="tm-subject-icon" aria-hidden="true"><SubjectIcon name={s.name}/></span><div className="tm-subject-card-copy"><div className="tm-subject-card-heading"><h3>{s.name}</h3>{s.educationLevel&&<small className="tm-subject-level">{levels[s.educationLevel]||s.educationLevel}</small>}</div></div><span className="tm-subject-card-arrow" aria-hidden="true"><RightOutlined /></span></article></Link>)}</div>;
}
