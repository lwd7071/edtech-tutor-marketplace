import {Skeleton} from 'antd';
export default function Loading(){return <div className="tm-container tm-page"><Skeleton active/><div className="tm-teacher-grid">{[1,2,3].map(i=><div className="tm-panel" key={i}><Skeleton active avatar/></div>)}</div></div>;}

