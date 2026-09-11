import {axiosClient} from '@/shared/api/axiosClient';
import type {ApiResponse} from '@/shared/backend';
import type {CreateInvoiceRequest,InvoiceDetail} from '../types';
export const paymentApi = {
 async createInvoice(data:CreateInvoiceRequest,idempotencyKey:string=crypto.randomUUID()):Promise<ApiResponse<InvoiceDetail>> {
  return (await axiosClient.post<ApiResponse<InvoiceDetail>>('/api/student/invoices',data,{headers:{'Idempotency-Key':idempotencyKey}})).data;
 },
 async getInvoiceDetail(id:string):Promise<ApiResponse<InvoiceDetail>> {
  return (await axiosClient.get<ApiResponse<InvoiceDetail>>('/api/student/invoices/'+id)).data;
 }
};
