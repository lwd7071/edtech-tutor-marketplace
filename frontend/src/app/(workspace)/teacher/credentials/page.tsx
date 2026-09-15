'use client';

import { useEffect, useState } from 'react';
import { teacherApi, TeacherCredential } from '@/shared/api/teacher';

const MAX = 10;
export default function TeacherCredentialsPage() {
  const [items, setItems] = useState<TeacherCredential[]>([]);
  const [label, setLabel] = useState('');
  const [proof, setProof] = useState<File | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const load = async () => setItems(await teacherApi.getCredentials());
  useEffect(() => { void load(); }, []);
  const submit = async (e: React.FormEvent) => {
    e.preventDefault(); setError(null);
    if (!label.trim() || !proof) { setError('Vui lòng nhập tên và tải lên minh chứng.'); return; }
    if (!['image/jpeg','image/png','application/pdf'].includes(proof.type)) { setError('Chỉ nhận JPG, PNG hoặc PDF.'); return; }
    if (proof.size > 10 * 1024 * 1024) { setError('Tệp không được vượt quá 10MB.'); return; }
    setBusy(true); try { await teacherApi.createCredential(label, proof); setLabel(''); setProof(null); await load(); } catch { setError('Không thể lưu minh chứng, vui lòng thử lại.'); } finally { setBusy(false); }
  };
  return <main className="tm-stack"><h1>Minh chứng năng lực</h1><p>Hãy nhập chứng chỉ, bằng cấp, điểm số của bạn. Chỉ mục được duyệt mới hiển thị trên hồ sơ công khai.</p><form onSubmit={submit} className="tm-panel tm-stack"><label>Tên chứng chỉ/bằng cấp<input value={label} onChange={e=>setLabel(e.target.value)} placeholder="Hãy nhập chứng chỉ, bằng cấp, điểm số của bạn" maxLength={120}/></label><label>Ảnh/PDF minh chứng<input type="file" accept=".jpg,.jpeg,.png,.pdf" onChange={e=>setProof(e.target.files?.[0] ?? null)}/></label>{error&&<p role="alert">{error}</p>}<button disabled={busy || items.length >= MAX}>{items.length >= MAX ? 'Đã đạt tối đa 10 mục' : busy ? 'Đang lưu…' : 'Gửi duyệt'}</button></form><section className="tm-stack">{items.map(item=><article className="tm-panel" key={item.id}><strong>{item.label}</strong><span>Trạng thái: {item.status}</span>{item.rejectedReason&&<small>Lý do từ chối: {item.rejectedReason}</small>}<button type="button" onClick={()=>teacherApi.deleteCredential(item.id,item.version).then(load)}>Xóa</button></article>)}</section></main>;
}
