'use client';

import { Layout, Row, Col, Typography, Space } from 'antd';
import Link from 'next/link';

const { Footer: AntFooter } = Layout;
const { Title, Text } = Typography;

export default function Footer() {
  return (
    <AntFooter style={{ backgroundColor: 'var(--color-primary-900)', color: 'var(--color-text-inverse)', padding: 'var(--space-16) var(--space-4)' }}>
      <div style={{ maxWidth: 'var(--size-container)', margin: '0 auto' }}>
        <Row gutter={[32, 32]}>
          <Col xs={24} md={8}>
            <Title level={3} style={{ color: 'var(--color-text-inverse)', margin: 0 }}>Edtech Tutor Marketplace</Title>
            <Text style={{ color: 'var(--color-text-inverse)', opacity: 0.8, display: 'block', marginTop: 'var(--space-4)' }}>
              Nền tảng kết nối gia sư và học sinh hàng đầu, mang đến trải nghiệm học tập 1-1 chất lượng cao.
            </Text>
          </Col>
          <Col xs={24} md={8}>
            <Title level={4} style={{ color: 'var(--color-text-inverse)' }}>Về chúng tôi</Title>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <Link href="/about" style={{ color: 'var(--color-text-inverse)', opacity: 0.8 }}>Giới thiệu</Link>
              <Link href="/terms" style={{ color: 'var(--color-text-inverse)', opacity: 0.8 }}>Điều khoản sử dụng</Link>
              <Link href="/privacy" style={{ color: 'var(--color-text-inverse)', opacity: 0.8 }}>Chính sách bảo mật</Link>
            </div>
          </Col>
          <Col xs={24} md={8}>
            <Title level={4} style={{ color: 'var(--color-text-inverse)' }}>Hỗ trợ</Title>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <Link href="/faq" style={{ color: 'var(--color-text-inverse)', opacity: 0.8 }}>Câu hỏi thường gặp</Link>
              <Link href="/contact" style={{ color: 'var(--color-text-inverse)', opacity: 0.8 }}>Liên hệ</Link>
            </div>
          </Col>
        </Row>
        <div style={{ borderTop: '1px solid rgba(255,255,255,0.1)', marginTop: 'var(--space-8)', paddingTop: 'var(--space-8)', textAlign: 'center' }}>
          <Text style={{ color: 'var(--color-text-inverse)', opacity: 0.6 }}>
            &copy; {new Date().getFullYear()} Edtech Tutor Marketplace. Bản quyền thuộc về nền tảng.
          </Text>
        </div>
      </div>
    </AntFooter>
  );
}
