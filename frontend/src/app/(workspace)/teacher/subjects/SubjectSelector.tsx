'use client';

import React, { useState } from 'react';
import { Button, Select, Spin, message } from 'antd';
import { teacherApi } from '@/shared/api/teacher';
import styles from './subjects.module.css';

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

  const fetchOptions = React.useMemo(() => (value: string) => {
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    setOptions([]);
    setFetching(true);
    timeoutRef.current = setTimeout(async () => {
      try {
        const data = await teacherApi.searchPublicSubjects(value);
        setOptions(data);
      } catch {
        // Keep the current no-results behavior when catalog search is unavailable.
      } finally {
        setFetching(false);
      }
    }, 400);
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
    <div className={styles.selector}>
      <label className={styles.selectorLabel} htmlFor="teacher-subject-search">Danh mục môn học</label>
      <Select
        id="teacher-subject-search"
        aria-label="Tìm kiếm môn học"
        className={styles.catalogSelect}
        showSearch
        size="large"
        placeholder="Tìm kiếm môn học (vd: Toán học, Vật lý...)"
        filterOption={false}
        onSearch={fetchOptions}
        onChange={value => setSelectedId(value)}
        value={selectedId}
        notFoundContent={fetching ? <Spin size="small" /> : 'Không tìm thấy kết quả'}
        options={options.map(option => ({ value: option.id, label: `${option.name} (${option.category})` }))}
      />
      <p className={styles.selectorHint}>Tìm theo tên môn, sau đó chọn một kết quả để thêm.</p>
      <Button type="primary" className={styles.addButton} loading={adding} onClick={handleAdd} aria-label="Thêm môn học">
        Thêm môn học
      </Button>
    </div>
  );
}
