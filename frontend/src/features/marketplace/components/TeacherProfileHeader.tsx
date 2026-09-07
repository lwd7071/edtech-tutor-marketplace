'use client';
import type {TeacherPublicDetail} from '@/shared/api/public';
import {Avatar} from '@/shared/components/data-display/Avatar';
export function TeacherProfileHeader({teacher}:{teacher:TeacherPublicDetail}){
 return <header className="tm-panel tm-profile-header"><Avatar src={teacher.avatarUrl} alt={teacher.fullName} size="xl"/><div style={{minWidth:0}}><p className="tm-eyebrow">Hồ sơ gia sư</p><h1>{teacher.fullName}</h1><div className="tm-chips">{teacher.subjects.map(s=><span className="tm-chip" key={s}>{s}</span>)}</div><p>{teacher.yearsOfExperience} năm kinh nghiệm {teacher.supportsOnline?'· Online ':''}{teacher.supportsOffline?'· Trực tiếp':''}</p><div className="tm-rating">{teacher.reviewCount>0?<><span className="tm-star">★</span> <strong>{teacher.averageRating.toFixed(1)}</strong> ({teacher.reviewCount} đánh giá)</>:'Chưa có đánh giá'}</div>{teacher.locationAddress&&<p>{teacher.locationAddress}</p>}{teacher.languages?.length>0&&<p>Ngôn ngữ: {teacher.languages.join(', ')}</p>}</div></header>;
}
