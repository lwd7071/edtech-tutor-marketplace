import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { AppProviders, ErrorBoundary } from './AppProviders';
import { useQuery } from '@tanstack/react-query';

// Component giả lập dùng react-query để kiểm tra provider
function QueryConsumerComponent() {
  const { data } = useQuery({
    queryKey: ['test-query'],
    queryFn: () => Promise.resolve('data-from-query'),
  });
  return <div>{data || 'loading'}</div>;
}

// Component cố ý ném lỗi để kiểm tra ErrorBoundary
function CrashingComponent({ shouldCrash }: { shouldCrash: boolean }) {
  if (shouldCrash) {
    throw new Error('Cố ý gây lỗi crash component');
  }
  return <div>Hoạt động bình thường</div>;
}

describe('AppProviders & ErrorBoundary (TDD)', () => {
  // Tắt console.error khi chạy test ErrorBoundary để không làm rác log
  const originalError = console.error;
  beforeAll(() => {
    console.error = jest.fn();
  });
  afterAll(() => {
    console.error = originalError;
  });

  it('should render children and provide QueryClient context', async () => {
    render(
      <AppProviders>
        <QueryConsumerComponent />
      </AppProviders>
    );

    expect(await screen.findByText('data-from-query')).toBeInTheDocument();
  });

  it('should catch runtime errors and display fallback error UI with retry button', () => {
    const { rerender } = render(
      <ErrorBoundary>
        <CrashingComponent shouldCrash={true} />
      </ErrorBoundary>
    );

    expect(screen.getByText(/Đã có lỗi xảy ra/i)).toBeInTheDocument();
    expect(screen.getByText(/Cố ý gây lỗi crash component/i)).toBeInTheDocument();

    const reloadBtn = screen.getByRole('button', { name: /Thử lại/i });
    expect(reloadBtn).toBeInTheDocument();
  });
});
