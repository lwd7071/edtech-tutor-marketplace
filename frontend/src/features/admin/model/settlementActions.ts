import type { BookingSettlementAdminView } from '../types/finance';

export type SettlementAction = 'reopen' | 'release' | 'retain';

export function availableSettlementActions(
  settlement: BookingSettlementAdminView,
  now = Date.now(),
): SettlementAction[] {
  if (settlement.status === 'DISPUTE_PENDING') return ['reopen', 'retain'];
  if (settlement.status === 'AWAITING_ADMIN_DECISION') return ['release', 'retain'];
  if (settlement.status === 'REOPENED' && settlement.reopenDeadline &&
      new Date(settlement.reopenDeadline).getTime() <= now &&
      !(settlement.teacherConfirmedAt && settlement.studentConfirmedAt)) {
    return ['release', 'retain'];
  }
  return [];
}
