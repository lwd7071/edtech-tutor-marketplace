'use client';
import {App,Button} from 'antd';
import {MessageOutlined} from '@ant-design/icons';
import {usePathname,useRouter} from 'next/navigation';
import {useAuthStore} from '@/features/auth';
import {chatApi} from '../api/chatApi';

export function MessageTeacherButton({teacherId,block=false}:{teacherId:string;block?:boolean}){
 const {user}=useAuthStore(); const router=useRouter(); const pathname=usePathname(); const {message}=App.useApp();
 const open=async()=>{if(!user){router.push('/auth/login?redirect='+encodeURIComponent(pathname));return;}if(user.role!=='STUDENT')return;
  try{const result=await chatApi.openTeacherConversation(teacherId);router.push('/student/messages?id='+result.data.id);}
  catch(e:any){if(e.response?.data?.errors?.[0]?.code==='CONVERSATION_NOT_ALLOWED'||e.response?.data?.errorCode==='CONVERSATION_NOT_ALLOWED')message.info('Bạn có thể nhắn tin sau khi gửi học thử hoặc mua gói học với gia sư này.');else message.error('Chưa mở được cuộc trò chuyện. Vui lòng thử lại.');}}
 return <Button icon={<MessageOutlined/>} block={block} onClick={()=>void open()}>Nhắn tin</Button>;
}
