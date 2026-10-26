'use client';
import {useState} from 'react';
import Link from 'next/link';
import {usePathname} from 'next/navigation';
import {Button, Drawer, Dropdown} from 'antd';
import {LoginOutlined, MenuOutlined, UserOutlined, SearchOutlined} from '@ant-design/icons';
import {useAuthStore, useLogout} from '@/features/auth';
import {roleHome} from '@/shared/lib/navigation';

export default function Navbar() {
  const [open, setOpen] = useState(false);
  const pathname = usePathname();
  const {user, isAuthenticated} = useAuthStore();
  const logout = useLogout();
  const home = roleHome(user?.role);

  const links = [
    {href: '/subjects', label: 'Tìm gia sư'},
    {href: '/become-a-tutor', label: 'Trở thành gia sư'},
    {href: '/how-it-works', label: 'Cách hoạt động'},
    {href: '/ranking', label: 'Xếp hạng'},
  ];

  const navigation = links.map(item => (
    <Link
      key={item.label}
      href={item.href}
      className="tm-nav-link"
      aria-current={pathname === item.href ? 'page' : undefined}
      onClick={() => setOpen(false)}
    >
      {item.label}
    </Link>
  ));

  const account = isAuthenticated && user ? (
    <Dropdown
      menu={{
        items: [
          {key: 'home', label: <Link href={home}>Không gian của tôi</Link>},
          ...(user.role !== 'ADMIN'
            ? [
                {key: 'profile', label: <Link href={home + '/profile'}>Hồ sơ</Link>},
                {key: 'notifications', label: <Link href={home + '/notifications'}>Thông báo</Link>},
              ]
            : []),
          {key: 'logout', label: 'Đăng xuất', danger: true, onClick: () => void logout()},
        ],
      }}
      trigger={['click']}
    >
      <Button className="tm-nav-user-btn" icon={<UserOutlined />}>
        {user.fullName}
      </Button>
    </Dropdown>
  ) : (
    <div className="tm-header-auth">
      <Link href="/auth/login" className="tm-nav-login-btn">
        <LoginOutlined aria-hidden="true" />
        <span>Đăng nhập</span>
      </Link>
      <Link href="/auth/register" className="tm-nav-cta-btn">Bắt đầu học</Link>
    </div>
  );

  return (
    <header className="tm-header">
      <div className="tm-container tm-header-inner">
        <div className="tm-header-left">
          <Link href="/" className="tm-brand" aria-label="tutormatch">
            <span>tutor<strong>match</strong></span>
          </Link>
          <nav className="tm-desktop-nav" aria-label="Điều hướng chính">
            {navigation}
          </nav>
        </div>

        <div className="tm-header-right">
          <Link href="/subjects" className="tm-header-search" aria-label="Tìm môn học hoặc gia sư"><SearchOutlined /></Link>
          <div className="tm-desktop-nav-account">
            {account}
          </div>
          <Button
            className="tm-mobile-only"
            aria-label="Mở menu"
            icon={<MenuOutlined />}
            onClick={() => setOpen(true)}
          />
        </div>

        <Drawer title="tutormatch" open={open} onClose={() => setOpen(false)}>
          <div className="tm-drawer-content">
            <nav className="tm-workspace-nav">
              {navigation}
            </nav>
            <div className="tm-drawer-account">
              {account}
            </div>
            <div className="tm-drawer-footer">
              <Link href="/support" onClick={() => setOpen(false)}>Trợ giúp & Hỗ trợ</Link>
            </div>
          </div>
        </Drawer>
      </div>
    </header>
  );
}
