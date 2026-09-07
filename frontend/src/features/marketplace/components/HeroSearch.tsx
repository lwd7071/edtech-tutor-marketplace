'use client';
import {useState} from 'react';
import {useRouter} from 'next/navigation';
import {Button,Select} from 'antd';
import {SearchOutlined} from '@ant-design/icons';
import type {SubjectSummary} from '@/shared/api/public';
export default function HeroSearch({subjects=[]}:{subjects?:SubjectSummary[]}) {
 const router=useRouter(); const [subject,setSubject]=useState<string>(); const [mode,setMode]=useState<string>();const [price,setPrice]=useState<string>();
 return <form className="tm-search-form" onSubmit={e=>{e.preventDefault();const query=new URLSearchParams();if(subject)query.set('subjectId',subject);if(mode)query.set('deliveryMode',mode);if(price)query.set('maxPrice',price);router.push('/teachers?'+query.toString());}}>
 <div className="tm-field"><label htmlFor="hero-subject">Bạn muốn học môn gì?</label><Select id="hero-subject" size="large" showSearch optionFilterProp="label" allowClear placeholder="Tất cả môn học" value={subject} onChange={setSubject} options={subjects.map(s=>({value:s.id,label:s.name}))}/></div>
 <div className="tm-field"><label htmlFor="hero-mode">Hình thức học</label><Select id="hero-mode" size="large" allowClear placeholder="Tất cả hình thức" value={mode} onChange={setMode} options={[{value:'ONLINE',label:'Online'},{value:'OFFLINE',label:'Trực tiếp'}]}/></div>
 <div className="tm-field"><label htmlFor="hero-price">Ngân sách mỗi gói</label><Select id="hero-price" size="large" allowClear placeholder="Chọn mức giá" value={price} onChange={setPrice} options={[{value:'500000',label:'Đến 500.000đ'},{value:'1000000',label:'Đến 1.000.000đ'},{value:'3000000',label:'Đến 3.000.000đ'}]}/></div>
 <Button className="tm-search-submit" type="primary" htmlType="submit" size="large" icon={<SearchOutlined/>}>Tìm gia sư</Button></form>;
}
