'use client';

import React from 'react';
import { Row, Col, Typography, Card } from 'antd';
import Image from 'next/image';

const { Title, Paragraph } = Typography;

export default function TrustSection() {
  const steps = [
    {
      title: 'Tìm kiếm dễ dàng',
      description: 'Dễ dàng tìm thấy gia sư phù hợp với môn học, trình độ và lịch học của bạn.',
      image: '/images/trust_step_1.png',
    },
    {
      title: 'Kết nối nhanh chóng',
      description: 'Trò chuyện và lên lịch trực tiếp với gia sư mà không qua trung gian.',
      image: '/images/trust_step_2.png',
    },
    {
      title: 'Học tập hiệu quả',
      description: 'Trải nghiệm môi trường học tập tương tác, nâng cao kiến thức một cách nhanh chóng.',
      image: '/images/trust_step_3.png',
    },
  ];

  return (
    <section style={{ padding: 'var(--space-16, 64px) 0', backgroundColor: 'var(--color-surface, #FFFFFF)' }}>
      <div style={{ textAlign: 'center', marginBottom: 'var(--space-12, 48px)' }}>
        <Title level={2}>Quy trình 3 bước đơn giản</Title>
        <Paragraph type="secondary" style={{ fontSize: '16px' }}>
          Cách hoạt động của nền tảng giúp bạn học tập tốt hơn
        </Paragraph>
      </div>
      <Row gutter={[32, 32]} justify="center">
        {steps.map((step, index) => (
          <Col xs={24} sm={24} md={8} key={index}>
            <Card variant="borderless" style={{ textAlign: 'center', backgroundColor: 'transparent' }}>
              <div style={{ position: 'relative', width: '100%', height: '240px', marginBottom: 'var(--space-6, 24px)' }}>
                <Image
                  src={step.image}
                  alt={step.title}
                  fill
                  style={{ objectFit: 'contain' }}
                  sizes="(max-width: 768px) 100vw, 33vw"
                />
              </div>
              <Title level={4}>{step.title}</Title>
              <Paragraph type="secondary">{step.description}</Paragraph>
            </Card>
          </Col>
        ))}
      </Row>
    </section>
  );
}
