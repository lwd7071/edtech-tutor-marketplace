'use client';
import {Suspense} from 'react';
import {useSearchParams} from 'next/navigation';
import Link from 'next/link';
import {Result,Spin} from 'antd';
import {useInvoiceDetail} from '@/features/payments/hooks/usePayments';
function Callback(){
 const params=useSearchParams(); const id=params.get('invoiceId')||'';
 const {data,isLoading,isError,refetch}=useInvoiceDetail(id,{polling:true}); const status=data?.data?.status;
 return <section className="tm-panel"><Result status={status==='PAID'?'success':status==='CANCELLED'||status==='EXPIRED'?'warning':'info'} title={status==='PAID'?'Thanh toán đã được xác nhận':status==='CANCELLED'?'Hóa đơn đã hủy':status==='EXPIRED'?'Hóa đơn đã hết hạn':'Đang đối chiếu thanh toán'} subTitle={isError?'Chưa đọc được trạng thái. Hãy thử kiểm tra lại.':!id?'Chưa có mã hóa đơn để đối chiếu. Kiểm tra gói học trong tài khoản; kết quả trên đường dẫn không xác nhận thanh toán.':'Gói học chỉ được cấp sau khi hệ thống xác nhận giao dịch.'} extra={<div className="tm-inline" style={{justifyContent:'center',flexWrap:'wrap'}}>{isLoading&&<Spin/>}{id&&<button className="tm-button" onClick={()=>refetch()}>Kiểm tra lại</button>}<Link className="tm-button tm-button-secondary" href="/student/packages">Xem gói học</Link></div>}/></section>;
}
export default function Page(){return <Suspense fallback={<Spin/>}><Callback/></Suspense>;}

