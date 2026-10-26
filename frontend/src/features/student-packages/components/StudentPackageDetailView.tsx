'use client';

import React from 'react';
import { Alert, Button, Tag } from 'antd';
import { StudentPackageDetail } from '../types';
import { SessionCounter } from './SessionCounter';
import { getPackageStatusConfig, formatVnd, formatDate } from './StudentPackageCard';
import { BackLink } from '@/shared/components/navigation/NavigationLinks';
import { MessageTeacherButton } from '@/features/chat/components/MessageTeacherButton';

interface StudentPackageDetailViewProps {
  packageData: StudentPackageDetail;
  onExtensionRequest?: () => void;
  onRefundRequest?: () => void;
  className?: string;
}

export const StudentPackageDetailView: React.FC<StudentPackageDetailViewProps> = ({
  packageData,
  onExtensionRequest,
  onRefundRequest,
  className,
}) => {
  const statusConfig = getPackageStatusConfig(packageData.status);
  const completedPercent = Math.round(
    ((packageData.completedSessions + packageData.refundedSessions) /
      (packageData.totalSessions || 1)) *
      100
  );

  return (
    <div className={`tm-student-package-detail ${className ?? ''}`.trim()}>
      <BackLink href="/student/packages" className="tm-student-package-detail-back">Danh sách gói học</BackLink>

      <header className="tm-student-package-detail-heading">
        <div className="tm-student-package-detail-title">
          <h1>{packageData.packageName}</h1>
          <p>Mã gói #{packageData.id.slice(0, 8)} <span aria-hidden="true">·</span> {packageData.subject.name}</p>
        </div>
        <div className="tm-student-package-detail-heading-actions">
          <Tag color={statusConfig.color}>{statusConfig.label}</Tag>
          <MessageTeacherButton teacherId={packageData.teacher.id} />
        </div>
      </header>

      {packageData.status === 'LOCKED_EXPIRED' && (
        <Alert
          className="tm-student-package-detail-alert"
          type="error"
          showIcon
          title="Gói học đã hết hạn"
          description={
            <div className="tm-student-package-alert-content">
              <p>Gói học này đã hết thời hạn sử dụng. Bạn không thể đặt thêm lịch học mới, tuy nhiên các buổi đã lên lịch trước đó vẫn diễn ra bình thường.</p>
              <div className="tm-student-package-alert-actions">
                <Button type="primary" onClick={onExtensionRequest}>Yêu cầu gia hạn</Button>
                {packageData.remainingSessions > 0 && <Button onClick={onRefundRequest}>Yêu cầu hoàn tiền</Button>}
              </div>
            </div>
          }
        />
      )}

      {packageData.status === 'REFUND_PENDING' && (
        <Alert
          className="tm-student-package-detail-alert"
          type="warning"
          showIcon
          title="Đang xử lý hoàn tiền"
          description="Gói học đang trong tiến trình xử lý hoàn tiền từ ban quản trị. Các thao tác đặt lịch học mới tạm thời bị khóa."
        />
      )}

      {packageData.status === 'ACTIVE' && packageData.remainingSessions > 0 && (
        <div className="tm-student-package-active-action">
          <Button onClick={onRefundRequest}>Yêu cầu hoàn tiền phần chưa học</Button>
        </div>
      )}

      <section className="tm-student-package-progress" aria-labelledby="tm-student-package-progress-title">
        <h2 id="tm-student-package-progress-title">Tiến độ học tập &amp; Buổi học</h2>
        <SessionCounter
          remainingSessions={packageData.remainingSessions}
          reservedSessions={packageData.reservedSessions}
          completedSessions={packageData.completedSessions}
          refundedSessions={packageData.refundedSessions}
          size="large"
        />
        <div className="tm-student-package-progress-summary">
          <div className="tm-student-package-progress-labels">
            <span>Tiến độ hoàn thành gói học ({packageData.completedSessions}/{packageData.totalSessions} buổi)</span>
            <strong>{completedPercent}%</strong>
          </div>
          <progress aria-label="Tiến độ hoàn thành gói học" value={completedPercent} max={100} />
        </div>
      </section>

      <section className="tm-student-package-information" aria-labelledby="tm-student-package-information-title">
        <h2 id="tm-student-package-information-title">Thông tin chi tiết gói học</h2>
        <dl className="tm-student-package-detail-facts">
          <div>
            <dt>Gia sư phụ trách</dt>
            <dd>{packageData.teacher.fullName}</dd>
          </div>
          <div>
            <dt>Môn học</dt>
            <dd>{packageData.subject.name}</dd>
          </div>
          <div>
            <dt>Thời gian hiệu lực</dt>
            <dd>{formatDate(packageData.startsAt)} — {formatDate(packageData.expiresAt)}</dd>
          </div>
          <div>
            <dt>Giá trị gói học</dt>
            <dd>{formatVnd(packageData.purchasePriceVnd)}</dd>
          </div>
        </dl>
        {packageData.description && (
          <div className="tm-student-package-description">
            <h3>Mô tả nội dung đào tạo</h3>
            <p>{packageData.description}</p>
          </div>
        )}
      </section>
    </div>
  );
};
