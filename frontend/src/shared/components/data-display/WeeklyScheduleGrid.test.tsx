import { render, screen } from '@testing-library/react';
import WeeklyScheduleGrid from './WeeklyScheduleGrid';

describe('WeeklyScheduleGrid', () => {
  it('renders readonly correctly', () => {
    render(<WeeklyScheduleGrid mode="readonly" availableSlots={[]} />);
    expect(screen.getByText('Thứ 2')).toBeInTheDocument();
    expect(screen.getByText('Chủ nhật')).toBeInTheDocument();
    expect(screen.getByText('08:00')).toBeInTheDocument();
  });
});
