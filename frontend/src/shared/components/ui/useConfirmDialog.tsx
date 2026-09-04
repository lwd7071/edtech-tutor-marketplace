'use client';

import React from 'react';
import { Modal } from 'antd';
import type { ModalFuncProps } from 'antd';
import { ExclamationCircleFilled } from '@ant-design/icons';

export type ConfirmVariant = 'normal' | 'danger' | 'stale';

interface UseConfirmDialogProps extends ModalFuncProps {
  variant?: ConfirmVariant;
}

export function useConfirmDialog() {
  const [modal, contextHolder] = Modal.useModal();

  const confirm = ({ variant = 'normal', ...props }: UseConfirmDialogProps) => {
    let customProps: ModalFuncProps = {};

    if (variant === 'danger') {
      customProps = {
        icon: <ExclamationCircleFilled style={{ color: 'var(--color-error-600)' }} />,
        okButtonProps: { danger: true },
        okType: 'primary',
      };
    } else if (variant === 'stale') {
      customProps = {
        icon: <ExclamationCircleFilled style={{ color: 'var(--color-warning-600)' }} />,
        okText: 'Tải lại',
        content: (
          <div>
            <p>Dữ liệu đã thay đổi ở nơi khác. Vui lòng tải lại trang để xem bản cập nhật mới nhất.</p>
            {props.content}
          </div>
        ),
        onOk: () => {
          if (props.onOk) props.onOk();
          else window.location.reload();
        }
      };
    }

    return modal.confirm({
      ...customProps,
      ...props,
    });
  };

  return { confirm, confirmContext: contextHolder };
}
