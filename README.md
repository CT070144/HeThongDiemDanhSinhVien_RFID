HƯỚNG DẪN CÀI ĐẶT VÀ SỬ DỤNG HỆ THỐNG CHẤM CÔNG BẰNG KHUÔN MẶT VÀ RFID SỬ DỤNG HỆ ĐIỀU HÀNH FREERTOS
1. Tổng quan hệ thống
Hệ thống chấm công RFID được cấu thành từ 4 phần chính:
•	BackEnd: Là ứng dụng Spring Boot đảm nhiệm việc xử lý API, xác thực, chấm công và kết nối với cơ sở dữ liệu MySQL.
•	FrontEnd: Là ứng dụng ReactJS cung cấp giao diện quản trị và theo dõi chấm công.
•	FaceRecognition: Là dịch vụ Python Flask dùng để mã hóa và so khớp khuôn mặt.
•	Embedded: Là chương trình trên ESP32 dùng để đọc thẻ RFID, gửi dữ liệu lên Backend và hiển thị trạng thái.
Kiến trúc hoạt động tổng quát diễn ra theo 4 bước:
1.	ESP32 tiến hành đọc thẻ RFID và gửi dữ liệu rfid + maThietBi lên máy chủ Backend.
2.	Backend tiếp nhận, xác định thông tin nhân viên, ca làm và trạng thái chấm công.
3.	Hệ thống yêu cầu xác minh khuôn mặt, Backend sẽ giao tiếp với dịch vụ FaceRecognition.
4.	Cuối cùng, FrontEnd hiển thị thông tin, bảng điều khiển và lịch sử chấm công theo thời gian thực.
2. Yêu cầu môi trường
Phần mềm cần thiết:
•	Cần có Docker Desktop và docker compose nếu muốn khởi chạy nhanh toàn bộ hệ thống.
•	Cần có Java 17+ để chạy Backend thủ công.
•	Cần có Node.js 16+ và npm để chạy FrontEnd thủ công.
•	Cần có Python 3.10+ để chạy dịch vụ FaceRecognition.
•	Cần có Arduino IDE để nạp mã nguồn cho ESP32.
•	Cần có Git để sao chép (clone) và cập nhật mã nguồn.
Phần cứng cần thiết:
•	Các linh kiện chính bao gồm: ESP32 Dev Module, RC522 RFID, LCD1602 I2C, Buzzer
•	Phụ kiện đi kèm gồm có: Dây cắm (jump), breadboard, cáp USB nạp ESP32, thẻ RFID để thử nghiệm.
•	Cần trang bị thêm Webcam/camera để sử dụng chức năng nhận diện khuôn mặt.
3. Cấu trúc thư mục liên quan
Các thư mục và tệp tin quan trọng trong dự án bao gồm:
•	Thư mục BackEnd.
•	Thư mục FrontEnd.
•	Thư mục FaceRecognition.
•	Tệp mã nguồn nhúng Embedded/FaceRecognize+RFID.ino.
•	Tệp cơ sở dữ liệu ScriptDatabase/rfid_attendance_system_backup.sql.
•	Tệp cấu hình Docker docker-compose.yml.
4. Hướng dẫn triển khai nhanh bằng Docker Compose
Đây là phương pháp được ưu tiên nếu người dùng muốn khởi chạy nhanh Backend, FrontEnd và MySQL.
1.	Mở terminal tại thư mục gốc của dự án (ví dụ: cd D:\KMALearn\RFID_Project).
2.	Khởi động hệ thống bằng lệnh: docker compose up --build.
Thông tin các container được cấu hình sẵn:
•	mysql: Sử dụng MySQL 8.0, ánh xạ cổng 3307:3306.
•	backend: Chạy Spring Boot, ánh xạ cổng 8080:8080 và socket 8099:8099.
•	frontend: Chạy ReactJS, ánh xạ cổng 3000:3000.
•	face-recognize: Chạy Python, ánh xạ cổng 5000:5000
Thông tin Cơ sở dữ liệu:
•	Tệp docker-compose.yml sẽ tự động nạp script từ ScriptDatabase/rfid_attendance_system_backup.sql.
•	Tên cơ sở dữ liệu được tạo mặc định là rfid_attendance_system.
•	Tài khoản MySQL: User là username, Password là 1, Root password là 1.
5. Cài đặt và vận hành thủ công các dịch vụ
5.1. Cài đặt Backend thủ công
1.	Chuẩn bị Database: Có thể dùng Docker Compose để tạo MySQL hoặc tự tạo và import tệp SQL bằng lệnh mysql -u root -p < ScriptDatabase/rfid_attendance_system_backup.sql.
2.	Cấu hình: Cần tạo tệp application.properties trong thư mục BackEnd/src/main/resources để cấu hình chuỗi kết nối MySQL (spring.datasource.url, username, password), cổng server (8080), khóa JWT (jwt.secret), và các đường dẫn kết nối đến dịch vụ Python FaceRecognition.
3.	Khởi chạy: Di chuyển vào thư mục BackEnd và chạy lệnh .\mvnw spring-boot:run hoặc mvn spring-boot:run.
5.2. Cài đặt FrontEnd thủ công
1.	Di chuyển vào thư mục FrontEnd và chạy lệnh npm install để cài đặt các thư viện phụ thuộc.
2.	Chạy lệnh npm start để khởi động giao diện.
3.	Có thể tạo tệp .env để cấu hình biến môi trường REACT_APP_API_URL (mặc định là http://localhost:8080/api) và REACT_APP_SOCKET_URL (mặc định là http://localhost:8099).
5.3. Cài đặt dịch vụ FaceRecognition
1.	Tạo môi trường ảo Python trong thư mục FaceRecognition bằng lệnh python -m venv .venv và kích hoạt nó.
2.	Cài đặt các thư viện cần thiết bằng lệnh pip install flask face-recognition numpy opencv-python.
3.	Chạy dịch vụ bằng lệnh python face_recognition.py (dịch vụ sẽ chạy tại http://localhost:5000).
6. Cài đặt phần cứng ESP32
Yêu cầu thư viện trên Arduino IDE:
•	Cần cài đặt các thư viện: MFRC522, ArduinoJson, LiquidCrystal_I2C, và WiFiManager.
•	Cần đảm bảo chọn đúng Board là ESP32 Dev Module và cổng COM tương ứng.
Sơ đồ kết nối phần cứng:
•	RC522 sang ESP32: Chân VCC nối VIN hoặc 3.3V, GND nối GND, RST nối GPIO 4, SS nối GPIO 5, MOSI nối GPIO 23, MISO nối GPIO 19, SCK nối GPIO 18.
•	LCD1602 I2C sang ESP32: Chân VCC nối 3.3V hoặc 5V, GND nối GND, SDA nối GPIO 21, SCL nối GPIO 22.
•	Buzzer sang ESP32: Chân dương (+) nối GPIO 15, chân âm (-) nối GND.
•	Nút nhấn sang ESP32: Một đầu nối GPIO 0, đầu còn lại nối GND.
Cấu hình và Nạp mã nguồn:
•	Biến serverURL trong code cần trỏ đúng địa chỉ IP LAN của máy tính chạy Backend
•	Lần đầu khởi động, thiết bị sẽ phát WiFi có tên RFID-Device (mật khẩu: 12345678) để người dùng truy cập cổng thông tin cấu hình WiFi, Device ID và Server URL.
7. Trình tự khởi động và Sử dụng cơ bản
Trình tự khởi động khuyến nghị:
1.	Khởi động MySQL.
2.	Khởi động dịch vụ FaceRecognition.
3.	Khởi động BackEnd.
4.	Khởi động FrontEnd.
5.	Nạp và chạy thiết bị ESP32.
Quy trình sử dụng cơ bản:
1.	Đăng nhập vào FrontEnd (http://localhost:3000) bằng tài khoản quản trị.
2.	Thêm thiết bị và thông tin nhân viên vào hệ thống.
3.	Gán mã RFID cho nhân viên.
4.	Tải ảnh lên để tạo faceid nhận diện khuôn mặt.
5.	Khởi động ESP32
6.	Khi nhân viên quẹt thẻ, hệ thống sẽ ghi nhận chấm công và cập nhật thời gian thực lên giao diện Dashboard.
8. Kiểm tra và khắc phục sự cố
•	FrontEnd không truy cập được: Kiểm tra trạng thái tiến trình npm start hoặc container frontend, đảm bảo cổng 3000 đang hoạt động.
•	FrontEnd không gọi được API: Xác minh Backend đang chạy tại cổng 8080 và kiểm tra lại các URL trong biến môi trường.
•	ESP32 quẹt thẻ nhưng không gửi dữ liệu: Kiểm tra cấu hình serverURL, đảm bảo ESP32 và máy chủ chung mạng LAN, và theo dõi log trên Serial Monitor.
•	Backend báo lỗi FaceRecognition: Đảm bảo dịch vụ Python đang chạy tại http://localhost:5000 và hình ảnh gửi lên là hợp lệ.
•	Backend không kết nối được MySQL: Kiểm tra tài khoản, mật khẩu, tên cơ sở dữ liệu. Nếu dùng Docker Compose, lưu ý MySQL được ánh xạ ra cổng 3307 của máy host.

