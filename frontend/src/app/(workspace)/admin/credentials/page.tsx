'use client';

import { useEffect, useState } from 'react';
import { Button, Modal, Tag } from 'antd';
import { adminApi } from '@/features/admin/api/adminApi';

type Item = { id:string; teacherId:string; label:string; status:string; version:number };

export default function AdminCredentialsPage() {
  const [items, setItems] = useState<Item[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [proof, setProof] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const load = async () => {
    try { const response = await adminApi.getCredentialApprovals(); setItems(response.data || []); setError(null); }
    catch { setError('Không tải được danh sách minh chứng.'); }
  };
  useEffect(() => { void load(); }, []);
  useEffect(() => () => { if (proof) URL.revokeObjectURL(proof); }, [proof]);
  const view = async (item: Item) => {
    setBusy(item.id); setError(null);
    try { setProof(URL.createObjectURL(await adminApi.getCredentialProof(item.id))); }
    catch { setError('Không mở được file minh chứng.'); }
    finally { setBusy(null); }
  };
  const approve = async (item: Item) => {
    setBusy(item.id); setError(null);
    try { await adminApi.approveCredential(item.id, item.version); await load(); }
    catch { setError('Không duyệt được minh chứng. Hãy tải lại và thử lại.'); }
    finally { setBusy(null); }
  };
  const reject = async (item: Item) => {
    const reason = window.prompt('Lý do từ chối?');
    if (!reason?.trim()) return;
    setBusy(item.id); setError(null);
    try { await adminApi.rejectCredential(item.id, reason.trim(), item.version); await load(); }
    catch { setError('Không từ chối được minh chứng. Hãy tải lại và thử lại.'); }
    finally { setBusy(null); }
  };
  return <main className="tm-stack">
    <header className="tm-page-heading"><h1>Duyệt minh chứng năng lực</h1><p>Kiểm tra file trước khi quyết định. Chỉ tên mục đã duyệt được hiển thị công khai.</p></header>
    {error && <p role="alert">{error}</p>}
    {items.length === 0 ? <section className="tm-panel">Không có minh chứng đang chờ duyệt.</section> : items.map(item => <article className="tm-panel tm-stack" key={item.id}>
      <div className="tm-toolbar"><strong>{item.label}</strong><Tag color="gold">Chờ duyệt</Tag></div>
      <small>Mã gia sư: {item.teacherId}</small>
      <div className="tm-inline"><Button onClick={() => void view(item)} loading={busy === item.id}>Xem file</Button><Button type="primary" onClick={() => void approve(item)} disabled={!!busy}>Duyệt</Button><Button danger onClick={() => void reject(item)} disabled={!!busy}>Từ chối</Button></div>
    </article>)}
    <Modal title="File minh chứng" open={!!proof} onCancel={() => setProof(null)} footer={null} width={900} destroyOnHidden><iframe src={proof ?? undefined} title="File minh chứng" style={{width:'100%',height:'70vh',border:0}} /></Modal>
  </main>;
}
