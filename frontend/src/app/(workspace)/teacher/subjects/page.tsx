'use client';

import React, { useEffect, useState } from 'react';
import { Typography, Spin, message, Alert, Empty, Card, Button, Popconfirm, Tag } from 'antd';
import { DeleteOutlined, BookOutlined } from '@ant-design/icons';
import { TeacherSubject, teacherApi } from '@/shared/api/teacher';
import SubjectSelector from './SubjectSelector';

const { Title, Text } = Typography;

export default function SubjectsPage() {
  const [subjects, setSubjects] = useState<TeacherSubject[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchSubjects = async () => {
    try {
      const data = await teacherApi.getSubjects();
      setSubjects(data);
    } catch (err) {
      setError('Không thể tải danh sách môn học.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSubjects();
  }, []);

  const handleAddSubject = async (subjectId: string) => {
    if (subjects.some(s => s.subjectId === subjectId)) {
      message.warning('Bạn đã đăng ký môn học này rồi.');
      return;
    }
    try {
      const newSubject = await teacherApi.addSubject(subjectId);
      setSubjects(prev => [...prev, newSubject]);
      message.success('Đã thêm môn học thành công.');
    } catch (err) {
      message.error('Thêm môn học thất bại.');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await teacherApi.deleteSubject(id);
      setSubjects(prev => prev.filter(s => s.id !== id));
      message.success('Đã xóa môn học.');
    } catch (err) {
      message.error('Xóa môn học thất bại.');
    }
  };

  if (loading) {
    return <div style={{ textAlign: 'center', padding: '40px' }}><Spin size="large" description="Đang tải danh sách môn học..." /></div>;
  }

  return (
    <div style={{ maxWidth: 880, margin: '0 auto' }}>
      <Title level={2}>Quản lý Môn dạy</Title>
      
      {error && <Alert type="error" title={error} style={{ marginBottom: 24 }} />}
      
      <Card style={{ marginBottom: 24 }}>
        <SubjectSelector onAdd={handleAddSubject} />
      </Card>
      
      <Card title="Danh sách môn học đang dạy">
        {subjects.length === 0 ? <Empty description="Chưa có môn học nào." /> : (
          <div role="list">{subjects.map((item) => (
            <div key={item.id} role="listitem" style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '12px 0', borderBottom: '1px solid var(--color-border)' }}>
              <BookOutlined style={{ fontSize: 24, color: 'var(--color-success-500)' }} />
              <div style={{ flex: 1 }}><Text strong>{item.name}</Text><div><Tag>{item.category}</Tag></div></div>
              <Popconfirm
                  key="delete"
                  title="Xóa môn học"
                  description="Bạn có chắc chắn muốn xóa môn này không?"
                  onConfirm={() => handleDelete(item.id)}
                  okText="Đồng ý"
                  cancelText="Hủy"
              ><Button danger type="text" icon={<DeleteOutlined />}>Xóa</Button></Popconfirm>
            </div>
          ))}</div>
        )}
      </Card>
    </div>
  );
}
