'use client';

import { useRouter } from 'next/navigation';
import { ErrorState } from '@/shared/components/feedback/ErrorState';

export default function ForbiddenPage() {
  const router = useRouter();

  return (
    <main className="tm-forbidden-page">
      <section className="tm-forbidden-state" aria-labelledby="tm-forbidden-title">
        <header className="tm-forbidden-heading">
          <h1 id="tm-forbidden-title">Truy cập bị từ chối</h1>
        </header>
        <div className="tm-forbidden-feedback">
          <ErrorState
            variant="inline"
            className="tm-forbidden-feedback-error"
            title="403"
            message="Bạn không có quyền truy cập vào trang này."
            onRetry={() => router.back()}
            actionText="Quay lại"
          />
        </div>
      </section>
    </main>
  );
}
