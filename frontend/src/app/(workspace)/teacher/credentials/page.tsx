'use client';

import { useEffect, useRef, useState } from 'react';
import { FilePdfOutlined, UploadOutlined } from '@ant-design/icons';
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
    <header className={styles.header}><p className={styles.eyebrow}>Hồ sơ gia sư</p><h1>Minh chứng năng lực</h1><p>Gửi bằng cấp, chứng chỉ hoặc thành tích chuyên môn. Chỉ tên mục được duyệt mới hiển thị trên hồ sơ công khai; file minh chứng chỉ bạn và admin được xem.</p></header>
    {notice && <p className={styles.notice} role="status">{notice}</p>}
    {error && <p className={styles.error} role="alert">{error}</p>}
    <form onSubmit={submit} className={styles.form}>
      <div className={styles.formHeading}><h2>{editing ? 'Cập nhật minh chứng' : 'Thêm minh chứng'}</h2><span>{items.length}/{MAX_ITEMS} mục</span></div>
      {editing?.status === 'APPROVED' && <p className={styles.warning}>Khi cập nhật, mục này sẽ tạm ẩn khỏi hồ sơ công khai cho đến khi được duyệt lại.</p>}
      <div className={styles.field}><label htmlFor="credential-label">Tên chứng chỉ hoặc bằng cấp</label><input id="credential-label" value={label} onChange={event => setLabel(event.target.value)} placeholder="VD: Chứng chỉ TOEIC 850" maxLength={120} autoComplete="off" /></div>
      <div className={styles.field}><label htmlFor="credential-proof">Ảnh/PDF minh chứng {editing && <span>(để trống nếu giữ file hiện tại)</span>}</label>
        <div className={`${styles.dropzone} ${dragging ? styles.dragging : ''}`} onDragOver={event => { event.preventDefault(); setDragging(true); }} onDragLeave={() => setDragging(false)} onDrop={event => { event.preventDefault(); setDragging(false); selectFile(event.dataTransfer.files[0]); }}>
          <input ref={inputRef} id="credential-proof" type="file" accept=".jpg,.jpeg,.png,.pdf" className={styles.fileInput} onChange={event => selectFile(event.target.files?.[0])} />
          {proof ? <div className={styles.selected}>{preview ? <img src={preview} alt="Xem trước file đã chọn" /> : <FilePdfOutlined aria-hidden="true" />}<div><strong>{proof.name}</strong><small>{(proof.size / 1024 / 1024).toFixed(2)} MB</small></div><button type="button" onClick={clearFile}>Bỏ file</button></div> : <label htmlFor="credential-proof" className={styles.dropLabel}><UploadOutlined aria-hidden="true" /><strong>Kéo thả file hoặc bấm để chọn</strong><small>PNG, JPG, PDF · tối đa 5MB</small></label>}
        </div>
      </div>
      <button className={styles.submit} disabled={busy || !label.trim() || (!editing && !proof) || (!editing && items.length >= MAX_ITEMS)}>{busy ? 'Đang gửi…' : editing ? 'Cập nhật và gửi lại' : items.length >= MAX_ITEMS ? 'Đã đạt tối đa 10 mục' : 'Gửi duyệt'}</button>
      {editing && <button type="button" className={styles.cancel} onClick={reset} disabled={busy}>Hủy chỉnh sửa</button>}
    </form>
    <section className={styles.list} aria-label="Minh chứng đã gửi"><div className={styles.listHeading}><h2>Minh chứng đã gửi</h2><span>{items.length} mục</span></div>
      {loading ? <p>Đang tải minh chứng…</p> : items.length === 0 ? <p className={styles.empty}>Chưa có minh chứng nào. Thêm chứng chỉ hoặc bằng cấp đầu tiên ở biểu mẫu phía trên.</p> : items.map(item => <article className={styles.item} key={item.id}>
        <div className={styles.itemHead}><strong>{item.label}</strong><span className={`${styles.badge} ${styles[item.status.toLowerCase()]}`}>{statusLabel[item.status]}</span></div>
        {item.status === 'APPROVED' && <p>Đang hiển thị trên hồ sơ công khai.</p>}
        {item.status === 'REJECTED' && <p className={styles.rejectReason}>Lý do từ chối: {item.rejectedReason || 'Cần cập nhật minh chứng.'}</p>}
        <div className={styles.actions}><button type="button" onClick={() => void viewProof(item)}>Xem file</button><button type="button" onClick={() => beginEdit(item)}>{item.status === 'REJECTED' ? 'Cập nhật và gửi lại' : 'Chỉnh sửa'}</button><button type="button" className={styles.delete} disabled={busy} onClick={() => void remove(item)}>Xóa</button></div>
      </article>)}
    </section>
    {proofView && <div className={styles.viewerBackdrop} role="presentation" onClick={() => setProofView(null)}><section className={styles.viewer} role="dialog" aria-modal="true" aria-label="Xem file minh chứng" onClick={event => event.stopPropagation()}><div><strong>File minh chứng</strong><button type="button" onClick={() => setProofView(null)}>Đóng</button></div><iframe src={proofView} title="File minh chứng" /></section></div>}
  </main>;
}
