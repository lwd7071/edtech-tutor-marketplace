'use client';
import WorkspaceLayout from '@/shared/components/layout/WorkspaceLayout';
import { RoleGuard } from '@/shared/components/guards/RoleGuard';
export default function AdminLayout({children}:{children:React.ReactNode}) { return <RoleGuard allowedRoles={['ADMIN']}><WorkspaceLayout role="ADMIN">{children}</WorkspaceLayout></RoleGuard>; }

