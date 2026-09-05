'use client';

import React, { useEffect, useState } from 'react';
import { useAuthStore } from '@/shared/store/useAuthStore';
import { Spin } from 'antd';
import Cookies from 'js-cookie';

export default function AuthProvider({ children }: { children: React.ReactNode }) {
  const [isHydrated, setIsHydrated] = useState(false);
  const setAuth = useAuthStore(state => state.setAuth);

  useEffect(() => {
    // Basic hydration from cookies and localstorage
    const token = Cookies.get('accessToken');
    const userStr = localStorage.getItem('user');
    
    if (token && userStr) {
      try {
        const user = JSON.parse(userStr);
        setAuth(user, token);
      } catch (e) {
        // failed to parse
      }
    }
    
    // Artificial small delay to prevent layout flashing, or wait for /me endpoint
    setIsHydrated(true);
  }, [setAuth]);

  if (!isHydrated) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', height: '100vh', backgroundColor: 'var(--color-surface)' }}>
        <div style={{ fontSize: 32, fontWeight: 'var(--weight-bold)', color: 'var(--color-primary-600)', marginBottom: 'var(--space-6)' }}>
          EdTech Tutor
        </div>
        <Spin size="large" />
      </div>
    );
  }

  return <>{children}</>;
}
