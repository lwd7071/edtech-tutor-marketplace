'use client';
import {useState} from 'react';
import {useQuery} from '@tanstack/react-query';
import {Alert,App,Button,Tag,Skeleton,Pagination} from 'antd';
import {teacherApi} from '@/shared/api/teacher';
import {teacherDashboardApi} from '../api/teacherDashboardApi';
import {teacherDashboardKeys} from '../data/teacherDashboardKeys';
export function PackageList({onCreate,onEdit}:{onCreate:()=>void;onEdit:(id:string)=>void}){
 const [page,setPage]=useState(0);const [busy,setBusy]=useState<string|null>(null);const {message}=App.useApp();
 const profile=useQuery({queryKey:teacherDashboardKeys.profile(),queryFn:teacherApi.getProfile});
 const q=useQuery({queryKey:teacherDashboardKeys.packages(page),queryFn:()=>teacherDashboardApi.getPackages(page,12)});
 return <div className="tm-stack"><header className="tm-toolbar"><div className="tm-page-heading"><h1>Gói học của bạn</h1><p>Giá trọn gói, số buổi và thời hạn rõ ràng cho học viên.</p></div><Button type="primary" disabled={profile.data?.approvalStatus!=='APPROVED'} onClick={onCreate}>Tạo gói học</Button></header>{profile.data?.approvalStatus!=='APPROVED'&&<Alert type="info" title="Hồ sơ cần được duyệt trước khi tạo gói học"/>}{q.isLoading?<Skeleton active/>:q.isError?<Alert type="error" title="Chưa tải được gói học" action={<Button onClick={()=>q.refetch()}>Thử lại</Button>}/>:<><div className="tm-package-grid">{q.data?.data.map(p=><article key={p.id} className="tm-panel"><Tag>{{DRAFT:'Bản nháp',ACTIVE:'Đang mở bán',INACTIVE:'Ngừng bán'}[p.status]||p.status}</Tag><h2>{p.name}</h2><p>{p.subjectName} · {p.totalSessions} buổi · {p.sessionDurationMinutes} phút/buổi</p><p>Hạn sử dụng {p.durationDays} ngày</p><p className="tm-package-price">{p.priceVnd.toLocaleString('vi-VN')}đ</p><div className="tm-inline"><Button onClick={()=>onEdit(p.id)}>Chỉnh sửa</Button><Button loading={busy===p.id} onClick={async()=>{setBusy(p.id);try{await teacherApi.updatePackageStatus(p.id,p.status==='ACTIVE'?'INACTIVE':'ACTIVE');await q.refetch();}catch{message.error('Chưa cập nhật được trạng thái');}finally{setBusy(null);}}}>{p.status==='ACTIVE'?'Ngừng bán':'Mở bán'}</Button></div></article>)}</div>{!q.data?.data.length&&<div className="tm-panel">Chưa có gói học. Hoàn thiện hồ sơ để bắt đầu tạo gói.</div>}<Pagination current={page+1} total={q.data?.meta?.totalElements||0} pageSize={12} showSizeChanger={false} onChange={n=>setPage(n-1)}/></>}</div>;
}
