'use client';

import React, { useEffect, useState } from 'react';
import { Card, Table, Typography, Space, Tag, Input, Tabs } from 'antd';
import { SearchOutlined, EyeOutlined } from '@ant-design/icons';
import { useRouter } from 'next/navigation';
import { DateTimeText } from '@/shared/components/data-display/DateTimeText';
import { StatusTag } from '@/shared/components/data-display/StatusTag';
import { learningApi } from '../api/learningApi';
import { TeacherAssignmentListItem } from '../types';

export const StudentAssignmentList: React.FC = () => {
  const router = useRouter();
  const [data, setData] = useState<TeacherAssignmentListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [activeTab, setActiveTab] = useState('to_do');
  
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });

  const fetchAssignments = async (page: number, size: number, tab: string) => {
    try {
      setLoading(true);
      // Map tab to status if backend supports it
      let statusParams = undefined;
      if (tab === 'to_do') statusParams = 'PUBLISHED';
      
      const res = await learningApi.getStudentAssignments(page, size, statusParams);
      if (res.data) {
        setData(res.data);
        if (res.meta) {
          setPagination(prev => ({ ...prev, total: res.meta!.totalElements }));
        }
      }
    } catch (e) {
      console.error('Failed to fetch assignments:', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAssignments(pagination.current - 1, pagination.pageSize, activeTab);
  }, [pagination.current, pagination.pageSize, activeTab]);

  const isOverdue = (dueAt: string) => {
    return new Date(dueAt).getTime() < new Date().getTime();
  };

  const columns = [
    {
      title: 'Tiêu đề',
      dataIndex: 'title',
      key: 'title',
      render: (text: string, record: TeacherAssignmentListItem) => (
        <span 
          className="font-medium text-primary-600 cursor-pointer hover:underline"
          onClick={() => router.push(`/student/assignments/${record.id}`)}
        >
          {text}
        </span>
      ),
    },
    {
      title: 'Hạn nộp',
      dataIndex: 'dueAt',
      key: 'dueAt',
      render: (dueAt: string, record: TeacherAssignmentListItem) => {
        const overdue = activeTab === 'to_do' && isOverdue(dueAt);
        return (
          <div className="flex flex-col">
            <DateTimeText value={dueAt} />
            {overdue && <span className="text-error-600 text-xs font-medium">Quá hạn</span>}
          </div>
        );
      }
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <StatusTag domain="Assignment" status={status} />
    },
    {
      title: 'Thao tác',
      key: 'action',
      render: (_: any, record: TeacherAssignmentListItem) => (
        <Space size="middle">
          <Typography.Link onClick={() => router.push(`/student/assignments/${record.id}`)}>
            <EyeOutlined /> Xem & Nộp
          </Typography.Link>
        </Space>
      ),
    },
  ];

  return (
    <Card className="shadow-sm rounded-xl">
      <div className="flex justify-between items-center mb-6">
        <Typography.Title level={4} className="m-0">Bài tập của tôi</Typography.Title>
        <Input
          placeholder="Tìm kiếm bài tập..."
          prefix={<SearchOutlined />}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{ width: 250 }}
        />
      </div>

      <Tabs 
        activeKey={activeTab} 
        onChange={(k) => {
          setActiveTab(k);
          setPagination(prev => ({ ...prev, current: 1 }));
        }}
        items={[
          { key: 'to_do', label: 'Chưa làm (Cần nộp)' },
          { key: 'submitted', label: 'Đã nộp (Chờ chấm)' },
          { key: 'graded', label: 'Đã chấm điểm' },
        ]} 
      />

      <Table
        columns={columns}
        dataSource={data.filter(item => item.title.toLowerCase().includes(search.toLowerCase()))}
        rowKey="id"
        loading={loading}
        pagination={{
          ...pagination,
          onChange: (page, pageSize) => setPagination(prev => ({ ...prev, current: page, pageSize })),
          showSizeChanger: true,
        }}
        rowClassName={(record) => (activeTab === 'to_do' && isOverdue(record.dueAt) ? 'bg-error-50/50' : '')}
      />
    </Card>
  );
};
