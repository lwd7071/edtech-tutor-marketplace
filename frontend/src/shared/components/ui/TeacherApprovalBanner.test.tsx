import { render, screen } from '@testing-library/react';
import TeacherApprovalBanner from './TeacherApprovalBanner';

describe('TeacherApprovalBanner', () => {
  it('renders DRAFT state correctly', () => {
    render(<TeacherApprovalBanner status="DRAFT" onSubmit={jest.fn()} />);
    expect(screen.getByText(/Gửi hồ sơ duyệt/)).toBeInTheDocument();
  });

  it('renders PENDING_APPROVAL state correctly', () => {
    render(<TeacherApprovalBanner status="PENDING_APPROVAL" submittedAt="2026-09-05T08:00:00Z" />);
    expect(screen.getByText(/Đang chờ xét duyệt/)).toBeInTheDocument();
    // It should render the date part
    expect(screen.getByText(/đã gửi lúc/i)).toBeInTheDocument();
  });

  it('renders REJECTED state correctly', () => {
    render(
      <TeacherApprovalBanner 
        status="REJECTED" 
        rejectionReason="Ảnh CMND mờ" 
        onEdit={jest.fn()} 
      />
    );
    expect(screen.getByText(/Hồ sơ bị từ chối/i)).toBeInTheDocument();
    expect(screen.getByText(/Ảnh CMND mờ/)).toBeInTheDocument();
    expect(screen.getByText(/Chỉnh sửa và gửi lại/)).toBeInTheDocument();
  });

  it('renders nothing for APPROVED state', () => {
    const { container } = render(<TeacherApprovalBanner status="APPROVED" />);
    expect(container.firstChild).toBeNull();
  });
});
