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
    <div className="tm-sort-bar">
      <p aria-live="polite"><strong>{totalElements}</strong> gia sư phù hợp</p>
      <div className="tm-sort-control">
        <label htmlFor="teacher-sort">Sắp xếp</label>
        <Select
          id="teacher-sort"
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

