'use client';

import React from 'react';
import { NotificationList } from '@/features/notifications/components/NotificationList';

export default function NotificationsPage() {
  return (
    <div className="max-w-[880px] mx-auto">
      <NotificationList />
    </div>
  );
}
