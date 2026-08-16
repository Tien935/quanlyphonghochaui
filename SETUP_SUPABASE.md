# Thiết lập Supabase cho HaUI Classroom

## 1. Lấy thông tin dự án

Trong Supabase Dashboard, mở **Project Settings > API Keys** và lấy:

- Project URL.
- Publishable key (`sb_publishable_...`) hoặc legacy `anon` key.

Không sử dụng `secret key` hoặc `service_role` trong ứng dụng Android.

## 2. Cập nhật `local.properties`

Giữ nguyên dòng `sdk.dir` do Android Studio tạo và thêm:

```properties
SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co/
SUPABASE_PUBLISHABLE_KEY=sb_publishable_xxxxxxxxxxxxxxxxxxxx
```

Không đặt giá trị trong dấu ngoặc kép. Sau khi sửa, chọn **Sync Project with
Gradle Files** để Android Studio tạo lại `BuildConfig`.

## 3. Kiểm tra kết nối

1. Chạy ứng dụng.
2. Nhấn **Kiểm tra kết nối**.
3. Ứng dụng gọi `GET /rest/v1/campuses?select=*&limit=1`.
4. HTTP 200 nghĩa là URL, key và REST API đã kết nối được.

Nếu HTTP 200 nhưng số bản ghi bằng 0, hãy kiểm tra dữ liệu bảng `campuses` và
chính sách RLS dành cho vai trò `anon`. Sau khi bổ sung đăng nhập, interceptor
sẽ tự động gửi access token của người dùng để RLS dùng vai trò `authenticated`.

## 4. Cho phép người dùng đọc vai trò của chính mình

Màn hình đăng nhập truy vấn `public.profiles` sau khi Supabase Auth trả về token.
Chạy đoạn SQL sau nếu project chưa có quyền/policy tương đương:

```sql
grant select on table public.profiles to authenticated;

alter table public.profiles enable row level security;

drop policy if exists "user_read_own_profile" on public.profiles;

create policy "user_read_own_profile"
on public.profiles
for select
to authenticated
using ((select auth.uid()) = id);
```

Policy này chỉ cho người dùng đã đăng nhập đọc hồ sơ có `id` trùng với UUID của
chính họ. Cột `role` phải có giá trị `admin` hoặc `student`.

## 5. Luồng đăng nhập

1. `POST /auth/v1/token?grant_type=password` xác thực email/mật khẩu.
2. `SessionManager` lưu access token, refresh token và UUID.
3. `GET /rest/v1/profiles?id=eq.<uuid>&select=role` lấy vai trò.
4. Ứng dụng chấp nhận hai vai trò `admin` và `student`.

## 6. Các lớp đã chuẩn bị

- `SupabaseConfig`: đọc và kiểm tra URL/key.
- `SessionManager`: lưu token và thông tin phiên đăng nhập.
- `SupabaseInterceptor`: tự động gắn `apikey` và `Authorization`.
- `RetrofitClient`: tạo một Retrofit/OkHttp client dùng chung.
- `SupabaseApiService`: khai báo endpoint REST của Supabase.
- `LoginActivity`: kiểm tra dữ liệu, đăng nhập và lấy vai trò người dùng.
