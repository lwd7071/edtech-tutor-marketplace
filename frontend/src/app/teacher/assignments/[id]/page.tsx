'use client';

import React, { useEffect, useState } from 'react';
import { Typography, Button, Space, Table, Tag, Spin } from 'antd';
import { ArrowLeftOutlined, EditOutlined } from '@ant-design/icons';
import { useRouter } from 'next/navigation';
import { DateTimeText } from '@/shared/components/data-display/DateTimeText';
import { FileViewer } from '@/shared/components/data-display/FileViewer';
import { learningApi } from '@/features/learning/api/learningApi';
import { TeacherAssignmentDetail, SubmissionListItem } from '@/features/learning/types';

export default function AssignmentDetailPage({ params }: { params: { id: string } }) {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [assignment, setAssignment] = useState<TeacherAssignmentDetail | null>(null);

  const fetchDetail = async () => {
    try {
      setLoading(true);
      const res = await learningApi.getTeacherAssignmentDetail(params.id);
      setAssignment(res.data);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDetail();
  }, [params.id]);

  const getSubmissionStatusTag = (status: string) => {
    switch (status) {
      case 'SUBMITTED': return <Tag color="warning">Chờ chấm</Tag>;
      case 'GRADED': return <Tag color="success">Đã chấm</Tag>;
      case 'DRAFT': return <Tag color="default">Chưa nộp</Tag>;
      default: return <Tag>{status}</Tag>;
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <Spin size="large" />
      </div>
    );
  }

  if (!assignment) {
    return <div>Không tìm thấy bài tập</div>;
  }

  const columns = [
    {
      title: 'Học sinh',
      dataIndex: 'studentName',
      key: 'studentName',
    },
    {
      title: 'Thời gian nộp',
      dataIndex: 'submittedAt',
      key: 'submittedAt',
      render: (date: string) => date ? <DateTimeText value={date} /> : '-',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => getSubmissionStatusTag(status),
    },
    {
      title: 'Điểm',
      dataIndex: 'score',
      key: 'score',
      render: (score: number | null) => score !== null ? <strong className="text-primary">{score}/10</strong> : '-',
    },
    {
      title: 'Hành động',
      key: 'action',
      render: (_: any, record: SubmissionListItem) => (
        <Button 
          type="primary" 
          size="small"
          disabled={record.status === 'DRAFT'}
          onClick={() => router.push(`/teacher/submissions/${record.id}`)}
        >
          {record.status === 'GRADED' ? 'Xem lại' : 'Chấm bài'}
        </Button>
      ),
    },
  ];

  // Extract attachments from contentBlocks (Mock logic for frontend)
  const mockFiles = [
    { id: '1', fileUrl: '#', originalName: 'De_bai_Toan.pdf', mimeType: 'application/pdf', fileSize: 1024000 } as any
  ];

  const textBlock = assignment.contentBlocks?.find(b => b.type === 'TEXT');

  return (
    <div className="flex flex-col gap-6">
      <Space className="mb-2">
        <Button 
          type="text" 
          icon={<ArrowLeftOutlined />} 
          onClick={() => router.back()}
        />
        <Typography.Title level={4} className="m-0">Chi tiết Bài tập</Typography.Title>
      </Space>

      <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
        <div className="flex justify-between items-start mb-6">
          <div>
            <Typography.Title level={3} className="m-0 mb-2">{assignment.title}</Typography.Title>
            <Space className="text-text-secondary">
              <span>Hạn nộp: <DateTimeText value={assignment.dueAt} /></span>
              <span>•</span>
              {assignment.status === 'PUBLISHED' ? <Tag color="blue">Đang giao</Tag> : <Tag>Bản nháp</Tag>}
            </Space>
          </div>
          <Button icon={<EditOutlined />}>Sửa bài tập</Button>
        </div>

        <div className="mb-6">
          <Typography.Title level={5}>Nội dung đề bài</Typography.Title>
          <div className="p-4 bg-neutral-50 rounded-lg border border-border">
            <p className="whitespace-pre-wrap">{textBlock?.content || 'Không có hướng dẫn văn bản.'}</p>
          </div>
        </div>

        <div>
          <Typography.Title level={5}>File đính kèm</Typography.Title>
          {/* Normally we map through contentBlocks to find IMAGE/FILE and resolve their Attachment details. We mock here. */}
          <FileViewer files={mockFiles} />
        </div>
      </div>

      <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
        <Typography.Title level={4} className="mb-6">Danh sách nộp bài</Typography.Title>
        <Table
          columns={columns}
          dataSource={assignment.submissions || [{
            id: 'mock-sub-1',
            studentId: '1',
            studentName: 'Nguyễn Văn A',
            status: 'SUBMITTED',
            submittedAt: new Date().toISOString(),
            score: null
          }]}
          rowKey="id"
          pagination={false}
        />
      </div>
    </div>
  );
}
