import {
  formatVietnamDate,
  formatVietnamDateTime,
  formatVietnamSession,
  formatVietnamTime,
  toVietnamLocalInput,
  vietnamDateRange,
  vietnamLocalInputToIso,
} from './vietnamTime';

describe('Vietnam time boundary', () => {
  it('formats an instant across the Vietnam day boundary', () => {
    expect(formatVietnamDate('2026-09-21T18:30:00Z')).toBe('22/09/2026');
    expect(formatVietnamTime('2026-09-21T18:30:00Z')).toBe('01:30');
    expect(formatVietnamDateTime('2026-09-21T18:30:00Z')).toBe('01:30, 22/09/2026');
    expect(formatVietnamSession('2026-09-21T18:30:00Z', '2026-09-21T19:30:00Z')).toContain('01:30 – 02:30');
  });

  it('converts local form input and date filter without using the device timezone', () => {
    expect(vietnamLocalInputToIso('2026-09-22T19:00')).toBe('2026-09-22T12:00:00.000Z');
    expect(toVietnamLocalInput('2026-09-22T12:00:00Z')).toBe('2026-09-22T19:00');
    expect(vietnamDateRange('2026-09-22')).toEqual({
      from: '2026-09-21T17:00:00.000Z',
      to: '2026-09-22T17:00:00.000Z',
    });
    expect(() => vietnamLocalInputToIso('2026-02-30T19:00')).toThrow(RangeError);
  });
});
