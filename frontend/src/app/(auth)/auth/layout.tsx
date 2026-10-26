import React from 'react';
import Link from 'next/link';
import Image from 'next/image';

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="tm-auth-shell">
      <aside className="tm-auth-story" aria-label="Giới thiệu Tutor Match">
        <Link href="/" className="tm-brand" aria-label="tutormatch">tutormatch</Link>
        <div className="tm-auth-story-content">
          <h1>Học đúng người.<br />Tiến đúng mục tiêu.</h1>
          <p>Kết nối học viên và gia sư trong những buổi học 1–1 theo mục tiêu của bạn.</p>
          <Image src="/images/tutor-hero-illustration.png" alt="Học viên và gia sư học cùng nhau" width={728} height={512} />
        </div>
      </aside>
      <div className="tm-auth-form-side">
        <div className="tm-auth-form-wrap">
          <Link href="/" className="tm-brand tm-auth-mobile-brand" aria-label="tutormatch">tutormatch</Link>
          {children}
        </div>
      </div>
    </div>
  );
}
