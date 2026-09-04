'use client';

import React, { Component, ErrorInfo, ReactNode, useState } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Button, Alert, Space, Typography } from 'antd';

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

/**
 * ErrorBoundary bắt các exception chưa được xử lý trong cây React Component
 */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    // Có thể gửi log tới Sentry hoặc hệ thống theo dõi ở đây
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div style={{ padding: 'var(--space-6)', maxWidth: 640, margin: 'var(--space-8) auto' }}>
          <Alert
            type="error"
            showIcon
            message="Đã có lỗi xảy ra trong quá trình hiển thị"
            description={
              <Space orientation="vertical" style={{ width: '100%', marginTop: 'var(--space-2)' }}>
                <Typography.Text type="secondary">
                  {this.state.error?.message || 'Lỗi không xác định. Vui lòng thử lại.'}
                </Typography.Text>
                <Button
                  type="primary"
                  danger
                  onClick={this.handleReset}
                  style={{ marginTop: 'var(--space-2)' }}
                >
                  Thử lại
                </Button>
              </Space>
            }
          />
        </div>
      );
    }

    return this.props.children;
  }
}

/**
 * Khởi tạo QueryClient với các cấu hình tối ưu mặc định
 */
export function createDefaultQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60 * 1000, // 1 phút
        gcTime: 5 * 60 * 1000, // 5 phút
        refetchOnWindowFocus: false, // Tránh gọi API liên tục khi chuyển tab
        retry: (failureCount, error: any) => {
          // Không retry với các lỗi client / xác thực / tài nguyên không tìm thấy
          const status = error?.response?.status;
          if (status === 401 || status === 403 || status === 404 || status === 422) {
            return false;
          }
          return failureCount < 2;
        },
      },
    },
  });
}

/**
 * AppProviders: Wrapper tổng bọc các providers toàn cục của ứng dụng
 */
export function AppProviders({ children }: { children: ReactNode }) {
  // Dùng useState để đảm bảo QueryClient là singleton trên mỗi client session
  const [queryClient] = useState(() => createDefaultQueryClient());

  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        {children}
      </QueryClientProvider>
    </ErrorBoundary>
  );
}
