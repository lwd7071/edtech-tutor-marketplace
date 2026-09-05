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
    <div className="availability-editor bg-surface p-6 rounded-xl border border-border shadow-sm max-w-5xl">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-bold text-text-primary">Lịch giảng dạy</h2>
        <Button 
          type="primary" 
          icon={<SaveOutlined />} 
          onClick={handleSave} 
          loading={saving}
        >
          Lưu lịch rảnh
        </Button>
      </div>
      
      <Alert
        message="Lưu ý: Thay đổi lịch rảnh sẽ ảnh hưởng đến các booking sắp tới. Học viên đã đặt lịch sẽ không bị thay đổi, nhưng các slot mới sẽ được áp dụng ngay lập tức."
        type="warning"
        showIcon
        className="mb-6"
      />

      <WeeklyScheduleGrid
        mode="editable"
        availableSlots={slots}
        onSlotClick={handleSlotClick}
      />
    </div>
  );
};
