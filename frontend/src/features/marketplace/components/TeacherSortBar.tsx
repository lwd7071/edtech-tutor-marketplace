import React from 'react';
import { Select } from 'antd';

interface TeacherSortBarProps {
  totalElements: number;
  value?: string;
  onChange: (sortValue?: string) => void;
}

export const TeacherSortBar: React.FC<TeacherSortBarProps> = ({
  totalElements,
  value,
  onChange
}) => {
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', backgroundColor: 'var(--color-surface)', padding: 'var(--space-4)', borderRadius: 'var(--radius-xl)', boxShadow: 'var(--shadow-sm)', border: '1px solid var(--color-border)', gap: 'var(--space-4)', marginBottom: 'var(--space-6)' }}>
      <span style={{ color: 'var(--color-text-primary)', fontWeight: 500 }} aria-live="polite">
        Tìm thấy {totalElements} giáo viên
      </span>
      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
        <span style={{ color: 'var(--color-text-secondary)', whiteSpace: 'nowrap' }}>Sắp xếp theo:</span>
        <Select
          data-testid="sort-select"
          value={value || ''}
          onChange={(val) => onChange(val === '' ? undefined : val)}
          style={{ width: '192px' }}
          options={[
            { label: 'Mặc định', value: '' },
            { label: 'Giá tăng dần', value: 'price_asc' },
            { label: 'Giá giảm dần', value: 'price_desc' },
            { label: 'Đánh giá cao nhất', value: 'rating_desc' },
            { label: 'Nhiều kinh nghiệm nhất', value: 'experience_desc' }
          ]}
        />
      </div>
    </div>
  );
};
