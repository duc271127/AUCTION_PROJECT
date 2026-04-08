# Backend - Auction Project

## 1. Giới thiệu
Backend của Auction Project được xây dựng bằng:

- Java 17+
- Spring Boot
- Spring Data JPA
- MySQL
- Maven

Hiện tại backend đã có:

- kết nối MySQL
- module `Task`
- CRUD cơ bản cho `Task`
- validation request
- exception handling
- chuẩn hóa response API

---

## 2. Cách chạy project

### Yêu cầu
- JDK 17 hoặc cao hơn
- MySQL đang chạy
- Maven Wrapper có sẵn trong project

### Tạo database
Mở MySQL và chạy:

```sql
CREATE DATABASE auction_project;
```

### Cấu hình database
Mở file:

```text
src/main/resources/application.properties
```

Điền thông tin:

```properties
spring.application.name=backend
server.port=8081

spring.datasource.url=jdbc:mysql://localhost:3306/auction_project?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### Chạy backend
Trong thư mục `backend`, chạy:

```bash
.\\mvnw.cmd spring-boot:run
```

Sau khi chạy thành công, backend sẽ chạy tại:

```text
http://localhost:8081
```

---

## 3. Cấu trúc source code

```text
src/main/java/com/team/backend
├─ controller
├─ service
├─ repository
├─ entity
├─ dto
├─ config
├─ exception
└─ BackendApplication.java
```

### Ý nghĩa
- `controller`: nhận request từ client
- `service`: xử lý logic
- `repository`: truy cập database
- `entity`: ánh xạ bảng trong database
- `dto`: object request/response
- `config`: cấu hình project
- `exception`: xử lý lỗi chung

---

## 4. API hiện có

### 4.1 Lấy tất cả task
**GET**
```text
/api/tasks
```

### 4.2 Lấy task theo id
**GET**
```text
/api/tasks/{id}
```

### 4.3 Tạo task mới
**POST**
```text
/api/tasks
```

Body mẫu:

```json
{
  "title": "Learn Spring Boot"
}
```

### 4.4 Cập nhật task
**PUT**
```text
/api/tasks/{id}
```

Body mẫu:

```json
{
  "title": "Update backend",
  "status": "DONE"
}
```

### 4.5 Xóa task
**DELETE**
```text
/api/tasks/{id}
```

---

## 5. Format response API

### Thành công
```json
{
  "success": true,
  "message": "Get tasks successfully",
  "data": []
}
```

### Lỗi
```json
{
  "success": false,
  "message": "Task with id 999 not found",
  "data": null
}
```

### Lỗi validation
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "title": "Title must not be blank"
  }
}
```

---

## 6. Test API

Có thể test bằng:
- Postman
- trình duyệt với request GET
- curl trong terminal

Ví dụ:

### GET all tasks
```bash
curl http://localhost:8081/api/tasks
```

### POST create task
```bash
curl -X POST http://localhost:8081/api/tasks -H "Content-Type: application/json" -d "{\"title\":\"Learn Spring Boot\"}"
```

---

## 7. Quy trình phát triển hiện tại

1. Chạy MySQL
2. Tạo database `auction_project`
3. Cấu hình `application.properties`
4. Chạy backend bằng Maven Wrapper
5. Test API bằng Postman hoặc curl
6. Kiểm tra dữ liệu trong MySQL Workbench
7. Commit và push lên branch `feature/hardcore`

---

## 8. Gợi ý commit message

```bash
git add .
git commit -m "docs: add backend setup and task api guide"
git push origin feature/hardcore
```

---

## 9. Ghi chú
- Port hiện tại: `8081`
- Database hiện tại: `auction_project`
- Module đã hoàn thành: `Task`
- Các module tiếp theo dự kiến: `User`, `Auction`, `Bid`
