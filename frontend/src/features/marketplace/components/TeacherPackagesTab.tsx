'use client';

import { useRef, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { App, Button, Modal } from 'antd';
import type { PricingPackageView } from '@/shared/api/public';
import { useAuthStore } from '@/features/auth';
import { paymentApi } from '@/features/payments';

const formatVnd = (amount: number) => new Intl.NumberFormat('vi-VN').format(amount);

export function TeacherPackagesTab({ packages }: { packages: PricingPackageView[] }) {
  const { user } = useAuthStore();
  const router = useRouter();
  const pathname = usePathname();
  const { message } = App.useApp();
  const [selected, setSelected] = useState<PricingPackageView | null>(null);
  const [busy, setBusy] = useState(false);
  const key = useRef<string | null>(null);

  const choose = (pkg: PricingPackageView) => {
    if (!user) {
      router.push('/auth/login?redirect=' + encodeURIComponent(pathname + '?packageId=' + pkg.id + '#packages'));
      return;
    }
    if (user.role !== 'STUDENT') return;
    key.current = crypto.randomUUID();
    setSelected(pkg);
  };

  const buy = async () => {
    if (!selected || busy) return;
    setBusy(true);
    try {
      const callback = window.location.origin + '/student/payments/callback';
      const result = await paymentApi.createInvoice({
        pricingPackageId: selected.id,
        returnUrl: callback,
        cancelUrl: callback,
      }, key.current!);
      if (!result.data.id) throw new Error('Chưa nhận được mã hóa đơn.');
      router.push('/student/checkout/' + result.data.id);
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Chưa tạo được hóa đơn. Thử lại với cùng yêu cầu.');
    } finally {
      setBusy(false);
    }
  };

  if (!packages.length) {
    return <p className="tm-package-empty">Gia sư chưa mở gói học. Bạn có thể xem lịch rảnh và gửi yêu cầu học thử.</p>;
  }

  return (
    <>
      <ul className="tm-package-list" aria-label="Các gói học đang mở bán">
        {packages.map(pkg => (
          <li className="tm-package-entry" key={pkg.id}>
            <article>
              <div className="tm-package-overview">
                <header className="tm-package-main">
                  <span className="tm-chip">{pkg.subjectName || 'Gói học 1–1'}</span>
                  <h3 className="tm-package-title">{pkg.name}</h3>
                  {pkg.description && <p className="tm-package-description">{pkg.description}</p>}
                </header>
                <div className="tm-package-price-block">
                  <span>Giá trọn gói</span>
                  <strong>{formatVnd(pkg.priceVnd)}đ</strong>
                </div>
              </div>
              <dl className="tm-package-terms">
                <div>
                  <dt>Số buổi</dt>
                  <dd>{pkg.totalSessions ?? pkg.sessionCount}</dd>
                </div>
                <div>
                  <dt>Thời lượng</dt>
                  <dd>{pkg.sessionDurationMinutes ?? pkg.durationMinutes} phút/buổi</dd>
                </div>
                {pkg.durationDays && (
                  <div>
                    <dt>Thời hạn</dt>
                    <dd>{pkg.durationDays} ngày</dd>
                  </div>
                )}
              </dl>
              {(!user || user.role === 'STUDENT') && (
                <Button type="primary" size="large" onClick={() => choose(pkg)}>
                  Chọn gói học
                </Button>
              )}
            </article>
          </li>
        ))}
      </ul>

      <Modal
        open={!!selected}
        title="Xác nhận gói học"
        onCancel={() => { if (!busy) setSelected(null); }}
        onOk={buy}
        confirmLoading={busy}
        okText="Tạo hóa đơn thanh toán"
        cancelText="Quay lại"
        closable={!busy}
        mask={{ closable: !busy }}
      >
        {selected && (
          <div className="tm-package-confirmation">
            <h3>{selected.name}</h3>
            <dl className="tm-package-confirmation-terms">
              <div><dt>Số buổi</dt><dd>{selected.totalSessions ?? selected.sessionCount}</dd></div>
              <div><dt>Thời lượng</dt><dd>{selected.sessionDurationMinutes ?? selected.durationMinutes} phút/buổi</dd></div>
              <div><dt>Giá trọn gói</dt><dd><strong>{formatVnd(selected.priceVnd)}đ</strong></dd></div>
            </dl>
            <p>Sau khi thanh toán được xác nhận, gói học sẽ xuất hiện trong tài khoản. Gia sư sẽ thống nhất và tạo lịch học.</p>
          </div>
        )}
      </Modal>
    </>
  );
}
