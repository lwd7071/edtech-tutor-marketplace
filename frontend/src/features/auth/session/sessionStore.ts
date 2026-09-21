import { create } from 'zustand';
import { clearSessionStorage, createSessionId, publishSessionChange, readSession, readSessionChange, writeSession, writeTokens } from './sessionPersistence';

export type UserRole = 'STUDENT' | 'TEACHER' | 'ADMIN';
export type UserStatus = 'ACTIVE' | 'LOCKED' | 'PENDING' | 'APPROVED' | string;
export type SessionStatus = 'booting' | 'anonymous' | 'authenticated';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  status: UserStatus;
  avatarUrl?: string | null;
}

export type UserSummary = User;

export interface AuthResultLike {
  user: User;
  accessToken: string;
  refreshToken?: string | null;
}

interface SessionState {
  status: SessionStatus;
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  sessionId: string | null;
  revision: string | null;
  remember: boolean;
  isAuthenticated: boolean;
  hydrate: () => void;
  establish: (result: AuthResultLike, remember?: boolean) => void;
  rotate: (accessToken: string, refreshToken?: string | null) => void;
  clear: () => void;
}

const anonymousState = {
  status: 'anonymous' as const,
  user: null,
  accessToken: null,
  refreshToken: null,
  sessionId: null,
  revision: null,
  remember: false,
  isAuthenticated: false,
};

export const useAuthStore = create<SessionState>((set, get) => ({
  status: 'booting',
  user: null,
  accessToken: null,
  refreshToken: null,
  sessionId: null,
  revision: null,
  remember: false,
  isAuthenticated: false,

  hydrate: () => {
    const session = readSession();
    if (!session) {
      clearSessionStorage();
      set({ ...anonymousState, revision: readSessionChange()?.revision ?? null });
      return;
    }
    const upgradedSession = session.sessionId ? session : { ...session, sessionId: createSessionId() };
    if (!session.sessionId) writeSession(upgradedSession);
    set({ ...upgradedSession, status: 'authenticated', isAuthenticated: true, revision: readSessionChange()?.revision ?? null });
  },

  establish: (result, remember = false) => {
    const session = {
      user: result.user,
      accessToken: result.accessToken,
      refreshToken: result.refreshToken ?? null,
      remember,
      sessionId: createSessionId(),
    };
    writeSession(session);
    const change = publishSessionChange('establish', session.sessionId);
    set({ ...session, status: 'authenticated', isAuthenticated: true, revision: change?.revision ?? null });
  },

  rotate: (accessToken, refreshToken) => {
    const current = get();
    const nextRefreshToken = refreshToken === undefined ? current.refreshToken : refreshToken;
    writeTokens(accessToken, nextRefreshToken, current.remember);
    const change = publishSessionChange('rotate', current.sessionId);
    set({ accessToken, refreshToken: nextRefreshToken, revision: change?.revision ?? current.revision });
  },

  clear: () => {
    clearSessionStorage();
    const change = publishSessionChange('clear', get().sessionId);
    set({ ...anonymousState, revision: change?.revision ?? null });
  },

}));
