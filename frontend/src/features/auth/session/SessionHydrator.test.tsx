import React from 'react';
import { render, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from './sessionStore';
import { SessionHydrator } from './SessionHydrator';
import { publishSessionChange, writeSession } from './sessionPersistence';

const replace = jest.fn();
jest.mock('next/navigation', () => ({
  useRouter: () => ({ replace }),
  usePathname: () => '/student/bookings',
}));

const student = { id: 'student-1', email: 'student@example.test', fullName: 'Student', role: 'STUDENT' as const, status: 'ACTIVE' };
const teacher = { id: 'teacher-1', email: 'teacher@example.test', fullName: 'Teacher', role: 'TEACHER' as const, status: 'APPROVED' };

describe('SessionHydrator cross-tab synchronization', () => {
  beforeEach(() => {
    replace.mockReset();
    useAuthStore.getState().clear();
    localStorage.clear();
  });

  it('updates the receiving tab, clears query cache, and redirects to the new role home', async () => {
    const queryClient = new QueryClient();
    useAuthStore.getState().establish({ user: student, accessToken: 'student-access', refreshToken: 'student-refresh' });
    render(<QueryClientProvider client={queryClient}><SessionHydrator><div>workspace</div></SessionHydrator></QueryClientProvider>);
    queryClient.setQueryData(['student', 'student-1'], { studentId: 'student-1' });

    writeSession({ user: teacher, accessToken: 'teacher-access', refreshToken: 'teacher-refresh', remember: false, sessionId: crypto.randomUUID() });
    const change = publishSessionChange('establish', crypto.randomUUID())!;
    window.dispatchEvent(new StorageEvent('storage', { key: 'tutor-match.session.revision', newValue: JSON.stringify(change) }));

    await waitFor(() => expect(useAuthStore.getState().user?.role).toBe('TEACHER'));
    expect(replace).toHaveBeenCalledWith('/teacher');
    expect(queryClient.getQueryData(['student', 'student-1'])).toBeUndefined();
  });
});
