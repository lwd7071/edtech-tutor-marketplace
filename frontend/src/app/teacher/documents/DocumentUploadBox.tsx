'use client';

import React from 'react';
import { Upload, message, Typography } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import type { UploadProps } from 'antd';
import { TeacherDocument, teacherApi } from '@/shared/api/teacher';

const { Dragger } = Upload;
const { Text } = Typography;

interface DocumentUploadBoxProps {
  onSuccess: (newDoc: TeacherDocument) => void;
}

export default function DocumentUploadBox({ onSuccess }: DocumentUploadBoxProps) {
  const props: UploadProps = {
    name: 'file',
    multiple: true,
    customRequest: async (options) => {
      const { file, onSuccess: onUploadSuccess, onError } = options;
      try {
        const newDoc = await teacherApi.uploadDocument(file as File);
        onUploadSuccess?.('ok');
        onSuccess(newDoc);
      } catch (err) {
        onError?.(err as Error);
        message.error(`Tải lên file ${(file as File).name} thất bại.`);
      }
    },
    onChange(info) {
      const { status } = info.file;
      if (status === 'done') {
        // Success is handled in customRequest via onSuccess prop to update parent state
      } else if (status === 'error') {
        message.error(`${info.file.name} tải lên thất bại.`);
      }
    },
    onDrop(e) {
      console.log('Dropped files', e.dataTransfer.files);
    },
  };

  return (
    <Dragger {...props} style={{ padding: 24 }}>
      <p className="ant-upload-drag-icon">
        <InboxOutlined />
      </p>
      <p className="ant-upload-text">Kéo thả hoặc nhấp để tải tài liệu lên</p>
      <p className="ant-upload-hint">
        Hỗ trợ tải lên nhiều file cùng lúc. Các định dạng được hỗ trợ: PDF, PNG, JPG, JPEG.
      </p>
    </Dragger>
  );
}
