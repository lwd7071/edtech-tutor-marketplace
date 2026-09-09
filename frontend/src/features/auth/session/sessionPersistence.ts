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
    const stored = parsed as { user?: unknown; remember?: unknown };
    const user = isUser(stored.user) ? stored.user : isUser(parsed) ? parsed : null;
    if (!user) return null;
    return {
      user,
      accessToken,
      refreshToken: Cookies.get(REFRESH_TOKEN_COOKIE) ?? null,
      remember: stored.remember === true,
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
  localStorage.setItem(SESSION_KEY, JSON.stringify({ user: session.user, remember: session.remember }));
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
