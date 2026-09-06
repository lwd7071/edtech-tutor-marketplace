import React from 'react';
import { render, screen } from '@testing-library/react';
import { SessionCounterSimple } from './SessionCounterSimple';

jest.mock('@ant-design/icons', () => ({
  QuestionCircleOutlined: () => <span data-testid="question-icon" />
}));

describe('SessionCounter Component', () => {
  const props = {
    remaining: 5,
    onHold: 2,
    completed: 3,
    total: 10, // Assuming total is completed + remaining + onHold, or provided
  };

  it('renders horizontal variant with all indicators', () => {
    render(<SessionCounterSimple {...props} variant="horizontal" />);
    expect(screen.getAllByText((content, element) => element?.textContent === 'Còn lại: 5')[0]).toBeInTheDocument();
    expect(screen.getAllByText((content, element) => element?.textContent === 'Đang giữ: 2 ')[0]).toBeInTheDocument();
    expect(screen.getAllByText((content, element) => element?.textContent === 'Đã học: 3')[0]).toBeInTheDocument();
    expect(screen.getAllByText((content, element) => element?.textContent === 'Tổng: 10')[0]).toBeInTheDocument();
  });

  it('renders compact variant correctly', () => {
    render(<SessionCounterSimple {...props} variant="compact" />);
    // compact might just show "5/10" or similar with a tooltip, let's say "5 Còn lại"
    expect(screen.getByText('5 / 10')).toBeInTheDocument();
  });
});
