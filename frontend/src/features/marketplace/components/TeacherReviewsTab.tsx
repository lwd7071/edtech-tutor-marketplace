import React from 'react';
import { Review, PageMeta } from '@/shared/api/public';
import { EmptyState } from '@/shared/components/feedback/EmptyState';
import { DateTimeText } from '@/shared/components/data-display/DateTimeText';
import { Avatar } from '@/shared/components/data-display/Avatar';
import { Pagination } from 'antd';

interface TeacherReviewsTabProps {
  reviews: Review[];
  meta: PageMeta;
  onPageChange: (page: number) => void;
}

export const TeacherReviewsTab: React.FC<TeacherReviewsTabProps> = ({ reviews, meta, onPageChange }) => {
  if (!reviews || reviews.length === 0) {
    return (
      <EmptyState 
        title="Chưa có đánh giá nào"
        description="Giáo viên này chưa có đánh giá từ học viên."
      />
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="grid grid-cols-1 gap-6">
        {reviews.map(review => (
          <div key={review.id} className="bg-surface rounded-xl p-6 border border-border shadow-sm">
            <div className="flex items-start gap-4">
              <Avatar alt={review.reviewerName} size="lg" />
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between mb-1">
                  <span className="font-semibold text-text-primary text-base">{review.reviewerName}</span>
                  <span className="text-text-secondary text-sm">
                    <DateTimeText value={review.createdAt} />
                  </span>
                </div>
                <div className="flex items-center gap-1 mb-3 text-yellow-500">
                  {/* Rating stars rendering */}
                  {Array.from({ length: 5 }).map((_, i) => (
                    <span key={i} className={i < review.rating ? 'text-yellow-500' : 'text-neutral-300'}>
                      ★
                    </span>
                  ))}
                </div>
                {review.comment && (
                  <p className="text-text-primary text-sm whitespace-pre-line m-0">
                    {review.comment}
                  </p>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {meta.totalPages > 1 && (
        <div className="flex justify-center mt-6">
          <Pagination 
            current={meta.page + 1} 
            total={meta.totalElements} 
            pageSize={meta.size} 
            onChange={(page) => onPageChange(page - 1)} 
            showSizeChanger={false}
          />
        </div>
      )}
    </div>
  );
};
