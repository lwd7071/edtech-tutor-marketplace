# Task 4 — PayOS end-to-end theo TDD

> Tài liệu thi công dành cho agent triển khai. Làm tuần tự từ Gate 0 đến Gate 12.
> Không gộp nhiều gate, không viết hàng loạt test trước rồi mới implement.

## 1. Mục tiêu hoàn thành

Khi Task 4 kết thúc, hệ thống phải chứng minh được toàn bộ luồng sau:

1. Student gửi `POST /api/student/invoices` với `Idempotency-Key`.
2. Backend tạo đúng một Invoice `PENDING` trong transaction ngắn.
3. Backend gọi PayOS ngoài mọi database transaction.
4. Nếu kết quả tạo link bị mất do timeout, lần retry đối soát bằng `orderCode`; không blind-create link thứ hai.
5. Webhook PayOS hợp lệ chuyển Invoice sang `PAID` đúng một lần.
6. Cùng transaction webhook tạo đúng một StudentPackage, credit đúng teacher net vào Wallet và append đúng một LedgerEntry.
7. Webhook lặp hoặc chạy đồng thời không tạo effect lần hai.
8. Invoice quá hạn được expiry job xử lý; webhook hợp lệ đến muộn vẫn được honor theo chính sách trong tài liệu này.
9. Student chỉ đọc được Invoice và StudentPackage thuộc chính mình.

## 2. Phạm vi và quyết định đã khóa

### 2.1 API công khai

- `POST /api/student/invoices` → luôn trả `201` khi tạo mới hoặc replay thành công cùng idempotency key.
- `GET /api/student/invoices/{id}` → `200` nếu đúng owner, nếu không tìm thấy hoặc không sở hữu thì theo contract hiện hữu.
- `GET /api/student/packages` → phân trang, filter `status` nếu contract hỗ trợ.
- `GET /api/student/packages/{id}` → chi tiết package thuộc student.
- `POST /api/webhooks/payos` → không JWT, nhưng bắt buộc verify signature.

Không tự thêm endpoint ngoài danh sách trên.

### 2.2 Idempotency của create invoice

- Key là UUID từ header `Idempotency-Key`.
- Cùng student + cùng key + cùng request fingerprint → trả lại đúng Invoice cũ.
- Cùng student + cùng key + fingerprint khác → `409 IDEMPOTENCY_KEY_REUSED`.
- Key không có TTL ứng dụng. Unique constraint hiện tại là nguồn sự thật.
- Hai request đồng thời cùng key phải tạo đúng một row.
- Không bắt `DataIntegrityViolationException` rồi query lại bên trong chính transaction đã fail.
  Transaction insert thua race phải rollback hoàn toàn; lớp orchestration bên ngoài transaction mới query lại Invoice thắng race.

### 2.3 Ranh giới transaction/network

Luồng checkout bắt buộc có ba pha:

```text
InvoiceCommandService.createPending(...)  [transaction DB ngắn]
PaymentGateway.create/find(...)           [không có transaction DB]
InvoiceCommandService.attachLink(...)     [transaction DB ngắn]
```

- Controller chỉ parse/authenticate/map response; không chứa state machine.
- Dùng một application orchestrator/facade để điều phối ba pha.
- Không dùng `@Transactional` trên orchestrator.
- Không dùng self-invocation để mong Spring mở transaction.
- Mỗi transaction boundary phải nằm trên public method của một Spring bean riêng.

### 2.4 Reconciliation sau kết quả mập mờ

Với Invoice đã tồn tại nhưng chưa có link:

1. Gọi `findPaymentLink(orderCode)` trước.
2. Nếu tìm thấy link PENDING: validate `orderCode`, attach vào Invoice, trả link.
3. Nếu PayOS trả NOT_FOUND chắc chắn: mới được gọi create.
4. Nếu lookup timeout, 5xx hoặc không phân biệt được NOT_FOUND: trả `503`; không create.
5. Nếu Invoice đã có link: không gọi PayOS, trả link đang lưu.

Không biến mọi exception lookup thành `Optional.empty()`. `Optional.empty()` chỉ có nghĩa là provider xác nhận không tồn tại.

### 2.5 Webhook và expiry

- Verify signature trước khi truy cập/mutate dữ liệu nghiệp vụ.
- Sau verify phải kiểm tra `orderCode`, `amount`, provider reference và trạng thái thanh toán thành công.
- Khóa Invoice theo `payosOrderCode` bằng pessimistic write lock.
- `PENDING → PAID` là luồng bình thường.
- `EXPIRED → PAID` được phép chỉ từ luồng provider đã verify, nhằm honor khoản tiền đã nhận.
- `CANCELLED → PAID` không tự động cho phép trong Task 4.
- Duplicate webhook hợp lệ trả `200`, không tạo thêm PaymentTransaction, Package hoặc Ledger.
- Không dùng pattern `exists(...)` rồi `insert(...)` làm hàng rào concurrency duy nhất.
- Unique constraints của `payment_transactions.provider_reference`, `student_packages.invoice_id` và `ledger_entries.idempotency_key` là lớp phòng thủ cuối.

### 2.6 Tiền và commission

- `amountVnd` và số dư dùng `long`, không dùng `double`/`float`.
- Commission snapshot lấy từ `PlatformSettingsFacade`, không hard-code `5.00` trong service.
- Bổ sung accessor `getCommissionRate()` cho facade nếu chưa có.
- Công thức:

```text
commissionVnd = amountVnd × commissionRate / 100, làm tròn HALF_UP về VND nguyên
teacherNetVnd = amountVnd - commissionVnd
```

- Wallet pending chỉ credit `teacherNetVnd`, không credit gross amount.
- StudentPackage lưu gross purchase price và commission rate snapshot.
- Ledger `PACKAGE_FUNDED` ghi teacher net, key cố định `invoice:{invoiceId}:package-funded`.

### 2.7 Chính sách PayOS client

- Dùng `vn.payos:payos-java:2.0.1`.
- Tạo client bằng `ClientOptions`, truyền client ID, API key, checksum key và base URL từ `PaymentProviderProperties`.
- Cấu hình connect/read timeout hiện có bằng custom HTTP client nếu SDK chỉ có một `timeoutMs`.
- Đặt SDK automatic retry cho create payment link về `0`. Retry nghiệp vụ phải đi qua reconciliation của ứng dụng.
- Không log credential, signature, raw provider response hoặc full webhook payload.
- Phân loại lỗi:
  - signature sai → `InvalidPaymentSignatureException`;
  - provider từ chối/4xx hợp lệ → `PaymentGatewayRejectedException`;
  - timeout, connection, 429 và 5xx tạm thời → `PaymentGatewayUnavailableException`;
  - response thiếu/sai invariant → provider error, không coi là timeout.

### 2.8 Fake và mock

- Giữ `FakePaymentGateway` trong test source.
- Dùng fake stateful cho integration test checkout/reconciliation.
- Chỉ mock lớp SDK PayOS ở unit test của adapter.
- Không mock InvoiceService, StudentPackageService hoặc FinanceFundingService trong behavior/integration test.

## 3. Quy tắc bắt buộc cho agent

Ở mỗi Gate:

1. Đọc các file được liệt kê trong gate.
2. Viết đúng một test cho hành vi tiếp theo.
3. Chạy riêng test đó và ghi nhận nó fail vì capability chưa tồn tại, không phải vì lỗi setup.
4. Implement lượng code tối thiểu để test xanh.
5. Chạy lại test vừa thêm.
6. Chạy nhóm regression được chỉ định.
7. Refactor chỉ khi tất cả test đang xanh.
8. Cập nhật `docs/B/PROGRESS_BE_B.md` bằng test command và kết quả thật.
9. Chỉ chuyển sang gate tiếp theo khi Exit criteria của gate hiện tại đạt đủ.

Không được:

- sửa assertion để hợp thức hóa behavior sai;
- dùng `@Disabled`, giả vờ pass hoặc bỏ Testcontainers khi Docker lỗi;
- sửa migration V1–V20; mọi schema change mới phải là V21 trở lên;
- đưa credential thật vào Git;
- gọi PayOS thật trong automated test;
- giữ transaction khi gọi network;
- thêm generic `save/update/delete` vào Invoice repository;
- catch exception rồi trả `200` cho webhook chưa xử lý thành công, trừ duplicate đã hoàn tất;
- tiếp tục gate sau khi full regression của gate hiện tại đỏ.

## 4. Gate 0 — Baseline và runtime gate Task 3

### Đọc trước

- `docs/B/Task3.md`
- `docs/B/PROGRESS_BE_B.md`
- `backend/src/main/resources/db/migration/V20__prepare_payment_domain.sql`
- `backend/src/test/java/com/edtech/platform/payment/repository/PaymentDomainPersistenceIntegrationTest.java`
- `backend/src/test/java/com/edtech/platform/migration/FlywayV20PaymentDomainTest.java`

### Thực hiện

1. Chạy full suite hiện tại từ `backend`: `mvn test`.
2. Không sửa code Task 4 nếu baseline có unit failure không liên quan.
3. Xác nhận Docker/Testcontainers thực sự chạy migration V1–V20; không chấp nhận skipped test.
4. Ghi lại số test run/fail/error/skipped.

### Exit criteria

- Main compile xanh.
- Toàn bộ test cũ xanh.
- Payment persistence integration chạy thật trên PostgreSQL.
- Migration V1–V20 chạy từ database rỗng.
- Không có test migration/payment persistence bị skip.

## 5. Gate 1 — Production gateway wiring

### File dự kiến

- sửa `backend/pom.xml`
- sửa `payment/config/PaymentProviderProperties.java` nếu cần retry/timeout mapping
- tạo `payment/config/PayOsConfig.java`
- tạo `payment/gateway/PayOsPaymentGateway.java`
- tạo `payment/gateway/DisabledPaymentGateway.java`
- tạo test dưới `src/test/java/com/edtech/platform/payment/config` và `gateway`

### Cycle 1.1 — Provider disabled

RED: context với `app.payment.provider=disabled` có đúng một `PaymentGateway`, kiểu disabled; không có `PayOS` bean.

GREEN: thêm conditional beans tối thiểu.

Exit: application khởi động được mà không cần PayOS credentials khi provider disabled; gọi gateway disabled fail-fast bằng domain exception, không phải `NoSuchBeanDefinitionException`.

### Cycle 1.2 — Provider PayOS thiếu credential

RED: context với provider `payos` nhưng thiếu một credential phải fail startup với thông báo không lộ secret.

GREEN: dùng validation hiện có; không tạo cơ chế validation thứ hai nếu không cần.

### Cycle 1.3 — Mapping create link

RED: mock SDK trả response hợp lệ; adapter map đủ `orderCode`, `paymentLinkId`, `checkoutUrl`, `qrCode`, `expiresAt`.

GREEN: implement `createPaymentLink` bằng SDK v2.

Thêm lần lượt các cycle sau, mỗi lần một test:

- response có orderCode khác request → rejected/provider error;
- response thiếu link ID hoặc checkout URL → provider error;
- SDK connection/timeout → unavailable;
- SDK business/API rejection → rejected;
- lookup 404 chắc chắn → `Optional.empty()`;
- lookup timeout/5xx → throw unavailable;
- verify webhook sai signature → `InvalidPaymentSignatureException`;
- verify webhook hợp lệ → map `VerifiedPayment` và chỉ giữ sanitized payload.

### Regression

```text
mvn -Dtest=PaymentProviderPropertiesTest,PayOsConfigTest,PayOsPaymentGatewayTest test
```

### Exit criteria

- Cả disabled và payos wiring có test.
- Adapter không rò SDK type ra khỏi package gateway/config.
- Không có network call thật.
- `FakePaymentGatewayContractTest` vẫn xanh.

## 6. Gate 2 — DTO và read model trước checkout

### File dự kiến

- tạo `payment/dto/request/CreateInvoiceRequest.java`
- tạo `payment/dto/response/InvoiceDetail.java`
- tạo mapper/query service dưới `payment/service/query`
- dùng `PricingPackageFacade.getPurchasablePackage(...)`

### Cycle 2.1 — Request validation

RED qua MockMvc:

- thiếu `Idempotency-Key` → `400 IDEMPOTENCY_KEY_REQUIRED`;
- header không phải UUID → validation response chuẩn;
- thiếu pricing package ID → validation response chuẩn.

GREEN: request/header parsing tối thiểu. Không gọi gateway ở các case invalid.

### Cycle 2.2 — Snapshot mua hàng

RED: với package ACTIVE, query qua facade trả đủ teacher, subject, name, session count, duration và giá dùng để tạo Invoice.

GREEN: chỉ gọi public `PricingPackageFacade`; payment không import entity/repository của catalog.

### Exit criteria

- DTO không nhận amount từ client.
- Amount luôn lấy từ snapshot server-side.
- Fingerprint được tính từ dữ liệu canonical hiện có, không từ JSON raw.

## 7. Gate 3 — Tạo PENDING Invoice idempotent

### File dự kiến

- tạo `payment/service/command/InvoiceCommandService.java`
- tạo result type biểu diễn `CREATED` hoặc `REPLAYED`
- chỉnh repository tối thiểu nếu cần làm unique violation xuất hiện trước khi transaction return
- tạo integration test dùng PostgreSQL

### Cycle 3.1 — Tạo mới

RED: public command tạo một Invoice PENDING với invoice number, PayOS order code, owner, snapshot price, key và fingerprint đúng.

GREEN: transaction ngắn lấy hai sequence và insert Invoice.

### Cycle 3.2 — Replay tuần tự

RED: cùng student/key/fingerprint trả cùng invoice ID và orderCode; tổng row vẫn là một.

GREEN: query existing trước insert; so fingerprint.

### Cycle 3.3 — Reuse sai payload

RED: cùng student/key nhưng pricing package hoặc canonical payload khác → `409 IDEMPOTENCY_KEY_REUSED`.

GREEN: so fingerprint trước khi trả replay.

### Cycle 3.4 — Hai request đồng thời

RED: dùng hai transaction/thread thực, barrier để cả hai cùng thử tạo; kết quả cuối có đúng một Invoice và cả hai caller nhận cùng invoice.

GREEN:

- insert attempt chạy trong transaction riêng;
- unique violation làm transaction thua rollback;
- orchestration ngoài transaction query winner và kiểm tra fingerprint;
- không query tiếp trên persistence context đã fail.

### Exit criteria

- Không dựa riêng vào `synchronized` hoặc lock trong JVM.
- Test concurrency dùng PostgreSQL thật.
- Không thêm TTL cho idempotency key.

## 8. Gate 4 — Checkout tracer bullet thành công

### File dự kiến

- tạo `payment/service/InvoiceCheckoutOrchestrator.java` hoặc facade tương đương
- tạo `payment/controller/StudentInvoiceController.java`
- cấu hình test dùng `FakePaymentGateway`

### Cycle 4.1 — Happy path qua HTTP

RED: authenticated STUDENT gọi POST hợp lệ → `201`, response có invoice ID, status PENDING, orderCode, checkout URL, QR và expiry; GET invoice sau đó trả cùng dữ liệu.

GREEN: orchestrator gọi create PENDING → gateway → attach link. Controller không điều phối transaction.

### Cycle 4.2 — Network nằm ngoài transaction

RED: fake gateway ghi nhận `TransactionSynchronizationManager.isActualTransactionActive()` là false tại lúc create và lookup.

GREEN: sửa boundary bean/orchestrator; không dùng annotation để làm test pass giả.

### Cycle 4.3 — Replay có link

RED: POST lại cùng key/payload trả cùng invoice/link và create call count của fake không tăng.

GREEN: short-circuit khi Invoice đã có link.

### Exit criteria

- HTTP behavior đúng API contract.
- Gateway không bao giờ chạy trong DB transaction.
- Controller không chứa business state machine.

## 9. Gate 5 — Timeout và reconciliation

Mở rộng fake theo trạng thái đã có; không thay bằng Mockito orchestration.

### Cycle 5.1 — Timeout trước khi provider lưu

RED: create ở `TIMEOUT_BEFORE_STORE` → HTTP `503`, DB vẫn có đúng một Invoice PENDING chưa có link.

GREEN: map unavailable; không rollback pha tạo Invoice.

### Cycle 5.2 — Provider lưu rồi response bị mất

RED: create ở `STORE_THEN_TIMEOUT` → `503`, provider fake có link, DB chưa có link.

GREEN: không attach dữ liệu suy đoán.

### Cycle 5.3 — Retry khôi phục link

RED: retry cùng key sau STORE_THEN_TIMEOUT → lookup thấy link, attach, trả `201`; create count vẫn bằng một.

GREEN: existing invoice chưa có link luôn đi lookup trước.

### Cycle 5.4 — Lookup xác nhận NOT_FOUND

RED: retry sau TIMEOUT_BEFORE_STORE, lookup trả empty xác định → được create đúng một lần mới và attach thành công.

GREEN: chỉ empty xác định mới mở quyền create.

### Cycle 5.5 — Lookup mập mờ

RED: lookup unavailable → `503`; create count không tăng; Invoice vẫn PENDING chưa link.

GREEN: không convert unavailable thành empty.

### Cycle 5.6 — Attach race

RED: hai retry cùng reconcile một link → cả hai trả cùng Invoice; attach idempotent; không overwrite link khác.

GREEN: `attachPaymentLink` chạy dưới Invoice row lock.

### Exit criteria

- Có test cho cả timeout-before-store và store-then-timeout.
- Không tồn tại đường code blind-create sau ambiguous failure.

## 10. Gate 6 — Webhook security tracer bullet

### File dự kiến

- tạo `payment/controller/PaymentWebhookController.java`
- tạo `payment/service/PaymentWebhookService.java`
- sửa `common/config/SecurityConfig.java` để permit đúng `POST /api/webhooks/payos`
- tạo contract/integration tests

### Cycle 6.1 — Security route

RED: không JWT vẫn tới controller; endpoint student khác vẫn cần STUDENT role.

GREEN: permit matcher hẹp cho webhook; không permit `/api/webhooks/**` nếu chỉ có PayOS.

### Cycle 6.2 — Signature sai

RED: fake/adapter báo invalid signature → HTTP và error code đúng `ERROR_CODES.md` hiện hành (`PAYMENT_SIGNATURE_INVALID` đang là 401). Không mutate DB.

GREEN: exception mapping chuẩn; không trả raw SDK message.

### Cycle 6.3 — Signed payload nhưng dữ liệu sai

Thêm từng test riêng:

- orderCode không tồn tại;
- amount lệch đúng 1 VND;
- provider reference blank;
- event/status không biểu thị thanh toán thành công;
- orderCode/payment link mismatch nếu SDK trả đủ dữ liệu để kiểm tra.

Mỗi case không được tạo PaymentTransaction, Package hay Ledger.

### Exit criteria

- Signature được verify trước business processing.
- Webhook response không dùng generic `ApiResponse` nếu contract PayOS yêu cầu body khác.
- Không log raw payload chứa dữ liệu nhạy cảm.

## 11. Gate 7 — Atomic payment effects

### Chuẩn bị boundary liên module

- Payment gọi Enrollment và Finance qua facade/public service contract, không import repository/entity nội bộ trái architecture rule.
- Bổ sung `PlatformSettingsFacade.getCommissionRate()` và test riêng.
- Nếu Wallet chưa chắc được tạo lúc teacher onboarding, Finance phải cung cấp thao tác atomic `getOrCreateForUpdate(teacherId)`.
- Không dùng `find → save` cho wallet chưa tồn tại vì hai Invoice khác nhau của cùng teacher có thể fund đồng thời.
- Cách ưu tiên với PostgreSQL: insert-if-absent bằng `ON CONFLICT DO NOTHING`, sau đó select row bằng pessimistic lock trong cùng transaction.

### Cycle 7.1 — Happy path atomic

RED qua webhook HTTP + database thật:

- Invoice thành PAID với `paidAt` từ verified provider data;
- đúng một PaymentTransaction được append;
- đúng một StudentPackage ACTIVE được tạo;
- package snapshot đúng price/name/session/duration/commission;
- Wallet pending tăng đúng teacher net;
- đúng một LedgerEntry PACKAGE_FUNDED được append.

GREEN: một public `@Transactional` orchestration method thực hiện toàn bộ mutation. Không gọi network bên trong method này; verified DTO được tạo trước transaction.

### Cycle 7.2 — Rollback Invoice

RED: fault injection tại bước package → webhook fail; Invoice vẫn PENDING, không PaymentTransaction/Package/Ledger, Wallet không đổi.

### Cycle 7.3 — Rollback Finance

RED: fault injection sau package nhưng trước/ở ledger append → mọi effect rollback như trên.

Không mock internal service để tạo rollback test. Dùng test-only boundary/fault hook hoặc dữ liệu gây lỗi có kiểm soát ở integration profile, sao cho transaction thật bị rollback.

### Cycle 7.4 — Commission rounding

RED với ít nhất ba giá trị:

- `100_000`, rate `5.00` → commission `5_000`, teacher net `95_000`;
- amount tạo kết quả phần lẻ → xác nhận HALF_UP;
- rate `0.00` → teacher net bằng gross.

GREEN: gom phép tính vào value/helper thuần, test độc lập rồi dùng trong FinanceFundingService.

### Exit criteria

- Một transaction thật bao phủ tất cả DB effects.
- Failure giữa chừng không để lại partial state.
- Wallet creation/funding an toàn khi hai invoice cùng teacher được xử lý đồng thời.

## 12. Gate 8 — Webhook idempotency và concurrency

### Cycle 8.1 — Sequential duplicate

RED: gửi cùng webhook hai lần → cả hai trả `200`; counts của PaymentTransaction, Package và Ledger đều là một; Wallet chỉ tăng một lần.

GREEN: khóa Invoice trước; nếu đã PAID và provider reference khớp payment đã ghi thì no-op thành công.

### Cycle 8.2 — Concurrent duplicate

RED: hai thread gửi cùng payload đồng thời → cả hai kết thúc `2xx`; chỉ một effect set.

GREEN: pessimistic Invoice lock serialize processing; unique constraints vẫn giữ làm defense-in-depth.

### Cycle 8.3 — Provider reference collision

RED: cùng provider reference nhưng orderCode/invoice khác → không coi là harmless duplicate; trả lỗi và không mutate invoice thứ hai.

### Cycle 8.4 — Invoice đã PAID nhưng payload khác

RED: amount hoặc reference khác → không trả duplicate success; ghi log/correlation ID và trả lỗi phù hợp.

### Exit criteria

- Không có TOCTOU `exists → insert` quyết định correctness.
- Duplicate chỉ no-op khi thực sự là cùng payment.

## 13. Gate 9 — Expiry job và late payment

### File dự kiến

- tạo `scheduler/InvoiceExpiryJob.java`
- bổ sung command query cần thiết cho batch + row lock
- sửa domain Invoice để có transition provider-only cho late payment

### Cycle 9.1 — Expire đúng đối tượng

RED với clock cố định:

- PENDING có `paymentExpiredAt < now` → EXPIRED;
- PENDING chưa đến hạn → giữ nguyên;
- PAID/CANCELLED → giữ nguyên;
- soft-deleted → không được chọn.

GREEN: batch nhỏ bằng Pageable; mỗi Invoice được load lại under lock trước transition.

### Cycle 9.2 — Multi-node guard

RED/config test: job có ShedLock name cố định và lock duration hợp lý. Dùng ShedLock đã cấu hình sẵn; không tạo config/provider thứ hai.

### Cycle 9.3 — Webhook thắng trước

RED: webhook giữ Invoice lock và mark PAID; job chạy sau lock thấy PAID và no-op.

### Cycle 9.4 — Job thắng trước, webhook hợp lệ đến sau

RED: job mark EXPIRED; webhook verified sau đó chuyển EXPIRED → PAID và tạo đủ effects đúng một lần.

GREEN: thêm domain method có tên thể hiện nguồn tin cậy, ví dụ `markPaidFromVerifiedProvider`; không nới `markPaid()` chung để mọi caller chuyển EXPIRED thành PAID.

### Exit criteria

- Không có lost update giữa job và webhook.
- Late payment chỉ được honor sau provider verification.
- Job không giữ lock cả batch trong thời gian dài hơn cần thiết.

## 14. Gate 10 — Student read APIs

### Cycle 10.1 — Invoice ownership

RED qua MockMvc:

- owner GET invoice → `200` và đúng `InvoiceDetail`;
- student khác không đọc được;
- link nullable khi create/reconciliation chưa hoàn tất;
- status PAID/EXPIRED phản ánh đúng.

GREEN: query service read-only dùng `InvoiceQueryRepository.findByIdAndStudentId`.

### Cycle 10.2 — Package list

RED: chỉ trả package của authenticated student, pagination/meta đúng, filter status đúng.

GREEN: service/controller tối thiểu.

### Cycle 10.3 — Package detail ownership

RED: owner đọc được; student khác không đọc được.

### Exit criteria

- Không nhận student ID từ request để xác định owner; lấy từ `AuthenticatedUser`.
- Không expose entity JPA trực tiếp.

## 15. Gate 11 — Architecture và full regression

### Bổ sung/điều chỉnh guard nếu cần

- SDK `vn.payos.*` chỉ được xuất hiện trong adapter/config và adapter test.
- Controller không phụ thuộc repository.
- Payment service không dùng EntityManager trực tiếp.
- Invoice command/query boundary hiện hữu vẫn xanh.
- Payment không import catalog/enrollment/finance repositories xuyên module; dùng facade/service contract được công khai.

### Chạy

```text
mvn test
```

Ghi đầy đủ:

- tests run;
- failures;
- errors;
- skipped;
- Docker/Testcontainers có thực sự hoạt động;
- migration V1–latest trên database rỗng;
- tên các integration/concurrency/rollback tests đã chạy.

### Exit criteria

- Full suite xanh, zero unexpected skipped.
- Không sửa/bỏ guard cũ để code mới lọt qua.
- `git diff` không có credential, secret hoặc payload thật.

## 16. Gate 12 — Manual PayOS smoke test

Gate này chỉ chạy khi chủ dự án cung cấp credentials và đồng ý giao dịch tiền thật.

### Chuẩn bị

1. Có public HTTPS webhook URL; nếu local thì dùng tunnel được chủ dự án chấp thuận.
2. Cấu hình webhook URL trên PayOS và xác nhận endpoint nhận được verification/sample request đúng contract.
3. Điền credentials chỉ trong file/environment bị Git ignore.
4. Bật `APP_PAYMENT_PROVIDER=payos`.
5. Dùng giao dịch giá trị nhỏ đã được chủ dự án duyệt.

### Kiểm tra

1. Tạo Invoice từ API bằng một idempotency key mới.
2. Xác nhận response có checkout URL/QR và DB có PENDING Invoice.
3. Thanh toán thật.
4. Xác nhận webhook trả `2xx` dưới mục tiêu latency của SPEC.
5. Poll GET Invoice đến PAID.
6. Xác nhận StudentPackage ACTIVE.
7. Xác nhận Wallet pending tăng teacher net chính xác.
8. Xác nhận đúng một PaymentTransaction và một LedgerEntry.
9. Replay cùng webhook fixture đã sanitize trong môi trường test và xác nhận không double-effect.
10. Kiểm tra log không chứa credential/signature/full raw payload.

### Không được tự động hóa

- Không commit credentials.
- Không tự tạo giao dịch tiền thật nếu chưa có đồng ý rõ ràng.
- Không tuyên bố E2E hoàn tất chỉ vì automated tests xanh.

## 17. Definition of Done cuối cùng

Task 4 chỉ được đánh dấu hoàn tất khi tất cả mục sau đều đúng:

- [ ] Runtime gate Task 3 được đóng bằng Testcontainers thật.
- [ ] Provider disabled và PayOS production adapter đều wiring đúng.
- [ ] POST Invoice idempotent cả tuần tự lẫn concurrent.
- [ ] Không có network call trong DB transaction.
- [ ] Timeout-before-store và store-then-timeout đều có test.
- [ ] Retry luôn reconcile trước khi có quyền create lại.
- [ ] Webhook verify signature và validate orderCode/amount/reference/status.
- [ ] Webhook duplicate/concurrent chỉ tạo một effect set.
- [ ] Payment effects rollback atomic khi package hoặc finance lỗi.
- [ ] Wallet funding concurrent an toàn, kể cả wallet chưa tồn tại.
- [ ] Commission được snapshot và tính teacher net đúng quy tắc rounding.
- [ ] Expiry job và webhook race có test hai chiều.
- [ ] Student GET APIs enforce ownership và pagination.
- [ ] Architecture tests và full Maven suite xanh, không skip bất thường.
- [ ] Manual PayOS smoke test được ghi nhận riêng; nếu chưa có credentials thì ghi rõ là gate còn mở.

## 18. Mẫu báo cáo sau mỗi gate

Agent phải thêm một đoạn ngắn vào `docs/B/PROGRESS_BE_B.md`:

```markdown
### Gate N — <tên gate>

- Behavior vừa thêm: ...
- RED: `<test command>` — fail vì ...
- GREEN: `<test command>` — pass ... tests
- Regression: `<test command>` — pass/fail/skipped ...
- Files changed: ...
- Invariant đã chứng minh: ...
- Gate còn mở/blocker: ...
```

Không ghi “logic đúng” nếu chưa có test hoặc runtime evidence tương ứng.
