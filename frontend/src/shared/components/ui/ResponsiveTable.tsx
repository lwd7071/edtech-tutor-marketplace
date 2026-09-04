'use client';

import React from 'react';
import { Table, Card, Space, Typography } from 'antd';
import type { TableProps } from 'antd';
import { useMediaQuery } from 'react-responsive';

const { Text } = Typography;

export interface ResponsiveTableProps<RecordType> extends TableProps<RecordType> {
  mobileCardRender?: (record: RecordType) => React.ReactNode;
}

export default function ResponsiveTable<RecordType extends object>({
  mobileCardRender,
  columns,
  dataSource,
  ...props
}: ResponsiveTableProps<RecordType>) {
  const isMobile = useMediaQuery({ maxWidth: 767 });

  if (isMobile && mobileCardRender && dataSource) {
    return (
      <div className="responsive-table-mobile-cards" style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
        {dataSource.map((record: RecordType, index) => {
          const key = props.rowKey 
            ? (typeof props.rowKey === 'function' ? props.rowKey(record, index) : (record as Record<string, unknown>)[props.rowKey as string]) 
            : index;
          return (
            <Card key={key as React.Key} size="small" style={{ borderRadius: 'var(--radius-md)' }}>
              {mobileCardRender(record)}
            </Card>
          );
        })}
      </div>
    );
  }

  // Auto-generate fallback for mobile card if not provided
  if (isMobile && !mobileCardRender && dataSource && columns) {
    return (
      <div className="responsive-table-mobile-cards" style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
        {dataSource.map((record: RecordType, index) => {
          const key = props.rowKey 
            ? (typeof props.rowKey === 'function' ? props.rowKey(record, index) : (record as Record<string, unknown>)[props.rowKey as string]) 
            : index;
          return (
            <Card key={key as React.Key} size="small" style={{ borderRadius: 'var(--radius-md)' }}>
              <Space direction="vertical" style={{ width: '100%' }}>
                {columns.map((col: import('antd/es/table').ColumnType<RecordType>, colIdx) => (
                  <div key={colIdx} style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--color-border)', paddingBottom: '4px' }}>
                    <Text type="secondary">{col.title as React.ReactNode}:</Text>
                    <Text strong>{col.render ? (col.render((record as Record<string, unknown>)[col.dataIndex as string], record, index) as unknown as React.ReactNode) : (record as Record<string, unknown>)[col.dataIndex as string] as React.ReactNode}</Text>
                  </div>
                ))}
              </Space>
            </Card>
          );
        })}
      </div>
    );
  }

  return (
    <Table
      columns={columns}
      dataSource={dataSource}
      className="responsive-table-desktop"
      rowClassName={() => 'hover-row-effect'}
      {...props}
    />
  );
}
