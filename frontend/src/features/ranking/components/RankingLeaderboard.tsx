'use client';

import React, { useEffect, useState } from 'react';
import { Table, Card, Typography, Select, Space, Avatar, Tag, Button } from 'antd';
import { TrophyOutlined, StarFilled } from '@ant-design/icons';
import { useRouter } from 'next/navigation';
import { rankingApi } from '../api/rankingApi';
import { TeacherRankingItem } from '../types';
import { getPublicSubjects, SubjectSummary } from '@/shared/api/public';

export const RankingLeaderboard: React.FC = () => {
  const router = useRouter();
  const [data, setData] = useState<TeacherRankingItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [subjects, setSubjects] = useState<SubjectSummary[]>([]);
  const [selectedSubject, setSelectedSubject] = useState<string | undefined>(undefined);
  
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });

  const fetchSubjects = async () => {
    try {
      const res = await getPublicSubjects();
      if (res.data) {
        setSubjects(res.data);
      }
    } catch (e) {
      console.error('Failed to fetch subjects', e);
    }
  };

  const fetchRanking = async (page: number, size: number, subjectId?: string) => {
    try {
      setLoading(true);
      const res = await rankingApi.getGlobalRanking(page, size, subjectId);
      if (res.data) {
        setData(res.data);
        if (res.meta) {
          setPagination(prev => ({ ...prev, total: res.meta!.totalElements }));
        }
      }
    } catch (e) {
      console.error('Failed to fetch ranking', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSubjects();
  }, []);

  useEffect(() => {
    fetchRanking(pagination.current - 1, pagination.pageSize, selectedSubject);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pagination.current, pagination.pageSize, selectedSubject]);

  const renderRank = (rank: number) => {
    if (rank === 1) return <TrophyOutlined style={{ fontSize: 24, color: '#F59E0B' }} />;
    if (rank === 2) return <TrophyOutlined style={{ fontSize: 24, color: '#9CA3AF' }} />;
    if (rank === 3) return <TrophyOutlined style={{ fontSize: 24, color: '#B45309' }} />; // Accent color for top 3
    return <span className="font-bold text-text-secondary text-lg">#{rank}</span>;
  };

  const columns = [
    {
      title: 'Hạng',
      key: 'rank',
      dataIndex: 'globalRank',
      width: 100,
      align: 'center' as const,
      render: (rank: number) => renderRank(rank),
    },
    {
      title: 'Giáo viên',
      key: 'teacher',
      render: (_: any, record: TeacherRankingItem) => (
        <div className="flex items-center gap-4 cursor-pointer" onClick={() => router.push(`/teachers/${record.teacherId}`)}>
          <Avatar src={record.avatarUrl} size={48} className="border border-border shrink-0">
            {record.fullName?.[0]}
          </Avatar>
          <div className="flex flex-col">
            <span className={`text-base font-semibold ${record.globalRank <= 3 ? 'text-accent' : 'text-text-primary'}`}>
              {record.fullName}
            </span>
            <span className="text-sm text-text-secondary line-clamp-1">{record.bioExcerpt}</span>
          </div>
        </div>
      ),
    },
    {
      title: 'Số buổi dạy',
      key: 'sessions',
      dataIndex: 'completedSessionCount',
      width: 150,
      align: 'center' as const,
      render: (count: number) => (
        <Tag color="green" className="text-sm px-3 py-1 m-0 tabular-nums font-medium">
          {count} buổi
        </Tag>
      ),
    },
    {
      title: 'Điểm đánh giá',
      key: 'rating',
      dataIndex: 'bayesianRating',
      width: 150,
      align: 'center' as const,
      render: (rating: number) => (
        <div className="flex items-center justify-center gap-1 text-orange-500 font-bold text-base tabular-nums">
          <StarFilled />
          <span>{Number(rating).toFixed(2)}</span>
        </div>
      ),
    },
  ];

  return (
    <div className="flex flex-col gap-6 max-w-6xl mx-auto px-4 py-8">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <Typography.Title level={2} className="m-0 text-primary-700">
            Bảng Xếp Hạng Giáo Viên
          </Typography.Title>
          <Typography.Text type="secondary" className="text-base">
            Vinh danh những giáo viên xuất sắc nhất nền tảng
          </Typography.Text>
        </div>
        
        <Select
          allowClear
          placeholder="Lọc theo môn học"
          value={selectedSubject}
          onChange={(val) => {
            setSelectedSubject(val);
            setPagination(prev => ({ ...prev, current: 1 })); // reset page
          }}
          style={{ width: 240 }}
          options={subjects.map(s => ({ value: s.id, label: s.name }))}
          size="large"
        />
      </div>

      <div className="hidden md:block">
        <Table
          dataSource={data}
          columns={columns}
          rowKey="teacherId"
          loading={loading}
          pagination={{
            ...pagination,
            onChange: (page, pageSize) => setPagination({ ...pagination, current: page, pageSize }),
            showSizeChanger: true,
          }}
          className="shadow-sm border border-border rounded-xl overflow-hidden"
          rowClassName={(record) => record.globalRank <= 3 ? 'bg-orange-50/30' : ''}
        />
      </div>

      {/* Mobile Card View */}
      <div className="md:hidden flex flex-col gap-4">
        {data.map((record) => (
          <Card 
            key={record.teacherId} 
            className={`shadow-sm rounded-xl border ${record.globalRank <= 3 ? 'border-orange-200 bg-orange-50/30' : 'border-border'}`}
            styles={{ body: { padding: '16px' } }}
            onClick={() => router.push(`/teachers/${record.teacherId}`)}
          >
            <div className="flex items-start gap-4">
              <div className="flex flex-col items-center gap-2">
                <Avatar src={record.avatarUrl} size={56} className="border border-border shrink-0">
                  {record.fullName?.[0]}
                </Avatar>
                <div className="w-8 h-8 rounded-full bg-white shadow-sm border border-border flex items-center justify-center -mt-6 z-10 relative">
                  {renderRank(record.globalRank)}
                </div>
              </div>
              <div className="flex-1 flex flex-col gap-2">
                <span className={`text-base font-semibold ${record.globalRank <= 3 ? 'text-accent' : 'text-text-primary'}`}>
                  {record.fullName}
                </span>
                <span className="text-sm text-text-secondary line-clamp-2">{record.bioExcerpt}</span>
                
                <div className="flex items-center gap-4 mt-1">
                  <div className="flex items-center gap-1 text-orange-500 font-bold tabular-nums">
                    <StarFilled />
                    <span>{Number(record.bayesianRating).toFixed(2)}</span>
                  </div>
                  <Tag color="green" className="m-0 border-none bg-green-50 text-green-700 font-medium">
                    {record.completedSessionCount} buổi
                  </Tag>
                </div>
              </div>
            </div>
          </Card>
        ))}
        {loading && (
          <div className="text-center p-4">Loading...</div>
        )}
        {!loading && data.length === 0 && (
          <div className="text-center p-8 text-text-secondary border border-border border-dashed rounded-xl">
            Chưa có dữ liệu xếp hạng
          </div>
        )}
        {!loading && data.length > 0 && (
          <div className="flex justify-center mt-4">
            <Space>
              <Button 
                disabled={pagination.current === 1} 
                onClick={() => setPagination(prev => ({ ...prev, current: prev.current - 1 }))}
              >
                Trước
              </Button>
              <span className="text-text-secondary">Trang {pagination.current}</span>
              <Button 
                disabled={data.length < pagination.pageSize}
                onClick={() => setPagination(prev => ({ ...prev, current: prev.current + 1 }))}
              >
                Tiếp
              </Button>
            </Space>
          </div>
        )}
      </div>
    </div>
  );
};
