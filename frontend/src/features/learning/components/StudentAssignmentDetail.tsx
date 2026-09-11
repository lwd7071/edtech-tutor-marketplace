'use client';
import {useEffect,useState} from 'react';
import {useQuery} from '@tanstack/react-query';
import {Alert,App,Button,Form,Input,Skeleton} from 'antd';
import {learningApi} from '../api/learningApi';
import {learningKeys} from '../data/learningKeys';
import FileUpload from '@/shared/components/ui/FileUpload';
import {FileViewer} from '@/shared/components/data-display/FileViewer';

export function StudentAssignmentDetail({assignmentId}:{assignmentId:string}){
 const q=useQuery({queryKey:learningKeys.studentAssignment(assignmentId),queryFn:()=>learningApi.getStudentAssignmentDetail(assignmentId)});
 const [busy,setBusy]=useState(false); const [currentTime,setCurrentTime]=useState<number|null>(null);
 const [attachmentIds,setAttachmentIds]=useState<string[]>([]); const {message}=App.useApp();
 useEffect(()=>setCurrentTime(Date.now()),[]);
 if(q.isLoading)return <Skeleton active/>;
 if(q.isError||!q.data?.data)return <Alert type="error" title="Chưa đọc được bài tập" action={<Button onClick={()=>q.refetch()}>Thử lại</Button>}/>;
 const a=q.data.data; const submission=a.submission;
 const previousAnswer=submission?.contentBlocks?.find(b=>b.type==='TEXT')?.content||'';
 const overdue=currentTime!==null&&!!a.dueAt&&new Date(a.dueAt).getTime()<currentTime;
 const canSubmit=a.status==='PUBLISHED'&&!overdue&&submission?.status!=='GRADED';
 return <div className="tm-stack">
  <section className="tm-panel"><h1>{a.title}</h1><p>Hạn nộp: {a.dueAt?new Date(a.dueAt).toLocaleString('vi-VN'):'Chưa đặt'}</p>{a.contentBlocks?.filter(b=>b.type==='TEXT').map((b,i)=><p className="tm-prose" key={i}>{b.content}</p>)}<FileViewer files={a.assignmentAttachments}/></section>
  {overdue&&<Alert type="warning" title="Đã quá hạn nộp bài"/>}
  {submission&&<section className="tm-panel"><h2>Bài đã nộp</h2>{submission.contentBlocks?.filter(b=>b.type==='TEXT').map((b,i)=><p className="tm-prose" key={i}>{b.content}</p>)}<FileViewer files={a.submissionAttachments}/>{submission.status==='GRADED'?<><strong>Điểm: {submission.score}/10</strong><p>{submission.feedbackText}</p></>:<p>Đang chờ gia sư chấm.</p>}</section>}
  {canSubmit&&<section className="tm-panel"><h2>{submission?'Cập nhật bài làm':'Nộp bài làm'}</h2><Form layout="vertical" initialValues={{answer:previousAnswer}} onFinish={async(v:{answer?:string})=>{setBusy(true);try{const oldFiles=submission?.contentBlocks?.filter(b=>b.attachmentId).map(b=>b.attachmentId!)||[];await learningApi.submitAssignment(assignmentId,{contentBlocks:[...(v.answer?.trim()?[{type:'TEXT' as const,content:v.answer.trim()}]:[]),...[...new Set([...oldFiles,...attachmentIds])].map(attachmentId=>({type:'FILE' as const,attachmentId}))]});setAttachmentIds([]);await q.refetch();message.success('Đã nộp bài');}catch{message.error('Chưa nộp được bài. Kiểm tra hạn nộp và thử lại.');}finally{setBusy(false);}}}><Form.Item name="answer" label="Câu trả lời"><Input.TextArea rows={8}/></Form.Item><Form.Item label="Tệp đính kèm"><FileUpload attachableType="SUBMISSION" multiple onUploadSuccess={file=>setAttachmentIds(ids=>[...ids,file.id])}/></Form.Item><Button htmlType="submit" type="primary" loading={busy}>Nộp bài</Button></Form></section>}
 </div>;
}
