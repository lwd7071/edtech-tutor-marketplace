'use client';

import React, { useState } from 'react';
import { PricingPackageView } from '@/shared/api/public';
import { MoneyText } from '@/shared/components/data-display/MoneyText';
import { EmptyState } from '@/shared/components/feedback/EmptyState';
import { BookOutlined, ClockCircleOutlined, ShoppingCartOutlined } from '@ant-design/icons';
import { Button, message } from 'antd';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/features/auth';
import { paymentApi } from '@/features/payments/api/paymentApi';

interface TeacherPackagesTabProps {
  packages: PricingPackageView[];
}

export const TeacherPackagesTab: React.FC<TeacherPackagesTabProps> = ({ packages }) => {
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();
  const [loadingPkgId, setLoadingPkgId] = useState<string | null>(null);

  const handleBuy = async (pkg: PricingPackageView) => {
    if (!isAuthenticated) {
      message.info('Vui lòng đăng nhập để mua gói học');
      router.push('/auth/login');
      return;
    }

    try {
      setLoadingPkgId(pkg.id);
      
      const returnUrl = `${window.location.origin}/student/payments/callback?success=true`;
      const cancelUrl = `${window.location.origin}/student/payments/callback?success=false`;

      const response = await paymentApi.createInvoice({
        pricingPackageId: pkg.id,
        returnUrl,
        cancelUrl,
      });

      if (response?.data?.checkoutUrl) {
        window.location.assign(response.data.checkoutUrl);
      } else {
        message.error('Không tạo được link thanh toán. Vui lòng thử lại sau.');
      }
    } catch (error: any) {
      console.error('Lỗi khi thanh toán:', error);
      message.error(error?.response?.data?.message || 'Có lỗi xảy ra khi xử lý thanh toán');
    } finally {
      setLoadingPkgId(null);
    }
  };

  if (!packages || packages.length === 0) {
    return (
      <EmptyState 
        title="Chưa có gói học nào"
        description="Giáo viên này hiện tại chưa công khai gói học nào."
      />
    );
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
      {packages.map(pkg => (
        <div key={pkg.id} className="bg-surface rounded-xl border border-border shadow-sm p-6 hover:shadow-md transition-shadow flex flex-col h-full">
          <div className="mb-4 flex-1">
            <h3 className="text-h4 text-text-primary mb-2 line-clamp-2">{pkg.name}</h3>
            {pkg.description && (
              <p className="text-text-secondary text-sm line-clamp-3 mb-4">{pkg.description}</p>
            )}
            <div className="flex flex-col gap-2 mt-4 text-text-primary">
              <div className="flex items-center gap-2">
                <BookOutlined className="text-text-secondary" />
                <span>{pkg.sessionCount} buổi</span>
              </div>
              <div className="flex items-center gap-2">
                <ClockCircleOutlined className="text-text-secondary" />
                <span>{pkg.durationMinutes} phút/buổi</span>
              </div>
            </div>
          </div>
          
          <div className="mt-4 pt-4 border-t border-border flex items-end justify-between">
            <div className="flex flex-col">
              <span className="text-xs text-text-secondary">Giá trọn gói</span>
              <span className="text-xl font-bold text-primary">
                <MoneyText amount={pkg.priceVnd} />
              </span>
            </div>
            <Button 
              type="primary" 
              icon={<ShoppingCartOutlined />}
              onClick={() => handleBuy(pkg)}
              loading={loadingPkgId === pkg.id}
            >
              Đăng ký
            </Button>
          </div>
        </div>
      ))}
    </div>
  );
};
