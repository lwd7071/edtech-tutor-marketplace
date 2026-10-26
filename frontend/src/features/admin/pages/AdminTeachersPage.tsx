'use client';

import React from 'react';
import { TeacherApprovalTable } from '../components/TeacherApprovalTable';
import styles from './AdminTeachersPage.module.css';

export function AdminTeachersPage() {
  return <main className={styles.page}>
    <header className={styles.heading}>
      <h1>Xét duyệt hồ sơ gia sư</h1>
      <p>Xem thông tin và giấy tờ trước khi phê duyệt hoặc từ chối hồ sơ.</p>
    </header>
    <TeacherApprovalTable />
  </main>;
}
