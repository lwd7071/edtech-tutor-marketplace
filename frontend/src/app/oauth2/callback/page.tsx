'use client';

import { Suspense, useEffect, useRef, useState } from 'react';
import { Alert, Spin } from 'antd';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { authApi } from '@/shared/api/auth';
import { useAuthStore } from '@/shared/store/useAuthStore';
import { roleHome } from '@/shared/lib/navigation';

function OAuthCallbackContent() {
  const router = useRouter();
  const params = useSearchParams();
  const setAuth = useAuthStore((state) => state.setAuth);
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
        setAuth(result.data.user, result.data.accessToken, result.data.refreshToken, true);
        router.replace(roleHome(result.data.user.role));
      })
      .catch((requestError) => {
        setError(requestError.response?.data?.message || 'Khong the hoan tat dang nhap Google.');
      });
  }, [params, router, setAuth]);

  if (error) {
    return (
      <Alert
        type="error"
        showIcon
        message={error}
        action={<Link href="/auth/login">Quay lai dang nhap</Link>}
      />
    );
  }

  return <Spin tip="Dang hoan tat dang nhap Google..." />;
}

export default function OAuthCallbackPage() {
  return (
    <Suspense fallback={<Spin />}>
      <OAuthCallbackContent />
    </Suspense>
  );
}
