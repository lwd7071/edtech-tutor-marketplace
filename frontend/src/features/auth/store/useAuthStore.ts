import { create } from 'zustand';
import Cookies from 'js-cookie';

export type UserRole = 'STUDENT' | 'TEACHER' | 'ADMIN';
export type UserStatus = 'ACTIVE' | 'LOCKED' | 'PENDING' | 'APPROVED';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  status: string; // use string to match UserSummary
  avatarUrl?: string | null;
}

export type UserSummary = User;

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  
  // From B
  setUser: (user: User | null) => void;
  setToken: (token: string | null) => void;
  setTokens: (accessToken: string | null, refreshToken?: string | null) => void;
  logout: () => void;

  // From A
  setAuth: (user: User, accessToken: string, refreshToken?: string, remember?: boolean) => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  accessToken: null,
  refreshToken: null,
  isAuthenticated: !!Cookies.get('accessToken'), // Initialize based on cookie like A did

  // From B
  setUser: (user) => set({ user, isAuthenticated: !!user }),
  setToken: (accessToken) => set({ accessToken }),
  setTokens: (accessToken, refreshToken = null) => {
    if (accessToken) {
      Cookies.set('accessToken', accessToken);
    } else {
      Cookies.remove('accessToken');
    }
    
    if (refreshToken) {
      Cookies.set('refreshToken', refreshToken);
    } else if (refreshToken === null) {
      Cookies.remove('refreshToken');
    }
    
    set({ accessToken, refreshToken });
  },
  logout: () => {
    Cookies.remove('accessToken');
    Cookies.remove('refreshToken');
    if (typeof window !== 'undefined') {
      localStorage.removeItem('user');
    }
    set({ user: null, accessToken: null, refreshToken: null, isAuthenticated: false });
  },

  // From A
  setAuth: (user, accessToken, refreshToken, remember = false) => {
    const cookieOptions = remember ? { expires: 30 } : {};
    
    Cookies.set('accessToken', accessToken, cookieOptions);
    if (refreshToken) {
      Cookies.set('refreshToken', refreshToken, cookieOptions);
    }
    
    if (typeof window !== 'undefined') {
      localStorage.setItem('user', JSON.stringify(user));
    }

    set({ user, accessToken, refreshToken: refreshToken || null, isAuthenticated: true });
  },

  clearAuth: () => {
    Cookies.remove('accessToken');
    Cookies.remove('refreshToken');
    if (typeof window !== 'undefined') {
      localStorage.removeItem('user');
    }
    set({ user: null, accessToken: null, refreshToken: null, isAuthenticated: false });
  },
}));
