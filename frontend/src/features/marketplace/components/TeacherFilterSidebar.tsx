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
    <div className="bg-surface rounded-xl border border-border p-6 shadow-sm flex flex-col gap-6 w-full">
      <div className="flex items-center justify-between">
        <h3 className="text-h4 m-0">Lọc kết quả</h3>
        <Button type="link" onClick={onClear} className="p-0 text-text-secondary h-auto">
          Xóa bộ lọc
        </Button>
      </div>

      {/* Môn học */}
      <div className="flex flex-col gap-2">
        <label className="font-semibold text-text-primary">Môn học</label>
        <Select
          data-testid="subject-select"
          allowClear
          placeholder="Chọn môn học"
          value={filters.subjectId || null}
          onChange={(value) => onChange({ subjectId: value || undefined })}
          options={subjects?.map(s => ({ label: s.name, value: s.id }))}
          className="w-full h-10"
        />
      </div>

      {/* Hình thức học */}
      <div className="flex flex-col gap-2">
        <label className="font-semibold text-text-primary">Hình thức học</label>
        <Radio.Group 
          value={filters.deliveryMode || ''} 
          onChange={(e) => onChange({ deliveryMode: e.target.value || undefined })}
          className="flex flex-col gap-2"
        >
          <Radio value="">Tất cả</Radio>
          <Radio value="ONLINE">Học Online</Radio>
          <Radio value="OFFLINE">Học Offline</Radio>
        </Radio.Group>
      </div>

      {/* Khoảng giá */}
      <div className="flex flex-col gap-2">
        <label className="font-semibold text-text-primary">Khoảng giá (VNĐ)</label>
        <div className="flex items-center gap-2">
          <InputNumber
            data-testid="min-price"
            placeholder="Tối thiểu"
            value={filters.minPrice}
            onChange={(val) => onChange({ minPrice: val as number | undefined })}
            className="w-full"
            min={0}
            step={50000}
          />
          <span className="text-text-secondary">-</span>
          <InputNumber
            data-testid="max-price"
            placeholder="Tối đa"
            value={filters.maxPrice}
            onChange={(val) => onChange({ maxPrice: val as number | undefined })}
            className="w-full"
            min={0}
            step={50000}
          />
        </div>
      </div>

      {/* Đánh giá */}
      <div className="flex flex-col gap-2">
        <label className="font-semibold text-text-primary">Đánh giá</label>
        <Radio.Group 
          value={filters.minRating} 
          onChange={(e) => onChange({ minRating: e.target.value })}
          className="flex flex-col gap-2"
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
