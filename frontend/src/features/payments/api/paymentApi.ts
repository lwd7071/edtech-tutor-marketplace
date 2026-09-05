import { axiosClient } from '@/shared/api/axiosClient';
import { ApiResponse } from '@/shared/api/types';
import { CreateInvoiceRequest, InvoiceDetail } from '../types';

export const paymentApi = {
  /**
   * Tạo hóa đơn thanh toán cho gói học (payOS checkout)
   */
  createInvoice: async (
    data: CreateInvoiceRequest
  ): Promise<ApiResponse<InvoiceDetail>> => {
    const response = await axiosClient.post<ApiResponse<InvoiceDetail>>(
      '/api/student/invoices',
      data
    );
    return response.data;
  },

  /**
   * Lấy chi tiết hóa đơn và trạng thái thanh toán
   */
  getInvoiceDetail: async (
    id: string
  ): Promise<ApiResponse<InvoiceDetail>> => {
    const response = await axiosClient.get<ApiResponse<InvoiceDetail>>(
      `/api/student/invoices/${id}`
    );
    return response.data;
  },
};
