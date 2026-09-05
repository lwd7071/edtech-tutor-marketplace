'use client';

import React from 'react';
import { Typography, Button } from 'antd';
import Link from 'next/link';

const { Title, Paragraph } = Typography;

export default function CTASection() {
  // In a real app we'd use useAuthStore here, but for UI layout we just assume guest for now
  // Or handle it from props/page level
  return (
    <section 
      style={{ 
        padding: 'var(--space-20, 80px) 24px', 
        textAlign: 'center',
        backgroundImage: 'url(/images/cta_background.png)',
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        borderRadius: 'var(--radius-xl, 24px)',
        margin: 'var(--space-16, 64px) 0',
        color: '#FFFFFF'
      }}
    >
      <Title level={2} style={{ color: '#FFFFFF', marginBottom: 'var(--space-4, 16px)' }}>
        Bắt đầu hành trình học tập cùng chúng tôi
      </Title>
      <Paragraph style={{ color: 'rgba(255, 255, 255, 0.8)', fontSize: '18px', maxWidth: '600px', margin: '0 auto var(--space-8, 32px)' }}>
        Hàng ngàn giáo viên xuất sắc đang chờ đợi để giúp bạn chinh phục mọi mục tiêu học tập.
      </Paragraph>
      <Link href="/auth/register" passHref>
        <Button type="primary" size="large" style={{ height: '48px', padding: '0 32px', fontSize: '16px' }}>
          Đăng ký ngay
        </Button>
      </Link>
    </section>
  );
}
