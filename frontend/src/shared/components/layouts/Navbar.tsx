'use client';
import {useState} from 'react';
import Link from 'next/link';
import {usePathname} from 'next/navigation';
import {Button, Drawer, Dropdown} from 'antd';
import {MenuOutlined, UserOutlined} from '@ant-design/icons';
import {useAuthStore} from '@/features/auth';
import {roleHome} from '@/shared/lib/navigation';
export default function Navbar() {
 const [open,setOpen]=useState(false);
 const pathname=usePathname();
 const {user,isAuthenticated,logout}=useAuthStore();
 const home=roleHome(user?.role);
 const links=[{href:'/teachers',label:'Tìm gia sư'},{href:'/subjects',label:'Môn học'},{href:'/ranking',label:'Xếp hạng'},{href:'/how-it-works',label:'Cách hoạt động'}];
 const navigation=links.map(item=><Link key={item.href} href={item.href} aria-current={pathname===item.href?'page':undefined} onClick={()=>setOpen(false)}>{item.label}</Link>);
 const account=isAuthenticated&&user?<Dropdown menu={{items:[
 {key:'home',label:<Link href={home}>Không gian của tôi</Link>},
 ...(user.role!=='ADMIN'?[{key:'profile',label:<Link href={home+'/profile'}>Hồ sơ</Link>},{key:'notifications',label:<Link href={home+'/notifications'}>Thông báo</Link>}]:[]),
 {key:'logout',label:'Đăng xuất',danger:true,onClick:logout},
 ]}} trigger={['click']}><Button icon={<UserOutlined />}>{user.fullName}</Button></Dropdown>:<div className="tm-inline"><Link href="/auth/login">Đăng nhập</Link><Link className="tm-button" href="/auth/register">Bắt đầu học</Link></div>;
 return <header className="tm-header"><div className="tm-container tm-header-inner"><Link href="/" className="tm-brand">tutor<span>match</span><span className="tm-brand-dot">.</span></Link><nav className="tm-desktop-nav" aria-label="Điều hướng chính">{navigation}</nav><div className="tm-desktop-nav">{account}</div><Button className="tm-mobile-only" aria-label="Mở menu" icon={<MenuOutlined />} onClick={()=>setOpen(true)}/><Drawer title="Tutor Match" open={open} onClose={()=>setOpen(false)}><nav className="tm-workspace-nav">{navigation}<Link href="/become-a-tutor" onClick={()=>setOpen(false)}>Trở thành gia sư</Link></nav>{account}</Drawer></div></header>;
}
