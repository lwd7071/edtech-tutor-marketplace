import { create } from 'zustand';
import { clearSessionStorage, readSession, writeSession, writeTokens } from './sessionPersistence';

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
  remember: false,
  isAuthenticated: false,
};

export const useAuthStore = create<SessionState>((set, get) => ({
  status: 'booting',
  user: null,
  accessToken: null,
  refreshToken: null,
  remember: false,
  isAuthenticated: false,

  hydrate: () => {
    const session = readSession();
    if (!session) {
      clearSessionStorage();
      set(anonymousState);
      return;
    }
    set({ ...session, status: 'authenticated', isAuthenticated: true });
  },

  establish: (result, remember = false) => {
    const session = {
      user: result.user,
      accessToken: result.accessToken,
      refreshToken: result.refreshToken ?? null,
      remember,
    };
    writeSession(session);
    set({ ...session, status: 'authenticated', isAuthenticated: true });
  },

  rotate: (accessToken, refreshToken) => {
    const current = get();
    const nextRefreshToken = refreshToken === undefined ? current.refreshToken : refreshToken;
    writeTokens(accessToken, nextRefreshToken, current.remember);
    set({ accessToken, refreshToken: nextRefreshToken });
  },

  clear: () => {
    clearSessionStorage();
    set(anonymousState);
  },
}));
