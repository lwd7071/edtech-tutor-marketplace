import React from 'react';
import { Button, Select, Radio, InputNumber } from 'antd';
import { TeacherSearchParams, SubjectSummary } from '@/shared/api/public';

interface TeacherFilterSidebarProps {
  filters: TeacherSearchParams;
  subjects?: SubjectSummary[];
  onChange: (newFilters: Partial<TeacherSearchParams>) => void;
  onClear: () => void;
}

export const TeacherFilterSidebar: React.FC<TeacherFilterSidebarProps> = ({
  filters,
  subjects,
  onChange,
  onClear
}) => {
  return (
    <div style={{ backgroundColor: 'var(--color-surface)', borderRadius: 'var(--radius-xl)', border: '1px solid var(--color-border)', padding: 'var(--space-6)', boxShadow: 'var(--shadow-sm)', display: 'flex', flexDirection: 'column', gap: 'var(--space-6)', width: '100%' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <h3 style={{ fontSize: 'var(--text-h4)', margin: 0 }}>Lọc kết quả</h3>
        <Button type="link" onClick={onClear} style={{ padding: 0, color: 'var(--color-text-secondary)', height: 'auto' }}>
          Xóa bộ lọc
        </Button>
      </div>

      {/* Môn học */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
        <label style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>Môn học</label>
        <Select
          data-testid="subject-select"
          allowClear
          placeholder="Chọn môn học"
          value={filters.subjectId || null}
          onChange={(value) => onChange({ subjectId: value || undefined })}
          options={subjects?.map(s => ({ label: s.name, value: s.id }))}
          style={{ width: '100%', height: '40px' }}
        />
      </div>

      {/* Hình thức học */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
        <label style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>Hình thức học</label>
        <Radio.Group 
          value={filters.deliveryMode || ''} 
          onChange={(e) => onChange({ deliveryMode: e.target.value || undefined })}
          style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}
        >
          <Radio value="">Tất cả</Radio>
          <Radio value="ONLINE">Học Online</Radio>
          <Radio value="OFFLINE">Học Offline</Radio>
        </Radio.Group>
      </div>

      {/* Khoảng giá */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
        <label style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>Khoảng giá (VNĐ)</label>
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
          <InputNumber
            data-testid="min-price"
            placeholder="Tối thiểu"
            value={filters.minPrice}
            onChange={(val) => onChange({ minPrice: val as number | undefined })}
            style={{ width: '100%' }}
            min={0}
            step={50000}
          />
          <span style={{ color: 'var(--color-text-secondary)' }}>-</span>
          <InputNumber
            data-testid="max-price"
            placeholder="Tối đa"
            value={filters.maxPrice}
            onChange={(val) => onChange({ maxPrice: val as number | undefined })}
            style={{ width: '100%' }}
            min={0}
            step={50000}
          />
        </div>
      </div>

      {/* Đánh giá */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
        <label style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>Đánh giá</label>
        <Radio.Group 
          value={filters.minRating} 
          onChange={(e) => onChange({ minRating: e.target.value })}
          style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}
        >
          <Radio value={undefined}>Tất cả</Radio>
          <Radio value={4.5}>Từ 4.5 sao</Radio>
          <Radio value={4.0}>Từ 4.0 sao</Radio>
          <Radio value={3.0}>Từ 3.0 sao</Radio>
        </Radio.Group>
      </div>
    </div>
  );
};
