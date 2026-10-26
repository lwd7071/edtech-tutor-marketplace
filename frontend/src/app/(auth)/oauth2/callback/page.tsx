'use client';

import { Suspense, useEffect, useRef, useState } from 'react';
import { Spin } from 'antd';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { authApi } from '@/shared/api/auth';
import { useAuthStore } from '@/features/auth';
import { roleHome } from '@/shared/lib/navigation';
import { useQueryClient } from '@tanstack/react-query';

function OAuthCallbackContent() {
  const router = useRouter();
  const params = useSearchParams();
  const establish = useAuthStore((state) => state.establish);
  const queryClient = useQueryClient();
  const handled = useRef(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (handled.current) return;
    handled.current = true;

    if (params.get('error')) {
      setError('Google không thể xác thực tài khoản. Vui lòng thử lại.');
      return;
    }

    const registrationToken = params.get('registrationToken');
    if (registrationToken) {
      router.replace('/auth/oauth/role?registrationToken=' + encodeURIComponent(registrationToken));
      return;
    }

    const exchangeCode = params.get('exchangeCode');
    if (!exchangeCode) {
      setError('Phản hồi xác thực từ Google không hợp lệ.');
      return;
    }

    authApi.exchangeOAuthToken({ exchangeCode })
      .then((result) => {
        if (!result.data) throw new Error('OAuth response has no data');
        queryClient.clear();
        establish(result.data, true);
        router.replace(roleHome(result.data.user.role));
      })
      .catch((requestError) => {
        setError(requestError.response?.data?.message || 'Khong the hoan tat dang nhap Google.');
      });
  }, [params, router, establish, queryClient]);

  if (error) {
    return (
      <main className="tm-oauth-callback" aria-labelledby="oauth-callback-title">
        <section className="tm-oauth-callback-content" aria-live="assertive">
          <p className="tm-oauth-callback-status">Không thể đăng nhập</p>
          <h1 id="oauth-callback-title">Xác thực Google chưa hoàn tất</h1>
          <p className="tm-oauth-callback-message">{error}</p>
          <Link className="tm-oauth-callback-recovery" href="/auth/login">Quay lại đăng nhập</Link>
        </section>
      </main>
    );
  }

  return (
    <main className="tm-oauth-callback" aria-labelledby="oauth-callback-title" aria-busy="true">
      <section className="tm-oauth-callback-content" aria-live="polite">
        <Spin className="tm-oauth-callback-spinner" />
        <h1 id="oauth-callback-title">Đang hoàn tất đăng nhập</h1>
        <p className="tm-oauth-callback-message">Đang xác thực tài khoản Google và chuẩn bị không gian học tập của bạn.</p>
      </section>
    </main>
  );
}

export default function OAuthCallbackPage() {
  return (
    <Suspense fallback={
      <main className="tm-oauth-callback" aria-busy="true" aria-label="Đang tải phản hồi xác thực">
        <section className="tm-oauth-callback-content"><Spin className="tm-oauth-callback-spinner" /><p role="status">Đang tải phản hồi xác thực...</p></section>
      </main>
    }>
      <OAuthCallbackContent />
    </Suspense>
  );
}
