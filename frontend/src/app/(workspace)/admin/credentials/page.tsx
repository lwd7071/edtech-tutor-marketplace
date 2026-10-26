'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { Alert, Button, Empty, Modal, Skeleton, Tabs, Tag } from 'antd';
import { adminApi } from '@/features/admin/api/adminApi';
import styles from './credentials.module.css';

type Item = { id: string; teacherId: string; label: string; status: string; version: number; rejectedReason?: string };
type CredentialStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

const statusLabels: Record<CredentialStatus, string> = {
  PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Đã từ chối',
};

function CredentialLoading() {
  return <div className={styles.loading} aria-label="Đang tải minh chứng">
    {[0, 1, 2].map(row => <div className={styles.loadingRow} key={row}>
      <Skeleton active title={{ width: '38%' }} paragraph={{ rows: 1, width: ['26%'] }} />
    </div>)}
  </div>;
}

export default function AdminCredentialsPage() {
  const [items, setItems] = useState<Item[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [proof, setProof] = useState<string | null>(null);
  const [proofItem, setProofItem] = useState<Item | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [status, setStatus] = useState<CredentialStatus>('PENDING');
  const [loading, setLoading] = useState(true);
  const requestSequence = useRef(0);

  const load = useCallback(async (selectedStatus: CredentialStatus) => {
    const request = ++requestSequence.current;
    setLoading(true);
    try {
      const response = await adminApi.getCredentialApprovals(selectedStatus);
      if (request === requestSequence.current) {
        setItems(response.data || []);
        setError(null);
      }
    } catch {
      if (request === requestSequence.current) setError('Không tải được danh sách minh chứng.');
    } finally {
      if (request === requestSequence.current) setLoading(false);
    }
  }, []);

  useEffect(() => { void load(status); }, [load, status]);
  useEffect(() => () => { if (proof) URL.revokeObjectURL(proof); }, [proof]);

  const view = async (item: Item) => {
    setBusy(item.id);
    setError(null);
    try {
      setProof(URL.createObjectURL(await adminApi.getCredentialProof(item.id)));
    } catch {
      setError('Không mở được file minh chứng.');
    } finally {
      setBusy(null);
    }
  };

  const approve = async (item: Item) => {
    setBusy(item.id);
    setError(null);
    try {
      await adminApi.approveCredential(item.id, item.version);
      await load(status);
    } catch {
      setError('Không duyệt được minh chứng. Hãy tải lại và thử lại.');
    } finally {
      setBusy(null);
    }
  };

  const reject = async (item: Item) => {
    const reason = window.prompt('Lý do từ chối?');
    if (!reason?.trim()) return;
    setBusy(item.id);
    setError(null);
    try {
      await adminApi.rejectCredential(item.id, reason.trim(), item.version);
      await load(status);
    } catch {
      setError('Không từ chối được minh chứng. Hãy tải lại và thử lại.');
    } finally {
      setBusy(null);
    }
  };

  const closeProof = () => {
    setProof(null);
    setProofItem(null);
  };

  const retry = () => {
    if (error === 'Không mở được file minh chứng.' && proofItem) {
      void view(proofItem);
      return;
    }
    void load(status);
  };

  return <div className={styles.page}>
    <section className={styles.heading} aria-labelledby="credentials-page-title">
      <h1 id="credentials-page-title">Duyệt minh chứng năng lực</h1>
      <p>Kiểm tra file trước khi quyết định. Chỉ tên mục đã duyệt được hiển thị công khai.</p>
    </section>

    <section className={styles.queue} role="region" aria-label="Hàng đợi minh chứng">
      <div className={styles.queueHeading}>
        <h2>Minh chứng gửi lên</h2>
        <Tabs
          activeKey={status}
          onChange={value => setStatus(value as CredentialStatus)}
          items={(Object.keys(statusLabels) as CredentialStatus[]).map(value => ({ key: value, label: statusLabels[value] }))}
        />
      </div>

      {error && <Alert
        className={styles.error}
        type="error"
        showIcon
        message={error}
        action={<Button onClick={retry}>Thử lại</Button>}
      />}

      {loading ? <CredentialLoading /> : !error && items.length === 0 ? <div className={styles.empty}>
        <Empty description="Không có minh chứng ở trạng thái này." />
      </div> : !error && <ul className={styles.items}>
        {items.map(item => <li key={item.id}>
          <article className={styles.item}>
            <div className={styles.itemContent}>
              <div className={styles.itemHeading}>
                <div className={styles.itemIdentity}>
                  <h3>{item.label}</h3>
                  <p>Mã gia sư: <span>{item.teacherId}</span></p>
                </div>
                <Tag color={item.status === 'APPROVED' ? 'success' : item.status === 'REJECTED' ? 'error' : 'warning'}>
                  {statusLabels[item.status as CredentialStatus] ?? item.status}
                </Tag>
              </div>
              {item.status === 'REJECTED' && item.rejectedReason && <p className={styles.rejection}>
                <strong>Lý do từ chối:</strong> {item.rejectedReason}
              </p>}
            </div>

            <div className={styles.actions}>
              <Button loading={busy === item.id && !proof} onClick={() => { setProofItem(item); void view(item); }}>
                Xem file
              </Button>
              {item.status === 'PENDING' && <>
                <div className={styles.decisions}>
                  <Button type="primary" aria-label={`Duyệt ${item.label}`} onClick={() => void approve(item)} disabled={!!busy} loading={busy === item.id}>
                    Duyệt
                  </Button>
                  <Button danger aria-label={`Từ chối ${item.label}`} onClick={() => void reject(item)} disabled={!!busy}>
                    Từ chối
                  </Button>
                </div>
              </>}
            </div>
          </article>
        </li>)}
      </ul>}
    </section>

    <Modal
      className={styles.preview}
      rootClassName={styles.previewRoot}
      title={proofItem?.label ?? 'File minh chứng'}
      open={!!proof}
      onCancel={closeProof}
      width={920}
      destroyOnHidden
      footer={<Button onClick={closeProof}>Đóng</Button>}
    >
      <iframe src={proof ?? undefined} title="File minh chứng" />
    </Modal>
  </div>;
}
