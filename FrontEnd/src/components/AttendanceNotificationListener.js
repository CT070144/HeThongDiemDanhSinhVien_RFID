import React, { useEffect, useRef } from 'react';
import { toast } from 'react-toastify';
import io from 'socket.io-client';
import { useAuth } from '../contexts/AuthContext';

const AttendanceNotificationListener = () => {
  const socketRef = useRef(null);
  const notifiedIdsRef = useRef(new Set());
  const notifiedRfidsRef = useRef(new Set());
  const { isAuthenticated } = useAuth();

  useEffect(() => {
    // Chỉ kết nối WebSocket khi đã đăng nhập
    if (!isAuthenticated()) {
      return;
    }

    // Khởi tạo kết nối WebSocket một lần
    if (!socketRef.current) {
      console.log("Initializing global attendance notification listener...");

      const connectionUrl = "http://localhost:8099";

      socketRef.current = io(connectionUrl, {
        path: "/socket.io",
        query: { token: localStorage.getItem('token') },
        transports: ["websocket", "polling"],
        reconnection: true,
        reconnectionAttempts: 10,
        reconnectionDelay: 1000,
      });

      socketRef.current.on("connect", () => {
        console.log("Global attendance notification socket connected");
      });

      socketRef.current.on("disconnect", () => {
        console.log("Global attendance notification socket disconnected");
      });

      socketRef.current.on("connect_error", (err) => {
        console.error("Global attendance notification socket connect_error:", err.message || err);
      });

      // Lắng nghe event điểm danh mới
      socketRef.current.on("update-attendance", (result) => {
        console.log("New attendance record received:", result);
        
        try {
          // Parse result nếu là string
          const attendanceRecord = typeof result === 'string' ? JSON.parse(result) : result;
          
          // Kiểm tra xem đây có phải là bản ghi mới không
          if (!attendanceRecord.id) {
            return;
          }

          // Tránh thông báo trùng lặp - kiểm tra ID và timestamp
          const recordKey = `${attendanceRecord.id}_${attendanceRecord.createdAt || Date.now()}`;
          if (notifiedIdsRef.current.has(recordKey)) {
            console.log("Duplicate notification ignored:", recordKey);
            return;
          }
          notifiedIdsRef.current.add(recordKey);

          // Giới hạn số lượng ID đã thông báo để tránh memory leak
          if (notifiedIdsRef.current.size > 1000) {
            const firstKey = Array.from(notifiedIdsRef.current)[0];
            notifiedIdsRef.current.delete(firstKey);
          }

          // Kiểm tra xem có thông tin sinh viên không
          const hasStudentInfo = attendanceRecord.maSinhVien && 
                                 attendanceRecord.tenSinhVien && 
                                 attendanceRecord.tenSinhVien.trim() !== '';

          if (hasStudentInfo) {
            // Có thông tin sinh viên -> Phiếu điểm danh mới
            const studentName = attendanceRecord.tenSinhVien;
            const studentCode = attendanceRecord.maSinhVien;
            const room = attendanceRecord.phongHoc || 'N/A';
            const ca = attendanceRecord.ca ? `Ca ${attendanceRecord.ca}` : '';
            
            toast.success(
              `${studentName} (${studentCode}) đã điểm danh - ${room} ${ca}`,
              {
                position: "top-right",
                autoClose: 5000,
                hideProgressBar: false,
                closeOnClick: true,
                pauseOnHover: true,
                draggable: true,
                toastId: `attendance-${attendanceRecord.id}`, // Tránh duplicate toast
              }
            );
          }
        } catch (error) {
          console.error("Error processing attendance notification:", error);
        }
      });

      // Lắng nghe event RFID không hợp lệ (chưa đăng ký)
      socketRef.current.on("invalid-rfid", (result) => {
        console.log("Invalid RFID received:", result);
        
        try {
          // Parse result nếu là string (RFID string)
          const rfid = typeof result === 'string' ? JSON.parse(result) : result;
          const rfidString = typeof rfid === 'string' ? rfid : (rfid.rfid || rfid);
          
          if (!rfidString || rfidString.trim() === '') {
            return;
          }

          // Tránh thông báo trùng lặp cho cùng một RFID trong 5 giây
          const rfidKey = `invalid-${rfidString}`;
          const now = Date.now();
          
          // Kiểm tra xem RFID này đã được thông báo gần đây chưa (trong 5 giây)
          if (notifiedRfidsRef.current.has(rfidKey)) {
            console.log("Duplicate invalid RFID notification ignored:", rfidString);
            return;
          }
          
          notifiedRfidsRef.current.add(rfidKey);
          
          // Xóa khỏi Set sau 5 giây để có thể thông báo lại nếu cần
          setTimeout(() => {
            notifiedRfidsRef.current.delete(rfidKey);
          }, 5000);

          toast.warning(
            `Phát hiện RFID mới: ${rfidString}`,
            {
              position: "top-right",
              autoClose: 7000,
              hideProgressBar: false,
              closeOnClick: true,
              pauseOnHover: true,
              draggable: true,
              toastId: `invalid-rfid-${rfidString}`, // Tránh duplicate toast
            }
          );
        } catch (error) {
          console.error("Error processing invalid RFID notification:", error);
        }
      });

      // Lắng nghe event khuôn mặt không khớp
      socketRef.current.on("invalid-face", (result) => {
        console.log("Invalid face received:", result);

        try {
          const payload = typeof result === 'string' ? JSON.parse(result) : result;
          const uid = typeof payload === 'string' ? payload : (payload?.uid || payload?.rfid || payload);
          const uidString = uid && `${uid}`.trim() !== '' ? `${uid}`.trim() : '';

          if (!uidString) return;

          toast.warning(`Chấm công khuôn mặt không khớp: ${uidString}`, {
            position: "top-right",
            autoClose: 7000,
            hideProgressBar: false,
            closeOnClick: true,
            pauseOnHover: true,
            draggable: true,
            toastId: `invalid-face-${uidString}`,
          });
        } catch (error) {
          console.error("Error processing invalid-face notification:", error);
        }
      });
    }

    // Cleanup function - disconnect socket khi component unmount
    return () => {
      if (socketRef.current) {
        console.log("Disconnecting global attendance notification socket...");
        socketRef.current.disconnect();
        socketRef.current = null;
        notifiedIdsRef.current.clear();
        notifiedRfidsRef.current.clear();
      }
    };
  }, [isAuthenticated]);

  // Component này không render gì, chỉ lắng nghe WebSocket
  return null;
};

export default AttendanceNotificationListener;

