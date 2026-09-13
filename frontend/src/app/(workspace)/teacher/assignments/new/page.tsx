'use client';
import {useState} from 'react';
import {useRouter} from 'next/navigation';
import {useQuery} from '@tanstack/react-query';
import {Alert,App,Button,Form,Input,Select,Skeleton} from 'antd';
import {getLearners} from '@/features/teacher-dashboard/components/LearnerList';
import {learningApi} from '@/features/learning/api/learningApi';
import {learningKeys} from '@/features/learning/data/learningKeys';
export default function Page(){
 const q=useQuery({queryKey:learningKeys.assignmentLearners(),queryFn:()=>getLearners(0,100)}); const [busy,setBusy]=useState(false); const router=useRouter(); const {message}=App.useApp();
 if(q.isLoading)return <Skeleton active/>;
 if(q.isError)return <Alert type="error" title="Chưa tải được danh sách học viên" action={<Button onClick={()=>q.refetch()}>Thử lại</Button>}/>;
 return <section className="tm-panel" style={{maxWidth:820}}><h1>Giao bài tập</h1><Form layout="vertical" initialValues={{status:'DRAFT'}} onFinish={async(v:{packageId:string;title:string;instructions:string;dueAt:string;status:'DRAFT'|'PUBLISHED'})=>{const selected=q.data?.data.find(r=>r.studentPackage.id===v.packageId);if(!selected)return;setBusy(true);try{await learningApi.createAssignment({studentId:selected.studentId,subjectId:selected.studentPackage.subjectId,title:v.title,assignmentType:'FREEFORM',status:v.status,dueAt:new Date(v.dueAt).toISOString(),contentBlocks:[{type:'TEXT',content:v.instructions}],version:0});message.success(v.status==='DRAFT'?'Đã lưu nháp':'Đã giao bài');router.push('/teacher/assignments');}catch{message.error('Chưa giao được bài. Kiểm tra thông tin và quan hệ học viên.');}finally{setBusy(false);}}}><Form.Item name="packageId" label="Học viên và môn học" rules={[{required:true}]}><Select showSearch optionFilterProp="label" options={q.data?.data.filter(r=>['ACTIVE','COMPLETED'].includes(r.studentPackage.status)).map(r=>({value:r.studentPackage.id,label:r.studentName+' · '+r.subjectName+' · '+r.studentPackage.packageName}))}/></Form.Item><Form.Item name="title" label="Tên bài tập" rules={[{required:true}]}><Input/></Form.Item><Form.Item name="instructions" label="Đề bài và hướng dẫn" rules={[{required:true}]}><Input.TextArea rows={8}/></Form.Item><Form.Item name="dueAt" label="Hạn nộp (giờ địa phương)" rules={[{required:true}]}><Input type="datetime-local"/></Form.Item><Form.Item name="status" label="Trạng thái"><Select options={[{value:'DRAFT',label:'Lưu nháp'},{value:'PUBLISHED',label:'Phát hành ngay'}]}/></Form.Item><Button htmlType="submit" type="primary" loading={busy}>Giao bài tập</Button></Form></section>;
}
