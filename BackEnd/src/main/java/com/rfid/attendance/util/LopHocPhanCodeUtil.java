package com.rfid.attendance.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LopHocPhanCodeUtil {

    public static String generateMaLopHocPhan(String tenLopHocPhan) {
        if (tenLopHocPhan == null || tenLopHocPhan.isBlank()) {
            return "";
        }

        // --- 1️⃣ Tách phần trong ngoặc (ví dụ: A2002)
        Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(tenLopHocPhan);

        String maTrongNgoac = "";
        if (matcher.find()) {
            maTrongNgoac = matcher.group(1).trim();
        }

        // --- 2️⃣ Lấy phần tên trước dấu ngoặc
        String ten = tenLopHocPhan.split("\\(")[0].trim();

        // --- 3️⃣ Tạo mã viết tắt: lấy ký tự đầu mỗi từ (bỏ số và ký tự '-')
        String[] words = ten.split("[\\s\\-]+");
        StringBuilder maVietTat = new StringBuilder();

        for (String w : words) {
            if (!w.isEmpty() && Character.isLetter(w.charAt(0))) {
                maVietTat.append(Character.toUpperCase(w.charAt(0)));
            }
        }

        // --- 4️⃣ Ghép kết quả
        return maVietTat + "-" + maTrongNgoac;
    }

}
