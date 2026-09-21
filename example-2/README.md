# Ví dụ 2 — Custom Login bằng username hoặc email

Project Maven độc lập, không phụ thuộc source hoặc cấu hình runtime của `example-1`.

## Tài liệu gốc

- `../docs/HƯỚNG DẪN CHỨC NĂNG LOGIN BẰNG SPRING SECURITY 7.pdf`: Ví dụ 2, trang 20–40.
- `../docs/HƯỚNG DẪN CUSTOM LOGIN VỚI SPRING BOOT 4.pdf`: trang 1–19.
- Database `webst9`, cổng 8081: tài liệu thứ nhất, trang 25–26.
- Entity, DTO, mapper, repository và service: trang 31–34.
- Security, principal và đăng nhập username/email: trang 26–30.
- Giao diện dùng Thymeleaf Layout Dialect: trang 36–40.

## Công nghệ

Spring Boot 4.1.1, Java 21, Spring Security do Spring Boot quản lý version,
Spring Data JPA/Hibernate, SQL Server, BCrypt, Thymeleaf, Thymeleaf Layout Dialect,
MapStruct 1.6.3, Jakarta Validation, Lombok và Maven.

## Cấu hình và chạy

Chạy lệnh trong thư mục `example-2`, vì `.env` được đọc tương đối từ thư mục làm việc.

1. Dùng `.env` cục bộ hiện có, hoặc sao chép `.env.example` thành `.env` nếu chưa có.
2. Điền thông tin SQL Server đúng vào `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
   Database phải là **webst9**, không dùng webst8.
3. Nếu database webst9 chưa tồn tại, người dùng tự tạo database rỗng trong SQL Server.
   Project không có script tự tạo/xóa database.
4. Cấu hình theo PDF dùng `ddl-auto=update`: khi kết nối thành công, Hibernate quản lý
   schema `users`, `roles` trong webst9. Initializer chỉ thêm role/tài khoản chưa có;
   không cập nhật mật khẩu, role hoặc trạng thái của tài khoản có sẵn.
5. Chọn JDK 21 cho Maven và ứng dụng, rồi build/run:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
mvn -version
mvn clean package
& "$env:JAVA_HOME\bin\java.exe" -jar target/springboot1-9-1.0.jar
```

Điều chỉnh đường dẫn `JAVA_HOME` nếu JDK 21 trên máy nằm ở vị trí khác.
Các lệnh trên chỉ đặt biến cho tiến trình PowerShell hiện tại.

Mở `http://localhost:8081/login`. Login thành công về `http://localhost:8081/`.
Logout bằng nút **Đăng xuất** trong header (POST có CSRF token).

## Tài khoản mẫu

| Role | Username | Email | Password mẫu |
|---|---|---|---|
| ROLE_ADMIN | admin | admin@iotstar.vn | 123456 |
| ROLE_USER | user01 | user01@gmail.com | 123456 |

Có thể đổi thông tin seed qua `ADMIN_*` và `USER_*` trong `.env` trước lần chạy đầu.
Mật khẩu được BCrypt hóa trước khi ghi database. Nếu username hoặc email đã tồn tại,
initializer bỏ qua, không ghi đè tài khoản. Thay đổi `.env` không reset mật khẩu user đã có.
`.env` được `.gitignore` loại trừ; `.env.example` không chứa mật khẩu SQL Server thật.

## Luồng login và principal

- Form POST `/login` dùng `name="username"` và `name="password"`.
- Giá trị `username` có thể là username hoặc email.
- `CustomUserDetailsService` gọi đúng `findByUsernameOrEmail(login, login)`.
- Không tìm thấy thì ném `UsernameNotFoundException`.
- `DaoAuthenticationProvider` kiểm tra BCrypt và trạng thái `enabled`.
- Principal là `CustomUserDetails`: id, username, email, password, fullName,
  images, role, enabled. Authority giữ nguyên `ROLE_USER` hoặc `ROLE_ADMIN`.
- Đăng nhập thành công luôn redirect `/`, lỗi về `/login?error=true`.
- Header lấy fullname, username, email, role và ảnh từ `#authentication.principal`.
- Ảnh trống/null dùng `/images/avatar-default.svg`. Ảnh mẫu là `/images/user.svg`.
- Layout dùng `layout:decorate`, `layout:fragment` và `layout:title-pattern`.
- Thymeleaf tự chèn CSRF token cho form POST có `th:action`.

## Phân quyền

- Public: `/login`, `/css/**`, `/js/**`, `/images/**`, `/uploads/**`.
- `/admin/**`: ADMIN.
- Còn lại, kể cả `/`: phải đăng nhập.
- Không tạo trang admin nghiệp vụ vì PDF không cung cấp endpoint tương ứng.
  Test dùng `/admin/probe` chỉ trong `src/test`; endpoint này không có trong ứng dụng đóng gói.
  Do đó không dùng một URL admin chưa tồn tại để mong nhận trang quản trị khi login ADMIN.

## Test và kết quả kiểm tra

`mvn clean package` bằng JDK 21.0.12.1: **BUILD SUCCESS**, 18 test,
0 failures, 0 errors, 0 skipped.

- 15 lượt kiểm tra MVC/Security: public login, chặn anonymous, username/email,
  principal, header/layout, mật khẩu sai, disabled, user không tồn tại,
  avatar dự phòng và file ảnh, USER/ADMIN, logout và CSRF.
- 2 test initializer: BCrypt/role của tài khoản mới và không ghi đè dữ liệu đã có.
- 1 test mapper: profile và role được ánh xạ đúng.

Test dùng mock repository, không khởi tạo JPA hoặc kết nối SQL Server thật.
Kết quả chi tiết ở `target/surefire-reports/` sau build.

Lần chạy thật ngày 21/09/2026 đã thử bằng JDK 21 và JAR đóng gói:

- SQL Server phản hồi tại localhost:1433.
- Tomcat bắt đầu khởi tạo cổng 8081, nhưng ứng dụng không hoàn tất startup.
- SQL Server báo **18456: Login failed for user 'sa'**.
- Kiểm tra JDBC chỉ đọc tới master bằng cùng thông tin đăng nhập cũng thất bại.
- Chưa xác nhận webst9 tồn tại; chưa kiểm tra HTTP login/logout trên SQL Server thật.
- Không tạo database, không đổi mật khẩu SQL Server, không ghi được dữ liệu seed.

Người dùng cần kiểm tra DB credentials/SQL Authentication trong `.env` của project này
và sự tồn tại/quyền truy cập webst9, sau đó chạy lại. Thông tin kết nối ban đầu đã được
sao chép riêng từ `.env` hiện có của example-1 và đổi database sang webst9; không sửa example-1.

## Các điều chỉnh so với PDF

- Java 21 thay Java 26 theo yêu cầu.
- Dùng `.env`, là tùy chọn tài liệu cho phép tại trang 25.
- POM giữ các thư viện phục vụ Ví dụ 2; không đưa các dependency H2, Mail,
  DevTools và những starter-test không sử dụng từ POM tổng hợp vào project.
  Không có datasource H2 hay email service.
- Thêm tài khoản ADMIN bên cạnh user01 để kiểm tra phân quyền theo yêu cầu;
  tách seed vào `DataInitializer`, kiểm tra cả username và email trước khi thêm.
- Cung cấp ảnh SVG cục bộ thay các đường dẫn PNG mẫu không kèm file ảnh.
- Thêm `id`/`for` cho label/input và viewport cho layout; không thay luồng giao diện mẫu.
- Thêm automated tests; không bổ sung chức năng nghiệp vụ khác.

Không có OTP, Register, Forgot Password, CRUD User/Product, search, pagination,
thống kê, upload ảnh, Cloudinary hoặc email service. `LoginDTO` và `UserService.findById`
được giữ đúng phạm vi mẫu; việc xử lý POST login thuộc Spring Security.

## Danh sách file nguồn và cấu hình

```text
example-2/
├── .env                         # cấu hình riêng, không commit
├── .env.example
├── .gitignore
├── pom.xml
├── README.md
├── src/main/java/vn/iotstar/
│   ├── Springboot19Application.java
│   ├── config/
│   │   ├── DataInitializer.java
│   │   └── SecurityConfig.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   └── HomeController.java
│   ├── dto/
│   │   ├── LoginDTO.java
│   │   └── UserDTO.java
│   ├── entity/
│   │   ├── Role.java
│   │   └── User.java
│   ├── mapper/UserMapper.java
│   ├── repository/
│   │   ├── RoleRepository.java
│   │   └── UserRepository.java
│   ├── security/
│   │   ├── CustomUserDetails.java
│   │   └── CustomUserDetailsService.java
│   └── service/
│       ├── UserService.java
│       └── impl/UserServiceImpl.java
├── src/main/resources/
│   ├── application.properties
│   ├── static/images/
│   │   ├── avatar-default.svg
│   │   └── user.svg
│   └── templates/
│       ├── auth/login.html
│       ├── fragments/header.html
│       ├── layouts/layout.html
│       └── home.html
└── src/test/java/vn/iotstar/
    ├── ExampleTwoSecurityTest.java
    ├── config/DataInitializerTest.java
    └── mapper/UserMapperTest.java
```

`target/` chứa JAR, mã MapStruct sinh tự động và báo cáo test; được bỏ qua trong Git.
`target/runtime.log` lưu log lần chạy SQL Server thực tế. Các file chẩn đoán trong
`target/` không thuộc source ứng dụng và sẽ được xóa bởi `mvn clean`.
