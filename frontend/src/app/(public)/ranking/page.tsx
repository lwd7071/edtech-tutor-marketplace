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
    <div className="ranking-page bg-surface min-h-screen py-12 px-4">
      <div className="max-w-4xl mx-auto text-center mb-10">
        <Title level={1} className="flex items-center justify-center gap-3 !mb-2">
          <TrophyOutlined className="text-amber-500" />
          Bảng Xếp Hạng Gia Sư
        </Title>
        <Paragraph className="text-lg text-text-secondary">
          Tôn vinh những gia sư xuất sắc nhất dựa trên đánh giá và số buổi học đã hoàn thành
        </Paragraph>

        <div className="mt-8 max-w-xs mx-auto">
          <Select
            allowClear
            placeholder="Lọc theo môn học"
            className="w-full text-left"
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
        <div data-testid="ranking-loading" className="flex justify-center py-20">
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
