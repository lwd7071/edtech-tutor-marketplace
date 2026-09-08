import React from 'react';
import { useMediaQuery } from 'react-responsive';

export interface TimeSlot {
  dayOfWeek: number; // 1 (Mon) - 7 (Sun)
  startTime: string; // '08:00'
  endTime: string; // '09:00'
  isBooked?: boolean;
}

interface WeeklyScheduleGridProps {
  mode: 'readonly' | 'editable' | 'booking-overlay';
  availableSlots: TimeSlot[];
  onSlotClick?: (day: number, hour: number) => void;
  className?: string;
}

const DAYS = [
  { id: 1, label: 'Thứ 2' },
  { id: 2, label: 'Thứ 3' },
  { id: 3, label: 'Thứ 4' },
  { id: 4, label: 'Thứ 5' },
  { id: 5, label: 'Thứ 6' },
  { id: 6, label: 'Thứ 7' },
  { id: 7, label: 'Chủ nhật' },
];

const HOURS = Array.from({ length: 15 }, (_, i) => i + 8); // 08:00 to 22:00

export default function WeeklyScheduleGrid({
  mode,
  availableSlots,
  onSlotClick,
  className = '',
}: WeeklyScheduleGridProps) {
  const isMobile = useMediaQuery({ maxWidth: 767 });

  const getSlotStatus = (day: number, hour: number) => {
    const timeStr = `${hour.toString().padStart(2, '0')}:00`;
    const slot = availableSlots.find(s => s.dayOfWeek === day && s.startTime <= timeStr && s.endTime > timeStr);
    
    if (!slot) return 'unavailable';
    if (slot.isBooked) return 'booked';
    return 'available';
  };

  // Simple agenda view for mobile (fallback UI)
  if (isMobile && mode !== 'editable') {
    return (
      <div className={`weekly-schedule agenda-view ${className}`}>
        <p className="agenda-notice">Phiên bản Mobile hiển thị dưới dạng danh sách (Agenda).</p>
        <div className="agenda-list">
          {DAYS.map(day => {
            const daySlots = availableSlots.filter(s => s.dayOfWeek === day.id);
            return (
              <div key={day.id} className="agenda-day">
                <div className="day-label">{day.label}</div>
                {daySlots.length > 0 ? (
                  <div className="day-slots">
                    {daySlots.map((slot, i) => (
                      <span key={i} className={`slot-badge ${slot.isBooked ? 'booked' : 'available'}`}>
                        {slot.startTime} - {slot.endTime}
                      </span>
                    ))}
                  </div>
                ) : (
                  <div className="no-slots">Không có lịch rảnh</div>
                )}
              </div>
            );
          })}
        </div>
        <style>{`
          .agenda-view {
            background: var(--color-surface);
            border: var(--border-default);
            border-radius: var(--radius-md);
            padding: var(--space-4);
          }
          .agenda-notice {
            color: var(--color-text-secondary);
            font-size: var(--text-caption);
            margin-bottom: var(--space-4);
          }
          .agenda-day {
            margin-bottom: var(--space-3);
            border-bottom: var(--border-default);
            padding-bottom: var(--space-2);
          }
          .day-label {
            font-weight: var(--weight-bold);
            margin-bottom: var(--space-2);
          }
          .day-slots {
            display: flex;
            flex-wrap: wrap;
            gap: var(--space-2);
          }
          .slot-badge {
            font-size: var(--text-caption);
            padding: 4px 8px;
            border-radius: var(--radius-sm);
            background: var(--color-primary-50);
            color: var(--color-primary-700);
          }
          .slot-badge.booked {
            background: var(--color-disabled-bg);
            color: var(--color-text-disabled);
            text-decoration: line-through;
          }
          .no-slots {
            font-size: var(--text-caption);
            color: var(--color-text-tertiary);
          }
        `}</style>
      </div>
    );
  }

  return (
    <div className={`weekly-schedule ${className}`}>
      <div className="schedule-grid">
        <div className="grid-header-corner">GMT+7</div>
        {DAYS.map(day => (
          <div key={day.id} className="grid-header-day">{day.label}</div>
        ))}

        {HOURS.map(hour => (
          <React.Fragment key={hour}>
            <div className="grid-time-label">
              {hour.toString().padStart(2, '0')}:00
            </div>
            {DAYS.map(day => {
              const status = getSlotStatus(day.id, hour);
              const isInteractive = mode === 'editable' || (mode === 'booking-overlay' && status === 'available');
              return (
                <div 
                  key={`${day.id}-${hour}`} 
                  className={`grid-cell ${status} ${isInteractive ? 'interactive' : ''}`}
                  onClick={() => {
                    if (isInteractive && onSlotClick) {
                      onSlotClick(day.id, hour);
                    }
                  }}
                >
                  {status === 'booked' && mode !== 'readonly' && <span className="booked-text">Đã đặt</span>}
                </div>
              );
            })}
          </React.Fragment>
        ))}
      </div>

      <style>{`
        .weekly-schedule {
          border: var(--border-default);
          border-radius: var(--radius-md);
          overflow-x: auto;
          overflow-y: hidden;
          background: var(--color-surface);
        }
        .schedule-grid {
          display: grid;
          grid-template-columns: 64px repeat(7, 1fr);
          min-width: 760px;
        }
        .grid-header-corner, .grid-header-day {
          background-color: var(--color-surface-sunken);
          padding: var(--space-2);
          font-size: var(--text-caption);
          font-weight: var(--weight-semibold);
          text-align: center;
          border-bottom: var(--border-default);
          border-right: var(--border-default);
        }
        .grid-time-label {
          background-color: var(--color-surface-sunken);
          padding: var(--space-2);
          font-size: var(--text-caption);
          text-align: center;
          border-bottom: var(--border-default);
          border-right: var(--border-default);
          display: flex;
          align-items: center;
          justify-content: center;
        }
        .grid-cell {
          border-bottom: var(--border-default);
          border-right: var(--border-default);
          min-height: 40px;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: background-color 0.1s;
        }
        .grid-cell:nth-child(8n) {
          border-right: none;
        }
        .grid-cell.available {
          background-color: var(--color-primary-100);
        }
        .grid-cell.booked {
          background-color: var(--color-disabled-bg);
          cursor: not-allowed;
        }
        .booked-text {
          font-size: 10px;
          color: var(--color-text-disabled);
        }
        .grid-cell.interactive:hover {
          background-color: var(--color-primary-50);
          cursor: pointer;
        }
        .grid-cell.interactive.available:hover {
          background-color: var(--color-primary-500);
        }
      `}</style>
    </div>
  );
}
