'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Badge } from 'antd';
import { 
  AppstoreOutlined, 
  CalendarOutlined, 
  BookOutlined, 
  MessageOutlined,
  WalletOutlined,
  TeamOutlined,
  MenuOutlined
} from '@ant-design/icons';
import { useAuthStore } from '@/features/auth';

export default function BottomNavigation() {
  const pathname = usePathname();
  const { user } = useAuthStore();
  const isTeacher = user?.role === 'TEACHER';

  const studentItems = [
    { key: '/student/dashboard', icon: <AppstoreOutlined />, label: 'Tổng quan' },
    { key: '/student/packages', icon: <BookOutlined />, label: 'Gói học' },
    { key: '/student/schedule', icon: <CalendarOutlined />, label: 'Lịch học' },
    { key: '/student/assignments', icon: <BookOutlined />, label: 'Bài tập' },
    { key: '/student/chat', icon: <MessageOutlined />, label: 'Tin nhắn', hasBadge: true }, // mocked badge
  ];

  const teacherItems = [
    { key: '/teacher/dashboard', icon: <AppstoreOutlined />, label: 'Tổng quan' },
    { key: '/teacher/schedule', icon: <CalendarOutlined />, label: 'Lịch dạy' },
    { key: '/teacher/students', icon: <TeamOutlined />, label: 'Học sinh' },
    { key: '/teacher/wallet', icon: <WalletOutlined />, label: 'Ví' },
    { key: '/teacher/more', icon: <MenuOutlined />, label: 'Thêm', hasBadge: true }, // mocked badge
  ];

  const items = isTeacher ? teacherItems : studentItems;

  return (
    <>
      <nav className="bottom-nav">
        {items.map(item => {
          const isActive = pathname.startsWith(item.key);
          return (
            <Link key={item.key} href={item.key} className={`nav-item ${isActive ? 'active' : ''}`}>
              <Badge dot={item.hasBadge} color="var(--color-error-600)">
                <div className="icon">{item.icon}</div>
              </Badge>
              <div className="label">{item.label}</div>
            </Link>
          );
        })}
      </nav>
      <style>{`
        .bottom-nav {
          display: none;
        }
        @media (max-width: 768px) {
          .bottom-nav {
            display: flex;
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            height: calc(56px + env(safe-area-inset-bottom));
            padding-bottom: env(safe-area-inset-bottom);
            background-color: var(--color-surface);
            border-top: var(--border-default);
            z-index: var(--z-header);
            justify-content: space-around;
            align-items: center;
          }
          .nav-item {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            color: var(--color-text-secondary);
            text-decoration: none;
            gap: 2px;
            flex: 1;
          }
          .nav-item.active {
            color: var(--color-primary-600);
          }
          .nav-item .icon {
            font-size: 20px;
          }
          .nav-item .label {
            font-size: 10px;
            font-weight: var(--weight-medium);
          }
        }
      `}</style>
    </>
  );
}
