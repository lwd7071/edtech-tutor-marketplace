export type PaymentStatus = 'PENDING' | 'PAID' | 'CANCELLED' | 'EXPIRED' | 'FAILED';

export interface CheckoutRequest {
  packageId: string;
  returnUrl?: string;
  cancelUrl?: string;
}

export interface CheckoutResponse {
  invoiceId: string;
  paymentUrl: string;
  orderCode: number;
  amount: number;
  status: PaymentStatus;
}

export interface InvoiceDetail {
  id: string;
  invoiceNumber: string;
  amount: number;
  status: PaymentStatus;
  paymentMethod: string;
  createdAt: string;
  expiresAt: string | null;
  paidAt: string | null;
}
