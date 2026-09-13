import type { ApiErrorDetail, ApiResponse } from './contracts';

export class ApiContractError extends Error {
  readonly code = 'API_CONTRACT_INVALID';
  constructor(message = 'Máy chủ trả về response thiếu data theo contract') {
    super(message);
    this.name = 'ApiContractError';
  }
}

export type ApiResponseWithData<T> = ApiResponse<T> & { data: T };

export function requireApiData<T>(response: ApiResponse<T>): ApiResponseWithData<T> {
  if (!response.success || response.data == null) throw new ApiContractError();
  return response as ApiResponseWithData<T>;
}

export function isConcurrentModification(error: unknown): boolean {
  return parseApiError(error).code === 'CONCURRENT_MODIFICATION';
}

export interface ParsedApiError {
  code: string;
  message: string;
  fieldErrors: Record<string, string>;
  rawErrors: ApiErrorDetail[];
  status?: number;
  isValidationError: boolean;
  isConflictError: boolean;
  isAuthError: boolean;
  isForbiddenError: boolean;
  isNotFoundError: boolean;
}

export function parseApiError(error: unknown): ParsedApiError {
  const errObj = error as { response?: { status?: number; data?: Partial<ApiResponse<unknown>> }; message?: string };
  const status = errObj?.response?.status;
  const data = errObj?.response?.data;
  const rawErrors = Array.isArray(data?.errors) ? data.errors : [];
  const fieldErrors = Object.fromEntries(rawErrors.filter((item) => item.field).map((item) => [item.field as string, item.message]));

  let code = rawErrors[0]?.code ?? 'UNKNOWN_ERROR';
  if (code === 'UNKNOWN_ERROR') {
    if (status === 401) code = 'UNAUTHORIZED';
    else if (status === 403) code = 'FORBIDDEN_RESOURCE';
    else if (status === 404) code = 'RESOURCE_NOT_FOUND';
    else if (status === 409) code = 'CONFLICT';
    else if (!errObj.response && errObj.message?.includes('Network')) code = 'NETWORK_ERROR';
  }

  const fallbackByCode: Record<string, string> = {
    NETWORK_ERROR: 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra đường truyền.',
    UNAUTHORIZED: 'Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.',
    FORBIDDEN_RESOURCE: 'Bạn không có quyền thực hiện thao tác này.',
    RESOURCE_NOT_FOUND: 'Không tìm thấy tài nguyên yêu cầu.',
  };
  const message = (rawErrors.length === 1 ? rawErrors[0].message : data?.message || rawErrors[0]?.message)
    || fallbackByCode[code]
    || (status === 500 ? 'Đã có lỗi xảy ra trên hệ thống. Vui lòng thử lại sau.' : undefined)
    || errObj.message
    || 'Đã có lỗi xảy ra. Vui lòng thử lại.';

  return {
    code, message, fieldErrors, rawErrors, status,
    isValidationError: code === 'VALIDATION_ERROR' || status === 400,
    isConflictError: code === 'CONFLICT' || status === 409 || code.includes('CONFLICT'),
    isAuthError: code === 'UNAUTHORIZED' || status === 401 || code === 'AUTH_TOKEN_EXPIRED',
    isForbiddenError: code === 'FORBIDDEN_RESOURCE' || status === 403,
    isNotFoundError: code === 'RESOURCE_NOT_FOUND' || status === 404,
  };
}
