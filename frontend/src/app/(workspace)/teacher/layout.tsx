'use client';
import WorkspaceLayout from '@/shared/components/layout/WorkspaceLayout';
import {RoleGuard} from '@/shared/components/guards/RoleGuard';
export default function TeacherLayout({children}:{children:React.ReactNode}){return <RoleGuard allowedRoles={['TEACHER']}><WorkspaceLayout role="TEACHER">{children}</WorkspaceLayout></RoleGuard>;}

