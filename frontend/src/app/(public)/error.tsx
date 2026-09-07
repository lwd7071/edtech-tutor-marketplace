'use client';
import {Result,Button} from 'antd';
export default function ErrorPage({reset}:{reset:()=>void}){return <Result status="warning" title="Chưa tải được trang" subTitle="Kết nối dữ liệu đang gặp lỗi. Hãy thử tải lại." extra={<Button onClick={reset}>Tải lại</Button>}/>;}

