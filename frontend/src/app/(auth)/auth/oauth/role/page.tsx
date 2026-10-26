'use client';

import React, { Suspense, useState } from 'react';
import { Alert, Button } from 'antd';
import { useRouter, useSearchParams } from 'next/navigation';
import { useQueryClient } from '@tanstack/react-query';
import { authApi } from '@/shared/api/auth';
import { useAuthStore } from '@/features/auth';
import RadioCard from '@/shared/components/ui/RadioCard';
import { roleHome } from '@/shared/lib/navigation';

function OAuthRoleContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const registrationToken = searchParams.get('registrationToken');
  const establish = useAuthStore((state) => state.establish);
  const queryClient = useQueryClient();
  const [role, setRole] = useState<'STUDENT' | 'TEACHER'>('STUDENT');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const onSubmit = async () => {
    if (!registrationToken) {
      setErrorMsg('Token không hợp lệ. Vui lòng đăng nhập lại.');
      return;
    }

    try {
      setLoading(true);
      setErrorMsg(null);
      const payload = { registrationToken, role };
      const res = await authApi.completeOAuthRegistration(payload);
      if (res.data) {
        queryClient.clear();
        establish(res.data, true);
        router.replace(roleHome(res.data.user.role));
      }
    } catch (error: any) {
      setErrorMsg(error.response?.data?.message || 'Đã có lỗi xảy ra. Vui lòng thử lại sau.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="tm-oauth-role-page">
      <header className="tm-oauth-role-heading">
        <h1>Hoàn tất đăng ký</h1>
        <p>Bạn muốn tham gia với vai trò gì?</p>
      </header>

      <Alert
        className="tm-oauth-role-warning"
        title="Lưu ý quan trọng"
        description="Vai trò này không thể thay đổi sau khi bạn hoàn tất đăng ký."
        type="warning"
        showIcon
      />

      {errorMsg && (
        <Alert className="tm-oauth-role-error" title={errorMsg} type="error" showIcon role="alert" />
      )}

      <fieldset className="tm-oauth-role-options">
        <legend>Chọn vai trò</legend>
        <RadioCard
          className="tm-oauth-role-option"
          title="Học viên / Phụ huynh"
          description="Tìm kiếm gia sư và tham gia các khóa học"
          value="STUDENT"
          checked={role === 'STUDENT'}
          onChange={() => setRole('STUDENT')}
        />
        <RadioCard
          className="tm-oauth-role-option"
          title="Gia sư"
          description="Trở thành người giảng dạy và tạo thu nhập"
          value="TEACHER"
          checked={role === 'TEACHER'}
          onChange={() => setRole('TEACHER')}
        />
      </fieldset>

      <Button
        className="tm-oauth-role-submit"
        type="primary"
        size="large"
        block
        loading={loading}
        onClick={onSubmit}
      >
        Hoàn tất đăng ký
      </Button>
    </main>
  );
}

export default function OAuthRolePage() {
  return (
    <Suspense fallback={<main className="tm-oauth-role-page tm-oauth-role-loading" aria-busy="true">Đang tải...</main>}>
      <OAuthRoleContent />
    </Suspense>
  );
}
