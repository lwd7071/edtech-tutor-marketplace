import { NextRequest, NextResponse } from 'next/server';
import { revalidateTag } from 'next/cache';
import { z } from 'zod';

const RevalidatePayloadSchema = z.object({
  teacherId: z.string().uuid(),
  eventType: z.enum([
    'TEACHER_CREDENTIAL_APPROVED',
    'TEACHER_CREDENTIAL_REVOKED',
    'TEACHER_RESIDENCE_UPDATED',
  ]),
});

export async function POST(request: NextRequest) {
  const configuredSecret = process.env.INTERNAL_REVALIDATE_SECRET;
  const secretHeader = request.headers.get('x-internal-secret');

  if (!configuredSecret || !secretHeader || secretHeader !== configuredSecret) {
    return NextResponse.json(
      { success: false, message: 'Unauthorized' },
      { status: 401 }
    );
  }

  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json(
      { success: false, message: 'Invalid JSON body' },
      { status: 400 }
    );
  }

  const parseResult = RevalidatePayloadSchema.safeParse(body);
  if (!parseResult.success) {
    return NextResponse.json(
      {
        success: false,
        message: 'Invalid request payload',
        errors: parseResult.error.flatten(),
      },
      { status: 400 }
    );
  }

  const { teacherId, eventType } = parseResult.data;
  const tags: string[] = [];

  switch (eventType) {
    case 'TEACHER_CREDENTIAL_APPROVED':
    case 'TEACHER_CREDENTIAL_REVOKED':
      tags.push(`public-teacher:${teacherId}`);
      break;
    case 'TEACHER_RESIDENCE_UPDATED':
      tags.push(`public-teacher:${teacherId}`);
      tags.push('public-teachers');
      break;
  }

  for (const tag of tags) {
    revalidateTag(tag, { expire: 0 });
  }

  return NextResponse.json({
    success: true,
    revalidated: tags,
  });
}
