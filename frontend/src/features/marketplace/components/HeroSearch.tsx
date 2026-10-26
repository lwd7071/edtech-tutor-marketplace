import Link from 'next/link';
import HomeIcon from './HomeIcon';

export default function HeroSearch() {
  return (
    <Link className="tm-button tm-hero-cta" href="/subjects">
      <HomeIcon name="search" />
      <span>Tìm gia sư</span>
    </Link>
  );
}
