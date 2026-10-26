'use client';

import { useRouter, useSearchParams } from 'next/navigation';
import { Select, Spin } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { getGlobalRanking, type SubjectSummary, type TeacherRankingItem } from '@/shared/api/public';
import { RankingList } from '@/features/ranking/components/RankingList';

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
    initialData: selectedSubjectId ? undefined : { success: true, message: null, data: initialRanking, errors: null, meta: { page: 0, size: 50, totalElements: initialRanking.length, totalPages: 1, hasNext: false, hasPrevious: false } },
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
    <main className="tm-container tm-page tm-ranking-page">
      <header className="tm-ranking-header">
        <div className="tm-ranking-intro">
          <h1>Bảng xếp hạng gia sư</h1>
          <p>Tôn vinh những gia sư xuất sắc nhất dựa trên đánh giá và số buổi học đã hoàn thành</p>
        </div>
        <div className="tm-ranking-filter">
          <label htmlFor="ranking-subject">Môn học</label>
          <Select
            id="ranking-subject"
            aria-label="Lọc theo môn học"
            allowClear
            placeholder="Lọc theo môn học"
            value={selectedSubjectId}
            onChange={updateSubject}
            options={subjects.map(subject => ({ value: subject.id, label: subject.name }))}
            size="large"
          />
        </div>
      </header>

      <section className="tm-ranking-results" aria-label="Danh sách xếp hạng" aria-busy={loading}>
        {loading ? (
          <div className="tm-ranking-loading" data-testid="ranking-loading" role="status">
            <Spin size="small" />
            <span>Đang tải bảng xếp hạng</span>
          </div>
        ) : error ? (
          <p className="tm-ranking-error" role="alert">Chưa tải được bảng xếp hạng.</p>
        ) : (
          <RankingList teachers={teachers} />
        )}
      </section>
    </main>
  );
}
