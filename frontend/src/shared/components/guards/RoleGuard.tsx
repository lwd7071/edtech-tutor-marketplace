'use client';
import {useEffect} from 'react';
import {usePathname,useRouter} from 'next/navigation';
import {useAuthStore,type UserRole} from '@/features/auth';
import {Result,Button} from 'antd';
import Link from 'next/link';
export function RoleGuard({children,allowedRoles,fallback}:{children:React.ReactNode;allowedRoles:UserRole[];fallback?:React.ReactNode}){
 const {status,user,isAuthenticated}=useAuthStore();const router=useRouter();const pathname=usePathname();
 useEffect(()=>{if(status==='anonymous')router.replace('/auth/login?redirect='+encodeURIComponent(pathname));},[status,router,pathname]);
 if(status==='booting')return null;
 if(!isAuthenticated||!user)return <Result title="Đang chuyển đến đăng nhập"/>;
 if(!allowedRoles.includes(user.role))return fallback||<Result status="403" title="Bạn không có quyền truy cập" extra={<Link href="/"><Button>Về trang chủ</Button></Link>}/>;
 if(['LOCKED','DISABLED'].includes(user.status))return <Result status="warning" title="Tài khoản hiện không thể sử dụng" subTitle="Vui lòng liên hệ đơn vị vận hành để kiểm tra trạng thái tài khoản."/>;
 if(user.status==='PENDING_VERIFICATION')return <Result status="info" title="Xác minh email để tiếp tục" extra={<Link href="/auth/verify-email">Xác minh email</Link>}/>;
 return <>{children}</>;
}
