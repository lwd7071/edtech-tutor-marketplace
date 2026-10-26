'use client';

import React, { useEffect, useState } from 'react';
import { Alert, Button, Card, Col, Row, Skeleton, Typography } from 'antd';
import { CheckCircleOutlined, StarOutlined, TrophyOutlined } from '@ant-design/icons';
import { rankingApi } from '../api/rankingApi';
import { TeacherStatsView } from '../types';

export const TeacherStatsOverview: React.FC = () => {
  const [stats, setStats] = useState<TeacherStatsView | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const fetchStats = async () => {
    setLoading(true);
    setError(false);
    try {
      const response = await rankingApi.getTeacherStats();
      setStats(response.data || null);
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchStats(); }, []);

  const metric = (title: string, value: React.ReactNode, detail: string, icon: React.ReactNode) => (
    <Card style={{ height: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, marginBottom: 16 }}>
        <Typography.Text type="secondary" strong>{title}</Typography.Text>
        <span style={{ width: 40, height: 40, borderRadius: '50%', display: 'grid', placeItems: 'center', background: 'var(--color-primary-50)', color: 'var(--color-primary-600)', fontSize: 18 }}>{icon}</span>
      </div>
      {loading ? <Skeleton.Input active size="large" /> : <><div style={{ fontSize: 30, fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>{value}</div><Typography.Text type="secondary">{detail}</Typography.Text></>}
    </Card>
  );

  return (
    <div className="tm-stack">
      <header className="tm-page-heading"><h1>Thống kê hoạt động</h1><p>Kết quả giảng dạy được tổng hợp từ các buổi học và đánh giá đã xác nhận.</p></header>
      {error ? <Alert type="error" showIcon title="Chưa tải được thống kê" action={<Button onClick={fetchStats}>Thử lại</Button>} /> : (
        <Row gutter={[20, 20]}>
          <Col xs={24} md={8}>{metric('Buổi học hoàn thành', stats?.completedSessionCount ?? 0, `Tỷ lệ hoàn thành: ${stats?.completionRate ?? 0}%`, <CheckCircleOutlined />)}</Col>
          <Col xs={24} md={8}>{metric('Đánh giá trung bình', stats?.averageRating ? Number(stats.averageRating).toFixed(1) : '0.0', `${stats?.reviewCount ?? 0} lượt đánh giá`, <StarOutlined />)}</Col>
          <Col xs={24} md={8}>{metric('Xếp hạng hiện tại', stats?.globalRank ? `#${stats.globalRank}` : '—', stats?.globalRank ? `Đang đứng thứ ${stats.globalRank}` : 'Chưa đủ dữ liệu xếp hạng', <TrophyOutlined />)}</Col>
        </Row>
      )}
    </div>
  );
};
