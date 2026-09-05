import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { paymentApi } from '../api/paymentApi';
import { CreateInvoiceRequest } from '../types';

export const PAYMENT_KEYS = {
  all: ['payments'] as const,
  invoices: () => [...PAYMENT_KEYS.all, 'invoice'] as const,
  invoice: (id: string) => [...PAYMENT_KEYS.invoices(), id] as const,
};

/**
 * Hook tạo hóa đơn thanh toán (payOS checkout link)
 */
export function useCreateInvoice() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateInvoiceRequest) => paymentApi.createInvoice(data),
    onSuccess: (response) => {
      if (response?.data?.id) {
        queryClient.invalidateQueries({
          queryKey: PAYMENT_KEYS.invoice(response.data.id),
        });
      }
    },
  });
}

interface UseInvoiceDetailOptions {
  polling?: boolean;
  pollingInterval?: number;
}

/**
 * Hook lấy chi tiết hóa đơn, hỗ trợ polling tự động (mặc định 3s theo SPEC-FE:E.5)
 * Tự động dừng polling khi status chuyển thành terminal: PAID, CANCELLED, EXPIRED
 */
export function useInvoiceDetail(
  id: string,
  options?: UseInvoiceDetailOptions
) {
  const pollingInterval = options?.pollingInterval ?? 3000;
  const isPollingEnabled = options?.polling ?? false;

  return useQuery({
    queryKey: PAYMENT_KEYS.invoice(id),
    queryFn: () => paymentApi.getInvoiceDetail(id),
    enabled: !!id,
    refetchInterval: (query) => {
      if (!isPollingEnabled) return false;
      const status = query.state.data?.data?.status;
      // Dừng polling khi đã đạt trạng thái cuối
      if (status === 'PAID' || status === 'CANCELLED' || status === 'EXPIRED') {
        return false;
      }
      return pollingInterval;
    },
    refetchIntervalInBackground: false,
  });
}
