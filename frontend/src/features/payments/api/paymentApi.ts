import {axiosClient} from '@/shared/api/axiosClient';
import { requireApiData } from '@/shared/backend';
import type {ApiResponse, ApiResponseWithData} from '@/shared/backend';
import type {CreateInvoiceRequest,InvoiceDetail} from '../types';
export const paymentApi = {
 async createInvoice(data:CreateInvoiceRequest,idempotencyKey:string=crypto.randomUUID()):Promise<ApiResponseWithData<InvoiceDetail>> {
  return requireApiData((await axiosClient.post<ApiResponse<InvoiceDetail>>('/api/student/invoices',data,{headers:{'Idempotency-Key':idempotencyKey}})).data);
 },
 async getInvoiceDetail(id:string):Promise<ApiResponseWithData<InvoiceDetail>> {
  return requireApiData((await axiosClient.get<ApiResponse<InvoiceDetail>>('/api/student/invoices/'+id)).data);
 }
};
