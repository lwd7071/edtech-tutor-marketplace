import LearnerList from '@/features/teacher-dashboard/components/LearnerList';
export default async function Page({params}:{params:Promise<{id:string}>}){const {id}=await params;return <LearnerList studentId={id}/>;}

