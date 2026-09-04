import { create } from 'zustand';

export type UserRole = 'STUDENT' | 'TEACHER' | 'ADMIN';
export type UserStatus = 'ACTIVE' | 'LOCKED' | 'PENDING' | 'APPROVED'; // Teacher has APPROVED

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  status: UserStatus;
  avatarUrl: string | null;
}

interface AuthState {
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  setUser: (user: User | null) => void;
  setToken: (token: string | null) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  accessToken: null,
  isAuthenticated: false,
  setUser: (user) => set({ user, isAuthenticated: !!user }),
  setToken: (accessToken) => set({ accessToken }),
  logout: () => set({ user: null, accessToken: null, isAuthenticated: false }),
}));
