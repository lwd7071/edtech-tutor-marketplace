import React from 'react';
import { render, screen } from '@testing-library/react';
import { SessionCounter } from './SessionCounter';

describe('SessionCounter (TDD)', () => {
  it('should render all 4 session counters with labels', () => {
    render(
      <SessionCounter
        remainingSessions={7}
        reservedSessions={1}
        completedSessions={2}
        refundedSessions={0}
      />
    );

    expect(screen.getByText('Còn lại')).toBeInTheDocument();
    expect(screen.getByText('7')).toBeInTheDocument();

    expect(screen.getByText('Đang giữ')).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument();

    expect(screen.getByText('Đã học')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();

    expect(screen.getByText('Đã hoàn')).toBeInTheDocument();
    expect(screen.getByText('0')).toBeInTheDocument();
  });
});
