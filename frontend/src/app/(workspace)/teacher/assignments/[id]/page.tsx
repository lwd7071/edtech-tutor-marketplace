'use client';
import {useParams} from 'next/navigation';
import {useState} from 'react';
import {useQuery} from '@tanstack/react-query';
import {Alert,App,Button,Form,Input,Skeleton} from 'antd';
import {learningApi} from '@/features/learning/api/learningApi';
import {learningKeys} from '@/features/learning/data/learningKeys';
import {BackLink, InlineActionLink} from '@/shared/components/navigation/NavigationLinks';
export default function Page(){
 const [busy,setBusy]=useState(false);const {message}=App.useApp();
 const {id}=useParams<{id:string}>();const q=useQuery({queryKey:learningKeys.teacherAssignment(id),queryFn:()=>learningApi.getTeacherAssignmentDetail(id)});
 if(q.isLoading)return <Skeleton active/>;
 if(q.isError||!q.data?.data)return <Alert type="error" title="Chưa đọc được bài tập" action={<Button onClick={()=>q.refetch()}>Thử lại</Button>}/>;
 const a=q.data.data;
 async function perform(action:()=>Promise<unknown>){setBusy(true);try{await action();await q.refetch();message.success('Đã cập nhật bài tập');}catch{message.error('Chưa cập nhật được. Kiểm tra trạng thái bài và hạn nộp.');}finally{setBusy(false);}}
 return <div className="tm-stack"><BackLink href="/teacher/assignments">Danh sách bài tập</BackLink><section className="tm-panel"><h1>{a.title}</h1><p>Trạng thái: {a.status==='DRAFT'?'Bản nháp':a.status==='PUBLISHED'?'Đã phát hành':'Đã đóng'}</p><p>Hạn nộp: {a.dueAt?new Date(a.dueAt).toLocaleString('vi-VN'):'Chưa đặt'}</p>
 {a.status==='DRAFT'?<Form key={JSON.stringify(a.contentBlocks)+a.title+a.dueAt} layout="vertical" initialValues={{title:a.title,instructions:a.contentBlocks.filter(b=>b.type==='TEXT').map(b=>b.content).join('\n\n'),dueAt:a.dueAt?new Date(new Date(a.dueAt).getTime()-new Date(a.dueAt).getTimezoneOffset()*60000).toISOString().slice(0,16):undefined}} onFinish={(v:{title:string;instructions:string;dueAt:string})=>perform(()=>learningApi.updateDraft(id,{studentId:a.studentId,subjectId:a.subjectId,title:v.title,assignmentType:'FREEFORM',status:'DRAFT',dueAt:new Date(v.dueAt).toISOString(),contentBlocks:[{type:'TEXT',content:v.instructions},...a.contentBlocks.filter(b=>b.type!=='TEXT')]}))}>
 <Form.Item name="title" label="Tên bài tập" rules={[{required:true},{max:255}]}><Input/></Form.Item><Form.Item name="instructions" label="Đề bài" rules={[{required:true}]}><Input.TextArea rows={8}/></Form.Item><Form.Item name="dueAt" label="Hạn nộp" rules={[{required:true}]}><Input type="datetime-local"/></Form.Item><Button htmlType="submit" loading={busy}>Lưu chỉnh sửa</Button>
 </Form>:a.contentBlocks?.filter(b=>b.type==='TEXT').map((b,i)=><p className="tm-prose" key={i}>{b.content}</p>)}
 {a.status==='DRAFT'&&<Button type="primary" loading={busy} onClick={()=>perform(()=>learningApi.publishAssignment(id))}>Phát hành bản đã lưu</Button>}
 {a.status==='PUBLISHED'&&<Button loading={busy} onClick={()=>perform(()=>learningApi.closeAssignment(id))}>Đóng nhận bài</Button>}
 </section><section className="tm-panel"><h2>Bài nộp</h2>{a.submissions.length?a.submissions.map(s=><div className="tm-toolbar" key={s.id}><span>{s.status==='GRADED'?'Đã chấm: '+s.score+'/10':'Đã nhận bài nộp'}</span><InlineActionLink href={'/teacher/submissions/'+s.id}>Xem và chấm bài</InlineActionLink></div>):<p>Học viên chưa nộp bài.</p>}</section></div>;
}
