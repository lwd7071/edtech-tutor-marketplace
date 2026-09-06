'use client';

import { Layout, Button, Dropdown, Avatar, Badge, Breadcrumb } from 'antd';
import { MenuOutlined, BellOutlined, UserOutlined, SettingOutlined, LogoutOutlined } from '@ant-design/icons';
import { useAuthStore } from '@/features/auth';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { NotificationBell } from '@/features/notifications/components/NotificationBell';

const { Header } = Layout;

interface TopHeaderProps {
  onMenuClick?: () => void;
}

export default function TopHeader({ onMenuClick }: TopHeaderProps) {
  const { user, logout } = useAuthStore();
  const pathname = usePathname();

  const userMenu = {
    items: [
      { key: 'profile', icon: <UserOutlined />, label: <Link href="/profile">Hồ sơ</Link> },
      { key: 'settings', icon: <SettingOutlined />, label: <Link href="/settings">Cài đặt</Link> },
      { type: 'divider' as const },
      { key: 'logout', icon: <LogoutOutlined style={{ color: 'var(--color-error-600)' }} />, label: <span style={{ color: 'var(--color-error-600)' }}>Đăng xuất</span>, onClick: logout },
    ],
  };

  const getBreadcrumb = () => {
    const paths = pathname.split('/').filter(p => p);
    if (paths.length === 0) return null;
    return (
      <Breadcrumb className="desktop-breadcrumb">
        {paths.map((p) => (
          <Breadcrumb.Item key={p} style={{ textTransform: 'capitalize' }}>
            {p}
          </Breadcrumb.Item>
        ))}
      </Breadcrumb>
    );
  };

  return (
    <Header
      style={{
        position: 'sticky',
        top: 0,
        zIndex: 'var(--z-header)',
        backgroundColor: 'var(--color-surface)',
        borderBottom: 'var(--border-default)',
        padding: '0 var(--space-4)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        height: 'var(--size-header)',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-4)' }}>
        <Button 
          type="text" 
          icon={<MenuOutlined />} 
          onClick={onMenuClick} 
          className="mobile-menu-toggle"
          aria-label="Toggle mobile menu"
        />
        {getBreadcrumb()}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-4)' }}>
        <NotificationBell />
        
        <Dropdown menu={userMenu} placement="bottomRight" trigger={['click']}>
          <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
            <Avatar src={user?.avatarUrl} icon={<UserOutlined />} />
            <div className="user-info" style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.2 }}>
              <span style={{ fontWeight: 'var(--weight-medium)', fontSize: 'var(--text-body-sm)' }}>{user?.fullName}</span>
              <span style={{ color: 'var(--color-text-tertiary)', fontSize: 'var(--text-caption)' }}>{user?.email}</span>
            </div>
          </div>
        </Dropdown>
      </div>

      <style>{`
        .mobile-menu-toggle { display: none !important; }
        @media (max-width: 768px) {
          .mobile-menu-toggle { display: block !important; }
          .desktop-breadcrumb { display: none !important; }
          .user-info { display: none !important; }
        }
      `}</style>
    </Header>
  );
}
