'use client';

import React, { useState } from 'react';
import { Select, Button, Space, message, Spin } from 'antd';
import { teacherApi } from '@/shared/api/teacher';

interface PublicSubject {
  id: string;
  name: string;
  category: string;
}

interface SubjectSelectorProps {
  onAdd: (subjectId: string) => Promise<void>;
}

export default function SubjectSelector({ onAdd }: SubjectSelectorProps) {
  const [options, setOptions] = useState<PublicSubject[]>([]);
  const [fetching, setFetching] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);

  const timeoutRef = React.useRef<NodeJS.Timeout | null>(null);

  const fetchOptions = React.useMemo(() => {
    return (value: string) => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
      setOptions([]);
      setFetching(true);
      timeoutRef.current = setTimeout(async () => {
        try {
          const data = await teacherApi.searchPublicSubjects(value);
          setOptions(data);
        } catch (err) {
          // ignore
        } finally {
          setFetching(false);
        }
      }, 400);
    };
  }, []);

  const handleAdd = async () => {
    if (!selectedId) {
      message.warning('Vui lòng chọn một môn học.');
      return;
    }
    setAdding(true);
    await onAdd(selectedId);
    setSelectedId(null);
    setAdding(false);
  };

  return (
    <Space style={{ width: '100%' }} orientation="vertical" size="middle">
      <div style={{ display: 'flex', gap: 16 }}>
        <Select
          showSearch
          style={{ flex: 1 }}
          placeholder="Tìm kiếm môn học (vd: Toán học, Vật lý...)"
          filterOption={false}
          onSearch={fetchOptions}
          onChange={(val) => setSelectedId(val)}
          value={selectedId}
          notFoundContent={fetching ? <Spin size="small" /> : 'Không tìm thấy kết quả'}
          options={options.map((opt) => ({
            value: opt.id,
            label: `${opt.name} (${opt.category})`,
          }))}
        />
        <Button type="primary" onClick={handleAdd} loading={adding}>
          Thêm môn học
        </Button>
      </div>
    </Space>
  );
}
