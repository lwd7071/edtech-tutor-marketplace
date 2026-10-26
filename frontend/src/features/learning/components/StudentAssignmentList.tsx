'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { Button, Input, Pagination, Skeleton } from 'antd';
import { DateTimeText } from '@/shared/components/data-display/DateTimeText';
import { learningApi } from '../api/learningApi';
import { TeacherAssignmentListItem } from '../types';

const progressByTab = { to_do: 'TODO', submitted: 'SUBMITTED', graded: 'GRADED' } as const;
const tabs = [
  { key: 'to_do', label: 'Chưa làm', progress: 'TODO', progressLabel: 'Chưa nộp' },
  { key: 'submitted', label: 'Đã nộp', progress: 'SUBMITTED', progressLabel: 'Đã nộp' },
  { key: 'graded', label: 'Đã chấm điểm', progress: 'GRADED', progressLabel: 'Đã chấm điểm' },
] as const;

export const StudentAssignmentList: React.FC = () => {
  const [data, setData] = useState<TeacherAssignmentListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [search, setSearch] = useState('');
  const [activeTab, setActiveTab] = useState('to_do');
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const { current, pageSize, total } = pagination;

  const fetchAssignments = async (page: number, size: number, tab: string) => {
    try {
      setLoading(true);
      setError(false);
      const progress = progressByTab[tab as keyof typeof progressByTab];
      const res = await learningApi.getStudentAssignments(page, size, progress);
      if (res.data) {
        setData(res.data);
        if (res.meta) setPagination(prev => ({ ...prev, total: res.meta!.totalElements }));
      }
    } catch (e) {
      console.error('Failed to fetch assignments:', e);
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void fetchAssignments(current - 1, pageSize, activeTab);
  }, [current, pageSize, activeTab]);

  const isOverdue = (dueAt: string) => new Date(dueAt).getTime() < new Date().getTime();
  const visibleAssignments = data.filter(item => item.title.toLowerCase().includes(search.toLowerCase()));

  return <main className="tm-stack tm-student-assignments">
    <header className="tm-student-assignments-heading">
      <h1>Bài tập của tôi</h1>
      <label className="tm-student-assignments-search">
        <span>Tìm theo tiêu đề</span>
        <Input aria-label="Tìm theo tiêu đề" placeholder="Nhập tiêu đề bài tập" value={search} onChange={event => setSearch(event.target.value)} />
      </label>
    </header>

    <nav className="tm-student-assignment-tabs" role="tablist" aria-label="Lọc theo tiến độ bài tập">
      {tabs.map(tab => <button key={tab.key} type="button" role="tab" aria-selected={activeTab === tab.key} onClick={() => {
        setActiveTab(tab.key);
        setPagination(prev => ({ ...prev, current: 1 }));
      }}>{tab.label}</button>)}
    </nav>

    <section className="tm-student-assignment-results" aria-labelledby="student-assignment-results-title">
      <div className="tm-student-assignment-results-heading">
        <h2 id="student-assignment-results-title">{tabs.find(tab => tab.key === activeTab)?.label}</h2>
        {!loading && !error && total > 0 && <span>{total} bài</span>}
      </div>

      {loading && <div className="tm-student-assignment-loading" role="status" aria-label="Đang tải bài tập"><Skeleton active paragraph={{ rows: 5 }} /></div>}
      {!loading && error && <div className="tm-student-assignment-state" role="alert"><div><h3>Chưa tải được bài tập</h3><p>Kiểm tra kết nối rồi thử tải lại danh sách.</p></div><Button onClick={() => void fetchAssignments(current - 1, pageSize, activeTab)}>Thử lại</Button></div>}
      {!loading && !error && total === 0 && <div className="tm-student-assignment-state"><h3>{search ? 'Không tìm thấy bài tập phù hợp' : 'Chưa có bài tập ở mục này'}</h3><p>{search ? 'Thử tìm bằng một phần khác của tiêu đề.' : 'Bài tập mới sẽ xuất hiện tại đây khi gia sư giao bài.'}</p></div>}
      {!loading && !error && total > 0 && visibleAssignments.length === 0 && <div className="tm-student-assignment-state"><h3>Không tìm thấy bài tập trên trang này</h3><p>Tìm kiếm đang lọc các bài đã tải ở trang hiện tại.</p></div>}
      {!loading && !error && visibleAssignments.length > 0 && <ol className="tm-student-assignment-list">
        {visibleAssignments.map(item => {
          const overdue = activeTab === 'to_do' && isOverdue(item.dueAt);
          return <li key={item.id}>
            <article className="tm-student-assignment-row">
              <div className="tm-student-assignment-main">
                <Link href={`/student/assignments/${item.id}`} className="tm-student-assignment-title">{item.title}</Link>
                <p>Hạn nộp: <DateTimeText value={item.dueAt} /></p>
              </div>
              <div className="tm-student-assignment-status">
                {overdue && <span className="is-overdue">Quá hạn</span>}
                <span>{tabs.find(tab => tab.key === activeTab)?.progressLabel}</span>
                <span>{item.status === 'CLOSED' ? 'Đã đóng' : 'Đã giao'}</span>
              </div>
              <Link href={`/student/assignments/${item.id}`} className="tm-student-assignment-open">Mở bài tập</Link>
            </article>
          </li>;
        })}
      </ol>}

      {!loading && !error && total > pageSize && <Pagination className="tm-student-assignment-pagination" current={current} pageSize={pageSize} total={total} showSizeChanger pageSizeOptions={[10, 20, 50, 100]} onChange={(page, size) => setPagination(prev => ({ ...prev, current: page, pageSize: size }))} />}
    </section>
  </main>;
};
