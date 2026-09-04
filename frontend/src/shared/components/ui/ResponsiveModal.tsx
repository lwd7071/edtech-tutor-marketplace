'use client';

import React from 'react';
import { Modal } from 'antd';
import type { ModalProps } from 'antd';
import { useMediaQuery } from 'react-responsive';

interface ResponsiveModalProps extends ModalProps {
  destructive?: boolean;
}

export default function ResponsiveModal({
  destructive = false,
  className = '',
  width = 480, // Default width based on spec (480/640/800)
  ...props
}: ResponsiveModalProps) {
  // Mobile breakpoint (< 768px)
  const isMobile = useMediaQuery({ maxWidth: 767 });

  const destructiveProps = destructive
    ? {
        okButtonProps: { danger: true },
      }
    : {};

  return (
    <Modal
      width={width}
      className={`responsive-modal ${isMobile ? 'mobile-sheet' : ''} ${className}`}
      transitionName={isMobile ? 'ant-slide-up' : 'ant-zoom'}
      style={isMobile ? { top: 0, margin: 0, maxWidth: '100vw', paddingBottom: 0 } : undefined}
      {...destructiveProps}
      {...props}
    >
      {props.children}
      <style>{`
        .responsive-modal.mobile-sheet {
          position: fixed !important;
          bottom: 0 !important;
          margin: 0 !important;
          padding: 0 !important;
          width: 100vw !important;
        }
        .responsive-modal.mobile-sheet .ant-modal-content {
          border-radius: 16px 16px 0 0 !important;
          padding-bottom: env(safe-area-inset-bottom) !important;
        }
      `}</style>
    </Modal>
  );
}
