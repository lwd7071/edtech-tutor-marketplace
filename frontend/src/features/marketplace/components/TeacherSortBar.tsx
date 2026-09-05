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
    <div className="flex flex-col sm:flex-row sm:items-center justify-between bg-surface p-4 rounded-xl shadow-sm border border-border gap-4 mb-6">
      <span className="text-text-primary font-medium" aria-live="polite">
        Tìm thấy {totalElements} giáo viên
      </span>
      <div className="flex items-center gap-3">
        <span className="text-text-secondary whitespace-nowrap">Sắp xếp theo:</span>
        <Select
          data-testid="sort-select"
          value={value || ''}
          onChange={(val) => onChange(val === '' ? undefined : val)}
          className="w-48"
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
