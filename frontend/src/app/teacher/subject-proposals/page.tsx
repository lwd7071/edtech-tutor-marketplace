'use client';

import React, { useEffect, useState } from 'react';
import { Typography, Spin, message, Alert, Card, Form, Input, Button, Table, Tag, Select } from 'antd';
import { TeacherSubjectProposal, teacherApi } from '@/shared/api/teacher';

const { Title } = Typography;
const EDUCATION_LABELS: Record<TeacherSubjectProposal['educationLevel'], string> = {
  ELEMENTARY: 'Tiểu học', MIDDLE_SCHOOL: 'THCS', HIGH_SCHOOL: 'THPT', UNIVERSITY: 'Đại học', OTHER: 'Khác',
};

export default function SubjectProposalsPage() {
  const [proposals, setProposals] = useState<TeacherSubjectProposal[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();

  const fetchProposals = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await teacherApi.getSubjectProposals();
      setProposals(data);
    } catch (err) {
      setError('Không thể tải danh sách đề xuất.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProposals();
  }, []);

  const onFinish = async (values: { proposedName: string; educationLevel: TeacherSubjectProposal['educationLevel']; description?: string }) => {
    setSubmitting(true);
    try {
      const newProp = await teacherApi.createSubjectProposal(values);
      setProposals(prev => [newProp, ...prev]);
      message.success('Gửi đề xuất thành công!');
      form.resetFields();
    } catch (err) {
      message.error('Gửi đề xuất thất bại.');
    } finally {
      setSubmitting(false);
    }
  };

  const columns = [
    {
      title: 'Tên môn học',
      dataIndex: 'proposedName',
      key: 'proposedName',
    },
    {
      title: 'Cấp học',
      dataIndex: 'educationLevel',
      key: 'educationLevel',
      render: (level: TeacherSubjectProposal['educationLevel']) => EDUCATION_LABELS[level] || level,
    },
    {
      title: 'Mô tả',
      dataIndex: 'description',
      key: 'description',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        let color = 'default';
        let text = status;
        if (status === 'PENDING') { color = 'warning'; text = 'Đang chờ'; }
        if (status === 'APPROVED') { color = 'success'; text = 'Chấp thuận'; }
        if (status === 'REJECTED') { color = 'error'; text = 'Từ chối'; }
        return <Tag color={color}>{text}</Tag>;
      }
    },
    {
      title: 'Phản hồi',
      dataIndex: 'reviewNote',
      key: 'reviewNote',
      render: (note?: string) => note || '—',
    },
  ];

  if (loading) {
    return <div style={{ textAlign: 'center', padding: '40px' }}><Spin size="large" description="Đang tải danh sách đề xuất..." /></div>;
  }

  return (
    <div style={{ maxWidth: 880, margin: '0 auto' }}>
      <Title level={2}>Đề xuất Môn học mới</Title>
      
      {error && <Alert type="error" title={error} action={<Button onClick={fetchProposals}>Thử lại</Button>} style={{ marginBottom: 24 }} />}
      
      <Card title="Gửi đề xuất mới" style={{ marginBottom: 24 }}>
        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Form.Item
            label="Tên môn học"
            name="proposedName"
            rules={[{ required: true, message: 'Vui lòng nhập tên môn học' }]}
          >
            <Input placeholder="Tên môn học" />
          </Form.Item>
          <Form.Item
            label="Cấp học"
            name="educationLevel"
            rules={[{ required: true, message: 'Vui lòng chọn cấp học' }]}
          >
            <Select options={[
              { value: 'ELEMENTARY', label: 'Tiểu học' },
              { value: 'MIDDLE_SCHOOL', label: 'THCS' },
              { value: 'HIGH_SCHOOL', label: 'THPT' },
              { value: 'UNIVERSITY', label: 'Đại học' },
              { value: 'OTHER', label: 'Khác' },
            ]} />
          </Form.Item>
          
          <Form.Item
            label="Mô tả"
            name="description"
          >
            <Input.TextArea rows={3} placeholder="Mô tả" />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={submitting}>
              Gửi đề xuất
            </Button>
          </Form.Item>
        </Form>
      </Card>
      
      <Card title="Lịch sử đề xuất">
        <Table
          dataSource={proposals}
          columns={columns}
          rowKey="id"
          pagination={{ pageSize: 5 }}
        />
      </Card>
    </div>
  );
}
