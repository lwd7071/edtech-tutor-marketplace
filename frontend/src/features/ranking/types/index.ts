export interface TeacherStatsView {
  teacherId: string;
  averageRating: number;
  bayesianRating: number;
  reviewCount: number;
  completedSessionCount: number;
  completionRate: number;
  trialSessionCount: number;
  trialConversionRate: number;
  globalRank: number;
  calculatedAt: string;
}

export interface TeacherRankingItem {
  teacherId: string;
  fullName: string;
  avatarUrl: string;
  bioExcerpt: string;
  bayesianRating: number;
  completedSessionCount: number;
  globalRank: number;
}
