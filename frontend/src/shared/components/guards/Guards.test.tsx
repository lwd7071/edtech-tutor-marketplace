import React from 'react';
import { render, screen } from '@testing-library/react';
import { AuthGuard } from './AuthGuard';
import { RoleGuard } from './RoleGuard';
import { TeacherApprovalGuard } from './TeacherApprovalGuard';
import { useAuthStore } from '@/features/auth';

const mockPush = jest.fn();
const mockReplace = jest.fn();
jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, replace: mockReplace }),
  usePathname: () => '/student/bookings',
}));

describe('Route Guards & Access Control (TDD)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    useAuthStore.getState().clear();
  });

  describe('AuthGuard', () => {
    it('should redirect to /auth/login with redirect param when unauthenticated', () => {
      render(
        <AuthGuard>
          <div>Nội dung được bảo vệ</div>
        </AuthGuard>
      );

      expect(mockPush).toHaveBeenCalledWith('/auth/login?redirect=%2Fstudent%2Fbookings');
      expect(screen.queryByText('Nội dung được bảo vệ')).not.toBeInTheDocument();
    });

    it('should render children when user is authenticated', () => {
      useAuthStore.getState().establish({ user: {
        id: '1',
        email: 'user@test.com',
        fullName: 'Test User',
        role: 'STUDENT',
        status: 'ACTIVE',
        avatarUrl: null,
      }, accessToken: 'test-token' });

      render(
        <AuthGuard>
          <div>Nội dung được bảo vệ</div>
        </AuthGuard>
      );

      expect(screen.getByText('Nội dung được bảo vệ')).toBeInTheDocument();
      expect(mockPush).not.toHaveBeenCalled();
    });
  });

  describe('RoleGuard', () => {
    it('should block access when user role is not in allowedRoles', () => {
      useAuthStore.getState().establish({ user: {
        id: '1',
        email: 'student@test.com',
        fullName: 'Student User',
        role: 'STUDENT',
        status: 'ACTIVE',
        avatarUrl: null,
      }, accessToken: 'test-token' });

      render(
        <RoleGuard allowedRoles={['TEACHER', 'ADMIN']}>
          <div>Khu vực Giáo viên & Admin</div>
        </RoleGuard>
      );

      expect(mockReplace).toHaveBeenCalledWith('/forbidden');
      expect(screen.getByText('Đang chuyển đến trang báo lỗi quyền truy cập')).toBeInTheDocument();
      expect(screen.queryByText('Khu vực Giáo viên & Admin')).not.toBeInTheDocument();
    });

    it('should allow access when user role is in allowedRoles', () => {
      useAuthStore.getState().establish({ user: {
        id: '2',
        email: 'teacher@test.com',
        fullName: 'Teacher User',
        role: 'TEACHER',
        status: 'APPROVED',
        avatarUrl: null,
      }, accessToken: 'test-token' });

      render(
        <RoleGuard allowedRoles={['TEACHER']}>
          <div>Khu vực Giáo viên</div>
        </RoleGuard>
      );

      expect(screen.getByText('Khu vực Giáo viên')).toBeInTheDocument();
    });
  });

  describe('TeacherApprovalGuard', () => {
    it('should show pending approval warning when teacher status is PENDING', () => {
      useAuthStore.getState().establish({ user: {
        id: '2',
        email: 'teacher@test.com',
        fullName: 'Teacher User',
        role: 'TEACHER',
        status: 'PENDING',
        avatarUrl: null,
      }, accessToken: 'test-token' });

      render(
        <TeacherApprovalGuard>
          <div>Trang tạo gói học & Lịch dạy</div>
        </TeacherApprovalGuard>
      );

      expect(screen.getByText(/Hồ sơ đang chờ phê duyệt/i)).toBeInTheDocument();
      expect(screen.queryByText('Trang tạo gói học & Lịch dạy')).not.toBeInTheDocument();
    });

    it('should render children when teacher is APPROVED', () => {
      useAuthStore.getState().establish({ user: {
        id: '2',
        email: 'teacher@test.com',
        fullName: 'Teacher User',
        role: 'TEACHER',
        status: 'APPROVED',
        avatarUrl: null,
      }, accessToken: 'test-token' });

      render(
        <TeacherApprovalGuard>
          <div>Trang tạo gói học & Lịch dạy</div>
        </TeacherApprovalGuard>
      );

      expect(screen.getByText('Trang tạo gói học & Lịch dạy')).toBeInTheDocument();
    });
  });
});
