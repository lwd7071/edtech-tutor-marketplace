import { getGlobalRankingServer, getPublicSubjectsServer } from '@/shared/api/public.server';
import RankingClient from './RankingClient';

export const revalidate = 60;

export default async function RankingPage() {
  const [subjects, ranking] = await Promise.allSettled([
    getPublicSubjectsServer({ size: 100 }),
    getGlobalRankingServer(undefined, 0, 50),
  ]);

  return (
    <RankingClient
      subjects={subjects.status === 'fulfilled' ? subjects.value.data : []}
      initialRanking={ranking.status === 'fulfilled' ? ranking.value.data : []}
      initialError={ranking.status === 'rejected'}
    />
  );
}
