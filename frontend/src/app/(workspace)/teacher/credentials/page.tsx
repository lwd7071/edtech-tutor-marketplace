'use client';

import { useEffect, useRef, useState } from 'react';
import { FilePdfOutlined, UploadOutlined } from '@ant-design/icons';
import { Button } from 'antd';
import { teacherApi, type TeacherCredential } from '@/shared/api/teacher';
import styles from './credentials.module.css';

const MAX_ITEMS = 10;
const MAX_BYTES = 5 * 1024 * 1024;
const ACCEPTED = ['image/jpeg', 'image/png', 'application/pdf'];
const statusLabel = { PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Cần bổ sung' };

export default function TeacherCredentialsPage() {
  const [items, setItems] = useState<TeacherCredential[]>([]);
  const [label, setLabel] = useState('');
  const [proof, setProof] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [editing, setEditing] = useState<TeacherCredential | null>(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [dragging, setDragging] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [proofView, setProofView] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const load = async () => {
    try { setItems(await teacherApi.getCredentials()); setError(null); }
    catch { setError('Không tải được minh chứng. Hãy thử lại.'); }
    finally { setLoading(false); }
  };
  useEffect(() => { void load(); }, []);
  useEffect(() => {
    if (!proof || !proof.type.startsWith('image/')) { setPreview(null); return; }
    const url = URL.createObjectURL(proof);
    setPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [proof]);
  useEffect(() => () => { if (proofView) URL.revokeObjectURL(proofView); }, [proofView]);

  const selectFile = (file?: File) => {
    if (!file) return;
    if (!ACCEPTED.includes(file.type)) { clearFile(); setError('Chỉ nhận tệp JPG, PNG hoặc PDF.'); return; }
    if (file.size > MAX_BYTES) { clearFile(); setError('Tệp không được vượt quá 5MB.'); return; }
    setProof(file); setError(null);
  };
  const clearFile = () => { setProof(null); if (inputRef.current) inputRef.current.value = ''; };
  const reset = () => { setEditing(null); setLabel(''); clearFile(); };
  const beginEdit = (item: TeacherCredential) => {
    setEditing(item); setLabel(item.label); clearFile(); setError(null); setNotice(null);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };
  const submit = async (event: React.FormEvent) => {
    event.preventDefault(); setError(null); setNotice(null);
    if (!label.trim()) { setError('Nhập tên chứng chỉ hoặc bằng cấp.'); return; }
    if (!editing && !proof) { setError('Chọn file minh chứng trước khi gửi.'); return; }
    setBusy(true);
    try {
      if (editing) await teacherApi.updateCredential(editing.id, label.trim(), editing.version, proof ?? undefined);
      else await teacherApi.createCredential(label.trim(), proof!);
      reset(); await load(); setNotice('Đã gửi minh chứng để duyệt.');
    } catch { setError('Chưa gửi được minh chứng. Kiểm tra kết nối hoặc tải lại nếu mục đã được thay đổi.'); }
    finally { setBusy(false); }
  };
  const remove = async (item: TeacherCredential) => {
    if (!window.confirm(`Xóa “${item.label}”?`)) return;
    setError(null); setNotice(null); setBusy(true);
    try { await teacherApi.deleteCredential(item.id, item.version); await load(); setNotice('Đã xóa minh chứng.'); }
    catch { setError('Không xóa được minh chứng. Hãy tải lại và thử lại.'); }
    finally { setBusy(false); }
  };
  const viewProof = async (item: TeacherCredential) => {
    setError(null);
    try {
      const blob = await teacherApi.getCredentialProof(item.id);
      setProofView(URL.createObjectURL(blob));
    } catch { setError('Không mở được file minh chứng. Hãy thử lại.'); }
  };

  return <main className={styles.page}>
    <header className={styles.header}>
      <h1>Minh chứng năng lực</h1>
      <p>Gửi bằng cấp, chứng chỉ hoặc thành tích chuyên môn. Chỉ tên mục được duyệt hiển thị trên hồ sơ công khai; file minh chứng chỉ bạn và admin được xem.</p>
    </header>
    {(notice || error) && <div className={styles.feedback}>
      {notice && <p className={styles.notice} role="status">{notice}</p>}
      {error && <div className={styles.error} role="alert"><span>{error}</span>{error.startsWith('Không tải được') && <Button type="default" onClick={() => void load()}>Thử tải lại</Button>}</div>}
    </div>}
    <div className={styles.workspace}>
      <section className={styles.list} aria-labelledby="credential-list-title" aria-busy={loading}>
        <div className={styles.listHeading}><div><h2 id="credential-list-title">Minh chứng đã gửi</h2><p>Trạng thái xét duyệt và nội dung đang hiển thị trên hồ sơ.</p></div><span className={styles.count}>{items.length}<small> / {MAX_ITEMS}</small></span></div>
        {loading ? <div className={styles.state} role="status">Đang tải minh chứng…</div> : items.length === 0
          ? <section className={styles.empty} aria-label="Chưa có minh chứng"><h3>Chưa có minh chứng nào</h3><p>Thêm chứng chỉ hoặc bằng cấp để bắt đầu xác minh năng lực.</p></section>
          : <ul className={styles.rows}>{items.map(item => <li className={styles.item} key={item.id}>
            <header className={styles.itemHead}><div className={styles.itemTitle}><strong>{item.label}</strong>{item.status === 'APPROVED' && <p>Đang hiển thị trên hồ sơ công khai.</p>}{item.status === 'REJECTED' && <p className={styles.rejectReason}>Lý do từ chối: {item.rejectedReason || 'Cần cập nhật minh chứng.'}</p>}</div><output className={`${styles.badge} ${styles[item.status.toLowerCase()]}`}>{statusLabel[item.status]}</output></header>
            <footer className={styles.actions}><Button type="default" onClick={() => void viewProof(item)}>Xem file</Button><Button type="default" onClick={() => beginEdit(item)}>{item.status === 'REJECTED' ? 'Cập nhật và gửi lại' : 'Chỉnh sửa'}</Button><Button type="default" className={styles.delete} disabled={busy} onClick={() => void remove(item)}>Xóa</Button></footer>
          </li>)}</ul>}
      </section>
      <form onSubmit={submit} className={styles.form} aria-labelledby="credential-form-title">
        <header className={styles.formHeading}><h2 id="credential-form-title">{editing ? 'Cập nhật minh chứng' : 'Thêm minh chứng'}</h2><p>Nhận JPG, PNG hoặc PDF, tối đa 5MB.</p></header>
        {editing?.status === 'APPROVED' && <aside className={styles.warning}>Khi cập nhật, mục này sẽ tạm ẩn khỏi hồ sơ công khai cho đến khi được duyệt lại.</aside>}
        <fieldset className={styles.field}><legend>Tên chứng chỉ hoặc bằng cấp</legend><input id="credential-label" aria-label="Tên chứng chỉ hoặc bằng cấp" value={label} onChange={event => setLabel(event.target.value)} placeholder="VD: Chứng chỉ TOEIC 850" maxLength={120} autoComplete="off" /></fieldset>
        <fieldset className={styles.field}><legend>Ảnh/PDF minh chứng {editing && <span>(để trống nếu giữ file hiện tại)</span>}</legend>
          <section aria-label="Chọn file minh chứng" className={`${styles.dropzone} ${dragging ? styles.dragging : ''}`} onDragOver={event => { event.preventDefault(); setDragging(true); }} onDragLeave={() => setDragging(false)} onDrop={event => { event.preventDefault(); setDragging(false); selectFile(event.dataTransfer.files[0]); }}>
            <input ref={inputRef} id="credential-proof" type="file" aria-label="Ảnh/PDF minh chứng" accept=".jpg,.jpeg,.png,.pdf" className={styles.fileInput} onChange={event => selectFile(event.target.files?.[0])} />
            {proof ? <figure className={styles.selected}>{preview ? <img src={preview} alt="Xem trước file đã chọn" /> : <FilePdfOutlined aria-hidden="true" />}<figcaption><strong>{proof.name}</strong><small>{(proof.size / 1024 / 1024).toFixed(2)} MB</small></figcaption><Button type="default" onClick={clearFile}>Bỏ file</Button></figure> : <div className={styles.dropLabel}><UploadOutlined aria-hidden="true" /><strong>Kéo thả file hoặc bấm để chọn</strong><small>JPG, PNG, PDF · tối đa 5MB</small><Button type="default" htmlType="button" onClick={() => inputRef.current?.click()}>Chọn file</Button><span className={styles.visuallyHidden} id="credential-proof-label">Ảnh/PDF minh chứng</span></div>}
          </section>
        </fieldset>
        <Button htmlType="submit" type="primary" className={styles.submit} disabled={busy || !label.trim() || (!editing && !proof) || (!editing && items.length >= MAX_ITEMS)}>{busy ? 'Đang gửi…' : editing ? 'Cập nhật và gửi lại' : items.length >= MAX_ITEMS ? 'Đã đạt tối đa 10 mục' : 'Gửi duyệt'}</Button>
        {editing && <Button type="text" className={styles.cancel} onClick={reset} disabled={busy}>Hủy chỉnh sửa</Button>}
      </form>
    </div>
    {proofView && <div className={styles.viewerBackdrop} role="presentation" onClick={() => setProofView(null)}><section className={styles.viewer} role="dialog" aria-modal="true" aria-label="Xem file minh chứng" onClick={event => event.stopPropagation()}><header><strong>File minh chứng</strong><Button type="default" onClick={() => setProofView(null)}>Đóng</Button></header><iframe src={proofView} title="File minh chứng" /></section></div>}
  </main>;
}
