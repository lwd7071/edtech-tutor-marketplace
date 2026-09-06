'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { Button, Avatar, Dropdown, Badge, Drawer, Menu } from 'antd';
import { MenuOutlined, BellOutlined, UserOutlined, SettingOutlined, LogoutOutlined } from '@ant-design/icons';
import { useAuthStore } from '@/features/auth';

const NAV_MENU = [
  { key: 'tutors', label: <Link href="/teachers">Tìm gia sư</Link> },
  { key: 'subjects', label: <Link href="/subjects">Môn học</Link> },
  { key: 'ranking', label: <Link href="/ranking">Bảng xếp hạng</Link> },
];

export default function Navbar() {
  const { isAuthenticated, user, logout } = useAuthStore();
  const [scrolled, setScrolled] = useState(false);
  const [drawerVisible, setDrawerVisible] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 8);
    };
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const userMenu = {
    items: [
      {
        key: 'profile',
        icon: <UserOutlined />,
        label: <Link href="/student/profile">Hồ sơ</Link>,
      },
      {
        key: 'settings',
        icon: <SettingOutlined />,
        label: <Link href="/student/settings">Cài đặt</Link>,
      },
      { type: 'divider' as const },
      {
        key: 'logout',
        icon: <LogoutOutlined />,
        label: 'Đăng xuất',
        danger: true,
        onClick: logout,
      },
    ],
  };

  return (
    <header
      style={{
        position: 'sticky',
        top: 0,
        height: 'var(--size-header)',
        backgroundColor: 'var(--color-surface)',
        borderBottom: 'var(--border-default)',
        boxShadow: scrolled ? 'var(--shadow-sm)' : 'none',
        zIndex: 'var(--z-header)',
        transition: 'box-shadow var(--duration-base) var(--ease-standard)',
        display: 'flex',
        alignItems: 'center',
        padding: '0 var(--space-6)',
      }}
    >
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 'var(--space-8)' }}>
        <Link href="/" style={{ fontSize: 'var(--text-h3)', fontWeight: 'var(--weight-bold)', color: 'var(--color-primary-600)', textDecoration: 'none' }}>
          Edtech Tutor
        </Link>
        <div className="desktop-menu" style={{ display: 'flex', gap: 'var(--space-6)' }}>
          {NAV_MENU.map((item) => (
            <div key={item.key} style={{ color: 'var(--color-text-primary)', fontWeight: 'var(--weight-medium)', fontSize: 'var(--text-nav)' }}>
              {item.label}
            </div>
          ))}
        </div>
      </div>

      <div className="desktop-auth" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-4)' }}>
        {isAuthenticated ? (
          <>
            <Badge dot color="var(--color-error-600)">
              <Button type="text" icon={<BellOutlined style={{ fontSize: 20 }} />} style={{ width: 40, height: 40 }} />
            </Badge>
            <Dropdown menu={userMenu} placement="bottomRight" trigger={['click']}>
              <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
                <Avatar src={user?.avatarUrl} icon={<UserOutlined />} />
                <span style={{ fontWeight: 'var(--weight-medium)', display: 'none' }} className="user-name">
                  {user?.fullName}
                </span>
              </div>
            </Dropdown>
          </>
        ) : (
          <>
            <Link href="/auth/login"><Button type="text">Đăng nhập</Button></Link>
            <Link href="/auth/register"><Button type="primary">Đăng ký</Button></Link>
          </>
        )}
      </div>

      <Button
        className="mobile-toggle"
        type="text"
        icon={<MenuOutlined />}
        onClick={() => setDrawerVisible(true)}
        style={{ display: 'none', marginLeft: 'var(--space-4)' }}
      />

      <Drawer
        title="Menu"
        placement="right"
        onClose={() => setDrawerVisible(false)}
        open={drawerVisible}
        size="default"
      >
        <Menu mode="vertical" items={NAV_MENU} style={{ border: 'none' }} />
      </Drawer>

      <style>{`
        @media (max-width: 768px) {
          .desktop-menu { display: none !important; }
          .desktop-auth { display: none !important; }
          .mobile-toggle { display: block !important; }
        }
        @media (min-width: 769px) {
          .user-name { display: block !important; }
        }
      `}</style>
    </header>
  );
}
