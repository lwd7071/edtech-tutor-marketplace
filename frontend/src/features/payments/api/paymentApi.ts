import {axiosClient} from '@/shared/api/axiosClient';
import type {ApiResponse} from '@/shared/api/types';
import type {CreateInvoiceRequest,InvoiceDetail} from '../types';
export const paymentApi = {
 async createInvoice(data:CreateInvoiceRequest,idempotencyKey:string=crypto.randomUUID()):Promise<ApiResponse<InvoiceDetail>> {
  const response=await axiosClient.post<InvoiceDetail>('/api/student/invoices',data,{headers:{'Idempotency-Key':idempotencyKey}});
  return {data:response.data,success:true,message:'',errors:[]};
 },
 async getInvoiceDetail(id:string):Promise<ApiResponse<InvoiceDetail>> {
  const response=await axiosClient.get<InvoiceDetail>('/api/student/invoices/'+id);
  return {data:response.data,success:true,message:'',errors:[]};
 }
};
