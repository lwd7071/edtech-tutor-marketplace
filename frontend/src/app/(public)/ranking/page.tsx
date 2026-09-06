'use client';

import React, { useEffect, useState } from 'react';
import { Typography, Spin, Select } from 'antd';
import { TrophyOutlined } from '@ant-design/icons';
import { getGlobalRanking, getPublicSubjects, TeacherRankingItem, SubjectSummary } from '@/shared/api/public';
import { RankingPodium } from '@/features/ranking/components/RankingPodium';
import { RankingList } from '@/features/ranking/components/RankingList';

const { Title, Paragraph } = Typography;
const { Option } = Select;

export default function RankingPage() {
  const [loading, setLoading] = useState(true);
  const [subjects, setSubjects] = useState<SubjectSummary[]>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState<string | undefined>(undefined);
  const [teachers, setTeachers] = useState<TeacherRankingItem[]>([]);

  useEffect(() => {
    const fetchSubjects = async () => {
      try {
        const res = await getPublicSubjects();
        setSubjects(res.data);
      } catch (e) {
        // Handle error silently or show toast
      }
    };
    fetchSubjects();
  }, []);

  useEffect(() => {
    const fetchRanking = async () => {
      setLoading(true);
      try {
        const res = await getGlobalRanking(selectedSubjectId, 0, 50); // Get top 50
        setTeachers(res.data);
      } catch (e) {
        // Error
      } finally {
        setLoading(false);
      }
    };
    fetchRanking();
  }, [selectedSubjectId]);

  return (
    <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh', padding: 'var(--space-12) var(--space-4)' }}>
      <div style={{ maxWidth: '896px', margin: '0 auto', textAlign: 'center', marginBottom: 'var(--space-10)' }}>
        <Title level={1} style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 'var(--space-3)', marginBottom: 'var(--space-2)' }}>
          <TrophyOutlined style={{ color: '#f59e0b' }} />
          Bảng Xếp Hạng Gia Sư
        </Title>
        <Paragraph style={{ fontSize: '18px', color: 'var(--color-text-secondary)' }}>
          Tôn vinh những gia sư xuất sắc nhất dựa trên đánh giá và số buổi học đã hoàn thành
        </Paragraph>

        <div style={{ marginTop: 'var(--space-8)', maxWidth: '320px', margin: '0 auto' }}>
          <Select
            allowClear
            placeholder="Lọc theo môn học"
            style={{ width: '100%', textAlign: 'left' }}
            size="large"
            value={selectedSubjectId}
            onChange={setSelectedSubjectId}
          >
            {subjects.map(sub => (
              <Option key={sub.id} value={sub.id}>{sub.name}</Option>
            ))}
          </Select>
        </div>
      </div>

      {loading ? (
        <div data-testid="ranking-loading" style={{ display: 'flex', justifyContent: 'center', padding: 'var(--space-20) 0' }}>
          <Spin size="large" />
        </div>
      ) : (
        <>
          <RankingPodium teachers={teachers} />
          <RankingList teachers={teachers.slice(3)} />
        </>
      )}
    </div>
  );
}
