'use client';

import React, { useEffect, useState } from 'react';
import { Typography, Spin, message, Alert, Card, Form, Input, Button, Table, Tag } from 'antd';
import { TeacherSubjectProposal, teacherApi } from '@/shared/api/teacher';

const { Title } = Typography;

export default function SubjectProposalsPage() {
  const [proposals, setProposals] = useState<TeacherSubjectProposal[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();

  const fetchProposals = async () => {
    try {
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

  const onFinish = async (values: { name: string; description: string }) => {
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
      dataIndex: 'name',
      key: 'name',
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
      title: 'Ngày gửi',
      dataIndex: 'submittedAt',
      key: 'submittedAt',
      render: (date: string) => new Date(date).toLocaleDateString('vi-VN'),
    },
  ];

  if (loading) {
    return <div style={{ textAlign: 'center', padding: '40px' }}><Spin size="large" tip="Đang tải danh sách đề xuất..." /></div>;
  }

  return (
    <div style={{ maxWidth: 880, margin: '0 auto' }}>
      <Title level={2}>Đề xuất Môn học mới</Title>
      
      {error && <Alert type="error" message={error} style={{ marginBottom: 24 }} />}
      
      <Card title="Gửi đề xuất mới" style={{ marginBottom: 24 }}>
        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Form.Item
            label="Tên môn học"
            name="name"
            rules={[{ required: true, message: 'Vui lòng nhập tên môn học' }]}
          >
            <Input placeholder="Tên môn học" />
          </Form.Item>
          
          <Form.Item
            label="Mô tả"
            name="description"
            rules={[{ required: true, message: 'Vui lòng nhập mô tả' }]}
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
