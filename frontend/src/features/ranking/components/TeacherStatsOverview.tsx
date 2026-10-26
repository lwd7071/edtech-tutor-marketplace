'use client';

import { useEffect, useState } from 'react';
import { Alert, Button, Skeleton } from 'antd';
import { DateTimeText } from '@/shared/components/data-display/DateTimeText';
import { rankingApi } from '../api/rankingApi';
import type { TeacherStatsView } from '../types';
import styles from './TeacherStatsOverview.module.css';

const numberFormat = new Intl.NumberFormat('vi-VN');
const ratingFormat = new Intl.NumberFormat('en-US', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const percentFormat = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 });

const formatCount = (value: number) => numberFormat.format(value);
const formatRating = (value: number) => ratingFormat.format(value);
const formatPercent = (value: number) => `${percentFormat.format(value * 100)}%`;

export const TeacherStatsOverview = () => {
  const [stats, setStats] = useState<TeacherStatsView | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const fetchStats = async () => {
    setLoading(true);
    setError(false);
    try {
      const response = await rankingApi.getTeacherStats();
      setStats(response.data || null);
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void fetchStats(); }, []);

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div>
          <h1>Thống kê hoạt động</h1>
          <p>Kết quả giảng dạy được tổng hợp từ các buổi học và đánh giá đã xác nhận.</p>
        </div>
        {stats && !loading && !error && (
          <p className={styles.updated}>
            Cập nhật <DateTimeText value={stats.calculatedAt} variant="full" />
          </p>
        )}
      </header>

      {error && (
        <Alert
          className={styles.error}
          type="error"
          showIcon
          title="Chưa tải được thống kê"
          action={<Button onClick={() => void fetchStats()}>Thử lại</Button>}
        />
      )}

      {loading ? (
        <section className={styles.loading} aria-label="Đang tải thống kê" role="status">
          <div className={styles.summary} aria-hidden="true">
            {['one', 'two', 'three'].map(slot => (
              <div className={styles.metric} key={slot}>
                <Skeleton.Input active size="small" />
                <Skeleton.Input active size="large" />
                <Skeleton.Input active size="small" />
              </div>
            ))}
          </div>
          <div className={styles.rows} aria-hidden="true">
            {[0, 1, 2].map(row => (
              <div className={styles.row} key={row}>
                <Skeleton.Input active size="small" />
                <Skeleton.Input active size="small" />
              </div>
            ))}
          </div>
        </section>
      ) : !error && !stats ? (
        <section className={styles.empty} aria-live="polite">
          <h2>Chưa có dữ liệu thống kê</h2>
          <p>Số liệu sẽ hiển thị khi hệ thống có kết quả giảng dạy được xác nhận.</p>
        </section>
      ) : !error && stats ? (
        <>
          <section className={styles.summary} aria-label="Chỉ số tổng quan">
            <div className={styles.metric}>
              <span className={styles.label}>Buổi học hoàn thành</span>
              <strong className={styles.value}>{formatCount(stats.completedSessionCount)}</strong>
              <span className={styles.detail}>Tổng số buổi đã xác nhận hoàn thành</span>
            </div>
            <div className={styles.metric}>
              <span className={styles.label}>Đánh giá trung bình</span>
              <strong className={styles.value}>{stats.reviewCount > 0 ? formatRating(stats.averageRating) : '—'}</strong>
              <span className={styles.detail}>
                {stats.reviewCount > 0 ? `${formatCount(stats.reviewCount)} lượt đánh giá` : 'Chưa có đánh giá'}
              </span>
            </div>
            <div className={styles.metric}>
              <span className={styles.label}>Xếp hạng toàn hệ thống</span>
              <strong className={styles.value}>{stats.globalRank > 0 ? `#${formatCount(stats.globalRank)}` : '—'}</strong>
              <span className={styles.detail}>
                {stats.globalRank > 0
                  ? `Điểm xếp hạng đã điều chỉnh: ${formatRating(stats.bayesianRating)}`
                  : 'Chưa đủ dữ liệu xếp hạng'}
              </span>
            </div>
          </section>

          <section className={styles.activity} aria-labelledby="teacher-stats-activity-heading">
            <div className={styles.sectionHeading}>
              <h2 id="teacher-stats-activity-heading">Hoạt động giảng dạy</h2>
              <p>Các tỷ lệ được tính từ dữ liệu booking và học thử hiện có.</p>
            </div>
            <dl className={styles.rows}>
              <div className={styles.row}>
                <dt>Tỷ lệ hoàn thành</dt>
                <dd>{formatPercent(stats.completionRate)}</dd>
              </div>
              <div className={styles.row}>
                <dt>Buổi học thử</dt>
                <dd>{formatCount(stats.trialSessionCount)}</dd>
              </div>
              <div className={styles.row}>
                <dt>Chuyển đổi từ học thử</dt>
                <dd>{formatPercent(stats.trialConversionRate)}</dd>
              </div>
            </dl>
          </section>
        </>
      ) : null}
    </div>
  );
};
