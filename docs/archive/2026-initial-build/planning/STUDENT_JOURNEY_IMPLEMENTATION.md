# Student journey implementation

## Contract changes

- Registration returns `{ email, verificationRequired }`; a pending account cannot log in or refresh a session.
- Student parent contact supports `GET` and nullable updates at `/api/student/parent-contact`.
- Invoice purchase terms are stored by `V26__snapshot_invoice_purchase_terms.sql`; fulfillment reads only the invoice snapshot.
- Student trial requests are listed at `GET /api/student/trial-requests` with status and server pagination.
- Student package list/detail responses include stable teacher, subject and purchase snapshots inside the standard API envelope.
- Assignment list accepts `progress=TODO|SUBMITTED|GRADED`; assignment detail returns the student's singular `submission` and attachment metadata.
- Eligible students open a tutor conversation idempotently with `PUT /api/student/conversations/teachers/{teacherId}`.
- Booking review state is available at `GET /api/student/bookings/{id}/review`.
- Notifications are grouped by `referenceType` and support server pagination.

## Student pages

- `/student` shows live counts for upcoming booking, remaining sessions, todo assignments, unread notifications and pending requests.
- `/student/requests` contains trial, refund and extension tabs.
- `/student/session-reports` lists reports and links back to each booking.
- Tutor profile, purchased package and booking detail expose the eligible conversation action.
- `/terms`, `/privacy` and `/support` resolve to minimal MVP pages.

## Verification

Run `scripts/test-student-journey-docker.ps1` from the repository root. It validates Compose, runs the full Maven/Testcontainers suite, then frontend typecheck, lint, Jest, build and Playwright. Logs are written under `test-logs/student-journey/`. The script does not reset a database, delete volumes, call a real payment provider or send real email.
