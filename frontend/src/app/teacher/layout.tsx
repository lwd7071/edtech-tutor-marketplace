'use client';

import React, { useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useAuthStore } from '@/shared/store/useAuthStore';
import { teacherApi, TeacherProfile } from '@/shared/api/teacher';
import TeacherApprovalBanner from '@/shared/components/ui/TeacherApprovalBanner';
import { Spin, message, Layout, Menu } from 'antd';
import { 
  UserOutlined, 
  BookOutlined, 
  CalendarOutlined, 
  AppstoreOutlined,
  FileTextOutlined 
} from '@ant-design/icons';
import Link from 'next/link';

const { Sider, Content } = Layout;

export default function TeacherLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const { user } = useAuthStore();
  const [mounted, setMounted] = useState(false);
  
  const [profile, setProfile] = useState<TeacherProfile | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (!mounted) return;

    if (!user || user.role !== 'TEACHER') {
      router.push('/auth/login?redirect=' + encodeURIComponent(pathname));
      return;
    }

    const fetchStatus = async () => {
      try {
        const data = await teacherApi.getProfile();
        setProfile(data);
      } catch (error: unknown) {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        if ((error as any).response?.status === 404) {
          // New profile
          setProfile({ approvalStatus: 'DRAFT' });
        } else {
          message.error('Không thể kiểm tra trạng thái hồ sơ.');
        }
      } finally {
        setLoading(false);
      }
    };
    
    fetchStatus();
  }, [user, mounted, router, pathname]);

  const handleSubmit = async () => {
    try {
      await teacherApi.submitProfile();
      message.success('Đã gửi hồ sơ duyệt!');
      setProfile((prev) => prev ? { ...prev, approvalStatus: 'PENDING_APPROVAL' } : null);
    } catch (err) {
      message.error('Gửi hồ sơ thất bại.');
    }
  };

  const handleEdit = () => {
    router.push('/teacher/profile');
  };

  if (!mounted || loading) {
    return (
      <div style={{ height: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Spin size="large" />
      </div>
    );
  }

  const menuItems = [
    { key: '/teacher/profile', icon: <UserOutlined />, label: <Link href="/teacher/profile">Hồ sơ cá nhân</Link> },
    { key: '/teacher/availability', icon: <CalendarOutlined />, label: <Link href="/teacher/availability">Lịch rảnh</Link> },
    { key: '/teacher/packages', icon: <AppstoreOutlined />, label: <Link href="/teacher/packages">Gói học</Link> },
    { key: '/teacher/subjects', icon: <BookOutlined />, label: <Link href="/teacher/subjects">Môn học</Link> },
    { key: '/teacher/documents', icon: <FileTextOutlined />, label: <Link href="/teacher/documents">Hồ sơ chứng chỉ</Link> },
  ];

  return (
    <Layout className="teacher-layout" style={{ minHeight: 'calc(100vh - 64px)' }}>
      <Sider width={250} theme="light" className="border-r border-border">
        <Menu
          mode="inline"
          selectedKeys={[pathname]}
          style={{ height: '100%', borderRight: 0 }}
          items={menuItems}
        />
      </Sider>
      <Layout>
        <Content style={{ padding: '24px', maxWidth: 1200, margin: '0 auto', width: '100%' }}>
          {profile && (
            <TeacherApprovalBanner 
              status={profile.approvalStatus || 'DRAFT'} 
              rejectionReason={profile.rejectionReason}
              onSubmit={pathname === '/teacher/profile' ? undefined : handleSubmit}
              onEdit={pathname === '/teacher/profile' ? undefined : handleEdit}
            />
          )}
          
          <div className="teacher-content mt-6">
            {children}
          </div>
        </Content>
      </Layout>
    </Layout>
  );
}
