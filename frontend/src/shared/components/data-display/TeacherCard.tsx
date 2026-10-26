import { Avatar } from './Avatar';

interface TeacherCardProps {
  id: string;
  name: string;
  avatarUrl?: string;
  isVerified?: boolean;
  subjects?: string[];
  rating?: number;
  reviewCount?: number;
  lowestPrice?: number;
  yearsOfExperience?: number;
  supportsOnline?: boolean;
  supportsOffline?: boolean;
  variant?: 'full' | 'compact';
  locationLabel?: string;
  className?: string;
  hoverable?: boolean;
}

export default function TeacherCard({
  name,
  avatarUrl,
  isVerified = false,
  subjects = [],
  rating = 0,
  reviewCount = 0,
  lowestPrice,
  yearsOfExperience,
  supportsOnline,
  supportsOffline,
  locationLabel,
  className = '',
}: TeacherCardProps) {
  const deliveryModes: string[] = [];
  if (supportsOnline) deliveryModes.push('Online');
  if (supportsOffline) deliveryModes.push('Trực tiếp');

  return (
    <article className={`tm-tutor-card ${className}`}>
      {/* Header: Avatar 44px + Tên, Verified, Rating và Môn học trong cùng cụm */}
      <div className="tm-tutor-header">
        <Avatar src={avatarUrl} alt={name} size="compact" isVerified={isVerified} />
        <div className="tm-tutor-head-info">
          <div className="tm-tutor-title-row">
            <h3 className="tm-tutor-name">{name}</h3>
            {reviewCount > 0 ? (
              <span className="tm-tutor-rating">
                <span className="tm-star" aria-hidden="true">★</span>
                <strong>{rating.toFixed(1)}</strong>
                <span className="tm-tutor-reviews">({reviewCount} đánh giá)</span>
              </span>
            ) : (
              <span className="tm-tutor-rating-empty">Chưa có đánh giá</span>
            )}
          </div>
          <div className="tm-chips">
            {subjects.slice(0, 2).map((subject) => (
              <span className="tm-chip" key={subject}>{subject}</span>
            ))}
            {subjects.length > 2 && <span className="tm-chip">+{subjects.length - 2}</span>}
            {subjects.length === 0 && <span className="tm-chip">Chưa cập nhật môn học</span>}
          </div>
        </div>
      </div>

      {/* Meta: Địa điểm, Kinh nghiệm, Hình thức học trong cụm ngắn gọn, tự wrap */}
      {(locationLabel || yearsOfExperience !== undefined || deliveryModes.length > 0) && (
        <div className="tm-tutor-meta">
          {locationLabel && <span>{locationLabel}</span>}
          {yearsOfExperience !== undefined && (
            <span>
              {locationLabel ? '· ' : ''}{yearsOfExperience} năm kinh nghiệm
            </span>
          )}
          {deliveryModes.length > 0 && (
            <span>
              {(locationLabel || yearsOfExperience !== undefined) ? '· ' : ''}{deliveryModes.join(' / ')}
            </span>
          )}
        </div>
      )}

      {/* Bottom: Giá và nút Xem hồ sơ trên cùng một hàng */}
      <div className="tm-tutor-bottom">
        <div className="tm-tutor-price">
          {lowestPrice !== undefined && lowestPrice > 0 ? (
            <>
              <small>Gói từ</small>
              <strong>{new Intl.NumberFormat('vi-VN').format(lowestPrice)}đ</strong>
            </>
          ) : (
            <small>Chưa mở gói học</small>
          )}
        </div>
        <span className="tm-tutor-action">Xem hồ sơ</span>
      </div>
    </article>
  );
}
