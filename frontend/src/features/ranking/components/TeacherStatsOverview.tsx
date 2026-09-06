'use client';

import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Typography, Select, Skeleton, Space } from 'antd';
import { TrophyOutlined, StarOutlined, CheckCircleOutlined, DollarOutlined } from '@ant-design/icons';
import { rankingApi } from '../api/rankingApi';
import { TeacherStatsView } from '../types';

export const TeacherStatsOverview: React.FC = () => {
  const [stats, setStats] = useState<TeacherStatsView | null>(null);
  const [loading, setLoading] = useState(true);
  const [period, setPeriod] = useState('month');

  const fetchStats = async () => {
    try {
      setLoading(true);
      const res = await rankingApi.getTeacherStats();
      if (res.data) {
        setStats(res.data);
      }
    } catch (e) {
      console.error('Failed to fetch stats:', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStats();
  }, [period]);

  const renderCard = (title: string, value: React.ReactNode, icon: React.ReactNode, subtext?: string, valueColor: string = 'text-text-primary') => (
    <Card className="h-full shadow-sm rounded-xl" bodyStyle={{ padding: '24px' }}>
      <div className="flex justify-between items-start mb-4">
        <Typography.Text type="secondary" className="font-medium">{title}</Typography.Text>
        <div className="w-10 h-10 rounded-full bg-neutral-50 flex items-center justify-center text-lg text-primary-600">
          {icon}
        </div>
      </div>
      {loading ? (
        <Skeleton.Input active size="large" />
      ) : (
        <div>
          <div className={`text-3xl font-bold tabular-nums ${valueColor}`}>
            {value}
          </div>
          {subtext && (
            <div className="text-sm text-text-secondary mt-1">{subtext}</div>
          )}
        </div>
      )}
    </Card>
  );

  return (
    <div className="flex flex-col gap-6">
      <div className="flex justify-between items-center">
        <Typography.Title level={4} className="m-0">Thống kê hoạt động</Typography.Title>
        <Select
          value={period}
          onChange={setPeriod}
          style={{ width: 140 }}
          options={[
            { value: 'month', label: 'Tháng này' },
            { value: 'last_month', label: 'Tháng trước' },
            { value: 'year', label: 'Năm nay' },
            { value: 'all_time', label: 'Tất cả thời gian' },
          ]}
        />
      </div>

      <Row gutter={[24, 24]}>
        <Col xs={24} sm={12} lg={6}>
          {/* We do not have revenue from API right now, so we leave it empty or 0 if stats is not providing it */}
          {renderCard(
            'Doanh thu', 
            <Space align="baseline">
              <span>{(0).toLocaleString('vi-VN')}</span>
              <span className="text-base font-medium">₫</span>
            </Space>, 
            <DollarOutlined />, 
            'Chưa khả dụng',
            'text-primary-600'
          )}
        </Col>
        <Col xs={24} sm={12} lg={6}>
          {renderCard(
            'Buổi học hoàn thành', 
            stats?.completedSessionCount ?? 0, 
            <CheckCircleOutlined />, 
            stats ? `Tỷ lệ hoàn thành: ${stats.completionRate ?? 0}%` : ''
          )}
        </Col>
        <Col xs={24} sm={12} lg={6}>
          {renderCard(
            'Đánh giá trung bình', 
            stats?.averageRating ? Number(stats.averageRating).toFixed(1) : '0.0', 
            <StarOutlined />, 
            `${stats?.reviewCount ?? 0} lượt đánh giá`,
            'text-orange-500'
          )}
        </Col>
        <Col xs={24} sm={12} lg={6}>
          {renderCard(
            'Xếp hạng hiện tại', 
            stats?.globalRank ? `#${stats.globalRank}` : '-', 
            <TrophyOutlined />, 
            stats?.globalRank ? `Top ${stats.globalRank}` : 'Chưa xếp hạng',
            'text-accent'
          )}
        </Col>
      </Row>

      {/* Charts Placeholder */}
      <Card className="shadow-sm rounded-xl mt-4" title="Biểu đồ hoạt động">
        <Row gutter={[32, 32]}>
          <Col xs={24} lg={12}>
            <div className="flex flex-col gap-2">
              <Typography.Text type="secondary" strong>Doanh thu (Placeholder)</Typography.Text>
              <div className="h-[250px] bg-neutral-50 border border-dashed border-border rounded-lg flex items-center justify-center text-text-secondary">
                [Biểu đồ Doanh thu]
              </div>
            </div>
          </Col>
          <Col xs={24} lg={12}>
            <div className="flex flex-col gap-2">
              <Typography.Text type="secondary" strong>Số buổi học (Placeholder)</Typography.Text>
              <div className="h-[250px] bg-neutral-50 border border-dashed border-border rounded-lg flex items-center justify-center text-text-secondary">
                [Biểu đồ Số buổi học]
              </div>
            </div>
          </Col>
        </Row>
      </Card>
    </div>
  );
};
