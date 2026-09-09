import Cookies from 'js-cookie';
import { useAuthStore } from './sessionStore';

const user = {
  id: 'student-1',
  email: 'student@example.test',
  fullName: 'Student Test',
  role: 'STUDENT' as const,
  status: 'ACTIVE',
};

describe('session store', () => {
  beforeEach(() => {
    useAuthStore.getState().clear();
    localStorage.clear();
  });

  it('establishes, rotates, and clears one canonical session', () => {
    useAuthStore.getState().establish({
      user,
      accessToken: 'access-1',
      refreshToken: 'refresh-1',
    }, true);

    expect(useAuthStore.getState()).toMatchObject({
      status: 'authenticated',
      user,
      accessToken: 'access-1',
      refreshToken: 'refresh-1',
      isAuthenticated: true,
    });
    expect(Cookies.get('accessToken')).toBe('access-1');

    useAuthStore.getState().rotate('access-2', 'refresh-2');
    expect(useAuthStore.getState()).toMatchObject({
      user,
      accessToken: 'access-2',
      refreshToken: 'refresh-2',
    });

    useAuthStore.getState().clear();
    expect(useAuthStore.getState()).toMatchObject({
      status: 'anonymous',
      user: null,
      accessToken: null,
      isAuthenticated: false,
    });
  });

  it('rejects malformed persisted users during hydration', () => {
    Cookies.set('accessToken', 'stale-token');
    localStorage.setItem('tutor-match.session', JSON.stringify({ user: { id: 1 } }));

    useAuthStore.setState({ status: 'booting' });
    useAuthStore.getState().hydrate();

    expect(useAuthStore.getState().status).toBe('anonymous');
    expect(Cookies.get('accessToken')).toBeUndefined();
  });
});
