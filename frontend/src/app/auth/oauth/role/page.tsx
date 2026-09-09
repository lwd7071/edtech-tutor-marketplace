'use client';

import React, { useState, Suspense } from 'react';
import { Button, Typography, Alert, Space } from 'antd';
import { useRouter, useSearchParams } from 'next/navigation';
import { authApi } from '@/shared/api/auth';
import { useAuthStore } from '@/features/auth';
import RadioCard from '@/shared/components/ui/RadioCard';
import { roleHome } from '@/shared/lib/navigation';

const { Title, Text } = Typography;

function OAuthRoleContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const registrationToken = searchParams.get('registrationToken');
  const establish = useAuthStore((state) => state.establish);

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
        establish(res.data, true);
        router.push(roleHome(res.data.user.role));
      }
    } catch (error: any) {
      setErrorMsg(error.response?.data?.message || 'Đã có lỗi xảy ra. Vui lòng thử lại sau.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div style={{ textAlign: 'center', marginBottom: 'var(--space-6)' }}>
        <Title level={3} style={{ marginBottom: 'var(--space-2)' }}>Hoàn tất đăng ký</Title>
        <Text style={{ color: 'var(--color-text-secondary)' }}>
          Bạn muốn tham gia với vai trò gì?
        </Text>
      </div>

      <Alert
        title="Lưu ý quan trọng"
        description="Vai trò này không thể thay đổi sau khi bạn hoàn tất đăng ký."
        type="warning"
        showIcon
        style={{ marginBottom: 'var(--space-6)', textAlign: 'left' }}
      />

      {errorMsg && (
        <Alert title={errorMsg} type="error" showIcon style={{ marginBottom: 'var(--space-4)' }} />
      )}

      <Space orientation="vertical" style={{ width: '100%', marginBottom: 'var(--space-8)' }} size="middle">
        <RadioCard
          title="Học viên / Phụ huynh"
          description="Tìm kiếm gia sư và tham gia các khóa học"
          value="STUDENT"
          checked={role === 'STUDENT'}
          onChange={() => setRole('STUDENT')}
        />
        <RadioCard
          title="Gia sư"
          description="Trở thành người giảng dạy và tạo thu nhập"
          value="TEACHER"
          checked={role === 'TEACHER'}
          onChange={() => setRole('TEACHER')}
        />
      </Space>

      <Button
        type="primary"
        size="large"
        block
        loading={loading}
        onClick={onSubmit}
      >
        Hoàn tất đăng ký
      </Button>
    </div>
  );
}

export default function OAuthRolePage() {
  return (
    <Suspense fallback={<div>Đang tải...</div>}>
      <OAuthRoleContent />
    </Suspense>
  );
}
