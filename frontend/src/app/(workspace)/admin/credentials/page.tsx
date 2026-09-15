'use client';
import { useEffect, useState } from 'react';
import { adminApi } from '@/features/admin/api/adminApi';
type Item = {id:string;teacherId:string;label:string;proofUrl:string;status:string;rejectedReason?:string;version:number};
export default function AdminCredentialsPage() {
  const [items,setItems]=useState<Item[]>([]); const [error,setError]=useState<string>();
  const load=()=>adminApi.getCredentialApprovals().then(r=>setItems(r.data||[])).catch(()=>setError('Không tải được danh sách minh chứng.'));
  useEffect(()=>{void load();},[]);
  return <main className="tm-stack"><h1>Duyệt minh chứng năng lực</h1>{error&&<p role="alert">{error}</p>}{items.map(i=><article className="tm-panel" key={i.id}><strong>{i.label}</strong><small>Teacher: {i.teacherId}</small><a href={i.proofUrl} target="_blank" rel="noreferrer">Xem proof</a><div><button onClick={()=>adminApi.approveCredential(i.id,i.version).then(load)}>Duyệt</button><button onClick={()=>{const reason=window.prompt('Lý do từ chối?'); if(reason) void adminApi.rejectCredential(i.id,reason,i.version).then(load);}}>Từ chối</button></div></article>)}</main>;
}
