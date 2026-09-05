import React from 'react';
import dayjs from 'dayjs';
import 'dayjs/locale/vi';
import relativeTime from 'dayjs/plugin/relativeTime';
import timezone from 'dayjs/plugin/timezone';
import utc from 'dayjs/plugin/utc';

dayjs.extend(utc);
dayjs.extend(timezone);
dayjs.extend(relativeTime);
dayjs.locale('vi');
dayjs.tz.setDefault('Asia/Ho_Chi_Minh');

export type DateTimeVariant = 'date' | 'time' | 'full' | 'range' | 'relative';

export interface DateTimeTextProps extends React.HTMLAttributes<HTMLSpanElement> {
  value: string | Date;
  endDate?: string | Date; // for range variant
  variant?: DateTimeVariant;
}

const mapDayOfWeek = (day: number) => {
  return day === 0 ? 'Chủ nhật' : `Thứ ${day + 1}`;
};

export const DateTimeText: React.FC<DateTimeTextProps> = ({
  value,
  endDate,
  variant = 'full',
  style,
  ...props
}) => {
  const dt = dayjs(value).tz('Asia/Ho_Chi_Minh');

  let text = '';

  switch (variant) {
    case 'date':
      text = dt.format('DD/MM/YYYY');
      break;
    case 'time':
      text = dt.format('HH:mm');
      break;
    case 'full':
      text = dt.format('HH:mm, DD/MM/YYYY');
      break;
    case 'range':
      if (endDate) {
        const endDt = dayjs(endDate).tz('Asia/Ho_Chi_Minh');
        // e.g. 19:00 – 20:30 · Thứ 5, 20/08/2026
        const dow = mapDayOfWeek(dt.day());
        text = `${dt.format('HH:mm')} \u2013 ${endDt.format('HH:mm')} \u00b7 ${dow}, ${dt.format('DD/MM/YYYY')}`;
      } else {
        text = dt.format('HH:mm, DD/MM/YYYY');
      }
      break;
    case 'relative':
      const diffDays = Math.abs(dayjs().diff(dt, 'day'));
      if (diffDays > 7) {
        text = dt.format('DD/MM/YYYY');
      } else {
        text = dt.fromNow();
      }
      break;
    default:
      text = dt.format('DD/MM/YYYY');
  }

  return (
    <span
      style={{
        color: 'var(--color-text-secondary)',
        ...style,
      }}
      {...props}
    >
      {text}
    </span>
  );
};
