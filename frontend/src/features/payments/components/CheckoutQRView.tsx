'use client';
import {Alert,Button,QRCode} from 'antd';
import Link from 'next/link';
import type {InvoiceDetail} from '../types';
export const formatVnd=(amount:number)=>amount.toLocaleString('vi-VN')+' ₫';
export function CheckoutQRView({invoice}:{invoice:InvoiceDetail}){
 const pending=invoice.status==='PENDING';
 return <section className="tm-panel" style={{maxWidth:560,margin:'auto',textAlign:'center'}}><p className="tm-eyebrow">Thanh toán gói học</p><h1>Hóa đơn {invoice.invoiceNumber}</h1><p className="tm-package-price">{formatVnd(invoice.amountVnd)}</p>{pending?<><Alert showIcon type="info" title="Đang chờ xác nhận thanh toán" description="Trang sẽ tự cập nhật sau khi hệ thống nhận kết quả giao dịch."/><div style={{display:'flex',justifyContent:'center',margin:24}}>{invoice.qrCode&&<QRCode value={invoice.qrCode} size={220}/>}</div>{invoice.checkoutUrl&&<Button type="primary" size="large" href={invoice.checkoutUrl}>Mở trang thanh toán</Button>}{invoice.paymentExpiredAt&&<p>Hết hạn: {new Date(invoice.paymentExpiredAt).toLocaleString('vi-VN')}</p>}</>:<Alert type={invoice.status==='PAID'?'success':'warning'} title={invoice.status==='PAID'?'Đã xác nhận thanh toán':invoice.status==='EXPIRED'?'Hóa đơn hết hạn':'Hóa đơn đã hủy'}/>}<p><Link href="/student/packages">Xem gói học của tôi →</Link></p></section>;
}
