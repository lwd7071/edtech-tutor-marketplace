'use client';

import React from 'react';
import { Upload, message, Button } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import type { UploadProps } from 'antd';
import { TeacherDocument, teacherApi } from '@/shared/api/teacher';

interface DocumentUploadBoxProps {
  onSuccess: (newDoc: TeacherDocument) => void;
}

export default function DocumentUploadBox({ onSuccess }: DocumentUploadBoxProps) {
  const props: UploadProps = {
    name: 'file',
    multiple: true,
    accept: '.pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg',
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

  return <Upload className="tm-teacher-documents__upload" {...props}>
    <div className="tm-teacher-documents__upload-control">
      <Button icon={<UploadOutlined aria-hidden="true" />}>Chọn giấy tờ</Button>
      <span>Kéo thả tệp vào đây hoặc chọn từ thiết bị.</span>
    </div>
  </Upload>;
}
