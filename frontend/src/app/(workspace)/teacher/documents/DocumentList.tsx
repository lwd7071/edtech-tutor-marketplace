'use client';

import React from 'react';
import { List, Card, Button, Popconfirm, Typography, Tag } from 'antd';
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
      <List
        dataSource={documents}
        renderItem={(item) => (
          <List.Item
            actions={[
              <Popconfirm
                key="delete"
                title="Xóa tài liệu"
                description="Bạn có chắc chắn muốn xóa tài liệu này không?"
                onConfirm={() => onDelete(item.id)}
                okText="Đồng ý"
                cancelText="Hủy"
              >
                <Button danger type="text" icon={<DeleteOutlined />}>
                  Xóa
                </Button>
              </Popconfirm>,
            ]}
          >
            <List.Item.Meta
              avatar={<FileOutlined style={{ fontSize: 24, color: '#1890ff' }} />}
              title={<a href={item.url} target="_blank" rel="noopener noreferrer">{item.name}</a>}
              description={
                <>
                  <Text type="secondary" style={{ marginRight: 8 }}>
                    {new Date(item.uploadedAt).toLocaleDateString('vi-VN')}
                  </Text>
                  {getStatusTag(item.status)}
                </>
              }
            />
          </List.Item>
        )}
      />
    </Card>
  );
}
