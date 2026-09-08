'use client';
import {useState} from 'react';
import {useParams} from 'next/navigation';
import {useQuery} from '@tanstack/react-query';
import {Alert,Button,Skeleton} from 'antd';
import {axiosClient} from '@/shared/api/axiosClient';
import type {ApiResponse} from '@/shared/api/types';
import type {BookingDetail} from '../types';
import {SessionReportModal} from '../components/SessionReportModal';
import {CancelBookingModal} from '../components/CancelBookingModal';
import {ReviewBookingModal} from '../components/ReviewBookingModal';
import {BackLink} from '@/shared/components/navigation/NavigationLinks';
export default function BookingDetailPage({role}:{role:'student'|'teacher'}){
 const {id}=useParams<{id:string}>();const [action,setAction]=useState<string>();
 const q=useQuery({queryKey:['workspace-booking',role,id],queryFn:async()=>(await axiosClient.get<ApiResponse<BookingDetail>>('/api/'+role+'/bookings/'+id)).data});
 if(q.isLoading)return <Skeleton active/>;
 if(q.isError||!q.data?.data)return <Alert type="error" title="Chưa đọc được buổi học" description="Buổi học có thể không tồn tại hoặc không thuộc tài khoản của bạn." action={<Button onClick={()=>q.refetch()}>Thử lại</Button>}/>;
 const b=q.data.data;const close=()=>{setAction(undefined);q.refetch();};
 return <div className="tm-stack"><BackLink href={'/'+role+'/bookings'}>Lịch học</BackLink><section className="tm-panel"><p className="tm-eyebrow">{b.trial?'Buổi học thử':'Buổi học'}</p><h1>{b.subject.name}</h1><p>Gia sư: {b.teacher.fullName} · Học viên: {b.student.fullName}</p><p>{new Date(b.startTime).toLocaleString('vi-VN')} – {new Date(b.endTime).toLocaleTimeString('vi-VN')}</p><p>Trạng thái: {{SCHEDULED:'Đã lên lịch',COMPLETED:'Đã hoàn thành',CANCELLED:'Đã hủy',EXPIRED:'Hết hạn'}[b.status]}</p>{b.deliveryMode==='ONLINE'&&b.meetingLink&&/^https?:\/\//.test(b.meetingLink)&&b.status==='SCHEDULED'&&<a className="tm-button" href={b.meetingLink} target="_blank" rel="noopener noreferrer">Mở phòng học ↗</a>}{b.locationAddress&&<p>Địa điểm: {b.locationAddress}</p>}{role==='teacher'&&b.status==='SCHEDULED'&&<div className="tm-inline"><Button type="primary" onClick={()=>setAction('complete')}>Hoàn thành và viết báo cáo</Button><Button onClick={()=>setAction('cancel')}>Hủy buổi học</Button></div>}{role==='student'&&b.status==='COMPLETED'&&<Button onClick={()=>setAction('review')}>Đánh giá buổi học</Button>}</section>{b.sessionReport&&<section className="tm-panel"><h2>Báo cáo buổi học</h2><p className="tm-prose">{b.sessionReport.content}</p><h3>Nhận xét</h3><p>{b.sessionReport.feedback}</p>{b.sessionReport.followUpNote&&<p>Việc cần làm tiếp: {b.sessionReport.followUpNote}</p>}</section>}<SessionReportModal open={action==='complete'} booking={b} onClose={close}/><CancelBookingModal open={action==='cancel'} booking={b} onClose={close}/><ReviewBookingModal visible={action==='review'} bookingId={b.id} teacherName={b.teacher.fullName} onCancel={close} onSuccess={close}/></div>;
}

