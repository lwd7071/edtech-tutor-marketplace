import React from 'react';
import { Alert, Button } from 'antd';
import { DateTimeText } from '../data-display/DateTimeText';

interface TeacherApprovalBannerProps {
  status: 'DRAFT' | 'PENDING_APPROVAL' | 'REJECTED' | 'APPROVED';
  submittedAt?: string;
  rejectionReason?: string;
  onSubmit?: () => void;
  onEdit?: () => void;
  className?: string;
}

export default function TeacherApprovalBanner({
  status,
  submittedAt,
  rejectionReason,
  onSubmit,
  onEdit,
  className = '',
}: TeacherApprovalBannerProps) {
  if (status === 'APPROVED') {
    return null; // Ẩn banner khi đã duyệt
  }

  const renderDescription = () => {
    switch (status) {
      case 'DRAFT':
        return (
          <div className="banner-content">
            <span>Hồ sơ của bạn chưa được gửi duyệt. Vui lòng hoàn tất thông tin và gửi yêu cầu để bắt đầu dạy học.</span>
            {onSubmit && <Button size="small" type="primary" onClick={onSubmit}>Gửi hồ sơ duyệt</Button>}
          </div>
        );
      case 'PENDING_APPROVAL':
        return (
          <div className="banner-content">
            <span>
              Hồ sơ của bạn đang được xét duyệt. Thời gian phản hồi dự kiến từ 1-2 ngày làm việc.
              {submittedAt && (
                <> (đã gửi lúc <DateTimeText value={submittedAt} variant="full" />)</>
              )}
            </span>
          </div>
        );
      case 'REJECTED':
        return (
          <div className="banner-content">
            <div>
              Hồ sơ của bạn không được chấp thuận. Lý do: <strong>{rejectionReason || 'Không có lý do cụ thể'}</strong>. Vui lòng cập nhật lại thông tin.
            </div>
            {onEdit && <Button size="small" danger type="primary" onClick={onEdit}>Chỉnh sửa và gửi lại</Button>}
          </div>
        );
      default:
        return null;
    }
  };

  const getType = () => {
    if (status === 'DRAFT') return 'info';
    if (status === 'PENDING_APPROVAL') return 'warning';
    return 'error';
  };

  const getMessage = () => {
    if (status === 'DRAFT') return 'Hồ sơ chưa gửi duyệt';
    if (status === 'PENDING_APPROVAL') return 'Đang chờ xét duyệt';
    return 'Hồ sơ bị từ chối';
  };

  return (
    <div className={`teacher-approval-banner ${className}`}>
      <Alert
        title={getMessage()}
        description={renderDescription()}
        type={getType()}
        showIcon
      />
      <style>{`
        .teacher-approval-banner {
          margin-bottom: var(--space-4);
        }
        .teacher-approval-banner .ant-alert {
          padding: 16px;
          border-radius: 12px;
          border: none;
        }
        .banner-content {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: var(--space-4);
          flex-wrap: wrap;
        }
        
        /* Apply exact colors from specs */
        .teacher-approval-banner .ant-alert-info {
          background-color: var(--color-info-bg);
        }
        .teacher-approval-banner .ant-alert-info .ant-alert-message {
          color: var(--color-info-600);
        }
        .teacher-approval-banner .ant-alert-warning {
          background-color: var(--color-warning-bg);
        }
        .teacher-approval-banner .ant-alert-warning .ant-alert-message {
          color: var(--color-warning-600);
        }
        .teacher-approval-banner .ant-alert-error {
          background-color: var(--color-error-bg);
        }
        .teacher-approval-banner .ant-alert-error .ant-alert-message {
          color: var(--color-error-600);
        }
      `}</style>
    </div>
  );
}
