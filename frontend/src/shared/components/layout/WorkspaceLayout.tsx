'use client';

import { useRef, useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Button, Drawer } from 'antd';
import { MenuOutlined, LogoutOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import { useAuthStore, useLogout, type UserRole } from '@/features/auth';
import { isWorkspaceLinkActive, teacherWorkspaceGroups, workspaceLinks, type WorkspaceLink } from '@/shared/lib/navigation';

export default function WorkspaceLayout({ role, children }: { role: UserRole; children: React.ReactNode }) {
  const [open, setOpen] = useState(false);
  const menuButtonRef = useRef<HTMLButtonElement>(null);
  const pathname = usePathname();
  const user = useAuthStore(state => state.user);
  const logout = useLogout();
  const links = workspaceLinks[role];
  const active = [...links].sort((a, b) => b.href.length - a.href.length)
    .find(item => isWorkspaceLinkActive(pathname, item.href));

  const navLink = (item: WorkspaceLink) =>
    <Link key={item.href} href={item.href} onClick={() => setOpen(false)}
      aria-current={active?.href === item.href ? 'page' : undefined}>{item.label}</Link>;
  const navigation = role === 'TEACHER' ? teacherWorkspaceGroups.map(group =>
    <div className="tm-workspace-nav-group" key={group.label}>
      {!(group.items.length === 1 && group.items[0].label === group.label) &&
        <p className="tm-workspace-nav-title">{group.label}</p>}
      <div className="tm-workspace-nav-items">{group.items.map(navLink)}</div>
    </div>) : links.map(navLink);
  const nav = <>
    <Link className="tm-brand" href="/">tutor<span>match</span><span className="tm-brand-dot">.</span></Link>
    <p className="tm-eyebrow">{role === 'STUDENT' ? 'Không gian học tập' : role === 'TEACHER' ? 'Không gian gia sư' : 'Quản trị hệ thống'}</p>
    <nav aria-label="Điều hướng không gian làm việc" className="tm-workspace-nav">{navigation}</nav>
    <Link className="tm-back" href="/"><ArrowLeftOutlined /> Khám phá gia sư</Link>
  </>;

  return <div className="tm-workspace">
    <aside className="tm-sidebar">{nav}</aside>
    <Drawer title="Điều hướng" open={open} onClose={() => setOpen(false)} placement="left"
      afterOpenChange={visible => { if (!visible) menuButtonRef.current?.focus(); }}>
      {nav}
    </Drawer>
    <div className="tm-workspace-main">
      <header className="tm-workspace-header">
        <div className="tm-inline">
          <Button ref={menuButtonRef} className="tm-mobile-only" aria-label="Mở menu" icon={<MenuOutlined />} onClick={() => setOpen(true)} />
          <strong>{active?.label || 'Không gian làm việc'}</strong>
        </div>
        <div className="tm-inline"><span>{user?.fullName}</span><Button aria-label="Đăng xuất" title="Đăng xuất" icon={<LogoutOutlined />} onClick={() => void logout()} /></div>
      </header>
      <main className="tm-workspace-content">{children}</main>
    </div>
  </div>;
}
