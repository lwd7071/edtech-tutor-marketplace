'use client';

import React from 'react';
import { Button } from 'antd';
import { TeacherPublicDetail } from '@/shared/api/public';
import { Avatar } from '@/shared/components/data-display/Avatar';
import { CheckCircleFilled, EnvironmentOutlined, TranslationOutlined, LaptopOutlined } from '@ant-design/icons';

interface TeacherProfileHeaderProps {
  teacher: TeacherPublicDetail;
}

export const TeacherProfileHeader: React.FC<TeacherProfileHeaderProps> = ({ teacher }) => {
  const handleBuyClick = () => {
    // Attempt to click the Packages tab if it's rendered by Ant Design Tabs
    const packagesTab = document.querySelector('.custom-tabs .ant-tabs-tab[data-node-key="packages"]') as HTMLElement;
    if (packagesTab) {
      packagesTab.click();
    }
    
    // Scroll to the tabs area
    const tabsContainer = document.querySelector('.custom-tabs');
    if (tabsContainer) {
      tabsContainer.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <div style={{ backgroundColor: 'var(--color-surface)', borderRadius: 'var(--radius-xl)', border: '1px solid var(--color-border)', boxShadow: 'var(--shadow-sm)', padding: 'var(--space-6)', display: 'flex', flexWrap: 'wrap', gap: 'var(--space-8)', marginBottom: 'var(--space-8)', position: 'relative' }}>
      {/* Avatar column */}
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', flexShrink: 0 }}>
        <Avatar src={teacher.avatarUrl} alt={teacher.fullName} size="xl" style={{ width: '128px', height: '128px', fontSize: '36px' }} />
        <div style={{ marginTop: 'var(--space-4)', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px', backgroundColor: 'var(--color-warning-bg)', color: 'var(--color-warning-600)', padding: '4px 12px', borderRadius: 'var(--radius-full)', fontWeight: 'bold' }}>
          ⭐ {teacher.averageRating.toFixed(1)} 
          <span style={{ color: '#d97706', fontWeight: 'normal', marginLeft: '4px' }}>({teacher.reviewCount} đánh giá)</span>
        </div>
      </div>

      {/* Info column */}
      <div style={{ flex: 1, minWidth: '300px' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)', marginBottom: 'var(--space-4)' }}>
          <h1 style={{ fontSize: 'var(--text-h2)', margin: 0, display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
            {teacher.fullName}
            <CheckCircleFilled style={{ color: 'var(--color-primary-600)', fontSize: '24px' }} title="Đã xác thực" />
          </h1>
          <div style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--text-body-lg)', display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 'var(--space-4)' }}>
            <span style={{ fontWeight: 500, color: 'var(--color-text-primary)' }}>{teacher.subjects?.join(', ')}</span>
            <span>•</span>
            <span>{teacher.yearsOfExperience} năm kinh nghiệm</span>
          </div>
        </div>

        <p style={{ color: 'var(--color-text-primary)', fontSize: 'var(--text-body-lg)', whiteSpace: 'pre-line', marginBottom: 'var(--space-6)' }}>
          {teacher.bio}
        </p>

        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--space-4)', color: 'var(--color-text-secondary)' }}>
          {teacher.locationAddress && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
              <EnvironmentOutlined />
              <span>{teacher.locationAddress}</span>
            </div>
          )}
          {teacher.languages?.length > 0 && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
              <TranslationOutlined />
              <span>{teacher.languages.join(', ')}</span>
            </div>
          )}
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
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
      <div style={{ flexShrink: 0, width: '100%', maxWidth: '256px', display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
        <Button onClick={handleBuyClick} type="primary" size="large" style={{ width: '100%', fontWeight: 'bold', height: '48px', boxShadow: 'var(--shadow-md)' }}>
          Mua gói
        </Button>
        <Button size="large" style={{ width: '100%', fontWeight: 600, height: '48px', borderColor: 'var(--color-primary-600)', color: 'var(--color-primary-600)' }}>
          Yêu cầu học thử
        </Button>
        <Button type="default" size="large" style={{ width: '100%', fontWeight: 600, height: '48px', backgroundColor: 'var(--color-surface-sunken)', border: 'none' }}>
          Nhắn tin
        </Button>
      </div>
    </div>
  );
};
