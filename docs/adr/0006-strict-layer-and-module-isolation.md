# 6. Strict Layer and Module Isolation for Controllers

Date: 2026-09-12

## Status

Accepted

## Context

During a routine architectural review, we identified several violations of our Modular Monolith boundaries:
1.  **Entity Leakage**: Some Controllers (`StudentInvoiceController`, `TrialRequestController`) were directly returning JPA Entities (`Invoice`, `TrialRequest`). This leaked the domain and persistence model directly to the web/delivery layer, breaking encapsulation.
2.  **Cross-Module Coupling**: Several Admin and Teacher controllers were placed in incorrect modules and were calling services of other modules directly, bypassing the Facade layer. For example, `AdminExtensionController`, `AdminPayoutController`, and `AdminRefundController` (in the `admin` module) were directly calling `ExtensionService`, `PayoutService`, and `RefundService` (in the `finance` module). `TeacherSubjectProposalController` (in `teacher`) was calling `SubjectProposalService` (in `subject`).

## Decision

To enforce strict layering and module boundaries, we have decided:

1.  **Strict DTO Usage at Controller Boundaries**: Controllers MUST NOT import or return Domain Entities. Services that are called directly by Controllers must return DTOs or View objects (e.g., `InvoiceDetail`, `TrialRequestView`). This ensures that the entity state is encapsulated and only intentional data is exposed to the API clients.
2.  **Controller Ownership**: Controllers must reside in the module that owns the underlying business domain.
    *   We moved `AdminExtensionController`, `AdminPayoutController`, and `AdminRefundController` to `com.edtech.platform.finance.controller`.
    *   We moved `TeacherSubjectProposalController` to `com.edtech.platform.subject.controller`.
3.  **Enforcement via ArchUnit**: We will continue to rely on our ArchUnit tests (e.g., `SolidGuardrailsArchitectureTest`) to guard against future regressions, ensuring that the domain does not depend on delivery layers and that controllers do not access persistence directly.

## Consequences

*   **Positive**: Improved domain encapsulation; changes to the internal entity structure will not inadvertently break API contracts.
*   **Positive**: Stronger module cohesion and reduced coupling. The `admin` and `teacher` modules are no longer tightly coupled to the internal services of `finance` and `subject`.
*   **Negative**: Slight increase in boilerplate code due to the necessity of mapping between Entities and DTOs at the Service boundary.
