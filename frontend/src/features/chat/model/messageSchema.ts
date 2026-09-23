import { z } from 'zod';

export const incomingMessageSchema = z.object({
  id: z.string().min(1),
  clientMessageId: z.string().min(1).optional(),
  conversationId: z.string().min(1),
  senderId: z.string().min(1),
  senderName: z.string().optional(),
  senderAvatar: z.string().nullable().optional(),
  content: z.string().nullable().optional(),
  messageType: z.enum(['TEXT', 'IMAGE', 'FILE']).optional(),
  attachmentId: z.string().nullable().optional(),
  attachmentUrl: z.string().nullable().optional(),
  attachment: z.object({
    id: z.string(), secureUrl: z.string(), originalFilename: z.string(),
    mimeType: z.string(), fileSize: z.number(),
  }).nullable().optional(),
  sentAt: z.string().optional(),
  createdAt: z.string().optional(),
});

export type IncomingMessage = z.infer<typeof incomingMessageSchema>;

export function parseIncomingMessage(body: string): IncomingMessage | null {
  try {
    const result = incomingMessageSchema.safeParse(JSON.parse(body));
    return result.success ? result.data : null;
  } catch {
    return null;
  }
}
