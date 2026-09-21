jest.mock('server-only', () => ({}));

import { getGlobalRankingServer, getPublicSubjectsServer, PublicApiError } from './public.server';

describe('public server API', () => {
  const fetchMock = jest.fn();
  const oldBackendUrl = process.env.BACKEND_API_URL;

  beforeEach(() => {
    global.fetch = fetchMock;
    process.env.BACKEND_API_URL = 'https://backend.example.test/';
    fetchMock.mockReset();
  });

  afterEach(() => {
    if (oldBackendUrl === undefined) delete process.env.BACKEND_API_URL;
    else process.env.BACKEND_API_URL = oldBackendUrl;
  });

  it('uses encoded query parameters and ranking cache policy', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ success: true, message: null, data: [], errors: null, meta: { page: 1, size: 10, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: true } }),
    });

    await getGlobalRankingServer('subject A', 1, 10);

    expect(fetchMock).toHaveBeenCalledWith(
      'https://backend.example.test/api/public/teachers/ranking?subjectId=subject+A&page=1&size=10',
      expect.objectContaining({
        cache: 'force-cache',
        next: { revalidate: 60, tags: ['public-ranking', 'public-ranking:subject A'] },
        signal: expect.any(AbortSignal),
      }),
    );
  });

  it('maps unsuccessful envelopes to a typed public API error', async () => {
    fetchMock.mockResolvedValue({ ok: false, status: 404, json: async () => ({ success: false, message: 'Không tìm thấy', data: null, errors: [{ code: 'RESOURCE_NOT_FOUND', field: null, message: 'Không tìm thấy' }], meta: null }) });
    await expect(getPublicSubjectsServer()).rejects.toEqual(expect.objectContaining({ status: 404, code: 'RESOURCE_NOT_FOUND' } satisfies Partial<PublicApiError>));
  });
});
