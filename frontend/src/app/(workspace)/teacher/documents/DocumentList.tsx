'use client';

import React from 'react';
import { Button, Popconfirm } from 'antd';
import { DeleteOutlined, FileOutlined } from '@ant-design/icons';
import { TeacherDocument } from '@/shared/api/teacher';
import { formatVietnamDate } from '@/shared/lib/vietnamTime';

interface DocumentListProps {
  documents: TeacherDocument[];
  onDelete: (id: string) => Promise<void>;
}

export default function DocumentList({ documents, onDelete }: DocumentListProps) {
  const getStatusTag = (status: string) => {
    switch (status) {
      case 'VERIFIED':
        return <span className="tm-teacher-documents__status tm-teacher-documents__status--verified">Đã xác thực</span>;
      case 'PENDING':
        return <span className="tm-teacher-documents__status tm-teacher-documents__status--pending">Đang chờ</span>;
      case 'REJECTED':
        return <span className="tm-teacher-documents__status tm-teacher-documents__status--rejected">Từ chối</span>;
      default:
        return <span className="tm-teacher-documents__status">{status}</span>;
    }
  };

  return (
    documents.length === 0 ? <p className="tm-teacher-documents__empty">Chưa có tài liệu nào. Thêm giấy tờ ở phần tải lên bên dưới.</p> :
      <ul className="tm-teacher-documents__items" aria-label="Tài liệu xác minh đã tải lên">
        {documents.map((item) => <li key={item.id} className="tm-teacher-documents__item">
          <FileOutlined className="tm-teacher-documents__file-icon" aria-hidden="true" />
          <div className="tm-teacher-documents__file-info">
            <strong className="tm-teacher-documents__file-name">{item.name}</strong>
            <div className="tm-teacher-documents__file-meta"><time dateTime={item.uploadedAt}>{formatVietnamDate(item.uploadedAt)}</time>{getStatusTag(item.status)}</div>
          </div>
          <Popconfirm
            title="Xóa tài liệu"
            description="Bạn có chắc chắn muốn xóa tài liệu này không?"
            onConfirm={() => onDelete(item.id)}
            okText="Đồng ý"
            cancelText="Hủy"
          ><Button className="tm-teacher-documents__delete" danger type="text" icon={<DeleteOutlined />} aria-label={`Xóa ${item.name}`}>Xóa</Button></Popconfirm>
        </li>)}
      </ul>
  );
}
