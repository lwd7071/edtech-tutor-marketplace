'use client';

import React from 'react';
import { Upload } from 'antd';
import type { UploadProps } from 'antd';
import { InboxOutlined } from '@ant-design/icons';

const { Dragger } = Upload;

interface FileUploadProps extends UploadProps {
  title?: string;
  hint?: string;
}

export default function FileUpload({
  title = 'Nhấp hoặc kéo thả file vào đây',
  hint = 'Hỗ trợ tải lên một hoặc nhiều file.',
  ...props
}: FileUploadProps) {
  return (
    <Dragger
      {...props}
      style={{
        padding: 'var(--space-6)',
        backgroundColor: 'var(--color-surface)',
        border: '1px dashed var(--color-border)',
        borderRadius: 'var(--radius-md)',
        ...props.style
      }}
    >
      <p className="ant-upload-drag-icon">
        <InboxOutlined style={{ color: 'var(--color-primary-600)' }} />
      </p>
      <p className="ant-upload-text" style={{ fontWeight: 'var(--weight-medium)', color: 'var(--color-text-primary)' }}>
        {title}
      </p>
      <p className="ant-upload-hint" style={{ color: 'var(--color-text-secondary)' }}>
        {hint}
      </p>
    </Dragger>
  );
}
