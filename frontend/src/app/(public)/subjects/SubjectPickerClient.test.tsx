import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { SubjectPickerClient } from './SubjectPickerClient';
import { useRouter } from 'next/navigation';

jest.mock('next/navigation', () => ({ useRouter: jest.fn() }));
jest.mock('next/link', () => {
  const MockLink = ({ children, href }: any) => <a href={href}>{children}</a>;
  MockLink.displayName = 'MockLink';
  return MockLink;
});

const subject = { id: 'subject-uuid', code: 'MATH', slug: 'toan', name: 'Toán', educationLevel: 'HIGH_SCHOOL', description: null };

describe('SubjectPickerClient', () => {
  const push = jest.fn();
  const refresh = jest.fn();
  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue({ push, refresh });
  });

  it('continues to filtered tutor results only after a subject is selected', () => {
    render(<SubjectPickerClient subjects={[subject]} keyword="" educationLevel="" currentPage={1} totalPages={1} hasError={false} />);
    const continueButton = screen.getByRole('button', { name: 'Tìm gia sư' });
    expect(continueButton).toBeDisabled();

    fireEvent.click(screen.getByRole('radio', { name: /Toán/ }));
    fireEvent.click(screen.getByRole('button', { name: /Tìm gia sư môn Toán/ }));

    expect(push).toHaveBeenLastCalledWith('/teachers?subjectId=subject-uuid');
  });

  it('shows a loading state while submitted search filters are being applied', () => {
    render(<SubjectPickerClient subjects={[subject]} keyword="" educationLevel="" currentPage={1} totalPages={1} hasError={false} />);

    fireEvent.change(screen.getByRole('searchbox', { name: 'Tìm môn học' }), { target: { value: 'Lập trình' } });
    fireEvent.click(screen.getByRole('button', { name: 'Tìm môn' }));

    expect(push).toHaveBeenCalledWith('/subjects?keyword=L%E1%BA%ADp+tr%C3%ACnh');
    expect(screen.getByRole('status')).toHaveTextContent('Đang tải danh sách môn học');
    expect(screen.queryByRole('group', { name: 'Danh sách môn học' })).not.toBeInTheDocument();
  });

  it('clears the selected subject when changing education level', () => {
    render(<SubjectPickerClient subjects={[subject]} selectedSubject={subject} keyword="" educationLevel="HIGH_SCHOOL" currentPage={1} totalPages={1} hasError={false} />);
    fireEvent.change(screen.getByRole('combobox', { name: 'Lọc theo cấp học' }), { target: { value: 'UNIVERSITY' } });

    expect(push).toHaveBeenCalledWith('/subjects?educationLevel=UNIVERSITY');
    expect(screen.getByRole('button', { name: 'Tìm gia sư' })).toBeDisabled();
  });

  it('opens the compact mobile subject search and level filters on demand', () => {
    render(<SubjectPickerClient subjects={[subject]} keyword="" educationLevel="" currentPage={1} totalPages={1} hasError={false} />);
    const toggle = screen.getByRole('button', { name: /Tìm kiếm hoặc lọc môn học/ });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    fireEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('searchbox', { name: 'Tìm môn học' })).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: 'Lọc theo cấp học' })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Danh sách môn học' })).toBeInTheDocument();
  });

  it('shows retry and a separate tutor-search path when the subject request fails', () => {
    render(<SubjectPickerClient subjects={[]} keyword="" educationLevel="" currentPage={1} totalPages={0} hasError />);

    expect(screen.getByText('Chưa tải được danh sách môn học')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Tải lại' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Xem tất cả gia sư' })).toHaveAttribute('href', '/teachers');
    expect(screen.queryByRole('group', { name: 'Danh sách môn học' })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Tải lại' }));
    expect(refresh).toHaveBeenCalledTimes(1);
  });

  it('shows the unavailable-subject warning without inserting fallback data', () => {
    render(<SubjectPickerClient subjects={[]} selectedUnavailable keyword="" educationLevel="" currentPage={1} totalPages={0} hasError={false} />);

    expect(screen.getByText('Môn học này hiện không còn trong danh mục.')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Chưa tìm thấy môn học phù hợp' })).toBeInTheDocument();
    expect(screen.queryByRole('radio')).not.toBeInTheDocument();
  });

  it('offers a clear-filter action when the result set is empty', () => {
    render(<SubjectPickerClient subjects={[]} keyword="Hóa hữu cơ" educationLevel="UNIVERSITY" currentPage={1} totalPages={0} hasError={false} />);

    fireEvent.click(screen.getByRole('button', { name: 'Xem tất cả môn học' }));

    expect(push).toHaveBeenCalledWith('/subjects');
    expect(screen.getByRole('status')).toHaveTextContent('Đang tải danh sách môn học');
  });

  it('keeps submitted filters, page and selected subject when paginating', () => {
    render(<SubjectPickerClient
      subjects={[subject]}
      selectedSubject={subject}
      keyword="Toán"
      educationLevel="HIGH_SCHOOL"
      currentPage={1}
      totalPages={2}
      hasError={false}
    />);

    fireEvent.click(screen.getByRole('button', { name: 'Trang sau' }));

    expect(push).toHaveBeenCalledWith('/subjects?keyword=To%C3%A1n&educationLevel=HIGH_SCHOOL&page=2&subjectId=subject-uuid');
  });
});
