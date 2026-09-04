import React from 'react';
import { render, screen } from '@testing-library/react';
import { BookingCalendarView } from './BookingCalendarView';
import { BookingDetail } from '../types';

describe('BookingCalendarView (TDD)', () => {
  const mockBookings: BookingDetail[] = [
    {
      id: 'b-1',
      teacher: { id: 't-1', fullName: 'Trần Thu Hà' },
      student: { id: 's-1', fullName: 'Nguyễn Minh An' },
      studentPackageId: 'pkg-1',
      subject: { id: 'sub-1', name: 'Toán 11' },
      startTime: '2026-08-20T19:00:00Z',
      endTime: '2026-08-20T20:30:00Z',
      deliveryMode: 'ONLINE',
      meetingLink: 'https://meet.google.com/abc-def',
      status: 'SCHEDULED',
      trial: false,
      outsideAvailabilityWarning: false,
      version: 1,
    },
    {
      id: 'b-2',
      teacher: { id: 't-1', fullName: 'Trần Thu Hà' },
      student: { id: 's-1', fullName: 'Nguyễn Minh An' },
      studentPackageId: 'pkg-1',
      subject: { id: 'sub-1', name: 'Toán 11' },
      startTime: '2026-08-22T19:00:00Z',
      endTime: '2026-08-22T20:30:00Z',
      deliveryMode: 'ONLINE',
      status: 'COMPLETED',
      trial: true,
      outsideAvailabilityWarning: false,
      sessionReport: {
        content: 'Hoàn thành chương Dao động điều hòa',
        feedback: 'Học sinh hiểu bài tốt',
        teacherSelfRating: 5,
      },
      version: 2,
    },
  ];

  it('should render bookings list with subject, teacher, status tag and trial badge', () => {
    render(<BookingCalendarView bookings={mockBookings} isLoading={false} />);

    expect(screen.getAllByText('Toán 11').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Trần Thu Hà').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Đã lên lịch').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Hoàn thành').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Học thử')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Vào lớp học/i })).toHaveAttribute(
      'href',
      'https://meet.google.com/abc-def'
    );
  });
});
