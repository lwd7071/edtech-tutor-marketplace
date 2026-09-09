'use client';

import React, { useEffect, useState } from 'react';
import { Button, message, Alert, Skeleton } from 'antd';
import { SaveOutlined } from '@ant-design/icons';
import { teacherApi } from '@/shared/api/teacher';
import WeeklyScheduleGrid, { TimeSlot } from '@/shared/components/data-display/WeeklyScheduleGrid';

const DAY_MAP: Record<string, number> = {
  'MONDAY': 1, 'TUESDAY': 2, 'WEDNESDAY': 3, 'THURSDAY': 4, 'FRIDAY': 5, 'SATURDAY': 6, 'SUNDAY': 7
};

const REV_DAY_MAP: Record<number, string> = {
  1: 'MONDAY', 2: 'TUESDAY', 3: 'WEDNESDAY', 4: 'THURSDAY', 5: 'FRIDAY', 6: 'SATURDAY', 7: 'SUNDAY'
};

export const AvailabilityEditor: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [slots, setSlots] = useState<TimeSlot[]>([]);
  const [loadError, setLoadError] = useState(false);

  useEffect(() => {
    const fetchAvailabilities = async () => {
      try {
        const data = await teacherApi.getAvailabilities();
        const mappedSlots: TimeSlot[] = data.map(a => ({
          dayOfWeek: DAY_MAP[a.dayOfWeek] || 1,
          startTime: a.startTime.substring(0, 5),
          endTime: a.endTime.substring(0, 5),
        }));
        setSlots(mappedSlots);
      } catch (error) {
        setLoadError(true);
        message.error('Không thể tải lịch rảnh');
      } finally {
        setLoading(false);
      }
    };
    fetchAvailabilities();
  }, []);

  const handleSlotClick = (day: number, hour: number) => {
    const startTime = `${hour.toString().padStart(2, '0')}:00`;
    const endTime = `${(hour + 1).toString().padStart(2, '0')}:00`;
    
    setSlots(prev => {
      const existingIdx = prev.findIndex(s => s.dayOfWeek === day && s.startTime === startTime);
      if (existingIdx >= 0) {
        // Remove slot
        const newSlots = [...prev];
        newSlots.splice(existingIdx, 1);
        return newSlots;
      } else {
        // Add slot
        return [...prev, { dayOfWeek: day, startTime, endTime }];
      }
    });
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Ho_Chi_Minh';
      const items = slots.map(s => ({
        dayOfWeek: REV_DAY_MAP[s.dayOfWeek],
        startTime: s.startTime,
        endTime: s.endTime,
      }));
      
      await teacherApi.replaceAvailabilities({ timezone, items });
      message.success('Cập nhật lịch rảnh thành công');
    } catch (error) {
      message.error('Lưu lịch rảnh thất bại');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div data-testid="loading-skeleton"><Skeleton active paragraph={{ rows: 10 }} /></div>;
  }

  return (
    <div className="tm-panel tm-stack">
      <div className="tm-toolbar">
        <div className="tm-page-heading" style={{ marginBottom: 0 }}><h1>Lịch rảnh giảng dạy</h1><p>Chọn các khung giờ bạn có thể nhận lịch học.</p></div>
        <Button 
          type="primary" 
          icon={<SaveOutlined />} 
          onClick={handleSave} 
          loading={saving}
          disabled={loadError}
        >
          Lưu lịch rảnh
        </Button>
      </div>
      {loadError && <Alert title="Chưa tải được lịch rảnh" description="Tải lại trang trước khi chỉnh sửa để tránh ghi đè lịch hiện có." type="error" showIcon />}
      
      <Alert
        title="Lịch đã xác nhận không thay đổi"
        description="Các khung giờ mới chỉ áp dụng cho những lịch học được tạo sau khi bạn lưu."
        type="warning"
        showIcon
      />

      <WeeklyScheduleGrid
        mode="editable"
        availableSlots={slots}
        onSlotClick={handleSlotClick}
      />
    </div>
  );
};
