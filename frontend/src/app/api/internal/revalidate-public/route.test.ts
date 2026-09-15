/**
 * @jest-environment node
 */
import { NextRequest } from 'next/server';
import { POST } from './route';
import { revalidateTag } from 'next/cache';

jest.mock('next/cache', () => ({
  revalidateTag: jest.fn(),
}));

describe('POST /api/internal/revalidate-public', () => {
  const originalEnv = process.env;
  const validSecret = 'test-internal-secret-value-123';
  const teacherId = '11111111-2222-4333-8444-555555555555';

  beforeEach(() => {
    jest.clearAllMocks();
    process.env = { ...originalEnv, INTERNAL_REVALIDATE_SECRET: validSecret };
  });

  afterEach(() => {
    process.env = originalEnv;
  });

  const createRequest = (body: unknown, secretHeader?: string) => {
    const headers = new Headers();
    headers.set('Content-Type', 'application/json');
    if (secretHeader !== undefined) {
      headers.set('x-internal-secret', secretHeader);
    }
    return new NextRequest('http://localhost:3000/api/internal/revalidate-public', {
      method: 'POST',
      headers,
      body: typeof body === 'string' ? body : JSON.stringify(body),
    });
  };

  it('rejects request with 401 when secret header is missing', async () => {
    const req = createRequest({ teacherId, eventType: 'TEACHER_RESIDENCE_UPDATED' });
    const res = await POST(req);
    expect(res.status).toBe(401);
    const data = await res.json();
    expect(data.success).toBe(false);
    expect(revalidateTag).not.toHaveBeenCalled();
  });

  it('rejects request with 401 when secret header is invalid', async () => {
    const req = createRequest({ teacherId, eventType: 'TEACHER_RESIDENCE_UPDATED' }, 'wrong-secret');
    const res = await POST(req);
    expect(res.status).toBe(401);
    expect(revalidateTag).not.toHaveBeenCalled();
  });

  it('rejects request with 400 when body is not JSON', async () => {
    const req = createRequest('invalid-json', validSecret);
    const res = await POST(req);
    expect(res.status).toBe(400);
    expect(revalidateTag).not.toHaveBeenCalled();
  });

  it('rejects request with 400 when teacherId is not a UUID', async () => {
    const req = createRequest({ teacherId: 'not-a-uuid', eventType: 'TEACHER_RESIDENCE_UPDATED' }, validSecret);
    const res = await POST(req);
    expect(res.status).toBe(400);
    expect(revalidateTag).not.toHaveBeenCalled();
  });

  it('rejects request with 400 when eventType is not in allowlist', async () => {
    const req = createRequest({ teacherId, eventType: 'UNKNOWN_EVENT' }, validSecret);
    const res = await POST(req);
    expect(res.status).toBe(400);
    expect(revalidateTag).not.toHaveBeenCalled();
  });

  it('revalidates public-teacher tag on TEACHER_CREDENTIAL_APPROVED with expire: 0', async () => {
    const req = createRequest({ teacherId, eventType: 'TEACHER_CREDENTIAL_APPROVED' }, validSecret);
    const res = await POST(req);
    expect(res.status).toBe(200);
    const data = await res.json();
    expect(data.success).toBe(true);
    expect(data.revalidated).toEqual([`public-teacher:${teacherId}`]);
    expect(revalidateTag).toHaveBeenCalledTimes(1);
    expect(revalidateTag).toHaveBeenCalledWith(`public-teacher:${teacherId}`, { expire: 0 });
  });

  it('revalidates public-teacher tag on TEACHER_CREDENTIAL_REVOKED with expire: 0', async () => {
    const req = createRequest({ teacherId, eventType: 'TEACHER_CREDENTIAL_REVOKED' }, validSecret);
    const res = await POST(req);
    expect(res.status).toBe(200);
    const data = await res.json();
    expect(data.success).toBe(true);
    expect(data.revalidated).toEqual([`public-teacher:${teacherId}`]);
    expect(revalidateTag).toHaveBeenCalledTimes(1);
    expect(revalidateTag).toHaveBeenCalledWith(`public-teacher:${teacherId}`, { expire: 0 });
  });

  it('revalidates public-teacher and public-teachers on TEACHER_RESIDENCE_UPDATED with expire: 0', async () => {
    const req = createRequest({ teacherId, eventType: 'TEACHER_RESIDENCE_UPDATED' }, validSecret);
    const res = await POST(req);
    expect(res.status).toBe(200);
    const data = await res.json();
    expect(data.success).toBe(true);
    expect(data.revalidated).toEqual([`public-teacher:${teacherId}`, 'public-teachers']);
    expect(revalidateTag).toHaveBeenCalledTimes(2);
    expect(revalidateTag).toHaveBeenCalledWith(`public-teacher:${teacherId}`, { expire: 0 });
    expect(revalidateTag).toHaveBeenCalledWith('public-teachers', { expire: 0 });
  });
});
