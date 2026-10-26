'use client';

import { useRouter, useSearchParams } from 'next/navigation';
import { Select, Spin, Typography } from 'antd';
import { TrophyOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { getGlobalRanking, type SubjectSummary, type TeacherRankingItem } from '@/shared/api/public';
import { RankingPodium } from '@/features/ranking/components/RankingPodium';
import { RankingList } from '@/features/ranking/components/RankingList';

const { Title, Paragraph } = Typography;

export default function RankingClient({
  subjects,
  initialRanking,
  initialError,
}: {
  subjects: SubjectSummary[];
  initialRanking: TeacherRankingItem[];
  initialError: boolean;
}) {
  const router = useRouter();
  const params = useSearchParams();
  const selectedSubjectId = params.get('subjectId') || undefined;
  const ranking = useQuery({
    queryKey: ['public-ranking', selectedSubjectId],
    queryFn: () => getGlobalRanking(selectedSubjectId, 0, 50),
    enabled: Boolean(selectedSubjectId),
    initialData: { success: true, message: null, data: initialRanking, errors: null, meta: { page: 0, size: 50, totalElements: initialRanking.length, totalPages: 1, hasNext: false, hasPrevious: false } },
    staleTime: 60_000,
  });
  const teachers = ranking.data?.data ?? [];
  const loading = ranking.isLoading || Boolean(selectedSubjectId && ranking.isFetching);
  const error = (initialError && !selectedSubjectId) || ranking.isError;
  const updateSubject = (value?: string) => {
    const next = new URLSearchParams(params.toString());
    if (value) next.set('subjectId', value); else next.delete('subjectId');
    router.push(next.toString() ? `/ranking?${next}` : '/ranking');
  };

  return (
    <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh', padding: 'var(--space-12) var(--space-4)' }}>
      <div style={{ maxWidth: '896px', margin: '0 auto', textAlign: 'center', marginBottom: 'var(--space-10)' }}>
        <Title level={1} style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 'var(--space-3)', marginBottom: 'var(--space-2)' }}><TrophyOutlined style={{ color: '#f59e0b' }} />Bảng Xếp Hạng Gia Sư</Title>
        <Paragraph style={{ fontSize: '18px', color: 'var(--color-text-secondary)' }}>Tôn vinh những gia sư xuất sắc nhất dựa trên đánh giá và số buổi học đã hoàn thành</Paragraph>
        <div style={{ marginTop: 'var(--space-8)', maxWidth: '320px', margin: '0 auto' }}>
          <Select allowClear placeholder="Lọc theo môn học" style={{ width: '100%', textAlign: 'left' }} size="large" value={selectedSubjectId} onChange={updateSubject} options={subjects.map(subject => ({ value: subject.id, label: subject.name }))} />
        </div>
      </div>
      {loading ? <div data-testid="ranking-loading" style={{ display: 'flex', justifyContent: 'center', padding: 'var(--space-20) 0' }}><Spin size="large" /></div> : error ? <p role="alert">Chưa tải được bảng xếp hạng.</p> : <><RankingPodium teachers={teachers} /><RankingList teachers={teachers.slice(3)} /></>}
    </div>
  );
}
