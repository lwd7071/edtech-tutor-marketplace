import { getPublicSubjectServer, getPublicSubjectsServer, PublicApiError } from '@/shared/api/public.server';
import { SubjectPickerClient } from './SubjectPickerClient';

export default async function SubjectsPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const query = await searchParams;
  const value = (key: string) => typeof query[key] === 'string' ? query[key] as string : '';
  const keyword = value('keyword');
  const educationLevel = value('educationLevel');
  const subjectId = value('subjectId');
  const requestedPage = value('page');
  const currentPage = requestedPage && /^\d+$/.test(requestedPage) ? Math.max(1, Number(requestedPage)) : 1;

  const [listResult, selectedResult] = await Promise.allSettled([
    getPublicSubjectsServer({ keyword, educationLevel: educationLevel || undefined, page: currentPage - 1, size: 11 }),
    subjectId ? getPublicSubjectServer(subjectId) : Promise.resolve(undefined),
  ]);

  const selectedSubject = selectedResult.status === 'fulfilled' ? selectedResult.value : undefined;
  const selectedUnavailable = selectedResult.status === 'rejected'
    && selectedResult.reason instanceof PublicApiError
    && selectedResult.reason.status === 404;

  return (
    <main className="tm-container tm-page tm-subject-page">
      <SubjectPickerClient
        subjects={listResult.status === 'fulfilled' ? listResult.value.data : []}
        selectedSubject={selectedSubject}
        selectedUnavailable={selectedUnavailable}
        keyword={keyword}
        educationLevel={educationLevel}
        currentPage={currentPage}
        totalPages={listResult.status === 'fulfilled' ? listResult.value.meta.totalPages : 0}
        hasError={listResult.status === 'rejected'}
      />
    </main>
  );
}
