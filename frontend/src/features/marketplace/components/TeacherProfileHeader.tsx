import React from 'react';
import { Button } from 'antd';
import { TeacherPublicDetail } from '@/shared/api/public';
import { Avatar } from '@/shared/components/data-display/Avatar';
import { CheckCircleFilled, EnvironmentOutlined, TranslationOutlined, LaptopOutlined } from '@ant-design/icons';

interface TeacherProfileHeaderProps {
  teacher: TeacherPublicDetail;
}

export const TeacherProfileHeader: React.FC<TeacherProfileHeaderProps> = ({ teacher }) => {
  return (
    <div className="bg-surface rounded-xl border border-border shadow-sm p-6 lg:p-8 flex flex-col md:flex-row gap-8 mb-8 relative">
      {/* Avatar column */}
      <div className="flex flex-col items-center shrink-0">
        <Avatar src={teacher.avatarUrl} alt={teacher.fullName} size="xl" className="w-32 h-32 text-4xl" />
        <div className="mt-4 flex items-center justify-center gap-1 bg-yellow-100 text-yellow-800 px-3 py-1 rounded-full font-bold">
          ⭐ {teacher.averageRating.toFixed(1)} 
          <span className="text-yellow-600 font-normal ml-1">({teacher.reviewCount} đánh giá)</span>
        </div>
      </div>

      {/* Info column */}
      <div className="flex-1 min-w-0">
        <div className="flex flex-col gap-2 mb-4">
          <h1 className="text-h2 m-0 flex items-center gap-2">
            {teacher.fullName}
            <CheckCircleFilled className="text-primary text-2xl" title="Đã xác thực" />
          </h1>
          <div className="text-text-secondary text-lg flex flex-wrap items-center gap-x-4 gap-y-2">
            <span className="font-medium text-text-primary">{teacher.subjects.join(', ')}</span>
            <span>•</span>
            <span>{teacher.yearsOfExperience} năm kinh nghiệm</span>
          </div>
        </div>

        <p className="text-text-primary text-base whitespace-pre-line mb-6">
          {teacher.bio}
        </p>

        <div className="flex flex-wrap gap-4 text-text-secondary mb-6 md:mb-0">
          {teacher.locationAddress && (
            <div className="flex items-center gap-2">
              <EnvironmentOutlined />
              <span>{teacher.locationAddress}</span>
            </div>
          )}
          {teacher.languages?.length > 0 && (
            <div className="flex items-center gap-2">
              <TranslationOutlined />
              <span>{teacher.languages.join(', ')}</span>
            </div>
          )}
          <div className="flex items-center gap-2">
            <LaptopOutlined />
            <span>
              {teacher.supportsOnline && teacher.supportsOffline 
                ? 'Dạy Online & Offline' 
                : teacher.supportsOnline 
                  ? 'Chỉ dạy Online' 
                  : 'Chỉ dạy Offline'}
            </span>
          </div>
        </div>
      </div>

      {/* CTA column (Sticky on Desktop) */}
      <div className="shrink-0 w-full md:w-64 flex flex-col gap-3">
        <Button type="primary" size="large" className="w-full font-bold h-12 shadow-md hover:shadow-lg transition-shadow">
          Mua gói
        </Button>
        <Button size="large" className="w-full font-semibold h-12 border-primary text-primary hover:bg-primary-50">
          Yêu cầu học thử
        </Button>
        <Button type="default" size="large" className="w-full font-semibold h-12 bg-neutral-100 hover:bg-neutral-200 border-none">
          Nhắn tin
        </Button>
      </div>
    </div>
  );
};
