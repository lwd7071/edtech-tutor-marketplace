'use client';

import { BookOutlined, CheckCircleOutlined, MessageOutlined, SearchOutlined, VideoCameraOutlined } from '@ant-design/icons';

const icons = {
  book: BookOutlined,
  check: CheckCircleOutlined,
  search: SearchOutlined,
  message: MessageOutlined,
  video: VideoCameraOutlined,
} as const;

export default function HomeIcon({ name, className }: { name: keyof typeof icons; className?: string }) {
  const Icon = icons[name];
  return <Icon className={className} aria-hidden="true" />;
}
