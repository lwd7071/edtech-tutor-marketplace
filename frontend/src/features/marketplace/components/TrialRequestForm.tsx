'use client';
import {useState} from 'react';
import {usePathname,useRouter} from 'next/navigation';
import {App,Button,Form,Input,Select,Alert} from 'antd';
import {useAuthStore} from '@/features/auth';
import {bookingApi} from '@/features/bookings/api/bookingApi';
export default function TrialRequestForm({teacherId,subjects}:{teacherId:string;subjects:{id:string;name:string}[]}){
 const {user}=useAuthStore();const router=useRouter();const path=usePathname();const {message}=App.useApp();const [busy,setBusy]=useState(false);const [sent,setSent]=useState(false);
 if(user&&user.role!=='STUDENT')return <p>Yêu cầu học thử dành cho tài khoản học viên.</p>;
 if(!user)return <Button block onClick={()=>router.push('/auth/login?redirect='+encodeURIComponent(path+'#trial'))}>Đăng nhập để yêu cầu học thử</Button>;
 if(sent)return <Alert type="success" showIcon title="Đã gửi yêu cầu học thử" description="Gia sư sẽ phản hồi yêu cầu. Thời gian mong muốn chưa phải lịch học được xác nhận."/>;
 return <Form layout="vertical" onFinish={async(values:{subjectId:string;time:string;note?:string})=>{if(busy)return;setBusy(true);try{await bookingApi.createTrialRequest({teacherId,subjectId:values.subjectId,preferredStartTime:new Date(values.time).toISOString(),note:values.note});setSent(true);}catch{message.error('Chưa gửi được yêu cầu. Kiểm tra thông tin và thử lại.');}finally{setBusy(false);}}}><Form.Item name="subjectId" label="Môn muốn học" rules={[{required:true,message:'Chọn môn học'}]}><Select options={subjects.map(s=>({label:s.name,value:s.id}))}/></Form.Item><Form.Item name="time" label="Thời gian mong muốn (giờ địa phương)" rules={[{required:true,message:'Chọn thời gian'},{validator:(_,value)=>!value||new Date(value).getTime()>Date.now()?Promise.resolve():Promise.reject(new Error('Chọn thời gian trong tương lai'))}]}><Input type="datetime-local"/></Form.Item><Form.Item name="note" label="Mục tiêu hoặc lời nhắn"><Input.TextArea rows={3} maxLength={1000}/></Form.Item><Button block htmlType="submit" type="primary" loading={busy} disabled={!subjects.length}>Gửi yêu cầu học thử</Button><p style={{fontSize:13,color:'var(--color-text-secondary)'}}>Gia sư cần chấp nhận yêu cầu trước khi buổi học được tạo.</p></Form>;
}
