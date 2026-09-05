'use client';

import React, { useEffect, useState } from 'react';
import { Typography, Spin, message, Alert } from 'antd';
import { TeacherDocument, teacherApi } from '@/shared/api/teacher';
import DocumentUploadBox from '@/app/teacher/documents/DocumentUploadBox';
import DocumentList from '@/app/teacher/documents/DocumentList';

const { Title } = Typography;

export default function DocumentsPage() {
  const [documents, setDocuments] = useState<TeacherDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchDocuments = async () => {
    try {
      const data = await teacherApi.getDocuments();
      setDocuments(data);
    } catch (err) {
      setError('Không thể tải tài liệu.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDocuments();
  }, []);

  const handleUploadSuccess = (newDoc: TeacherDocument) => {
    setDocuments((prev) => [...prev, newDoc]);
    message.success('Tải lên tài liệu thành công!');
  };

  const handleDelete = async (id: string) => {
    try {
      await teacherApi.deleteDocument(id);
      setDocuments((prev) => prev.filter(doc => doc.id !== id));
      message.success('Đã xóa tài liệu.');
    } catch (err) {
      message.error('Không thể xóa tài liệu.');
    }
  };

  if (loading) {
    return <div style={{ textAlign: 'center', padding: '40px' }}><Spin size="large" tip="Đang tải tài liệu..." /></div>;
  }

  return (
    <div style={{ maxWidth: 880, margin: '0 auto' }}>
      <Title level={2}>Tài liệu & Chứng chỉ</Title>
      
      {error && <Alert type="error" message={error} style={{ marginBottom: 24 }} />}
      
      <DocumentUploadBox onSuccess={handleUploadSuccess} />
      
      <div style={{ marginTop: 24 }}>
        <DocumentList documents={documents} onDelete={handleDelete} />
      </div>
    </div>
  );
}
