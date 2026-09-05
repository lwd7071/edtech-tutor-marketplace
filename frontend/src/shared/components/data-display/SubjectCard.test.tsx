import { render, screen } from '@testing-library/react';
import SubjectCard from './SubjectCard';

const defaultProps = {
  id: 's1',
  name: 'Toán học',
  description: 'Môn toán từ cơ bản đến nâng cao',
  teacherCount: 42,
  onClick: jest.fn(),
};

describe('SubjectCard', () => {
  it('renders correctly', () => {
    render(<SubjectCard {...defaultProps} />);
    
    expect(screen.getByText('Toán học')).toBeInTheDocument();
    expect(screen.getByText('Môn toán từ cơ bản đến nâng cao')).toBeInTheDocument();
    expect(screen.getByText('42 giáo viên')).toBeInTheDocument();
  });

  it('handles 0 teachers', () => {
    render(<SubjectCard {...defaultProps} teacherCount={0} />);
    expect(screen.getByText('Chưa có giáo viên')).toBeInTheDocument();
  });
});
