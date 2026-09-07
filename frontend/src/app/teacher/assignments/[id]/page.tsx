'use client';
import {useParams} from 'next/navigation';
import {useQuery} from '@tanstack/react-query';
import Link from 'next/link';
import {Alert,Button,Skeleton} from 'antd';
import {learningApi} from '@/features/learning/api/learningApi';
export default function Page(){
 const {id}=useParams<{id:string}>();const q=useQuery({queryKey:['teacher-assignment',id],queryFn:()=>learningApi.getTeacherAssignmentDetail(id)});
 if(q.isLoading)return <Skeleton active/>;
 if(q.isError||!q.data?.data)return <Alert type="error" title="Chưa đọc được bài tập" action={<Button onClick={()=>q.refetch()}>Thử lại</Button>}/>;
 const a=q.data.data;return <div className="tm-stack"><Link href="/teacher/assignments">← Danh sách bài tập</Link><section className="tm-panel"><h1>{a.title}</h1><p>Hạn nộp: {a.dueAt?new Date(a.dueAt).toLocaleString('vi-VN'):'Chưa đặt'}</p>{a.contentBlocks?.filter(b=>b.type==='TEXT').map((b,i)=><p className="tm-prose" key={i}>{b.content}</p>)}</section><section className="tm-panel"><h2>Bài nộp</h2>{a.submissions.length?a.submissions.map(s=><div className="tm-toolbar" key={s.id}><span>{s.status==='GRADED'?'Đã chấm: '+s.score+'/10':'Đã nhận bài nộp'}</span><Link href={'/teacher/submissions/'+s.id}>Xem và chấm bài →</Link></div>):<p>Học viên chưa nộp bài.</p>}</section></div>;
}
