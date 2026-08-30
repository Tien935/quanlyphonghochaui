# Quản lý Phòng học HAUI (HAUI Classroom Management)

Đây là ứng dụng di động dành cho hệ điều hành Android, được phát triển nhằm mục đích số hóa và tối ưu hóa quy trình quản lý, đặt phòng học tại trường Đại học Công nghiệp Hà Nội (HAUI). 

Dự án được xây dựng với ngôn ngữ **Java** kết hợp cùng **Supabase** (thay thế cho Firebase/Backend truyền thống) để cung cấp cơ sở dữ liệu thời gian thực và xác thực người dùng.

---

## 🌟 Các tính năng chính

### Dành cho Sinh viên (Student)
- **Đăng nhập:** Xác thực an toàn bằng tài khoản sinh viên qua Supabase Auth.
- **Trang chủ & Taskbar:** Giao diện điều hướng bằng BottomNavigationView hiện đại, dễ thao tác.
- **Xem thông báo & Lịch học:** Nắm bắt lịch học và các thông báo mới nhất.
- **Đặt phòng học:** Tìm kiếm phòng trống theo tòa nhà, sức chứa và đặt lịch sử dụng phòng học.
- **Báo cáo sự cố:** Báo cáo các sự cố về cơ sở vật chất (hỏng máy chiếu, điều hòa, v.v.) trực tiếp từ ứng dụng để phòng quản trị nắm bắt.

### Dành cho Quản trị viên (Admin)
- **Dashboard:** Thống kê tổng quan về số lượng phòng, lịch đặt, tài khoản và các báo cáo sự cố.
- **Quản lý phòng học:** Thêm, sửa, xóa thông tin phòng học và tòa nhà.
- **Quản lý sự cố:** 
  - Xem danh sách sự cố sinh viên báo cáo.
  - Phân loại trạng thái: `Chờ xử lý` (pending) -> `Đang bảo trì` (processing) -> `Đã giải quyết` (resolved).
  - Tự động khóa phòng học (chuyển trạng thái sang "Đang bảo trì") khi tiếp nhận sự cố và tự động mở lại phòng khi hoàn tất sửa chữa.
- **Quản lý tài khoản:** Khóa/mở khóa tài khoản của người dùng vi phạm.

---

## 🛠 Công nghệ & Kiến trúc sử dụng

- **Ngôn ngữ:** Java
- **UI Toolkit:** Android XML, Material Design Components (Material 3).
- **Kiến trúc điều hướng:** 
  - *Student:* Navigation Component (Single Activity Architecture với Fragment).
  - *Admin:* Activity-based Navigation.
- **Backend & Database:** [Supabase](https://supabase.com/) (PostgreSQL).
- **Mạng (Networking):** Retrofit2 + Gson (Xử lý HTTP requests tới Supabase REST API).
- **Khác:** Glide (tải ảnh - nếu có), SharedPreferences (quản lý session/token đăng nhập).

---

## 📂 Cấu trúc thư mục mã nguồn

Mã nguồn chính nằm tại: `app/src/main/java/com/example/phonghochaui/`

- **`data/`**: Quản lý dữ liệu và API.
  - `model/`: Chứa các class Object Data (VD: `Classroom`, `IncidentReport`, `User`...)
  - `remote/`: Chứa cấu hình Supabase, giao diện `SupabaseApiService` (Retrofit) và `SessionManager`.
- **`adapter/`**: Các RecyclerView Adapter để hiển thị danh sách (VD: `IncidentAdapter`, `RoomAdapter`).
- **`ui/`**: Chứa các Fragment của Sinh viên (VD: `StudentHomeFragment`, `ReportIncidentFragment`).
- **`Activities`**: Các màn hình Activity chính (VD: `LoginActivity`, `AdminHomeActivity`, `AdminIncidentsActivity`).

---

## ⚙️ Hướng dẫn cài đặt và chạy dự án

### Yêu cầu hệ thống
- **Android Studio:** Phiên bản Iguana hoặc mới nhất.
- **JDK:** Java 11 hoặc 17.
- **SDK:** Tối thiểu API 24 (Android 7.0), mục tiêu API 34.

### Các bước cài đặt
1. **Mở dự án:** Khởi động Android Studio -> Chọn *Open* -> Trỏ tới thư mục `Phonghochaui`.
2. **Cập nhật Gradle:** Chờ Android Studio đồng bộ hóa (Sync) các thư viện trong `build.gradle`.
3. **Cấu hình Supabase:** 
   - Đảm bảo trong mã nguồn (hoặc qua biến môi trường) đã cung cấp `SUPABASE_URL` và `SUPABASE_ANON_KEY`.
4. **Chạy ứng dụng:**
   - Kết nối điện thoại thật hoặc mở máy ảo (AVD).
   - Nhấn nút **Run** (mũi tên màu xanh) hoặc dùng phím tắt `Shift + F10`.

---

## 💡 Lưu ý về cơ sở dữ liệu (Supabase)

Để tính năng **Báo cáo sự cố** hoạt động chính xác, đảm bảo bảng `incident_reports` trong Supabase được cấu hình như sau:
- Cột `id` phải là `GENERATED ALWAYS AS IDENTITY`.
- Cột `status` phải có ràng buộc (Check constraint) cho phép các giá trị: `'pending'`, `'processing'`, `'resolved'`.

---

*Dự án được xây dựng và tối ưu nhằm mang lại trải nghiệm tiện lợi, chuyên nghiệp nhất cho công tác quản lý phòng học tại trường Đại học.*
