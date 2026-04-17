import React, { useEffect, useRef, useState } from 'react';
import { toast } from 'react-toastify';
import io from 'socket.io-client';
import { useAuth } from '../contexts/AuthContext';
import { attendanceAPI } from '../services/api';

const AttendanceNotificationListener = () => {
  const socketRef = useRef(null);
  const notifiedIdsRef = useRef(new Set());
  const notifiedRfidsRef = useRef(new Set());
  const { isAuthenticated } = useAuth();
  const [showCapture, setShowCapture] = useState(false);
  const [capturePreviewUrl, setCapturePreviewUrl] = useState(null);
  // Chống gửi trùng xác thực khuôn mặt (UID -> lastSentAt ms)
  const faceCaptureDedupRef = useRef(new Map());

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

      // Yêu cầu web mở camera và chụp ảnh xác thực
      socketRef.current.on("request-face-capture", async (result) => {
        try {
          const payload = typeof result === 'string' ? JSON.parse(result) : result;
          const uid = payload?.rfid || payload?.uid || payload;
          const maThietBi = payload?.maThietBi || undefined;
          const rfid = payload?.rfid || undefined;
          if (!uid || `${uid}`.trim() === '') return;

          // Nếu event bị broadcast/reconnect nhiều lần, tránh chụp+gửi 2 lần khiến hệ thống tự hiểu là "điểm danh ra"
          const uidKey = `${uid}`.trim();
          const nowMs = Date.now();
          const last = faceCaptureDedupRef.current.get(uidKey) || 0;
          if (nowMs - last < 5000) {
            console.log("Duplicate request-face-capture ignored for uid:", uidKey);
            return;
          }
          faceCaptureDedupRef.current.set(uidKey, nowMs);

          toast.info(`Yêu cầu xác thực khuôn mặt cho UID: ${uid}`, { autoClose: 3000 });

          // Mở camera và chụp 1 frame
          const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' } });
          const video = document.createElement('video');
          video.srcObject = stream;
          await video.play();
          // Đợi video ready
          await new Promise((res) => setTimeout(res, 400));
          const w = video.videoWidth || 640;
          const h = video.videoHeight || 480;
          const canvas = document.createElement('canvas');
          canvas.width = w; canvas.height = h;
          const ctx = canvas.getContext('2d');
          ctx.drawImage(video, 0, 0, w, h);
          // Dừng camera
          stream.getTracks().forEach(t => t.stop());
          // Canvas → Blob
          const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.85));
          if (!blob) {
            throw new Error("Không tạo được ảnh từ camera (blob=null)");
          }
          // Hiển thị overlay với ảnh đã chụp
          const previewUrl = URL.createObjectURL(blob);
          setCapturePreviewUrl(previewUrl);
          setShowCapture(true);

          // Gửi lên backend
          try {
            await attendanceAPI.submitFace(`${uid}`, blob, maThietBi);
          } catch (apiErr) {
            // Backend có thể trả về 400 kèm body { status, name } (ví dụ ngoài giờ làm)
            const data = apiErr?.response?.data;
            const backendMessage =
              (data && typeof data === 'object' && (data.name || data.message)) ? (data.name || data.message) : null;

            if (backendMessage) {
              toast.warning(`${backendMessage}`, { autoClose: 5000 });
            } else {
              toast.error("Gửi ảnh xác thực thất bại. Vui lòng thử lại.", { autoClose: 5000 });
            }
          }
          // Ẩn overlay sau khi nhận được response
          setShowCapture(false);
          if (previewUrl) {
            URL.revokeObjectURL(previewUrl);
            setCapturePreviewUrl(null);
          }
         
        } catch (error) {
          console.log(error);
          console.error("request-face-capture error:", error.message);
          toast.error("Không thể chụp/gửi ảnh xác thực. Vui lòng kiểm tra quyền camera.", { autoClose: 5000 });
          // Đảm bảo đóng overlay nếu đang mở
          setShowCapture(false);
          if (capturePreviewUrl) {
            URL.revokeObjectURL(capturePreviewUrl);
            setCapturePreviewUrl(null);
          }
        }
      });

      // Lắng nghe event chấm công mới
      socketRef.current.on("update-attendance", (result) => {
        console.log("New attendance record received:", result);
        
        try {
          // Parse result nếu là string
          const attendanceRecord = typeof result === 'string' ? JSON.parse(result) : result;
          console.log("Attendance record:", attendanceRecord);
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
            // Có thông tin sinh viên -> Phiếu chấm công mới
            const studentName = attendanceRecord.tenSinhVien;
            const studentCode = attendanceRecord.maSinhVien;
            const room = attendanceRecord.phongHoc || 'N/A';
            const ca = attendanceRecord.ca ? `Ca ${attendanceRecord.ca}` : '';
            
            toast.success(
              `${studentName} (${studentCode}) đã chấm công - ${room} ${ca}`,
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
          const viTri = typeof rfid === 'string' ? rfid : (rfid.viTri || rfid);
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
            `Phát hiện RFID lạ ${rfidString} quét tại ${viTri}`,
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

  // Dọn dẹp blob URL khi thay đổi hoặc unmount
  useEffect(() => {
    return () => {
      if (capturePreviewUrl) {
        URL.revokeObjectURL(capturePreviewUrl);
      }
    };
  }, [capturePreviewUrl]);

  // Hiển thị overlay xem trước ảnh đã chụp trong lúc gửi
  return (
    <>
      {showCapture && capturePreviewUrl && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: 'rgba(0,0,0,0.6)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999,
          }}
        >
          <div
            style={{
              background: '#111',
              padding: '12px',
              borderRadius: 8,
              boxShadow: '0 8px 24px rgba(0,0,0,0.5)',
              maxWidth: '90vw',
              maxHeight: '90vh',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 8,
            }}
          >
            <img
              src={capturePreviewUrl}
              alt="Face capture preview"
              style={{
                maxWidth: '80vw',
                maxHeight: '70vh',
                objectFit: 'contain',
                borderRadius: 6,
              }}
            />
            <div style={{ color: '#fff', fontSize: 14, opacity: 0.9 }}>
              Đang gửi xác thực khuôn mặt...
            </div>
          </div>
        </div>
      )}
      {null}
    </>
  );
};

export default AttendanceNotificationListener;

