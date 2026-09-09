import { bookingKeys } from './bookingKeys';
import { financeKeys } from '@/features/finance/data/financeKeys';
import { studentPackageKeys } from '@/features/student-packages/data/studentPackageKeys';

describe('feature query keys', () => {
  it('include every filter that can change a response', () => {
    expect(bookingKeys.list('teacher', { page: 1, status: 'SCHEDULED' }))
      .not.toEqual(bookingKeys.list('student', { page: 1, status: 'SCHEDULED' }));
    expect(bookingKeys.trialRequests('PENDING', 0, 10))
      .not.toEqual(bookingKeys.trialRequests('PENDING', 0, 20));
    expect(studentPackageKeys.list('ACTIVE', 0, 20, 'createdAt,desc'))
      .not.toEqual(studentPackageKeys.list('ACTIVE', 0, 20, 'expiresAt,asc'));
    expect(financeKeys.ledger(0, 20)).not.toEqual(financeKeys.ledger(1, 20));
  });

  it('exposes stable prefixes for targeted invalidation', () => {
    expect(bookingKeys.detail('teacher', 'booking-1').slice(0, 2)).toEqual(bookingKeys.details());
    expect(studentPackageKeys.list('ACTIVE', 0, 20).slice(0, 2)).toEqual(studentPackageKeys.lists());
    expect(financeKeys.wallet()).toEqual(['finance', 'wallet']);
  });
});
