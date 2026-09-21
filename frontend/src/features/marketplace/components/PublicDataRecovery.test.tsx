import React from 'react';
import { act, render } from '@testing-library/react';
import PublicDataRecovery from './PublicDataRecovery';

const refresh = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({ refresh }),
}));

describe('PublicDataRecovery', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    refresh.mockReset();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('keeps refreshing a failed public render until the server data recovers', () => {
    const view = render(<PublicDataRecovery hasError />);

    act(() => jest.advanceTimersByTime(5_000));
    expect(refresh).toHaveBeenCalledTimes(1);

    act(() => jest.advanceTimersByTime(10_000));
    expect(refresh).toHaveBeenCalledTimes(2);

    view.rerender(<PublicDataRecovery hasError={false} />);
    act(() => jest.advanceTimersByTime(30_000));
    expect(refresh).toHaveBeenCalledTimes(2);
  });
});
