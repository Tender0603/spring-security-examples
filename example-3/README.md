# Ví dụ 3 — IOTSTAR SHOP

Project Maven Spring Boot độc lập cho môn Lập trình Web. Không phụ thuộc hoặc sửa `example-1`, `example-2` và không có Git repository lồng bên trong.

## Tài liệu và phạm vi

- `docs/HƯỚNG DẪN CHỨC NĂNG LOGIN BẰNG SPRING SECURITY 7.pdf`: đề **Ví dụ 3** bắt đầu cuối trang 40, kết thúc trang 41.
- `docs/HƯỚNG DẪN SPRING BOOT-SECURITY7-MASTRUCT.pdf`: toàn bộ 55 trang là hướng dẫn chi tiết cho bài này. Trang 1–3: yêu cầu; 4–8: cấu trúc/cấu hình; 9–17: entity, DTO, mapper, database, repository; 17–26: service; 27–36: security/controller; 36–49: CSS, layout, form; 49–55: chạy và kết quả. Trang 40 của PDF này là phần view, không phải điểm bắt đầu Ví dụ 3.

Giữ package `vn.iotstar`, tên class và mô hình MVC → Service → Repository theo tài liệu. Giữ các route, layout IOTSTAR SHOP, cấu trúc bảng và luồng nghiệp vụ. Không thêm JWT, giỏ hàng, thanh toán hoặc profile riêng vì không thuộc ví dụ.

Điều chỉnh theo máy và yêu cầu:

- **Java 21**, **Spring Boot 4.1.1**, Spring Security 7 do Boot quản lý, Maven.
- SQL Server **localhost:1433**, database **webst10**, HTTP port **8082**.
- Không sử dụng Lombok. Constructor/getter/setter viết trực tiếp trong Java.
- `UserMapper`, `ProductMapper` là mapper Java thủ công tương đương mapping MapStruct 1.6.3 trong PDF. Không cần annotation processing của MapStruct/Lombok trong STS; compiler đặt `proc=none`.
- Spring Data JPA/Hibernate, Jakarta Validation, BCrypt, Spring Mail, Cloudinary HTTP5 2.4.0, Thymeleaf và Thymeleaf Layout Dialect. View dùng fragment `th:replace` như phần code chi tiết trong PDF.
- UTF-8 dùng auto-configuration của Boot, tránh trùng bean `characterEncodingFilter` trong mẫu.
- Bổ sung kiểm tra trùng username khi sửa, validation theo độ dài cột, khóa OTP khi xác minh, đổi mật khẩu và tiêu thụ OTP trong cùng transaction, thông báo lỗi trên form. User còn sản phẩm phải xóa sản phẩm trước để giữ quan hệ khóa ngoại theo tài liệu (không cascade xóa ngoài ý muốn).

## Chức năng

- Đăng nhập bằng **username**, BCrypt, lưu authentication trong session; tối đa một session/user, lần đăng nhập mới làm session cũ hết hiệu lực; logout bằng POST có CSRF.
- Đăng ký tài khoản `ROLE_USER`, trạng thái chưa kích hoạt; gửi email OTP, xác nhận và gửi lại OTP cho tài khoản đang chờ xác nhận.
- OTP ngẫu nhiên 6 số, lưu BCrypt hash, hết hạn sau 5 phút, tối đa 5 lần thử, dùng một lần; tách loại `REGISTER` và `RESET_PASSWORD`. Gửi lại thay OTP cũ.
- Quên mật khẩu → gửi OTP email → nhập email/OTP/mật khẩu mới/xác nhận → BCrypt mật khẩu mới. OTP không hợp lệ không đổi mật khẩu.
- ADMIN: thêm/sửa/xóa User, chọn role, bật/tắt tài khoản, tìm theo username/email/họ tên, phân trang, đếm sản phẩm của từng User. Password user do admin tạo mặc định `123456`.
- Người đã đăng nhập: thêm/sửa/xóa Product, tìm tên/mô tả, phân trang, upload/thay/xóa ảnh Cloudinary; giá không âm, tối đa 2 chữ số thập phân.
- Product thuộc người tạo (`User` 1–N `Product`). Người tạo lấy từ principal, không lấy userId tùy ý gửi từ form. Sửa không chuyển chủ sở hữu.
- **Đúng phân quyền PDF:** cả USER và ADMIN có thể quản lý toàn bộ Products; tài liệu không giới hạn edit/delete theo chủ sở hữu. `/users/**` chỉ ADMIN. Dashboard `/` công khai, hiển thị tổng User/Product; header hiển thị username và nút logout.
- Ảnh mới lưu `secure_url` trong `Product.imageUrl` và `public_id` trong `Product.imagePublicId`; dữ liệu cũ dạng `URL|publicId` vẫn được đọc tương thích. Giới hạn upload 10 MB/file, 20 MB/request.
- `DataInitializer` tạo role/tài khoản mẫu nếu chưa có, không ghi đè tài khoản tồn tại; kiểm tra cả username lẫn email.

## Cấu trúc

```text
example-3/
├── pom.xml
├── .gitignore
├── .env.example
├── database/create-database.sql
├── src/main/java/vn/iotstar/
│   ├── ShopApplication.java
│   ├── config/       # SecurityConfig, CloudinaryConfig, DataInitializer
│   ├── controller/   # Auth, Home, User, Product, GlobalExceptionHandler
│   ├── dto/          # User, Product, Register, Login, VerifyOtp, Forgot/ResetPassword
│   ├── entity/       # User, Role, OtpToken, Product
│   ├── mapper/       # UserMapper, ProductMapper (Java thuần)
│   ├── repository/
│   ├── security/     # CustomUserDetails, CustomUserDetailsService
│   └── service/impl/
├── src/main/resources/
│   ├── application.properties
│   ├── static/css/app.css
│   └── templates/    # auth, users, products, fragments, layouts, home, error
├── src/test/java/vn/iotstar/
│   ├── ShopIntegrationTest.java
│   ├── ExternalServicesTest.java
│   └── StartupTest.java
└── src/test/resources/application-test.properties
```

## Database và cấu hình trước khi chạy

1. Mở SQL Server Management Studio, kết nối SQL Server local. Nếu **webst10 chưa có**, chạy `database/create-database.sql` hoặc:

   ```sql
   IF DB_ID(N'webst10') IS NULL
       EXEC(N'CREATE DATABASE [webst10]');
   ```

   Script chỉ được cung cấp, không tự thực thi. Không sửa `webst8`/`webst9`. Hibernate tạo/cập nhật bảng trong `webst10` lúc startup với `DDL_AUTO=update`.

2. Từ thư mục `example-3`, tạo cấu hình local:

   ```powershell
   Copy-Item .env.example .env
   ```

3. Điền các biến trong `.env` hoặc Run Configurations → Environment của STS:

   | Biến | Giá trị cần điền |
   |---|---|
   | `DB_URL` | `jdbc:sqlserver://localhost:1433;databaseName=webst10;encrypt=true;trustServerCertificate=true` |
   | `DB_USERNAME` | SQL login có quyền truy cập/tạo bảng trong webst10 |
   | `DB_PASSWORD` | Mật khẩu SQL login của bạn |
   | `MAIL_HOST` | SMTP host; mặc định `smtp.gmail.com` |
   | `MAIL_PORT` | SMTP port; mặc định `587` |
   | `MAIL_USERNAME` | Tài khoản gửi email |
   | `MAIL_PASSWORD` | SMTP password/Gmail App Password |
   | `CLOUDINARY_CLOUD_NAME` | Cloud name |
   | `CLOUDINARY_API_KEY` | API key |
   | `CLOUDINARY_API_SECRET` | API secret |
   | `DDL_AUTO` | Tùy chọn, mặc định `update` |
   | `SHOW_SQL` | Tùy chọn, mặc định `false` |
   | `APP_SEED_DATA` | Tùy chọn, mặc định `true`; `false` để tắt seed |

   Port ứng dụng cố định `8082` trong properties. Nếu tắt seed ở database mới, cần tự tạo role trước khi đăng ký.

`.env` được đọc theo định dạng **Java properties**, không phải script shell: dùng `KEY=value`, không `export`, không bao giá trị bằng dấu nháy. Dùng `spring.config.import=optional:file:.env[.properties]` theo [cơ chế import file không có extension của Spring Boot](https://docs.spring.io/spring-boot/reference/features/external-config.html). File được tìm theo **working directory**. Optional chỉ cho phép thiếu file; các credentials vẫn phải được cung cấp qua environment nếu không dùng file.

Không có secret thật trong source. `.env`, `.env.*` (trừ `.env.example`) và `target/` được ignore. Không thêm `.env`/`target/` vào Git, không chạy `git init` trong project này.

## Chạy bằng STS/Eclipse

1. File → Import → Maven → Existing Maven Projects → chọn `C:\SPRING SECURITY\example-3`.
2. Preferences → Java → Installed JREs: chọn **JDK 21**; project Java Build Path/JRE System Library = JavaSE-21.
3. Maven → Update Project. Không cần cài Lombok hay bật annotation processing.
4. Run Configurations → Spring Boot App: main class `vn.iotstar.ShopApplication`, JRE = JDK 21, working directory = `${workspace_loc:example-3}` (hoặc đường dẫn tuyệt đối của project).
5. Hoàn tất database và `.env` ở trên; Run As → Spring Boot App. Mở `http://localhost:8082`.

Nếu Maven/STS còn lấy JDK 26, đổi JRE của Maven launch về 21. Máy kiểm tra có `java` trên PATH là 21 nhưng `JAVA_HOME` trỏ JDK 26 nên cần sửa riêng cho phiên terminal:

```powershell
cd 'C:\SPRING SECURITY\example-3'
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
mvn -version
mvn test
mvn clean package
mvn spring-boot:run
# Hoặc sau khi package:
java -jar target/example-3-1.0.0.jar
```

Thay đường dẫn JDK nếu máy cài ở vị trí khác. `mvn -version` phải hiển thị Java 21. Khi chạy JAR cũng dùng executable Java 21 và working directory `example-3`.

## Tài khoản mẫu

| Username | Email | Password | Role |
|---|---|---|---|
| `admin` | `admin@iotstar.vn` | `123456` | `ROLE_ADMIN` |
| `user01` | `user01@gmail.com` | `123456` | `ROLE_USER` |

Hai tài khoản mẫu được kích hoạt sẵn, password được BCrypt encode trước khi lưu. Password mẫu chỉ có tác dụng khi tài khoản được tạo lần đầu; initializer không reset password đã đổi.

## URL và kiểm tra thủ công

Base URL: **http://localhost:8082**.

| URL | Cách kiểm tra |
|---|---|
| `/` | Dashboard công khai, tổng User/Product, header sau đăng nhập |
| `/register` | Nhập username mới, email bạn nhận được thư, họ tên, password ≥ 6 ký tự và xác nhận |
| `/verify-otp?email=dia-chi-email` | Nhập OTP; thử mã sai/hết hạn, nút gửi lại; tài khoản chưa xác nhận không đăng nhập được |
| `/login` | Đăng nhập bằng username; thử sai password, tài khoản chưa kích hoạt, tài khoản mẫu |
| `/forgot-password` | Nhập email đã đăng ký, nhận OTP đặt lại password |
| `/reset-password` | Nhập email, OTP reset và password mới; mã đăng ký không dùng để reset được |
| `/users` | ADMIN: danh sách User, tìm kiếm, phân trang, số Product; USER nhận 403 |
| `/users/create` | ADMIN thêm User, chọn role và trạng thái; password mặc định 123456 |
| `/users/edit/{id}` | ADMIN sửa User; thử username/email trùng để xem validation |
| `/products` | USER/ADMIN: danh sách, tìm kiếm, phân trang |
| `/products/create` | Thêm sản phẩm có/không có ảnh; kiểm tra giá âm bị từ chối |
| `/products/edit/{id}` | Sửa sản phẩm, giữ ảnh khi không chọn file, thay ảnh Cloudinary |
| `/logout` | Dùng **nút Logout** để gửi POST có CSRF, không dùng link GET để đăng xuất |

Thay `{id}` bằng ID từ danh sách. Delete dùng nút trong bảng, gửi POST `/users/delete/{id}` hoặc `/products/delete/{id}` có CSRF. Thử `/users?keyword=user&page=0&size=1` và `/products?keyword=phone&page=0&size=1` để kiểm tra tìm kiếm/phân trang (page bắt đầu từ 0). Kích thước trang được giới hạn 1–100.

## Automated tests và giới hạn kiểm chứng

`mvn test` sử dụng H2 in-memory ở chế độ MSSQLServer, không kết nối SQL Server thật. Profile test chỉ nằm trong `src/test/resources`, không nằm trong JAR chạy thật. Email và Cloudinary đều được mock; không gửi email/upload thật.

- `ShopIntegrationTest`: 32 test, kiểm tra BCrypt/login/session/logout/CSRF/anonymous/USER–ADMIN, template thực tế, OTP dùng một lần/hết hạn/giới hạn thử/resend/phân loại, đăng ký/validation/rollback khi SMTP lỗi, reset password, CRUD/search/pagination/count, owner từ principal, ảnh và mapper, seed idempotent; bổ sung multipart upload → lưu URL → redirect → render ảnh và giữ form/dữ liệu khi upload lỗi.
- `CloudinaryUploadRegressionTest`: 6 test kiểm tra logging không lộ dữ liệu nhạy cảm, phân loại lỗi, phản hồi thiếu URL/public ID và tương thích dữ liệu ảnh cũ.
- `ExternalServicesTest`: 6 test kiểm tra nội dung email với mock JavaMailSender, Cloudinary upload/delete/validation/lỗi với mock SDK.
- `StartupTest`: 1 test khởi động ứng dụng với Tomcat thật trên port ngẫu nhiên và gọi HTTP dashboard/login, dùng H2 và mock dịch vụ ngoài.

Kết quả kiểm chứng: xem `VERIFICATION.md`.

Đã kiểm tra thủ công Cloudinary thật bằng ảnh PNG sinh riêng: upload/xóa thành công. Đã chạy controller multipart → Cloudinary thật → lưu URL/public ID trong H2 → redirect `/products` → render ảnh thành công; sau đó xóa đúng Product/ảnh thử. Kiểm tra này tách riêng khỏi automated tests. SMTP thật chưa được kiểm chứng.

Người dùng đã xác nhận STS kết nối SQL Server `webst10`, login và CRUD User hoạt động. Riêng JVM chẩn đoán độc lập bị SQL Server từ chối login 18456; không thay đổi `.env`/cấu hình SQL để xử lý khác biệt này và không xem đó là nguyên nhân upload ở STS.

## Kiểm tra Product upload sau bản sửa

1. Stop ứng dụng trong STS, Project → Clean, Maven → Update Project nếu cần, rồi Run As → Spring Boot App với Java 21 và working directory `example-3`. Phải restart sau khi thay đổi `.env` để bean Cloudinary nhận cấu hình mới.
2. Với `DDL_AUTO=update`, Hibernate thêm cột nullable `image_public_id` vào bảng `products`. Không xóa bảng/dữ liệu cũ; không cần nhập lại ảnh cũ.
3. Console có dòng `Cloudinary configuration: dotenvLoaded=true, requiredVariablesPresent=true` khi `.env` được nạp. Chỉ log trạng thái, không log giá trị. Nếu cấu hình bằng environment thay cho file thì `dotenvLoaded=false` vẫn hợp lệ.
4. Login admin → `http://localhost:8082/products/create` → nhập tên/giá → chọn JPG/PNG hợp lệ dưới 10 MB → Lưu. Kết quả mong đợi: redirect `/products` và nhìn thấy ảnh.
5. Nếu upload lỗi, form giữ dữ liệu nhập và hiển thị thông báo riêng của Cloudinary. Chọn lại file trước khi submit lần nữa. Console ghi `Cloudinary upload failed: type=..., category=..., message=...`; message được phân loại an toàn, không in exception/response thô vì chúng có thể chứa API key/chữ ký.

Đã xác nhận Spring nạp ba biến Cloudinary từ `.env` và dùng được để upload thật. Nguyên nhân khiến lỗi trước đây không xuất hiện trong Console là adapter bọc exception rồi `GlobalExceptionHandler` nuốt nguyên nhân bằng thông báo chung. Lỗi SDK cụ thể của lần trước chưa tái hiện được với PNG thử; không kết luận credentials sai hoặc suy đoán lỗi ảnh/mạng khi chưa có bằng chứng.
