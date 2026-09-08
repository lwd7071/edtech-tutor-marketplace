import { render, screen } from '@testing-library/react';
import { ActionLink, BackLink, InlineActionLink, PageActions } from './NavigationLinks';

describe('navigation links', () => {
  it('renders consistent navigation variants with accessible links', () => {
    render(
      <PageActions>
        <BackLink href="/teachers">Danh sách gia sư</BackLink>
        <ActionLink href="/auth/register">Đăng ký gia sư</ActionLink>
        <ActionLink href="/teacher/profile" variant="secondary">Tiếp tục hồ sơ</ActionLink>
        <InlineActionLink href="/subjects">Tất cả môn học</InlineActionLink>
      </PageActions>,
    );

    expect(screen.getByRole('link', { name: 'Danh sách gia sư' })).toHaveAttribute('href', '/teachers');
    expect(screen.getByRole('link', { name: 'Đăng ký gia sư' })).toHaveClass('tm-action-link-primary');
    expect(screen.getByRole('link', { name: 'Tiếp tục hồ sơ' })).toHaveClass('tm-action-link-secondary');
    expect(screen.getByRole('link', { name: 'Tất cả môn học' })).toHaveClass('tm-inline-link');
  });
});
