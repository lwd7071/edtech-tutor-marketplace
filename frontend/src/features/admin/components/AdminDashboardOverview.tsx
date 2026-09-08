'use client';

import React from 'react';
import { Row, Col, Card, Typography, Space } from 'antd';
import {
  DollarOutlined,
  PercentageOutlined,
  TeamOutlined,
  UserOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  AlertOutlined,
} from '@ant-design/icons';
import { AdminDashboardView } from '../types';

interface AdminDashboardOverviewProps {
  stats?: AdminDashboardView | null;
  loading?: boolean;
}

export const AdminDashboardOverview: React.FC<AdminDashboardOverviewProps> = ({
  stats,
  loading = false,
}) => {
  return (
    <div style={{ marginBottom: 24 }}>
      {/* Hàng 1: Thống kê Tài chính Sàn */}
      <Typography.Title level={4} style={{ marginBottom: 16 }}>
        Tổng quan Tài chính & Hàng đợi
      </Typography.Title>
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} md={6}>
          <Card loading={loading} style={{ borderRadius: 'var(--radius-lg, 12px)' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4, width: '100%' }}>
              <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                <DollarOutlined style={{ marginRight: 6, color: 'var(--color-primary-600, #0F766E)' }} />
                Tổng GMV Nền tảng
              </Typography.Text>
              <div style={{ fontSize: 24, fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>
                {(stats?.totalGmvVnd ?? 0).toLocaleString('vi-VN')} ₫
              </div>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                Tổng giá trị các gói học đã thanh toán
              </Typography.Text>
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card loading={loading} style={{ borderRadius: 'var(--radius-lg, 12px)' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4, width: '100%' }}>
              <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                <PercentageOutlined style={{ marginRight: 6, color: 'var(--color-success-600, #15803D)' }} />
                Doanh thu Hoa hồng
              </Typography.Text>
              <div
                style={{
                  fontSize: 24,
                  fontWeight: 700,
                  fontVariantNumeric: 'tabular-nums',
                  color: 'var(--color-success-600, #15803D)',
                }}
              >
                {(stats?.totalCommissionVnd ?? 0).toLocaleString('vi-VN')} ₫
              </div>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                Hoa hồng từ các buổi học hoàn thành
              </Typography.Text>
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card loading={loading} style={{ borderRadius: 'var(--radius-lg, 12px)' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4, width: '100%' }}>
              <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                <ClockCircleOutlined style={{ marginRight: 6, color: 'var(--color-warning-600, #B45309)' }} />
                Yêu cầu Rút tiền chờ duyệt
              </Typography.Text>
              <div
                style={{
                  fontSize: 24,
                  fontWeight: 700,
                  fontVariantNumeric: 'tabular-nums',
                  color: 'var(--color-warning-600, #B45309)',
                }}
              >
                {stats?.pendingPayoutsCount ?? 0} lệnh
              </div>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                Tổng tiền: {(stats?.pendingPayoutsAmountVnd ?? 0).toLocaleString('vi-VN')} ₫
              </Typography.Text>
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card loading={loading} style={{ borderRadius: 'var(--radius-lg, 12px)' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4, width: '100%' }}>
              <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                <AlertOutlined style={{ marginRight: 6, color: 'var(--color-error-600, #B91C1C)' }} />
                Yêu cầu Hoàn tiền chờ xử lý
              </Typography.Text>
              <div
                style={{
                  fontSize: 24,
                  fontWeight: 700,
                  fontVariantNumeric: 'tabular-nums',
                  color: (stats?.pendingRefundsCount ?? 0) > 0 ? 'var(--color-error-600, #B91C1C)' : undefined,
                }}
              >
                {stats?.pendingRefundsCount ?? 0} yêu cầu
              </div>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                Yêu cầu từ học viên cần thẩm định
              </Typography.Text>
            </div>
          </Card>
        </Col>
      </Row>

      {/* Hàng 2: Thống kê Người dùng & Vận hành Buổi học */}
      <Typography.Title level={4} style={{ marginBottom: 16 }}>
        Vận hành Người dùng & Lịch học
      </Typography.Title>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={6}>
          <Card loading={loading} style={{ borderRadius: 'var(--radius-lg, 12px)' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                <TeamOutlined style={{ marginRight: 6 }} />
                Gia sư trên sàn
              </Typography.Text>
              <div style={{ fontSize: 22, fontWeight: 600 }}>{stats?.totalTeachers ?? 0}</div>
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card loading={loading} style={{ borderRadius: 'var(--radius-lg, 12px)' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                <UserOutlined style={{ marginRight: 6 }} />
                Học viên trên sàn
              </Typography.Text>
              <div style={{ fontSize: 22, fontWeight: 600 }}>{stats?.totalStudents ?? 0}</div>
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card loading={loading} style={{ borderRadius: 'var(--radius-lg, 12px)' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                <CalendarOutlined style={{ marginRight: 6 }} />
                Tổng số Buổi học
              </Typography.Text>
              <div style={{ fontSize: 22, fontWeight: 600 }}>{stats?.totalBookings ?? 0}</div>
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <Card loading={loading} style={{ borderRadius: 'var(--radius-lg, 12px)' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                <CheckCircleOutlined style={{ marginRight: 6, color: 'var(--color-success-600, #15803D)' }} />
                Đã hoàn thành
              </Typography.Text>
              <div
                style={{
                  fontSize: 22,
                  fontWeight: 600,
                  color: 'var(--color-success-600, #15803D)',
                }}
              >
                {stats?.completedBookings ?? 0} buổi
              </div>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};
