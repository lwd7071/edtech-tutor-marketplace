'use client';
import {useState} from 'react';
import {useQuery} from '@tanstack/react-query';
import {Alert,App,Button,Form,Input,InputNumber,Select,Skeleton} from 'antd';
import {teacherApi,type CreatePackageRequest} from '@/shared/api/teacher';
import type {PricingPackageView} from '@/shared/api/public';
import {teacherDashboardKeys} from '../data/teacherDashboardKeys';
export function PackageForm({mode,packageId,initialValues,onSave,onCancel}:{mode:'create'|'edit';packageId?:string;initialValues?:Partial<PricingPackageView>;onSave:()=>void;onCancel:()=>void}){
 const subjects=useQuery({queryKey:teacherDashboardKeys.subjects(),queryFn:teacherApi.getSubjects});
 const pkg=useQuery({queryKey:teacherDashboardKeys.package(packageId ?? ''),queryFn:()=>teacherApi.getPackage(packageId!),enabled:mode==='edit'&&!!packageId});
 const [busy,setBusy]=useState(false);const {message}=App.useApp();
 if(subjects.isLoading||(mode==='edit'&&pkg.isLoading))return <Skeleton active/>;
 if(subjects.isError||pkg.isError)return <Alert type="error" title="Chưa tải được dữ liệu gói" action={<Button onClick={()=>{subjects.refetch();if(packageId)pkg.refetch();}}>Thử lại</Button>}/>;
 return <section className="tm-panel" style={{maxWidth:820}}><h1>{mode==='create'?'Tạo gói học':'Chỉnh sửa gói học'}</h1><Form layout="vertical" initialValues={pkg.data||initialValues||{totalSessions:10,sessionDurationMinutes:60,durationDays:60,status:'DRAFT'}} onFinish={async(v:CreatePackageRequest)=>{setBusy(true);try{if(mode==='edit'&&packageId)await teacherApi.updatePackage(packageId,v);else await teacherApi.createPackage(v);message.success('Đã lưu gói học');onSave();}catch{message.error('Chưa lưu được gói. Kiểm tra trạng thái hồ sơ và thông tin gói.');}finally{setBusy(false);}}}><Form.Item name="name" label="Tên gói học" rules={[{required:true}]}><Input/></Form.Item><Form.Item name="subjectId" label="Môn học" rules={[{required:true}]}><Select options={subjects.data?.map(s=>({value:s.subjectId,label:s.name}))}/></Form.Item><Form.Item name="description" label="Nội dung và mục tiêu"><Input.TextArea rows={4}/></Form.Item><div className="tm-package-grid">{[['totalSessions','Số buổi'],['sessionDurationMinutes','Thời lượng mỗi buổi (phút)'],['durationDays','Hạn sử dụng (ngày)'],['priceVnd','Giá trọn gói (VNĐ)']].map(([name,label])=><Form.Item key={name} name={name} label={label} rules={[{required:true}]}><InputNumber min={1} precision={0} style={{width:'100%'}}/></Form.Item>)}</div><Form.Item name="status" label="Trạng thái" rules={[{required:true}]}><Select options={[{value:'DRAFT',label:'Bản nháp'},{value:'ACTIVE',label:'Đang mở bán'},{value:'INACTIVE',label:'Ngừng bán'}]}/></Form.Item><div className="tm-inline"><Button htmlType="submit" type="primary" loading={busy}>Lưu gói học</Button><Button onClick={onCancel} disabled={busy}>Quay lại</Button></div></Form></section>;
}
