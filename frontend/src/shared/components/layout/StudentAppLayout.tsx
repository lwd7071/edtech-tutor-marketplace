'use client';

import React from 'react';
import type { MenuProps } from 'antd';
import { Layout, Menu, Typography, Space, Button } from 'antd';
import {
  DashboardOutlined,
  AppstoreOutlined,
  CalendarOutlined,
  BookOutlined,
  MessageOutlined,
  UserOutlined,
  HomeOutlined,
  BellOutlined,
  FileTextOutlined,
} from '@ant-design/icons';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { NotificationBell } from '@/features/notifications/components/NotificationBell';

const { Header, Sider, Content } = Layout;

interface StudentAppLayoutProps {
  children: React.ReactNode;
}

export const StudentAppLayout: React.FC<StudentAppLayoutProps> = ({ children }) => {
  const pathname = usePathname();

  const menuItems: MenuProps['items'] = [
    {
      type: 'group',
      label: 'HỌC TẬP',
      children: [
        {
          key: '/student',
          icon: <DashboardOutlined />,
          label: <Link href="/student">Tổng quan</Link>,
        },
        {
          key: '/student/packages',
          icon: <AppstoreOutlined />,
          label: <Link href="/student/packages">Gói học</Link>,
        },
        {
          key: '/student/bookings',
          icon: <CalendarOutlined />,
          label: <Link href="/student/bookings">Lịch học</Link>,
        },
        {
          key: '/student/assignments',
          icon: <BookOutlined />,
          label: <Link href="/student/assignments">Bài tập</Link>,
        },
      ]
    },
    {
      type: 'group',
      label: 'TRAO ĐỔI',
      children: [
        {
          key: '/student/messages',
          icon: <MessageOutlined />,
          label: <Link href="/student/messages">Tin nhắn</Link>,
        },
        {
          key: '/student/notifications',
          icon: <BellOutlined />,
          label: <Link href="/student/notifications">Thông báo</Link>,
        },
      ]
    },
    {
      type: 'group',
      label: 'YÊU CẦU',
      children: [
        {
          key: '/student/requests',
          icon: <FileTextOutlined />,
          label: <Link href="/student/requests">Yêu cầu của tôi</Link>,
        },
      ]
    },
    {
      type: 'group',
      label: 'TÀI KHOẢN',
      children: [
        {
          key: '/student/profile',
          icon: <UserOutlined />,
          label: <Link href="/student/profile">Hồ sơ</Link>,
        },
      ]
    },
  ];

  // Tìm active menu key ưu tiên path dài nhất
  const flatKeys = (menuItems || []).flatMap((g: any) => g?.children?.map((c: any) => c.key) || []);
  const selectedKey = [...flatKeys].sort((a, b) => b.length - a.length).find(k => pathname === k || (k !== '/student' && pathname.startsWith(k))) || '/student';

  return (
    <Layout style={{ minHeight: '100vh', backgroundColor: 'var(--color-background, #FBFAF8)' }}>
      {/* Sider cố định 248px theo SPEC-FE:4.1 & 9.3 */}
      <Sider
        width={248}
        style={{
          overflow: 'auto',
          height: '100vh',
          position: 'fixed',
          left: 0,
          top: 0,
          bottom: 0,
          borderRight: '1px solid var(--color-border, #E7E3DC)',
          backgroundColor: 'var(--color-surface, #FFFFFF)',
          zIndex: 200,
        }}
        breakpoint="lg"
        collapsedWidth="0"
      >
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            padding: '0 20px',
            borderBottom: '1px solid var(--color-border, #E7E3DC)',
          }}
        >
          <Typography.Title level={4} style={{ margin: 0, color: 'var(--color-primary-600, #0F766E)' }}>
            EdTech Student
          </Typography.Title>
        </div>

        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          style={{ borderRight: 'none', marginTop: 12 }}
        />

        <div
          style={{
            position: 'absolute',
            bottom: 16,
            left: 16,
            right: 16,
            paddingTop: 16,
            borderTop: '1px solid var(--color-border, #E7E3DC)',
          }}
        >
          <Link href="/">
            <Button icon={<HomeOutlined />} block type="text">
              Về Trang chủ
            </Button>
          </Link>
        </div>
      </Sider>

      {/* Main Content Area */}
      <Layout style={{ marginLeft: 248, minHeight: '100vh', backgroundColor: 'transparent' }}>
        <Header
          style={{
            height: 64,
            padding: '0 32px',
            backgroundColor: 'var(--color-surface, #FFFFFF)',
            borderBottom: '1px solid var(--color-border, #E7E3DC)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            position: 'sticky',
            top: 0,
            zIndex: 100,
          }}
        >
          <Typography.Text strong style={{ fontSize: 16 }}>
            Khu vực Học sinh
          </Typography.Text>
          <NotificationBell />
        </Header>

        <Content
          style={{
            padding: '24px 32px 48px',
            maxWidth: 1280,
            width: '100%',
          }}
        >
          {children}
        </Content>
      </Layout>
    </Layout>
  );
};
