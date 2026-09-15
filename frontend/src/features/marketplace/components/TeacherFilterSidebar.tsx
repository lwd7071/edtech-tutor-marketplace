'use client';

import {useEffect, useState} from 'react';
import {Alert, Button, Input, InputNumber, Select} from 'antd';
import type {TeacherSearchParams, SubjectSummary} from '@/shared/api/public';
import {getPublicProvinces, getPublicWards, type AdministrativeProvince, type AdministrativeWard} from '@/shared/api/public';

export function TeacherFilterSidebar({filters, subjects = [], onChange, onClear}: {
  filters: TeacherSearchParams;
  subjects?: SubjectSummary[];
  onChange: (v: Partial<TeacherSearchParams>) => void;
  onClear: () => void;
}) {
  const [day, setDay] = useState(filters.dayOfWeek);
  const [start, setStart] = useState(filters.startTime || '');
  const [end, setEnd] = useState(filters.endTime || '');
  const [min, setMin] = useState<number | null>(filters.minPrice ?? null);
  const [max, setMax] = useState<number | null>(filters.maxPrice ?? null);
  const [provinces, setProvinces] = useState<AdministrativeProvince[]>([]);
  const [wards, setWards] = useState<AdministrativeWard[]>([]);
  const [locationError, setLocationError] = useState(false);
  const invalid = min !== null && max !== null && min > max;

  useEffect(() => { getPublicProvinces().then(setProvinces).catch(() => setLocationError(true)); }, []);
  useEffect(() => {
    if (!filters.provinceCode) { setWards([]); return; }
    getPublicWards(filters.provinceCode).then(setWards).catch(() => setLocationError(true));
  }, [filters.provinceCode]);

  const clear = () => { setDay(undefined); setStart(''); setEnd(''); setMin(null); setMax(null); setWards([]); onClear(); };
  return <div className="tm-panel tm-stack">
    <div className="tm-toolbar"><strong>Lọc kết quả</strong><Button type="link" onClick={clear}>Xóa bộ lọc</Button></div>
    <div className="tm-field"><label htmlFor="filter-subject">Môn học</label><Select id="filter-subject" allowClear showSearch optionFilterProp="label" placeholder="Tất cả môn học" value={filters.subjectId} options={subjects.map(s => ({label: s.name, value: s.id}))} onChange={subjectId => onChange({subjectId})}/></div>
    <div className="tm-field"><label htmlFor="filter-mode">Hình thức</label><Select id="filter-mode" allowClear placeholder="Tất cả hình thức" value={filters.deliveryMode} options={[{value: 'ONLINE', label: 'Online'}, {value: 'OFFLINE', label: 'Trực tiếp'}]} onChange={deliveryMode => onChange({deliveryMode})}/></div>
    <div className="tm-field"><label htmlFor="filter-province">Nơi ở của gia sư</label><Select id="filter-province" allowClear showSearch optionFilterProp="label" placeholder="Tất cả tỉnh/thành" value={filters.provinceCode} options={provinces.map(p => ({label: p.name, value: p.code}))} onChange={provinceCode => onChange({provinceCode, wardCode: undefined})}/><Select id="filter-ward" allowClear showSearch optionFilterProp="label" disabled={!filters.provinceCode} placeholder="Tất cả xã/phường" value={filters.wardCode} options={wards.map(w => ({label: w.name, value: w.code}))} onChange={wardCode => onChange({wardCode})}/>{locationError && <Alert type="warning" title="Chưa tải được danh sách khu vực"/>}</div>
    <div className="tm-field"><label>Giá trọn gói (VNĐ)</label><InputNumber aria-label="Giá gói tối thiểu" min={0} value={min} onChange={setMin} placeholder="Tối thiểu" style={{width: '100%'}}/><InputNumber aria-label="Giá gói tối đa" min={0} value={max} onChange={setMax} placeholder="Tối đa" style={{width: '100%'}}/>{invalid && <Alert type="warning" title="Giá tối đa phải lớn hơn hoặc bằng tối thiểu"/>}<Button disabled={invalid} onClick={() => onChange({minPrice: min ?? undefined, maxPrice: max ?? undefined})}>Áp dụng giá</Button></div>
    <div className="tm-field"><label htmlFor="filter-rating">Đánh giá</label><Select id="filter-rating" allowClear placeholder="Tất cả đánh giá" value={filters.minRating} options={[4.5, 4, 3].map(value => ({value, label: 'Từ ' + value + ' sao'}))} onChange={minRating => onChange({minRating})}/></div>
    <div className="tm-field"><label htmlFor="filter-day">Ngày và giờ rảnh</label><Select id="filter-day" value={day} placeholder="Chọn ngày" onChange={setDay} options={['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'].map((value, i) => ({value, label: i === 6 ? 'Chủ nhật' : 'Thứ ' + (i + 2)}))}/><Input aria-label="Giờ bắt đầu" type="time" value={start} onChange={e => setStart(e.target.value)}/><Input aria-label="Giờ kết thúc" type="time" value={end} onChange={e => setEnd(e.target.value)}/><Button disabled={!day || !start || !end || start >= end} onClick={() => onChange({dayOfWeek: day, startTime: start, endTime: end})}>Áp dụng giờ</Button>{filters.dayOfWeek && <Button type="link" onClick={() => {setDay(undefined); setStart(''); setEnd(''); onChange({dayOfWeek: undefined, startTime: undefined, endTime: undefined});}}>Bỏ lọc giờ</Button>}</div>
  </div>;
}
