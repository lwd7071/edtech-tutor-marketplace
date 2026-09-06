'use client';

import React, { useEffect, useState } from 'react';
import { Table, Button, Input, Space, Typography, Tag, Tooltip } from 'antd';
import { PlusOutlined, SearchOutlined, EditOutlined, EyeOutlined } from '@ant-design/icons';
import { useRouter } from 'next/navigation';
import { DateTimeText } from '@/shared/components/data-display/DateTimeText';
import { learningApi } from '@/features/learning/api/learningApi';
import { TeacherAssignmentListItem } from '@/features/learning/types';

export default function TeacherAssignmentsPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<TeacherAssignmentListItem[]>([]);
  const [searchText, setSearchText] = useState('');

  const fetchAssignments = async () => {
    try {
      setLoading(true);
      const res = await learningApi.getTeacherAssignments();
      setData(res.data || []);
    } catch (error) {
      console.error('Failed to fetch assignments:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAssignments();
  }, []);
  const getStatusTag = (status: string) => {
    switch (status) {
      case 'PUBLISHED': return <Tag color="blue">Đang giao</Tag>;
      case 'DRAFT': return <Tag color="default">Bản nháp</Tag>;
      case 'CLOSED': return <Tag color="red">Đã đóng</Tag>;
      default: return <Tag>{status}</Tag>;
    }
  };

  const filteredData = data.filter(item => 
    item.title.toLowerCase().includes(searchText.toLowerCase()) ||
    item.studentName.toLowerCase().includes(searchText.toLowerCase())
  );

  const columns = [
    {
      title: 'Tiêu đề',
      dataIndex: 'title',
      key: 'title',
      render: (text: string, record: TeacherAssignmentListItem) => (
        <a 
          className="font-medium text-primary cursor-pointer hover:underline"
          onClick={() => router.push(`/teacher/assignments/${record.id}`)}
        >
          {text}
        </a>
      ),
    },
    {
      title: 'Học sinh',
      dataIndex: 'studentName',
      key: 'studentName',
    },
    {
      title: 'Hạn nộp',
      dataIndex: 'dueAt',
      key: 'dueAt',
      render: (dueAt: string) => <DateTimeText value={dueAt} />,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => getStatusTag(status),
    },
    {
      title: 'Tiến độ nộp',
      key: 'progress',
      render: (_: any, record: TeacherAssignmentListItem) => (
        <span>{record.submittedCount} / {record.totalStudents}</span>
      ),
    },
    {
      title: 'Hành động',
      key: 'action',
      render: (_: any, record: TeacherAssignmentListItem) => (
        <Space size="small">
          <Tooltip title="Chi tiết & Chấm bài">
            <Button 
              type="text" 
              icon={<EyeOutlined />} 
              onClick={() => router.push(`/teacher/assignments/${record.id}`)}
            />
          </Tooltip>
        </Space>
      ),
    },
  ];

  return (
    <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
      <div className="flex justify-between items-center mb-6">
        <Typography.Title level={4} className="m-0">Quản lý bài tập</Typography.Title>
        <Button 
          type="primary" 
          icon={<PlusOutlined />}
          onClick={() => router.push('/teacher/assignments/new')}
        >
          Tạo bài tập
        </Button>
      </div>

      <div className="mb-4 w-full md:w-1/3">
        <Input
          placeholder="Tìm kiếm bài tập, học sinh..."
          prefix={<SearchOutlined className="text-text-secondary" />}
          value={searchText}
          onChange={e => setSearchText(e.target.value)}
          allowClear
        />
      </div>

      <Table
        columns={columns}
        dataSource={filteredData}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 10 }}
        locale={{
          emptyText: loading ? 'Đang tải...' : 'Chưa có bài tập nào'
        }}
      />
    </div>
  );
}
