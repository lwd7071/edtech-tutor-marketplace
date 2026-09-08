'use client';

import React from 'react';
import { Card, Row, Col, Typography, Button, Space, Tooltip } from 'antd';
import {
  WalletOutlined,
  ClockCircleOutlined,
  LockOutlined,
  ArrowUpOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';
import { WalletView } from '../types';

interface WalletSummaryCardProps {
  wallet?: WalletView | null;
  loading?: boolean;
  onRequestPayout?: () => void;
}

export const WalletSummaryCard: React.FC<WalletSummaryCardProps> = ({
  wallet,
  loading = false,
  onRequestPayout,
}) => {
  const available = wallet?.availableBalanceVnd ?? 0;
  const pending = wallet?.pendingBalanceVnd ?? 0;
  const reserved = wallet?.reservedBalanceVnd ?? 0;

  const canPayout = available > 0;

  return (
    <div style={{ marginBottom: 24 }}>
      <Row gutter={[16, 16]}>
        {/* Bucket 1: Số dư khả dụng (Primary) */}
        <Col xs={24} sm={24} md={8}>
          <Card
            loading={loading}
            style={{
              borderRadius: 'var(--radius-lg, 12px)',
              border: '1px solid var(--color-primary-100, #CCFBF1)',
              background: 'var(--color-surface, #FFFFFF)',
              boxShadow: 'var(--shadow-sm)',
            }}
          >
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12, width: '100%' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography.Text type="secondary" style={{ fontSize: 13, fontWeight: 500 }}>
                  <WalletOutlined style={{ marginRight: 6, color: 'var(--color-primary-600, #0F766E)' }} />
                  Số dư khả dụng
                </Typography.Text>
                <Tooltip title="Số tiền có thể rút về tài khoản ngân hàng ngay bây giờ.">
                  <InfoCircleOutlined style={{ color: 'var(--color-text-tertiary, #8A837B)' }} />
                </Tooltip>
              </div>

              <div>
                <span
                  style={{
                    fontSize: 28,
                    fontWeight: 700,
                    fontVariantNumeric: 'tabular-nums',
                    color: 'var(--color-primary-600, #0F766E)',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {available.toLocaleString('vi-VN')}
                </span>
                <span
                  style={{
                    fontSize: 16,
                    fontWeight: 500,
                    color: 'var(--color-text-secondary, #57534E)',
                    marginLeft: 4,
                  }}
                >
                  ₫
                </span>
              </div>

              <Button
                type="primary"
                icon={<ArrowUpOutlined />}
                disabled={!canPayout}
                onClick={onRequestPayout}
                style={{ width: '100%', marginTop: 4, fontWeight: 600 }}
              >
                Yêu cầu rút tiền
              </Button>
            </div>
          </Card>
        </Col>

        {/* Bucket 2: Số dư chờ quyết toán (Pending) */}
        <Col xs={24} sm={12} md={8}>
          <Card
            loading={loading}
            style={{
              borderRadius: 'var(--radius-lg, 12px)',
              border: '1px solid var(--color-border, #E7E3DC)',
              background: 'var(--color-surface, #FFFFFF)',
            }}
          >
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12, width: '100%' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography.Text type="secondary" style={{ fontSize: 13, fontWeight: 500 }}>
                  <ClockCircleOutlined style={{ marginRight: 6, color: 'var(--color-warning-600, #B45309)' }} />
                  Chờ quyết toán
                </Typography.Text>
                <Tooltip title="Doanh thu từ các gói học đang dạy. Số tiền này sẽ tự động chuyển sang khả dụng sau khi từng buổi học được hoàn tất.">
                  <InfoCircleOutlined style={{ color: 'var(--color-text-tertiary, #8A837B)' }} />
                </Tooltip>
              </div>

              <div>
                <span
                  style={{
                    fontSize: 24,
                    fontWeight: 600,
                    fontVariantNumeric: 'tabular-nums',
                    color: 'var(--color-text-primary, #1C1917)',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {pending.toLocaleString('vi-VN')}
                </span>
                <span
                  style={{
                    fontSize: 15,
                    fontWeight: 500,
                    color: 'var(--color-text-secondary, #57534E)',
                    marginLeft: 4,
                  }}
                >
                  ₫
                </span>
              </div>

              <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block', minHeight: 32 }}>
                Sẽ giải ngân sau khi buổi học hoàn thành
              </Typography.Text>
            </div>
          </Card>
        </Col>

        {/* Bucket 3: Số dư đang rút (Reserved) */}
        <Col xs={24} sm={12} md={8}>
          <Card
            loading={loading}
            style={{
              borderRadius: 'var(--radius-lg, 12px)',
              border: '1px solid var(--color-border, #E7E3DC)',
              background: 'var(--color-surface, #FFFFFF)',
            }}
          >
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12, width: '100%' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography.Text type="secondary" style={{ fontSize: 13, fontWeight: 500 }}>
                  <LockOutlined style={{ marginRight: 6, color: 'var(--color-text-tertiary, #8A837B)' }} />
                  Đang xử lý rút
                </Typography.Text>
                <Tooltip title="Số tiền đang giữ trong các lệnh rút tiền đang chờ quản trị viên phê duyệt và chuyển khoản.">
                  <InfoCircleOutlined style={{ color: 'var(--color-text-tertiary, #8A837B)' }} />
                </Tooltip>
              </div>

              <div>
                <span
                  style={{
                    fontSize: 24,
                    fontWeight: 600,
                    fontVariantNumeric: 'tabular-nums',
                    color: 'var(--color-text-secondary, #57534E)',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {reserved.toLocaleString('vi-VN')}
                </span>
                <span
                  style={{
                    fontSize: 15,
                    fontWeight: 500,
                    color: 'var(--color-text-secondary, #57534E)',
                    marginLeft: 4,
                  }}
                >
                  ₫
                </span>
              </div>

              <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block', minHeight: 32 }}>
                Lệnh rút đang chờ duyệt & chuyển khoản
              </Typography.Text>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};
