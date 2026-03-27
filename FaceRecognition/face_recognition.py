from flask import Flask, request, jsonify
import face_recognition
import numpy as np
import json

app = Flask(__name__)


# =============================
# API 1 — ENCODE MULTIPLE IMAGES
# =============================
@app.route("/encode", methods=["POST"])
def encode_face():
    # Expect field name: "files" (can upload multiple)
    files = request.files.getlist("files")

    if not files:
        return jsonify({"error": "No files"}), 400

    all_encodings = []

    for file in files:
        if not file.filename.lower().endswith((".png", ".jpg", ".jpeg")):
            continue

        img = face_recognition.load_image_file(file)
        encodings = face_recognition.face_encodings(img)

        # Keep only first face per image (consistent with previous logic)
        if len(encodings) > 0:
            all_encodings.append(encodings[0].tolist())

    if len(all_encodings) == 0:
        return jsonify({"error": "No face found"}), 400

    return jsonify({
        "encodings": all_encodings
    })


# =============================
# API 2 — COMPARE MULTIPLE ENCODINGS
# =============================
@app.route("/compare", methods=["POST"])
def compare_face():
    # file: input image
    if "file" not in request.files:
        return jsonify({"error": "No file"}), 400

    # encodings: JSON string of embeddings (list[list[float]])
    if "encodings" not in request.form:
        return jsonify({"error": "No encodings"}), 400

    file = request.files["file"]
    encoding_str = request.form["encodings"]

    try:
        encoding_list = json.loads(encoding_str)
    except Exception:
        return jsonify({"error": "Invalid encodings JSON"}), 400

    # Backward compatible: nếu dữ liệu cũ là list[float] thì bọc thành list[list[float]].
    if isinstance(encoding_list, list) and len(encoding_list) > 0 and isinstance(encoding_list[0], (int, float)):
        encoding_list = [encoding_list]

    known_encodings = np.array(encoding_list)

    img = face_recognition.load_image_file(file)
    encodings = face_recognition.face_encodings(img)

    if len(encodings) == 0:
        return jsonify({
            "match": False,
            "error": "No face found"
        })

    unknown_encoding = encodings[0]

    # Use face distance and a threshold
    distances = face_recognition.face_distance(known_encodings, unknown_encoding)
    min_distance = float(np.min(distances))

    threshold = 0.45
    match = min_distance < threshold

    return jsonify({
        "match": match,
        "distance": min_distance
    })


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)