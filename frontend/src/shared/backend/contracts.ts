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

export interface PageResult<T> {
  items: T[];
  page: PaginationMeta;
}
