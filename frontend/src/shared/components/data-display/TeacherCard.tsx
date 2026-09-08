import { Avatar } from './Avatar';

interface TeacherCardProps {
  id: string; name: string; avatarUrl?: string; isVerified?: boolean; subjects?: string[];
  rating?: number; reviewCount?: number; lowestPrice?: number; yearsOfExperience?: number;
  supportsOnline?: boolean; supportsOffline?: boolean; variant?: 'full' | 'compact';
  onClick?: () => void; className?: string; hoverable?: boolean;
}

export default function TeacherCard({ name, avatarUrl, isVerified = false, subjects = [], rating = 0, reviewCount = 0, lowestPrice, yearsOfExperience, supportsOnline, supportsOffline, onClick, className = '' }: TeacherCardProps) {
  return (
    <article className={`tm-tutor-card ${className}`} onClick={onClick}>
      <div className="tm-tutor-header">
        <Avatar src={avatarUrl} alt={name} size="lg" isVerified={isVerified} />
        <div>
          <h3 className="tm-tutor-name">{name}</h3>
          <div className="tm-chips">
            {subjects.slice(0, 2).map(subject => <span className="tm-chip" key={subject}>{subject}</span>)}
            {subjects.length > 2 && <span className="tm-chip">+{subjects.length - 2}</span>}
            {subjects.length === 0 && <span className="tm-chip">Chưa cập nhật môn học</span>}
          </div>
        </div>
      </div>
      <div className="tm-tutor-meta">
        {yearsOfExperience !== undefined && <span>{yearsOfExperience} năm kinh nghiệm</span>}
        {supportsOnline && <span>· Online</span>}
        {supportsOffline && <span>· Trực tiếp</span>}
      </div>
      <div className="tm-rating">{reviewCount > 0 ? <><span className="tm-star">★</span> <strong>{rating.toFixed(1)}</strong> <span>({reviewCount} đánh giá)</span></> : <span>Chưa có đánh giá</span>}</div>
      <div className="tm-tutor-bottom">
        <div>{lowestPrice !== undefined && lowestPrice > 0 ? <><small>Gói từ</small><strong>{new Intl.NumberFormat('vi-VN').format(lowestPrice)}đ</strong></> : <small>Chưa mở gói học</small>}</div>
        <span>Xem hồ sơ →</span>
      </div>
    </article>
  );
}
