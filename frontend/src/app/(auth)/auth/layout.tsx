import React from 'react';
import Link from 'next/link';

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ display: 'flex', minHeight: '100vh', backgroundColor: 'var(--color-surface)' }}>
      {/* Left side: Illustration / Branding (hidden on mobile) */}
      <div 
        style={{ 
          flex: 1, 
          display: 'none', 
          backgroundColor: 'var(--color-primary-50)', 
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          padding: 'var(--space-10)',
        }}
        className="auth-illustration"
      >
        <div style={{ maxWidth: 480, textAlign: 'center' }}>
          <h1 style={{ fontSize: 'var(--text-display-md)', color: 'var(--color-primary-900)', marginBottom: 'var(--space-4)' }}>
            Học Trực Tuyến Cùng Gia Sư Giỏi
          </h1>
          <p style={{ fontSize: 'var(--text-body-lg)', color: 'var(--color-text-secondary)' }}>
            Nền tảng kết nối học viên với gia sư phù hợp, giúp bạn tiến gần mục tiêu học tập theo nhịp riêng.
          </p>
        </div>
      </div>
      <style>{`
        @media (min-width: 900px) {
          .auth-illustration {
            display: flex !important;
          }
        }
      `}</style>

      {/* Right side: Form (Card on desktop, full width on mobile) */}
      <div 
        style={{ 
          flex: 1, 
          display: 'flex', 
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          padding: 'var(--space-6)',
          backgroundColor: 'var(--color-surface)',
        }}
      >
        <div style={{ width: '100%', maxWidth: 440 }}>
          <div style={{ textAlign: 'center', marginBottom: 'var(--space-8)' }}>
            <Link href="/" style={{ textDecoration: 'none' }}>
              <div style={{ fontSize: 32, fontWeight: 'var(--weight-bold)', color: 'var(--color-primary-600)' }}>
                EdTech Tutor
              </div>
            </Link>
          </div>
          {children}
        </div>
      </div>
    </div>
  );
}
