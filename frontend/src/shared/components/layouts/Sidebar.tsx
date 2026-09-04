'use client';

import { useState } from 'react';
import { Layout, Menu, Tooltip } from 'antd';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { 
  AppstoreOutlined, 
  CalendarOutlined, 
  BookOutlined, 
  WalletOutlined, 
  TeamOutlined, 
  MessageOutlined,
  LockOutlined
} from '@ant-design/icons';
import { useAuthStore } from '@/features/auth';

const { Sider } = Layout;

export default function Sidebar() {
  const [collapsed, setCollapsed] = useState(false);
  const pathname = usePathname();
  const { user } = useAuthStore();

  const isTeacher = user?.role === 'TEACHER';
  const isPendingTeacher = isTeacher && user?.status !== 'APPROVED'; // Spec: Teacher endpoints require APPROVED

  const studentItems = [
    { key: '/student/dashboard', icon: <AppstoreOutlined />, label: <Link href="/student/dashboard">Tổng quan</Link> },
    { key: '/student/packages', icon: <BookOutlined />, label: <Link href="/student/packages">Gói học</Link> },
    { key: '/student/schedule', icon: <CalendarOutlined />, label: <Link href="/student/schedule">Lịch học</Link> },
    { key: '/student/assignments', icon: <BookOutlined />, label: <Link href="/student/assignments">Bài tập</Link> },
    { key: '/student/chat', icon: <MessageOutlined />, label: <Link href="/student/chat">Tin nhắn</Link> },
  ];

  const teacherItems = [
    { key: '/teacher/dashboard', icon: <AppstoreOutlined />, label: <Link href="/teacher/dashboard">Tổng quan</Link> },
    { 
      key: '/teacher/packages', 
      icon: isPendingTeacher ? <LockOutlined /> : <BookOutlined />, 
      label: isPendingTeacher ? 'Gói học' : <Link href="/teacher/packages">Gói học</Link>,
      disabled: isPendingTeacher,
    },
    { 
      key: '/teacher/schedule', 
      icon: isPendingTeacher ? <LockOutlined /> : <CalendarOutlined />, 
      label: isPendingTeacher ? 'Lịch dạy' : <Link href="/teacher/schedule">Lịch dạy</Link>,
      disabled: isPendingTeacher,
    },
    { 
      key: '/teacher/students', 
      icon: isPendingTeacher ? <LockOutlined /> : <TeamOutlined />, 
      label: isPendingTeacher ? 'Học sinh' : <Link href="/teacher/students">Học sinh</Link>,
      disabled: isPendingTeacher,
    },
    { 
      key: '/teacher/wallet', 
      icon: isPendingTeacher ? <LockOutlined /> : <WalletOutlined />, 
      label: isPendingTeacher ? 'Ví thu nhập' : <Link href="/teacher/wallet">Ví thu nhập</Link>,
      disabled: isPendingTeacher,
    },
  ];

  const items = isTeacher ? teacherItems : studentItems;

  // Enhance items with tooltips if disabled
  const enhancedItems = items.map(item => {
    if ('disabled' in item && item.disabled) {
      return {
        ...item,
        label: (
          <Tooltip title="Hồ sơ đang chờ duyệt" placement="right">
            <span>{item.label}</span>
          </Tooltip>
        )
      };
    }
    return item;
  });

  return (
    <Sider
      collapsible
      collapsed={collapsed}
      onCollapse={(value) => setCollapsed(value)}
      width={248}
      collapsedWidth={72}
      style={{
        backgroundColor: 'var(--color-surface)',
        borderRight: 'var(--border-default)',
        height: '100vh',
        position: 'sticky',
        top: 0,
        left: 0,
      }}
      trigger={null} // We will use a custom trigger in TopHeader if needed, or Ant Design default
    >
      <div style={{ height: 'var(--size-header)', display: 'flex', alignItems: 'center', justifyContent: 'center', borderBottom: 'var(--border-default)' }}>
        <Link href="/" style={{ fontSize: 'var(--text-h3)', fontWeight: 'var(--weight-bold)', color: 'var(--color-primary-600)', textDecoration: 'none' }}>
          {collapsed ? 'ET' : 'Edtech Tutor'}
        </Link>
      </div>

      <Menu
        mode="inline"
        selectedKeys={[pathname]}
        items={enhancedItems}
        style={{ borderRight: 'none', padding: 'var(--space-3)' }}
      />
    </Sider>
  );
}
