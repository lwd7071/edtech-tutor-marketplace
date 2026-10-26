import React from 'react';
import { render, screen } from '@testing-library/react';
import SubjectsLoading from './loading';

describe('SubjectsLoading', () => {
  it('announces loading without presenting placeholders as subject data', () => {
    const { container } = render(<SubjectsLoading />);

    expect(screen.getByRole('status')).toHaveTextContent('Đang tải danh sách môn học');
    expect(container.querySelector('[aria-busy="true"]')).toBeInTheDocument();
    expect(screen.queryByRole('radio')).not.toBeInTheDocument();
    expect(container.querySelectorAll('.tm-subject-skeleton').length).toBeGreaterThan(0);
  });
});
