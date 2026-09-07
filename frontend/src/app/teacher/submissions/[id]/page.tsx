'use client';
import {useState} from 'react';
import {useParams} from 'next/navigation';
import {useQuery} from '@tanstack/react-query';
import {Alert,App,Button,Form,Input,InputNumber,Skeleton} from 'antd';
import {learningApi} from '@/features/learning/api/learningApi';
export default function Page(){
 const {id}=useParams<{id:string}>();const [busy,setBusy]=useState(false);const {message}=App.useApp();const q=useQuery({queryKey:['submission',id],queryFn:()=>learningApi.getSubmissionDetail(id)});
 if(q.isLoading)return <Skeleton active/>;
 if(q.isError||!q.data?.data)return <Alert type="error" title="Chưa đọc được bài nộp" action={<Button onClick={()=>q.refetch()}>Thử lại</Button>}/>;
 const s=q.data.data;return <div className="tm-detail-grid"><section className="tm-panel"><h1>Bài làm của học viên</h1>{s.contentBlocks?.filter(b=>b.type==='TEXT').map((b,i)=><p className="tm-prose" key={i}>{b.content}</p>)}<p>Nộp lúc: {s.submittedAt?new Date(s.submittedAt).toLocaleString('vi-VN'):'Chưa nộp'}</p></section><section className="tm-panel"><h2>{s.status==='GRADED'?'Kết quả chấm':'Chấm bài'}</h2><Form key={s.status} layout="vertical" initialValues={{score:s.score,feedbackText:s.feedbackText}} disabled={s.status!=='SUBMITTED'||busy} onFinish={async(v:{score:number;feedbackText:string})=>{setBusy(true);try{await learningApi.gradeSubmission(id,v);await q.refetch();message.success('Đã lưu điểm');}catch{message.error('Chưa lưu được điểm. Bài có thể đã được chấm.');}finally{setBusy(false);}}}><Form.Item name="score" label="Điểm /10" rules={[{required:true}]}><InputNumber min={0} max={10} step={0.25}/></Form.Item><Form.Item name="feedbackText" label="Nhận xét"><Input.TextArea rows={5}/></Form.Item>{s.status==='SUBMITTED'&&<Button htmlType="submit" type="primary" loading={busy}>Lưu kết quả chấm</Button>}</Form></section></div>;
}
