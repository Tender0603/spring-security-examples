# Kết quả kiểm chứng Ví dụ 3

Ngày kiểm tra: 22/09/2026, máy local Windows.

## Build và tests

- Runtime build: Eclipse Temurin **21.0.12.1**; Maven **3.9.16**.
- Spring Boot parent/plugin: **4.1.1**; Java compiler release: **21**.
- `mvn -f example-3/pom.xml -B test`: **BUILD SUCCESS**.
- `mvn -f example-3/pom.xml -B clean package`: **BUILD SUCCESS**, gồm chạy lại toàn bộ test từ source sạch.
- Artifact: `target/example-3-1.0.0.jar`, executable Spring Boot JAR, main class `vn.iotstar.ShopApplication`.

| Test class | Tổng | Passed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| ShopIntegrationTest | 32 | 32 | 0 | 0 | 0 |
| CloudinaryUploadRegressionTest | 6 | 6 | 0 | 0 | 0 |
| ExternalServicesTest | 6 | 6 | 0 | 0 | 0 |
| StartupTest | 1 | 1 | 0 | 0 | 0 |
| **Tổng** | **45** | **45** | **0** | **0** | **0** |

XML/text reports nằm ở `target/surefire-reports/`. Log local `test-run.log`, `build-run.log` được ignore khỏi Git.

Test dùng database H2 in-memory ở chế độ MSSQLServer, email/Cloudinary mock. Test startup khởi động Tomcat thật trên cổng ngẫu nhiên và xác minh HTTP 200 cùng HTML cho dashboard/login. Không gửi email thật và không gọi Cloudinary thật.

## SQL Server và dịch vụ ngoài

- Cấu hình runtime: SQL Server `localhost:1433`, database `webst10`, port ứng dụng `8082`.
- Người dùng xác nhận STS đã kết nối `webst10`, login và CRUD User hoạt động. JVM kiểm tra độc lập vẫn nhận SQLState `S0001`, error **18456**; không thay đổi cấu hình SQL hoặc `.env` và không quy lỗi upload ở STS cho SQL Server.
- Không tự tạo database, không sửa `webst8`/`webst9`. Có sẵn `database/create-database.sql` để người dùng chạy nếu `webst10` chưa có.
- Spring Config Data xác nhận `.env` được nạp và ba biến Cloudinary hiệu lực khớp với file; chỉ xuất boolean, không xuất giá trị.
- Cloudinary thật: upload PNG sinh riêng và destroy chính asset vừa tạo thành công.
- Kiểm tra thủ công toàn luồng với Cloudinary thật + H2: multipart `/products/create` → upload → lưu URL và public ID riêng → redirect `/products` → render ảnh đều thành công. Sau đó xóa Product/ảnh thử. Không đưa phép gọi thật vào `mvn test`.
- SMTP thật chưa được kiểm chứng trong bản sửa upload này.

## Chẩn đoán và bản sửa upload

- Form multipart và byte array được SDK Cloudinary HTTP5 2.4.0 hỗ trợ; không phát hiện lỗi binding MultipartFile.
- Xác định lỗi quan sát: `CloudinaryServiceImpl` bọc exception SDK, `GlobalExceptionHandler` trả thông báo chung không log. Không thể truy hồi nguyên nhân SDK của request cũ; ảnh thử hiện tại upload được, nên không kết luận credentials sai.
- Thêm `CloudinaryOperationException` chỉ chứa type/category/message an toàn; không giữ cause hoặc response thô có thể lộ secret. Adapter log an toàn, ProductController trả lại form khi upload lỗi.
- Kiểm tra `secure_url`/`public_id` trước khi lưu; không ghi chuỗi `null` khi response thiếu field.
- `Product.imageUrl` chỉ lưu URL; cột nullable `image_public_id` giữ ID để thay/xóa asset. Đọc tương thích dữ liệu cũ `URL|publicId`; template lấy trực tiếp URL đã chuẩn hóa.
- Giữ nguyên authentication, authorization, OTP/email, User CRUD, search/pagination và application.properties.

## Rà soát source và Git

- `git diff -- example-1 example-2` không có thay đổi.
- Không tạo `.git` trong `example-3`, không commit/push.
- `.env` và `target/` được Git ignore; `.env.example` được phép đưa vào Git.
- Không có Lombok, builder hay annotation processor trong source/POM.
- Mapper Java thủ công, giữ mapping DTO/entity theo PDF; không phụ thuộc STS annotation processing.
- Không có secret thật hoặc cấu hình trỏ `webst8`/`webst9` trong source runtime.

Hướng dẫn tài liệu nguồn, cấu trúc, environment, import STS, tài khoản mẫu và toàn bộ URL kiểm tra nằm trong `README.md`.
