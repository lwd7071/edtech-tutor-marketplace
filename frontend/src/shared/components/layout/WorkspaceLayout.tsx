'use client';

import { useRef, useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Button, Drawer } from 'antd';
import { MenuOutlined, LogoutOutlined, ArrowLeftOutlined, HomeOutlined, CalendarOutlined, TeamOutlined, MessageOutlined, AppstoreOutlined } from '@ant-design/icons';
import { useAuthStore, useLogout, type UserRole } from '@/features/auth';
import { isWorkspaceLinkActive, workspaceLinks, type WorkspaceLink } from '@/shared/lib/navigation';

export default function WorkspaceLayout({ role, children }: { role: UserRole; children: React.ReactNode }) {
  const [open, setOpen] = useState(false);
  const menuButtonRef = useRef<HTMLButtonElement>(null);
  const pathname = usePathname();
  const user = useAuthStore(state => state.user);
  const logout = useLogout();
  const links = workspaceLinks[role];
  const priorityHrefs = role === 'TEACHER'
    ? ['/teacher', '/teacher/bookings', '/teacher/students', '/teacher/messages']
    : role === 'STUDENT'
      ? ['/student', '/student/bookings', '/student/assignments', '/student/messages']
      : ['/admin', '/admin/teachers', '/admin/subjects', '/admin/refunds'];
  const priority = priorityHrefs.map(href => links.find(item => item.href === href)).filter((item): item is WorkspaceLink => Boolean(item));
  const remaining = links.filter(item => !priorityHrefs.includes(item.href));
  const active = [...links].sort((a, b) => b.href.length - a.href.length)
    .find(item => isWorkspaceLinkActive(pathname, item.href));

  const navLink = (item: WorkspaceLink) =>
    <Link key={item.href} href={item.href} onClick={() => setOpen(false)}
      aria-current={active?.href === item.href ? 'page' : undefined}>{item.label}</Link>;
  const navigation = <>{priority.map(navLink)}<details className="tm-workspace-more" open={remaining.some(item => item.href === active?.href) ? true : undefined}>
    <summary>Thêm chức năng <span aria-hidden="true">⌄</span></summary>
    <div className="tm-workspace-more-list">{remaining.map(navLink)}</div>
  </details></>;
  const nav = <>
    <Link className="tm-brand" href="/">tutormatch</Link>
    <p className="tm-workspace-role">{role === 'STUDENT' ? 'Học viên' : role === 'TEACHER' ? 'Gia sư' : 'Quản trị viên'}</p>
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
    <nav className="tm-workspace-bottom" aria-label="Điều hướng nhanh">
      {priority.map((item, index) => <Link key={item.href} href={item.href} aria-current={active?.href === item.href ? 'page' : undefined}>
        {[<HomeOutlined key="home" />, <CalendarOutlined key="calendar" />, <TeamOutlined key="team" />, <MessageOutlined key="message" />][index]}
        <span>{item.label}</span>
      </Link>)}
      <button type="button" onClick={() => setOpen(true)} aria-label="Mở thêm chức năng"><AppstoreOutlined /><span>Thêm</span></button>
    </nav>
  </div>;
}
