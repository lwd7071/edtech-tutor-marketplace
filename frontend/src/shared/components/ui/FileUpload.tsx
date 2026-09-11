'use client';

import React from 'react';
import { Upload, message } from 'antd';
import type { UploadProps } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import { attachmentApi, AttachmentView } from '@/shared/api/attachmentApi';

const { Dragger } = Upload;

interface FileUploadProps extends Omit<UploadProps, 'customRequest'> {
  title?: string;
  hint?: string;
  attachableType?: 'ASSIGNMENT' | 'SUBMISSION' | 'MESSAGE';
  onUploadSuccess?: (attachment: AttachmentView) => void;
}

export default function FileUpload({
  title = 'Nhấp hoặc kéo thả file vào đây',
  hint = 'Hỗ trợ tải lên một hoặc nhiều file.',
  attachableType = 'ASSIGNMENT',
  onUploadSuccess,
  ...props
}: FileUploadProps) {
  
  const handleCustomRequest = async (options: any) => {
    const { file, onSuccess, onError, onProgress } = options;
    
    try {
      // Simulate progress since axios doesn't have an easy onUploadProgress in customRequest here without wrapping
      onProgress({ percent: 50 });
      
      const response = await attachmentApi.uploadAttachment(file, attachableType);
      
      onProgress({ percent: 100 });
      onSuccess(response.data, file);
      
      if (onUploadSuccess && response.data) {
        onUploadSuccess(response.data);
      }
      
      message.success(`Tải lên file ${file.name} thành công`);
    } catch (err: any) {
      onError(err);
      message.error(`Lỗi tải lên file ${file.name}`);
    }
  };

  return (
    <Dragger
      customRequest={handleCustomRequest}
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
