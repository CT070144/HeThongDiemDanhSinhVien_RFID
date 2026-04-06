from flask import Flask, request, jsonify
import face_recognition
import numpy as np
import cv2
import json

app = Flask(__name__)


# ==========================================
# API 1 — ENCODE & TỔNG HỢP (ENROLLMENT)
# Dùng để đăng ký nhân viên mới (1 hoặc nhiều ảnh)
# ==========================================
@app.route("/encode", methods=["POST"])
def encode_face():
    files = request.files.getlist("files")

    if not files:
        return jsonify({"error": "Không có file nào được gửi lên"}), 400

    all_encodings = []

    for file in files:
        if not file.filename.lower().endswith((".png", ".jpg", ".jpeg")):
            continue

        # 1. Đọc ảnh trực tiếp từ memory buffer (nhanh hơn, không tốn dung lượng ổ cứng)
        file_bytes = np.frombuffer(file.read(), np.uint8)
        img_bgr = cv2.imdecode(file_bytes, cv2.IMREAD_COLOR)

        if img_bgr is None:
            continue

        # 2. Chuyển hệ màu từ BGR (OpenCV) sang RGB (chuẩn của thư viện face_recognition)
        img_rgb = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)

        # 3. TỐI ƯU HIỆU NĂNG: Dò tìm vị trí khuôn mặt TRƯỚC
        face_locations = face_recognition.face_locations(img_rgb)

        if len(face_locations) == 0:
            continue

        # 4. Trích xuất vector dựa trên tọa độ đã tìm được
        encs = face_recognition.face_encodings(img_rgb, [face_locations[0]])

        if len(encs) > 0:
            all_encodings.append(encs[0].tolist())

    if len(all_encodings) == 0:
        return jsonify({"error": "Không tìm thấy khuôn mặt hợp lệ trong các file gửi lên"}), 400

    # 5. TỔNG HỢP VECTOR (Average Pooling)
    enc_arr = np.array(all_encodings, dtype=np.float32)
    aggregated = np.mean(enc_arr, axis=0).tolist()

    return jsonify({
        "encoding": aggregated,  # Vector 128 chiều tối ưu nhất (Dùng để LƯU DATABASE)
        "dimension": len(aggregated),
        "count": len(all_encodings)
    })


# ==========================================
# API 2 — COMPARE (CHẤM CÔNG)
# Dùng để so khớp ảnh camera gửi lên với DB
# ==========================================
@app.route("/compare", methods=["POST"])
def compare_face():
    if "file" not in request.files:
        return jsonify({"error": "Thiếu file ảnh chấm công"}), 400

    # SỬA: Đổi từ "encodings" sang "encoding"
    if "encoding" not in request.form:
        return jsonify({"error": "Thiếu dữ liệu vector (encoding) từ Database"}), 400

    file = request.files["file"]

    try:
        # 1. Parse chuỗi JSON thành List Python (chỉ là 1 mảng 128 con số)
        # SỬA: Đọc đúng key "encoding"
        encoding_data = json.loads(request.form["encoding"])

        # 2. Hàm face_distance luôn yêu cầu đối số đầu tiên phải là mảng 2 chiều (một list chứa các vector)
        # Vì ta chỉ truyền lên 1 vector tổng hợp, ta sẽ bọc nó vào một list: [np.array(...)]
        known_encodings = [np.array(encoding_data)]

    except Exception as e:
        return jsonify({"error": f"Định dạng vector Database không hợp lệ: {str(e)}"}), 400

    # 3. Đọc ảnh chấm công
    file_bytes = np.frombuffer(file.read(), np.uint8)
    img_bgr = cv2.imdecode(file_bytes, cv2.IMREAD_COLOR)

    if img_bgr is None:
        return jsonify({"match": False, "error": "File ảnh bị lỗi hoặc không đọc được"}), 400

    img_rgb = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)

    # 4. Tìm và trích xuất khuôn mặt nhân viên
    face_locations = face_recognition.face_locations(img_rgb)
    if len(face_locations) == 0:
        return jsonify({"match": False, "error": "Không tìm thấy khuôn mặt trước camera"}), 400

    encodings = face_recognition.face_encodings(img_rgb, [face_locations[0]])
    if len(encodings) == 0:
        return jsonify({"match": False, "error": "Không trích xuất được đặc trưng khuôn mặt"}), 400

    unknown_encoding = encodings[0]

    # 5. So sánh (Euclidean Distance)
    distances = face_recognition.face_distance(known_encodings, unknown_encoding)
    min_distance = float(np.min(distances))

    # 6. Ngưỡng quyết định (Threshold)
    threshold = 0.38
    match = bool(min_distance < threshold)  # Ép kiểu bool tĩnh để JSON parse an toàn

    return jsonify({
        "match": match,
        "distance": round(min_distance, 4),
        "threshold": threshold
    })


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)