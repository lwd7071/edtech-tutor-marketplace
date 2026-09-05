'use client';

import React, { useEffect, useState } from 'react';
import { Table, Button, Switch, Tooltip, message, Skeleton } from 'antd';
import { PlusOutlined, EditOutlined } from '@ant-design/icons';
import { teacherApi } from '@/shared/api/teacher';
import { MoneyText } from '@/shared/components/data-display/MoneyText';
import { StatusTag } from '@/shared/components/data-display/StatusTag';

interface PackageListProps {
  onCreate: () => void;
  onEdit: (id: string) => void;
}

export const PackageList: React.FC<PackageListProps> = ({ onCreate, onEdit }) => {
  const [packages, setPackages] = useState<any[]>([]);
  const [isApproved, setIsApproved] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [profile, pkgs] = await Promise.all([
          teacherApi.getProfile(),
          teacherApi.getPackages()
        ]);
        setIsApproved(profile.approvalStatus === 'APPROVED');
        setPackages(pkgs);
      } catch (error) {
        message.error('Lỗi khi tải dữ liệu gói học');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const handleToggleStatus = async (id: string, currentStatus: string) => {
    const newStatus = currentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await teacherApi.updatePackageStatus(id, newStatus);
      setPackages(prev => prev.map(p => p.id === id ? { ...p, status: newStatus } : p));
      message.success('Cập nhật trạng thái thành công');
    } catch (error) {
      message.error('Cập nhật trạng thái thất bại');
    }
  };

  const columns = [
    {
      title: 'Tên gói học',
      dataIndex: 'name',
      key: 'name',
      render: (text: string) => <span className="font-semibold">{text}</span>
    },
    {
      title: 'Giá tiền',
      dataIndex: 'priceVnd',
      key: 'priceVnd',
      render: (price: number) => <MoneyText amount={price} className="text-primary-700 font-bold" />
    },
    {
      title: 'Số buổi',
      dataIndex: 'sessionCount',
      key: 'sessionCount',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <StatusTag domain="StudentPackage" status={status} />
    },
    {
      title: 'Hoạt động',
      key: 'active',
      render: (_: any, record: any) => (
        <Switch 
          checked={record.status === 'ACTIVE'}
          onChange={() => handleToggleStatus(record.id, record.status)}
          checkedChildren="Bật"
          unCheckedChildren="Tắt"
        />
      )
    },
    {
      title: 'Thao tác',
      key: 'action',
      render: (_: any, record: any) => (
        <Button 
          type="text" 
          icon={<EditOutlined />} 
          onClick={() => onEdit(record.id)}
        >
          Sửa
        </Button>
      )
    }
  ];

  if (loading) {
    return <div data-testid="loading-skeleton"><Skeleton active /></div>;
  }

  return (
    <div className="package-list bg-surface p-6 rounded-xl border border-border shadow-sm">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-xl font-bold text-text-primary">Gói học của bạn</h2>
        <Tooltip title={!isApproved ? "Hồ sơ của bạn phải được duyệt trước khi tạo gói học" : ""}>
          <Button 
            type="primary" 
            icon={<PlusOutlined />} 
            onClick={onCreate}
            disabled={!isApproved}
          >
            Tạo gói học
          </Button>
        </Tooltip>
      </div>

      <Table 
        dataSource={packages}
        columns={columns}
        rowKey="id"
        pagination={false}
        locale={{ emptyText: 'Bạn chưa tạo gói học nào.' }}
      />
    </div>
  );
};
