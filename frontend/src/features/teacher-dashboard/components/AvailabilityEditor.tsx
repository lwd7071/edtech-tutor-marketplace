'use client';

import React, { useEffect, useState } from 'react';
import { Alert, Button, Skeleton, message } from 'antd';
import { useMediaQuery } from 'react-responsive';
import { teacherApi } from '@/shared/api/teacher';
import type { TimeSlot } from '@/shared/components/data-display/WeeklyScheduleGrid';
import styles from './AvailabilityEditor.module.css';

const DAY_MAP: Record<string, number> = {
  MONDAY: 1, TUESDAY: 2, WEDNESDAY: 3, THURSDAY: 4, FRIDAY: 5, SATURDAY: 6, SUNDAY: 7,
};

const REV_DAY_MAP: Record<number, string> = {
  1: 'MONDAY', 2: 'TUESDAY', 3: 'WEDNESDAY', 4: 'THURSDAY', 5: 'FRIDAY', 6: 'SATURDAY', 7: 'SUNDAY',
};

const DAYS = [
  { id: 1, label: 'Thứ 2' }, { id: 2, label: 'Thứ 3' }, { id: 3, label: 'Thứ 4' },
  { id: 4, label: 'Thứ 5' }, { id: 5, label: 'Thứ 6' }, { id: 6, label: 'Thứ 7' },
  { id: 7, label: 'Chủ nhật' },
];

const HOURS = Array.from({ length: 15 }, (_, index) => index + 8);
const timeLabel = (hour: number) => `${hour.toString().padStart(2, '0')}:00`;

export const AvailabilityEditor: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [slots, setSlots] = useState<TimeSlot[]>([]);
  const [loadError, setLoadError] = useState(false);
  const isMobile = useMediaQuery({ maxWidth: 767 });
  const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Ho_Chi_Minh';

  useEffect(() => {
    const fetchAvailabilities = async () => {
      try {
        const data = await teacherApi.getAvailabilities();
        const mappedSlots: TimeSlot[] = data.map(availability => ({
          dayOfWeek: DAY_MAP[availability.dayOfWeek] || 1,
          startTime: availability.startTime.substring(0, 5),
          endTime: availability.endTime.substring(0, 5),
        }));
        setSlots(mappedSlots);
      } catch {
        setLoadError(true);
        message.error('Không thể tải lịch rảnh');
      } finally {
        setLoading(false);
      }
    };
    fetchAvailabilities();
  }, []);

  const handleSlotClick = (day: number, hour: number) => {
    const startTime = timeLabel(hour);
    const endTime = timeLabel(hour + 1);

    setSlots(previous => {
      const existingIndex = previous.findIndex(slot => slot.dayOfWeek === day && slot.startTime === startTime);
      if (existingIndex >= 0) {
        const nextSlots = [...previous];
        nextSlots.splice(existingIndex, 1);
        return nextSlots;
      }
      return [...previous, { dayOfWeek: day, startTime, endTime }];
    });
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const items = slots.map(slot => ({
        dayOfWeek: REV_DAY_MAP[slot.dayOfWeek],
        startTime: slot.startTime,
        endTime: slot.endTime,
        timezone,
        isActive: true,
      }));

      await teacherApi.replaceAvailabilities({ items });
      message.success('Cập nhật lịch rảnh thành công');
    } catch {
      message.error('Lưu lịch rảnh thất bại');
    } finally {
      setSaving(false);
    }
  };

  const isAvailable = (day: number, hour: number) => {
    const startTime = timeLabel(hour);
    return slots.some(slot => slot.dayOfWeek === day && slot.startTime <= startTime && slot.endTime > startTime);
  };

  const renderSlot = (day: { id: number; label: string }, hour: number) => {
    const selected = isAvailable(day.id, hour);
    return (
      <button
        key={`${day.id}-${hour}`}
        type="button"
        className={`${styles.timeSlot} ${selected ? styles.selected : ''}`}
        aria-label={`${day.label}, ${timeLabel(hour)}–${timeLabel(hour + 1)}, ${selected ? 'đang rảnh' : 'chưa chọn'}`}
        aria-pressed={selected}
        disabled={loadError || saving}
        onClick={() => handleSlotClick(day.id, hour)}
      >
        {timeLabel(hour)}
      </button>
    );
  };

  if (loading) {
    return (
      <main className={styles.page}>
        <header className={styles.header}><h1>Lịch rảnh giảng dạy</h1></header>
        <div className={styles.loading} data-testid="loading-skeleton" role="status" aria-label="Đang tải lịch rảnh">
          <span>Đang tải thời gian rảnh...</span>
          <Skeleton active paragraph={{ rows: 10 }} />
        </div>
      </main>
    );
  }

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <div>
          <h1>Lịch rảnh giảng dạy</h1>
          <p>Chọn các khung giờ bạn có thể nhận lịch học.</p>
          <p className={styles.timezone}>Thời gian theo múi giờ {timezone}</p>
        </div>
      </header>

      {loadError && (
        <Alert
          className={styles.loadError}
          title="Chưa tải được lịch rảnh"
          description="Tải lại trang trước khi chỉnh sửa để tránh ghi đè lịch hiện có."
          type="error"
          showIcon
        />
      )}

      <Alert
        className={styles.notice}
        title="Lịch đã xác nhận không thay đổi"
        description="Các khung giờ mới chỉ áp dụng cho những lịch học được tạo sau khi bạn lưu."
        type="warning"
        showIcon
      />

      <section className={styles.schedule} aria-labelledby="weekly-availability-title">
        <div className={styles.scheduleHeading}>
          <div>
            <h2 id="weekly-availability-title">Khung giờ trong tuần</h2>
            <p>Chọn hoặc bỏ chọn từng giờ từ 08:00 đến 22:00.</p>
          </div>
          <div className={styles.legend} aria-label="Chú giải trạng thái">
            <span><i className={styles.legendSelected} />Đang rảnh</span>
            <span><i className={styles.legendUnselected} />Chưa chọn</span>
          </div>
        </div>

        {slots.length === 0 && <p className={styles.empty}>Chưa có khung giờ nào được chọn.</p>}

        {!isMobile ? (
          <div className={styles.tableScroll}>
            <table className={styles.weekTable}>
              <caption className={styles.visuallyHidden}>Lịch rảnh hàng tuần, múi giờ {timezone}</caption>
              <thead>
                <tr><th scope="col">Giờ</th>{DAYS.map(day => <th key={day.id} scope="col">{day.label}</th>)}</tr>
              </thead>
              <tbody>
                {HOURS.map(hour => (
                  <tr key={hour}>
                    <th scope="row">{timeLabel(hour)}</th>
                    {DAYS.map(day => <td key={day.id}>{renderSlot(day, hour)}</td>)}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className={styles.dayList}>
            {DAYS.map(day => (
              <section key={day.id} className={styles.daySection} aria-labelledby={`availability-day-${day.id}`}>
                <h3 id={`availability-day-${day.id}`}>{day.label}</h3>
                <div className={styles.mobileSlots}>
                  {HOURS.map(hour => renderSlot(day, hour))}
                </div>
              </section>
            ))}
          </div>
        )}
      </section>

      <footer className={styles.footer}>
        <span>{slots.length} khoảng thời gian trong lịch</span>
        <Button className={styles.saveButton} type="primary" onClick={handleSave} loading={saving} disabled={loadError}>
          Lưu lịch rảnh
        </Button>
      </footer>
    </main>
  );
};
