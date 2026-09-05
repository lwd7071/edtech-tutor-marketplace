/**
 * Chuẩn hóa API Response Envelope theo API_CONTRACT.md và ERROR_CODES.md
 */

export interface ApiErrorDetail {
  code: string;
  field: string | null;
  message: string;
}

export interface PaginationMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errors: ApiErrorDetail[];
  meta?: PaginationMeta | null;
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

/**
 * Trích xuất lỗi từ response hoặc exception thành cấu trúc chuẩn dễ sử dụng cho UI/Form
 */
export function parseApiError(error: unknown): ParsedApiError {
  const errObj = error as {
    response?: {
      status?: number;
      data?: Partial<ApiResponse<unknown>>;
    };
    message?: string;
  };

  const status = errObj?.response?.status;
  const data = errObj?.response?.data;
  const rawErrors: ApiErrorDetail[] = Array.isArray(data?.errors) ? data!.errors! : [];

  const fieldErrors: Record<string, string> = {};
  for (const item of rawErrors) {
    if (item.field) {
      fieldErrors[item.field] = item.message;
    }
  }

  // Xác định mã lỗi chính (primary code)
  let code = 'UNKNOWN_ERROR';
  if (rawErrors.length > 0 && rawErrors[0].code) {
    code = rawErrors[0].code;
  } else if (status === 401) {
    code = 'UNAUTHORIZED';
  } else if (status === 403) {
    code = 'FORBIDDEN_RESOURCE';
  } else if (status === 404) {
    code = 'RESOURCE_NOT_FOUND';
  } else if (status === 409) {
    code = 'CONFLICT';
  } else if (!errObj?.response && errObj?.message?.includes('Network')) {
    code = 'NETWORK_ERROR';
  }

  // Xác định message thân thiện
  let message = '';
  if (rawErrors.length === 1 && rawErrors[0].message) {
    message = rawErrors[0].message;
  } else {
    message = data?.message || (rawErrors.length > 0 ? rawErrors[0].message : '');
  }
  if (!message) {
    if (code === 'NETWORK_ERROR') {
      message = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra đường truyền.';
    } else if (status === 401) {
      message = 'Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.';
    } else if (status === 403) {
      message = 'Bạn không có quyền thực hiện thao tác này.';
    } else if (status === 404) {
      message = 'Không tìm thấy tài nguyên yêu cầu.';
    } else if (status === 500) {
      message = 'Đã có lỗi xảy ra trên hệ thống. Vui lòng thử lại sau.';
    } else {
      message = errObj?.message || 'Đã có lỗi xảy ra. Vui lòng thử lại.';
    }
  }

  return {
    code,
    message,
    fieldErrors,
    rawErrors,
    status,
    isValidationError: code === 'VALIDATION_ERROR' || status === 400,
    isConflictError: code === 'CONFLICT' || status === 409 || code.includes('CONFLICT'),
    isAuthError: code === 'UNAUTHORIZED' || status === 401 || code === 'AUTH_TOKEN_EXPIRED',
    isForbiddenError: code === 'FORBIDDEN_RESOURCE' || status === 403,
    isNotFoundError: code === 'RESOURCE_NOT_FOUND' || status === 404,
  };
}
