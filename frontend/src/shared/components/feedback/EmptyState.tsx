import React from 'react';
import { Space, Typography } from 'antd';
import { 
  InboxOutlined, 
  SearchOutlined, 
  AppstoreAddOutlined 
} from '@ant-design/icons';

const { Text, Title } = Typography;

export type EmptyStateVariant = 'no-data' | 'no-search' | 'first-use';

export interface EmptyStateProps {
  variant?: EmptyStateVariant;
  title?: React.ReactNode;
  description?: React.ReactNode;
  action?: React.ReactNode;
  style?: React.CSSProperties;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  variant = 'no-data',
  title,
  description,
  action,
  style,
}) => {
  const getIcon = () => {
    const iconStyle = { fontSize: 48, color: 'var(--color-text-placeholder)' };
    switch (variant) {
      case 'no-search':
        return <SearchOutlined style={iconStyle} />;
      case 'first-use':
        return <AppstoreAddOutlined style={iconStyle} />;
      case 'no-data':
      default:
        return <InboxOutlined style={iconStyle} />;
    }
  };

  const defaultTitle = {
    'no-data': 'Không có dữ liệu',
    'no-search': 'Không tìm thấy kết quả',
    'first-use': 'Bắt đầu sử dụng',
  }[variant];

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 'var(--space-8) var(--space-4)',
        textAlign: 'center',
        ...style,
      }}
    >
      <Space direction="vertical" size="middle" align="center">
        {getIcon()}
        
        <div>
          <Title level={5} style={{ margin: 0, color: 'var(--color-text-secondary)' }}>
            {title || defaultTitle}
          </Title>
          {description && (
            <Text style={{ color: 'var(--color-text-tertiary)' }}>
              {description}
            </Text>
          )}
        </div>
        
        {action && (
          <div style={{ marginTop: 'var(--space-2)' }}>
            {action}
          </div>
        )}
      </Space>
    </div>
  );
};
