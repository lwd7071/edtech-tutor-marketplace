# Rollout giấy tờ định danh riêng tư

Upload mới dùng authenticated delivery; các object cũ có thể vẫn có URL công khai. Thay đổi ứng dụng không thu hồi URL cũ và không tự chuyển hoặc xóa object. Không ghi URL, public ID, thông tin định danh hoặc secret vào báo cáo công khai.

Trước rollout, người vận hành được ủy quyền kiểm kê read-only các bản ghi `teacher_documents` đang hoạt động và đối chiếu với metadata Cloudinary theo public ID, loại delivery, resource type và định dạng. Ghi riêng số object public, authenticated, thiếu hoặc không đối chiếu được; không tải nội dung vào log. Xác nhận backup và khả năng khôi phục, sau đó thử quy trình trên bản sao không chứa dữ liệu thật.

Chuẩn bị phương án chuyển từng object sang authenticated delivery và đối chiếu nội dung, metadata, quyền xem qua proxy, khả năng xóa và cache/CDN trước khi thu hồi object hoặc URL cũ. Không giả định thay delivery type sẽ thu hồi ngay bản sao CDN. Đối chiếu số lượng trước/sau, xử lý object lỗi riêng và ghi lại bằng chứng không chứa dữ liệu nhạy cảm. Các object cũ có thể chưa xem được qua proxy mới cho tới khi chuyển đổi.

Chỉ thực hiện chuyển đổi sau khi người dùng xác nhận kế hoạch rollout, backup và phương án khôi phục. Kiểm thử Cloudinary thật phải dùng tài liệu thử, không dùng giấy tờ thật; nếu provider hoặc kiểm tra quyền không thành công, dừng rollout và ghi blocked.
