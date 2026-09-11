import React from 'react';
import { Space, Typography, Button } from 'antd';
import { 
  FilePdfOutlined, 
  FileWordOutlined, 
  FileExcelOutlined, 
  FileImageOutlined, 
  FileOutlined,
  DownloadOutlined
} from '@ant-design/icons';
import { AttachmentView } from '@/shared/api/attachmentApi';

interface FileViewerProps {
  files?: AttachmentView[];
}

export const getFileIcon = (mimeType: string) => {
  if (mimeType.includes('pdf')) return <FilePdfOutlined className="text-red-500" />;
  if (mimeType.includes('word') || mimeType.includes('document')) return <FileWordOutlined className="text-blue-500" />;
  if (mimeType.includes('excel') || mimeType.includes('spreadsheet')) return <FileExcelOutlined className="text-green-500" />;
  if (mimeType.includes('image')) return <FileImageOutlined className="text-purple-500" />;
  return <FileOutlined className="text-gray-500" />;
};

export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
};

export const FileViewer: React.FC<FileViewerProps> = ({ files }) => {
  if (!files || files.length === 0) {
    return null;
  }

  return (
    <div className="flex flex-col gap-2 mt-2">
      {files.map(file => (
        <div 
          key={file.id} 
          className="flex items-center justify-between p-3 bg-neutral-50 rounded-lg border border-border hover:bg-neutral-100 transition-colors"
        >
          <Space>
            <span className="text-2xl flex items-center justify-center">
              {getFileIcon(file.mimeType)}
            </span>
            <div className="flex flex-col">
              <Typography.Text 
                strong 
                ellipsis={{ tooltip: file.originalFilename }}
                style={{ maxWidth: '250px' }}
              >
                {file.originalFilename}
              </Typography.Text>
              <Typography.Text type="secondary" style={{ fontSize: '12px' }}>
                {formatFileSize(file.fileSize)}
              </Typography.Text>
            </div>
          </Space>
          <Button 
            type="text" 
            icon={<DownloadOutlined />} 
            href={file.secureUrl}
            target="_blank"
            rel="noopener noreferrer"
            title={`Tải xuống ${file.originalFilename}`}
            aria-label={`Tải xuống ${file.originalFilename}`}
          />
        </div>
      ))}
    </div>
  );
};
