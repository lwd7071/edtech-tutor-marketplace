export default function SubjectsLoading() {
  return (
    <main className="tm-container tm-page tm-subject-page tm-subject-loading" aria-busy="true">
      <p className="tm-visually-hidden" role="status">Đang tải danh sách môn học</p>
      <div className="tm-subject-picker" aria-hidden="true">
        <header className="tm-subject-picker-intro">
          <span className="tm-subject-skeleton tm-subject-skeleton-kicker" />
          <span className="tm-subject-skeleton tm-subject-skeleton-title" />
          <span className="tm-subject-skeleton tm-subject-skeleton-copy" />
        </header>
        <div className="tm-subject-loading-filter-row">
          <span className="tm-subject-skeleton tm-subject-skeleton-filter" />
          <span className="tm-subject-skeleton tm-subject-skeleton-filter" />
        </div>
        <div className="tm-subject-loading-list">
          {Array.from({ length: 6 }, (_, index) => (
            <div className="tm-subject-loading-option" key={index}>
              <span className="tm-subject-skeleton tm-subject-skeleton-radio" />
              <span className="tm-subject-loading-option-copy">
                <span className="tm-subject-skeleton tm-subject-skeleton-name" />
                <span className="tm-subject-skeleton tm-subject-skeleton-level" />
              </span>
            </div>
          ))}
        </div>
        <div className="tm-subject-skeleton tm-subject-skeleton-continue" />
      </div>
    </main>
  );
}
