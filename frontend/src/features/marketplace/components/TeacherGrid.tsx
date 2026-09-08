'use client';
import Link from 'next/link';
import {Skeleton as AntSkeleton} from 'antd';
import TeacherCard from '@/shared/components/data-display/TeacherCard';
import {EmptyState} from '@/shared/components/feedback/EmptyState';
import {ErrorState} from '@/shared/components/feedback/ErrorState';
import type {TeacherCard as Teacher} from '@/shared/api/public';
export default function TeacherGrid({teachers,isLoading,isError,onRetry}:{teachers:Teacher[];isLoading?:boolean;isError?:boolean;onRetry?:()=>void}){
 if(isError)return <ErrorState title="Chưa tải được danh sách gia sư" actionText="Tải lại" onRetry={onRetry}/>;
 if(isLoading)return <div className="tm-teacher-grid">{[1,2,3].map(i=><div className="tm-panel" key={i}><AntSkeleton active avatar/></div>)}</div>;
 if(!teachers?.length)return <EmptyState title="Không tìm thấy gia sư" description="Thử bỏ bớt bộ lọc hoặc chọn môn học khác."/>;
 return <div className="tm-teacher-grid">{teachers.map(t=><Link className="tm-card-link" href={'/teachers/'+t.id} key={t.id}><TeacherCard id={t.id} name={t.fullName||'Gia sư'} avatarUrl={t.avatarUrl} isVerified={t.verifiedBadge} subjects={t.subjects?.map(s=>typeof s==='string'?s:s.name)} rating={t.averageRating} reviewCount={t.reviewCount} lowestPrice={t.startingPriceVnd} yearsOfExperience={t.yearsOfExperience} supportsOnline={t.supportsOnline} supportsOffline={t.supportsOffline}/></Link>)}</div>;
}
