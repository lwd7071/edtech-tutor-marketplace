import { isConcurrentModification, parseApiError, ApiResponse } from '@/shared/backend';

describe('API Types and Error Envelope Parser (TDD)', () => {
  it('should correctly parse standard validation error with field mapping', () => {
    const errorResponse: ApiResponse<null> = {
      success: false,
      message: 'Dữ liệu không hợp lệ',
      data: null,
      errors: [
        { code: 'VALIDATION_ERROR', field: 'email', message: 'Email không đúng định dạng' },
        { code: 'VALIDATION_ERROR', field: 'password', message: 'Mật khẩu quá ngắn' },
      ],
      meta: null,
    };

    const parsed = parseApiError({ response: { data: errorResponse, status: 400 } });

    expect(parsed.code).toBe('VALIDATION_ERROR');
    expect(parsed.message).toBe('Dữ liệu không hợp lệ');
    expect(parsed.fieldErrors).toEqual({
      email: 'Email không đúng định dạng',
      password: 'Mật khẩu quá ngắn',
    });
    expect(parsed.isValidationError).toBe(true);
  });

  it('should correctly parse domain business error without field', () => {
    const errorResponse: ApiResponse<null> = {
      success: false,
      message: 'Không thể đặt lịch học',
      data: null,
      errors: [
        { code: 'BOOKING_TIME_CONFLICT', field: null, message: 'Khoảng thời gian bị trùng lịch' },
      ],
      meta: null,
    };

    const parsed = parseApiError({ response: { data: errorResponse, status: 409 } });

    expect(parsed.code).toBe('BOOKING_TIME_CONFLICT');
    expect(parsed.message).toBe('Khoảng thời gian bị trùng lịch');
    expect(parsed.isConflictError).toBe(true);
    expect(parsed.fieldErrors).toEqual({});
  });

  it('should provide fallback messages when error has no standard envelope', () => {
    const networkError = new Error('Network Error');
    const parsed = parseApiError(networkError);

    expect(parsed.code).toBe('NETWORK_ERROR');
    expect(parsed.message).toBe('Không thể kết nối đến máy chủ. Vui lòng kiểm tra đường truyền.');
    expect(parsed.fieldErrors).toEqual({});
  });

  it('should handle 401 unauthorized error gracefully', () => {
    const authError = { response: { status: 401, data: {} } };
    const parsed = parseApiError(authError);

    expect(parsed.code).toBe('UNAUTHORIZED');
    expect(parsed.isAuthError).toBe(true);
  });

  it('recognizes only CONCURRENT_MODIFICATION as stale data', () => {
    const error = (code: string) => ({ response: { status: 409, data: {
      success: false, message: null, data: null,
      errors: [{ code, field: null, message: 'conflict' }], meta: null,
    } } });

    expect(isConcurrentModification(error('CONCURRENT_MODIFICATION'))).toBe(true);
    expect(isConcurrentModification(error('BOOKING_TIME_CONFLICT'))).toBe(false);
  });
});
