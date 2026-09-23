import dayjs from 'dayjs';
import 'dayjs/locale/vi';
import utc from 'dayjs/plugin/utc';
import timezone from 'dayjs/plugin/timezone';

dayjs.extend(utc);
dayjs.extend(timezone);

export const VIETNAM_TIME_ZONE = 'Asia/Ho_Chi_Minh';

const inVietnam = (value: string | Date) => dayjs(value).tz(VIETNAM_TIME_ZONE);

export const formatVietnamDate = (value: string | Date) => inVietnam(value).format('DD/MM/YYYY');
export const formatVietnamTime = (value: string | Date) => inVietnam(value).format('HH:mm');
export const formatVietnamDateTime = (value: string | Date) => inVietnam(value).format('HH:mm, DD/MM/YYYY');
export const formatVietnamSession = (start: string | Date, end: string | Date) =>
  `${formatVietnamTime(start)} – ${formatVietnamTime(end)} · ${inVietnam(start).locale('vi').format('dddd, DD/MM/YYYY')}`;

/** Interpret an HTML datetime-local value as wall time in Vietnam, regardless of device timezone. */
export function vietnamLocalInputToIso(value: string): string {
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value)) throw new RangeError('Invalid local date/time');
  const parsed = dayjs.tz(value, VIETNAM_TIME_ZONE);
  if (parsed.format('YYYY-MM-DDTHH:mm') !== value) throw new RangeError('Invalid local date/time');
  return parsed.toISOString();
}

export const toVietnamLocalInput = (value: string | Date) => inVietnam(value).format('YYYY-MM-DDTHH:mm');

export function vietnamDateRange(date: string): { from: string; to: string } {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) throw new RangeError('Invalid date');
  const start = dayjs.tz(`${date}T00:00`, VIETNAM_TIME_ZONE);
  if (start.format('YYYY-MM-DD') !== date) throw new RangeError('Invalid date');
  return { from: start.toISOString(), to: start.add(1, 'day').toISOString() };
}
