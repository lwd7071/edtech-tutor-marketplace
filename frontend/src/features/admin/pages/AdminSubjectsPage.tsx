'use client';

import React from 'react';
import { SubjectProposalTable } from '../components/SubjectProposalTable';
import styles from './AdminSubjectsPage.module.css';

export function AdminSubjectsPage() {
  return <div className={styles.page}>
    <header className={styles.heading}>
      <h1>Duyệt đề xuất môn học</h1>
      <p>Xem nội dung gia sư gửi và quyết định đưa môn học vào danh mục.</p>
    </header>
    <SubjectProposalTable />
  </div>;
}
