/**
 * Kiểu dữ liệu và DTO cho phân hệ Thanh toán (Payments & Invoices)
 * Khớp chuẩn API Contract và SPEC-FE
 */

export type InvoiceStatus = 'PENDING' | 'PAID' | 'CANCELLED' | 'EXPIRED';

export interface CreateInvoiceRequest {
  pricingPackageId: string;
  returnUrl?: string;
  cancelUrl?: string;
}

export interface InvoiceDetail {
  id: string;
  invoiceNumber: string;
  pricingPackageId: string;
  packageName?: string;
  amountVnd: number;
  status: InvoiceStatus;
  checkoutUrl: string;
  qrCode: string;
  paymentExpiredAt: string;
  paidAt?: string | null;
}
