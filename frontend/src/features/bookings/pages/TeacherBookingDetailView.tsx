'use client';

import type { ReactNode } from 'react';
import { Alert, Tag } from 'antd';
import type { BookingDetail } from '../types';
import { formatVietnamDateTime, formatVietnamTime } from '@/shared/lib/vietnamTime';
import styles from './TeacherBookingDetailView.module.css';

export function TeacherBookingDetailView({ item, labels, settlementLabels, actions }: {
  item: BookingDetail;
  labels: Record<BookingDetail['status'], string>;
  settlementLabels: Record<NonNullable<BookingDetail['settlementStatus']>, string>;
  actions: ReactNode;
}) {
  const meetingAvailable = item.deliveryMode === 'ONLINE'
    && Boolean(item.meetingLink && /^https?:\/\//.test(item.meetingLink))
    && item.status === 'SCHEDULED';
  const settlement = item.settlement;

  return (
    <article className={styles.page}>
      <header className={styles.header}>
        <div className={styles.titleGroup}>
          <span className={styles.kind}>{item.trial ? 'Buổi học thử' : 'Buổi học'}</span>
          <h1 className={styles.title}>{item.subject.name}</h1>
        </div>
        <div className={styles.statuses} aria-label="Trạng thái buổi dạy">
          <Tag>{labels[item.status]}</Tag>
          {!item.trial && item.settlementStatus && (
            <Tag color={item.settlementStatus === 'RELEASED' ? 'green' : item.settlementStatus === 'HELD' ? 'orange' : 'blue'}>
              {settlementLabels[item.settlementStatus]}
            </Tag>
          )}
        </div>
        <time className={styles.time} dateTime={item.startTime}>
          {formatVietnamDateTime(item.startTime)} <span aria-hidden="true">–</span> {formatVietnamTime(item.endTime)}
        </time>
      </header>

      {item.outsideAvailabilityWarning && (
        <Alert className={styles.warning} type="warning" showIcon title="Buổi học nằm ngoài lịch rảnh đã công bố" />
      )}

      <div className={styles.contextGrid}>
        <section className={styles.facts} aria-labelledby="teacher-booking-facts-title">
          <h2 id="teacher-booking-facts-title">Thông tin buổi dạy</h2>
          <dl>
            <div><dt>Gia sư</dt><dd>{item.teacher.fullName}</dd></div>
            <div><dt>Học viên</dt><dd>{item.student.fullName}</dd></div>
            <div><dt>Hình thức</dt><dd>{item.deliveryMode === 'ONLINE' ? 'Trực tuyến' : 'Trực tiếp'}</dd></div>
            {meetingAvailable && <div><dt>Phòng học</dt><dd><a href={item.meetingLink!} target="_blank" rel="noopener noreferrer">Mở phòng học</a></dd></div>}
            {item.locationAddress && <div><dt>Địa điểm</dt><dd>{item.locationAddress}</dd></div>}
          </dl>
        </section>

        {!item.trial && item.settlementStatus && (
          <section className={styles.settlement} aria-labelledby="teacher-booking-settlement-title">
            <h2 id="teacher-booking-settlement-title">Quyết toán buổi học</h2>
            <p className={styles.settlementStatus}>{settlementLabels[item.settlementStatus]}</p>
            <dl>
              <div>
                <dt>Hạn xác nhận</dt>
                <dd>{settlement?.confirmationDeadline ? formatVietnamDateTime(settlement.confirmationDeadline) : '—'}</dd>
              </div>
              <div><dt>Gia sư</dt><dd>{settlement?.teacherConfirmedAt ? 'Đã xác nhận' : 'Chưa xác nhận'}</dd></div>
              <div><dt>Học viên</dt><dd>{settlement?.studentConfirmedAt ? 'Đã xác nhận' : 'Chưa xác nhận'}</dd></div>
            </dl>
          </section>
        )}
      </div>

      {item.status === 'CANCELLED' && item.cancelReason && (
        <section className={styles.cancelReason} aria-labelledby="teacher-booking-cancel-title">
          <h2 id="teacher-booking-cancel-title">Lý do hủy</h2>
          <p>{item.cancelReason}</p>
        </section>
      )}

      {actions && <section className={styles.actions} aria-label="Thao tác buổi dạy">{actions}</section>}

      {item.sessionReport && (
        <section className={styles.report} aria-labelledby="teacher-session-report-title">
          <h2 id="teacher-session-report-title">Báo cáo buổi học</h2>
          <div className={styles.reportGrid}>
            <div><h3>Nội dung buổi học</h3><p>{item.sessionReport.content}</p></div>
            <div><h3>Nhận xét</h3><p>{item.sessionReport.feedback}</p></div>
            {item.sessionReport.followUpNote && <div><h3>Việc cần làm tiếp</h3><p>{item.sessionReport.followUpNote}</p></div>}
          </div>
        </section>
      )}
    </article>
  );
}
