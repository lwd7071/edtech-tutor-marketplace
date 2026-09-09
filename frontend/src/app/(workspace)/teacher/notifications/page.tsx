'use client';

import React from 'react';
import { NotificationList } from '@/features/notifications/components/NotificationList';

export default function NotificationsPage() {
  return (
    <div style={{ maxWidth: 880, margin: '0 auto' }}>
      <NotificationList />
    </div>
  );
}
