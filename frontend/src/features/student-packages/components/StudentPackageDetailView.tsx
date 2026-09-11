'use client';

import React from 'react';
import { Card, Tag, Typography, Button, Space, Alert, Progress, Divider, Row, Col } from 'antd';
import { CalendarOutlined, ClockCircleOutlined, UserOutlined, BookOutlined } from '@ant-design/icons';
import { StudentPackageDetail } from '../types';
import { SessionCounter } from './SessionCounter';
import { getPackageStatusConfig, formatVnd, formatDate } from './StudentPackageCard';
import { BackLink } from '@/shared/components/navigation/NavigationLinks';
import {MessageTeacherButton} from '@/features/chat/components/MessageTeacherButton';

interface StudentPackageDetailViewProps {
  packageData: StudentPackageDetail;
  onExtensionRequest?: () => void;
  onRefundRequest?: () => void;
}

export const StudentPackageDetailView: React.FC<StudentPackageDetailViewProps> = ({
  packageData,
  onExtensionRequest,
  onRefundRequest,
}) => {
  const statusConfig = getPackageStatusConfig(packageData.status);
  const completedPercent = Math.round(
    ((packageData.completedSessions + packageData.refundedSessions) /
      (packageData.totalSessions || 1)) *
      100
  );

  return (
    <div style={{ maxWidth: 880, margin: '0 auto' }}>
      {/* Header điều hướng & Tiêu đề */}
      <div style={{ marginBottom: 20 }}>
        <BackLink href="/student/packages">Danh sách gói học</BackLink>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
          <div>
            <Typography.Title level={2} style={{ margin: 0 }}>
              {packageData.packageName}
            </Typography.Title>
            <Typography.Text type="secondary">
              Mã gói: #{packageData.id.slice(0, 8)} · Môn: {packageData.subject.name}
            </Typography.Text>
          </div>
          <Tag color={statusConfig.color} style={{ fontSize: 14, padding: '4px 12px', fontWeight: 600 }}>
            {statusConfig.label}
          </Tag>
          <MessageTeacherButton teacherId={packageData.teacher.id} />
        </div>
      </div>

      {/* Cảnh báo trạng thái nghiệp vụ theo SPEC-FE:3.4 */}
      {packageData.status === 'LOCKED_EXPIRED' && (
        <Alert
          type="error"
          showIcon
          title="Gói học đã hết hạn"
          description={
            <div style={{ width: '100%', marginTop: 8, display: 'flex', flexDirection: 'column', gap: 8 }}>
              <div>
                Gói học này đã hết thời hạn sử dụng. Bạn không thể đặt thêm lịch học mới, tuy nhiên các buổi đã lên lịch trước đó vẫn diễn ra bình thường.
              </div>
              <Space>
                <Button type="primary" size="small" onClick={onExtensionRequest}>
                  Yêu cầu gia hạn
                </Button>
                {packageData.remainingSessions > 0 && <Button size="small" onClick={onRefundRequest}>
                  Yêu cầu hoàn tiền
                </Button>}
              </Space>
            </div>
          }
          style={{ marginBottom: 20 }}
        />
      )}

      {packageData.status === 'REFUND_PENDING' && (
        <Alert
          type="warning"
          showIcon
          title="Đang xử lý hoàn tiền"
          description="Gói học đang trong tiến trình xử lý hoàn tiền từ ban quản trị. Các thao tác đặt lịch học mới tạm thời bị khóa."
          style={{ marginBottom: 20 }}
        />
      )}

      {packageData.status === 'ACTIVE' && packageData.remainingSessions > 0 && (
        <div style={{display:'flex',justifyContent:'flex-end',marginBottom:20}}>
          <Button onClick={onRefundRequest}>Yêu cầu hoàn tiền phần chưa học</Button>
        </div>
      )}

      {/* Card thống kê buổi học & tiến độ */}
      <Card
        title="Tiến độ học tập & Buổi học"
        style={{ marginBottom: 20, borderRadius: 'var(--radius-lg, 12px)' }}
        styles={{ body: { padding: 'var(--space-6, 24px)' } }}
      >
        <div style={{ marginBottom: 20 }}>
          <SessionCounter
            remainingSessions={packageData.remainingSessions}
            reservedSessions={packageData.reservedSessions}
            completedSessions={packageData.completedSessions}
            refundedSessions={packageData.refundedSessions}
            size="large"
          />
        </div>

        <div style={{ marginTop: 24 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
            <span style={{ fontSize: 13, color: 'var(--color-text-secondary, #57534E)' }}>
              Tiến độ hoàn thành gói học ({packageData.completedSessions}/{packageData.totalSessions} buổi)
            </span>
            <span style={{ fontSize: 13, fontWeight: 600 }}>{completedPercent}%</span>
          </div>
          <Progress percent={completedPercent} showInfo={false} strokeColor="var(--color-primary-600, #0F766E)" />
        </div>
      </Card>

      {/* Thông tin chi tiết gói học & Giáo viên */}
      <Card
        title="Thông tin chi tiết gói học"
        style={{ marginBottom: 20, borderRadius: 'var(--radius-lg, 12px)' }}
        styles={{ body: { padding: 'var(--space-6, 24px)' } }}
      >
        <Row gutter={[24, 16]}>
          <Col xs={24} sm={12}>
            <Space align="start">
              <UserOutlined style={{ fontSize: 18, color: 'var(--color-primary-600, #0F766E)', marginTop: 2 }} />
              <div>
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>Gia sư phụ trách</Typography.Text>
                <div><strong>{packageData.teacher.fullName}</strong></div>
              </div>
            </Space>
          </Col>

          <Col xs={24} sm={12}>
            <Space align="start">
              <BookOutlined style={{ fontSize: 18, color: 'var(--color-primary-600, #0F766E)', marginTop: 2 }} />
              <div>
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>Môn học</Typography.Text>
                <div><strong>{packageData.subject.name}</strong></div>
              </div>
            </Space>
          </Col>

          <Col xs={24} sm={12}>
            <Space align="start">
              <CalendarOutlined style={{ fontSize: 18, color: 'var(--color-primary-600, #0F766E)', marginTop: 2 }} />
              <div>
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>Thời gian hiệu lực</Typography.Text>
                <div>{formatDate(packageData.startsAt)} — {formatDate(packageData.expiresAt)}</div>
              </div>
            </Space>
          </Col>

          <Col xs={24} sm={12}>
            <Space align="start">
              <ClockCircleOutlined style={{ fontSize: 18, color: 'var(--color-primary-600, #0F766E)', marginTop: 2 }} />
              <div>
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>Giá trị gói học</Typography.Text>
                <div style={{ color: 'var(--color-primary-600, #0F766E)', fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>
                  {formatVnd(packageData.purchasePriceVnd)}
                </div>
              </div>
            </Space>
          </Col>
        </Row>

        {packageData.description && (
          <>
            <Divider style={{ margin: '16px 0' }} />
            <div>
              <Typography.Text strong style={{ display: 'block', marginBottom: 6 }}>
                Mô tả nội dung đào tạo
              </Typography.Text>
              <Typography.Paragraph type="secondary" style={{ margin: 0 }}>
                {packageData.description}
              </Typography.Paragraph>
            </div>
          </>
        )}
      </Card>
    </div>
  );
};
