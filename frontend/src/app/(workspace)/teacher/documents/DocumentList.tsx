'use client';

import React from 'react';
import { Card, Button, Empty, Popconfirm, Typography, Tag } from 'antd';
import { DeleteOutlined, FileOutlined } from '@ant-design/icons';
import { TeacherDocument } from '@/shared/api/teacher';

const { Text } = Typography;

interface DocumentListProps {
  documents: TeacherDocument[];
  onDelete: (id: string) => Promise<void>;
}

export default function DocumentList({ documents, onDelete }: DocumentListProps) {
  const getStatusTag = (status: string) => {
    switch (status) {
      case 'VERIFIED':
        return <Tag color="success">Đã xác thực</Tag>;
      case 'PENDING':
        return <Tag color="warning">Đang chờ</Tag>;
      case 'REJECTED':
        return <Tag color="error">Từ chối</Tag>;
      default:
        return <Tag>{status}</Tag>;
    }
  };

  return (
    <Card title="Danh sách tài liệu đã tải lên">
      {documents.length === 0 ? <Empty description="Chưa có tài liệu nào." /> : (
        <div role="list">{documents.map((item) => (
          <div key={item.id} role="listitem" style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '12px 0', borderBottom: '1px solid var(--color-border)' }}>
            <FileOutlined style={{ fontSize: 24, color: 'var(--color-primary-500)' }} />
            <div style={{ flex: 1 }}>
              <a href={item.url} target="_blank" rel="noopener noreferrer">{item.name}</a>
              <div><Text type="secondary" style={{ marginRight: 8 }}>{new Date(item.uploadedAt).toLocaleDateString('vi-VN')}</Text>{getStatusTag(item.status)}</div>
            </div>
            <Popconfirm
                key="delete"
                title="Xóa tài liệu"
                description="Bạn có chắc chắn muốn xóa tài liệu này không?"
                onConfirm={() => onDelete(item.id)}
                okText="Đồng ý"
                cancelText="Hủy"
            ><Button danger type="text" icon={<DeleteOutlined />}>Xóa</Button></Popconfirm>
          </div>
        ))}</div>
      )}
    </Card>
  );
}
