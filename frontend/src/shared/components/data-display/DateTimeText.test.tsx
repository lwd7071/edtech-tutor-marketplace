import React from 'react';
import { render, screen } from '@testing-library/react';
import { DateTimeText } from './DateTimeText';
import dayjs from 'dayjs';
import customParseFormat from 'dayjs/plugin/customParseFormat';
dayjs.extend(customParseFormat);

describe('DateTimeText Component', () => {
  const testDate = '2026-08-20T19:00:00+07:00'; // Thứ 5, 20/08/2026, 19:00 (Asia/Ho_Chi_Minh)
  const testDateEnd = '2026-08-20T20:30:00+07:00'; // 20:30

  it('renders date variant correctly', () => {
    render(<DateTimeText value={testDate} variant="date" />);
    expect(screen.getByText('20/08/2026')).toBeInTheDocument();
  });

  it('renders time variant correctly', () => {
    render(<DateTimeText value={testDate} variant="time" />);
    expect(screen.getByText('19:00')).toBeInTheDocument();
  });

  it('renders full variant correctly', () => {
    render(<DateTimeText value={testDate} variant="full" />);
    expect(screen.getByText('19:00, 20/08/2026')).toBeInTheDocument();
  });

  it('renders range variant correctly', () => {
    render(<DateTimeText value={testDate} endDate={testDateEnd} variant="range" />);
    // "19:00 - 20:30 · Thứ 5, 20/08/2026"
    // Actually the spec says "19:00 – 20:30 · Thứ 4, 20/08/2026". Note the En dash. 
    // And 20/08/2026 is Thursday (Thứ 5).
    expect(screen.getByText('19:00 \u2013 20:30 \u00b7 Thứ 5, 20/08/2026')).toBeInTheDocument();
  });

  it('renders relative variant correctly (within 7 days)', () => {
    const now = dayjs();
    const threeMinsAgo = now.subtract(3, 'minute').toISOString();
    render(<DateTimeText value={threeMinsAgo} variant="relative" />);
    expect(screen.getByText('3 phút trước')).toBeInTheDocument();
  });

  it('renders relative variant correctly (over 7 days)', () => {
    const now = dayjs();
    const tenDaysAgo = now.subtract(10, 'day').toISOString();
    render(<DateTimeText value={tenDaysAgo} variant="relative" />);
    // should fallback to date
    expect(screen.getByText(dayjs(tenDaysAgo).format('DD/MM/YYYY'))).toBeInTheDocument();
  });
});
