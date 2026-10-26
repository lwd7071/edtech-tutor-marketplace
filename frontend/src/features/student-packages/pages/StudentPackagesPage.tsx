'use client';

import React from 'react';
import { StudentPackageList } from '../components/StudentPackageList';

export const StudentPackagesPage: React.FC = () => (
  <div className="tm-student-packages-page">
    <header className="tm-student-packages-heading">
      <div>
        <h1>Gói học của tôi</h1>
        <p>Theo dõi số buổi, trạng thái và thời hạn của các gói học đã đăng ký.</p>
      </div>
    </header>
    <StudentPackageList />
  </div>
);
