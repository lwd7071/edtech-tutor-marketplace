import React from 'react';
import { render, screen, waitFor, act, fireEvent } from '@testing-library/react';
import SubjectsPage from './page';
import { teacherApi } from '@/shared/api/teacher';

// Mock dependencies
jest.mock('@/shared/api/teacher', () => ({
  teacherApi: {
    getSubjects: jest.fn(),
    addSubject: jest.fn(),
    deleteSubject: jest.fn(),
    searchPublicSubjects: jest.fn(),
  },
}));

jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: jest.fn(),
  }),
}));

const mockSubjects = [
  {
    id: 'ts-1',
    subjectId: 'sub-1',
    name: 'Toán học',
    category: 'Cấp 3',
  },
];

const mockPublicSubjects = [
  { id: 'sub-2', name: 'Vật lý', category: 'Cấp 3' },
];

jest.mock('./SubjectSelector', () => {
  return function MockSubjectSelector({ onAdd }: any) {
    return (
      <button onClick={() => onAdd('sub-2')}>Mock Thêm môn</button>
    );
  };
});

describe('Teacher Subjects Page', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (teacherApi.getSubjects as jest.Mock).mockResolvedValue(mockSubjects);
  });

  it('renders subject list with fetched data', async () => {
    await act(async () => {
      render(<SubjectsPage />);
    });

    expect(screen.queryByText('Đang tải danh sách môn học...')).not.toBeInTheDocument();
    expect(screen.getByText('Toán học')).toBeInTheDocument();
  });

  it('allows adding a new subject', async () => {
    (teacherApi.addSubject as jest.Mock).mockResolvedValue({
      id: 'ts-2',
      subjectId: 'sub-2',
      name: 'Vật lý',
      category: 'Cấp 3',
    });

    await act(async () => {
      render(<SubjectsPage />);
    });

    const mockAddButton = screen.getByRole('button', { name: /mock thêm môn/i });
    
    await act(async () => {
      fireEvent.click(mockAddButton);
    });

    await waitFor(() => {
      expect(teacherApi.addSubject).toHaveBeenCalledWith('sub-2');
    });

    expect(screen.getAllByText('Vật lý').length).toBeGreaterThan(0);
  });

  it('allows deleting a subject', async () => {
    (teacherApi.deleteSubject as jest.Mock).mockResolvedValue({});

    await act(async () => {
      render(<SubjectsPage />);
    });

    const deleteButtons = screen.getAllByRole('button', { name: /xóa/i });
    expect(deleteButtons.length).toBeGreaterThan(0);

    await act(async () => {
      fireEvent.click(deleteButtons[0]);
    });

    const okButton = await screen.findByRole('button', { name: /đồng ý/i });
    
    await act(async () => {
      fireEvent.click(okButton);
    });

    await waitFor(() => {
      expect(teacherApi.deleteSubject).toHaveBeenCalledWith('ts-1');
    });

    expect(screen.queryByText('Toán học')).not.toBeInTheDocument();
  });
});
