import Cookies from 'js-cookie';
import type { User } from './sessionStore';

const SESSION_KEY = 'tutor-match.session';
const LEGACY_USER_KEY = 'user';
const ACCESS_TOKEN_COOKIE = 'accessToken';
const REFRESH_TOKEN_COOKIE = 'refreshToken';

export interface PersistedSession {
  user: User;
  accessToken: string;
  refreshToken: string | null;
  remember: boolean;
  sessionId: string | null;
}

export type SessionChangeKind = 'establish' | 'rotate' | 'clear';

export interface SessionChange {
  revision: string;
  kind: SessionChangeKind;
  sessionId: string | null;
}

export function createSessionId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') return crypto.randomUUID();
  if (typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function') {
    const bytes = crypto.getRandomValues(new Uint8Array(16));
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
  }
  throw new Error('Secure random number generation is unavailable');
}

function cookieOptions(remember: boolean) {
  return {
    sameSite: 'lax' as const,
    secure: typeof window !== 'undefined' && window.location.protocol === 'https:',
    ...(remember ? { expires: 30 } : {}),
  };
}

function isUser(value: unknown): value is User {
  if (!value || typeof value !== 'object') return false;
  const user = value as Partial<User>;
  return typeof user.id === 'string'
    && typeof user.email === 'string'
    && typeof user.fullName === 'string'
    && (user.role === 'STUDENT' || user.role === 'TEACHER' || user.role === 'ADMIN')
    && typeof user.status === 'string';
}

export function readSession(): PersistedSession | null {
  if (typeof window === 'undefined') return null;
  const accessToken = Cookies.get(ACCESS_TOKEN_COOKIE);
  if (!accessToken) return null;

  try {
    const raw = localStorage.getItem(SESSION_KEY) ?? localStorage.getItem(LEGACY_USER_KEY);
    if (!raw) return null;
    const parsed: unknown = JSON.parse(raw);
    const stored = parsed as { user?: unknown; remember?: unknown; sessionId?: unknown };
    const user = isUser(stored.user) ? stored.user : isUser(parsed) ? parsed : null;
    if (!user) return null;
    return {
      user,
      accessToken,
      refreshToken: Cookies.get(REFRESH_TOKEN_COOKIE) ?? null,
      remember: stored.remember === true,
      sessionId: typeof stored.sessionId === 'string' ? stored.sessionId : null,
    };
  } catch {
    return null;
  }
}

export function writeSession(session: PersistedSession): void {
  if (typeof window === 'undefined') return;
  const options = cookieOptions(session.remember);
  Cookies.set(ACCESS_TOKEN_COOKIE, session.accessToken, options);
  if (session.refreshToken) Cookies.set(REFRESH_TOKEN_COOKIE, session.refreshToken, options);
  else Cookies.remove(REFRESH_TOKEN_COOKIE);
  localStorage.setItem(SESSION_KEY, JSON.stringify({ user: session.user, remember: session.remember, sessionId: session.sessionId }));
  localStorage.removeItem(LEGACY_USER_KEY);
}

export function writeTokens(accessToken: string, refreshToken: string | null, remember: boolean): void {
  const options = cookieOptions(remember);
  Cookies.set(ACCESS_TOKEN_COOKIE, accessToken, options);
  if (refreshToken) Cookies.set(REFRESH_TOKEN_COOKIE, refreshToken, options);
  else Cookies.remove(REFRESH_TOKEN_COOKIE);
}

export function clearSessionStorage(): void {
  Cookies.remove(ACCESS_TOKEN_COOKIE);
  Cookies.remove(REFRESH_TOKEN_COOKIE);
  if (typeof window !== 'undefined') {
    localStorage.removeItem(SESSION_KEY);
    localStorage.removeItem(LEGACY_USER_KEY);
  }
}

export function publishSessionChange(kind: SessionChangeKind, sessionId: string | null): SessionChange | null {
  if (typeof window === 'undefined') return null;
  const change = { revision: createSessionId(), kind, sessionId };
  localStorage.setItem('tutor-match.session.revision', JSON.stringify(change));
  return change;
}

export function readSessionChange(): SessionChange | null {
  if (typeof window === 'undefined') return null;
  try {
    const value = JSON.parse(localStorage.getItem('tutor-match.session.revision') ?? 'null') as Partial<SessionChange> | null;
    if (!value || typeof value.revision !== 'string' || !['establish', 'rotate', 'clear'].includes(value.kind ?? '')) return null;
    return { revision: value.revision, kind: value.kind as SessionChangeKind, sessionId: typeof value.sessionId === 'string' ? value.sessionId : null };
  } catch {
    return null;
  }
}
