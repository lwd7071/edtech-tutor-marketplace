import { create } from 'zustand';
import Cookies from 'js-cookie';
import { UserSummary } from '../api/auth';

interface AuthState {
  user: UserSummary | null;
  isAuthenticated: boolean;
  setAuth: (user: UserSummary, accessToken: string, refreshToken?: string, remember?: boolean) => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null, // Note: For persistence across reloads, we could hydrate this from localStorage or a /me endpoint
  isAuthenticated: !!Cookies.get('accessToken'),
  
  setAuth: (user, accessToken, refreshToken, remember = false) => {
    // Determine cookie expiration based on remember me
    const cookieOptions = remember ? { expires: 30 } : {}; // 30 days if remember me
    
    Cookies.set('accessToken', accessToken, cookieOptions);
    if (refreshToken) {
      Cookies.set('refreshToken', refreshToken, cookieOptions);
    }
    
    // Also save user info to localStorage for basic hydration
    if (typeof window !== 'undefined') {
      localStorage.setItem('user', JSON.stringify(user));
    }

    set({ user, isAuthenticated: true });
  },

  clearAuth: () => {
    Cookies.remove('accessToken');
    Cookies.remove('refreshToken');
    if (typeof window !== 'undefined') {
      localStorage.removeItem('user');
    }
    set({ user: null, isAuthenticated: false });
  },
}));
