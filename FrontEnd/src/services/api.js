import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Student API
export const studentAPI = {
  getAll: () => api.get('/sinhvien'),
  getByRfid: (rfid) => api.get(`/sinhvien/${rfid}`),
  search: (keyword) => api.get(`/sinhvien/search?keyword=${keyword}`),
  create: (student) => api.post('/sinhvien', student),
  update: (rfid, student) => api.put(`/sinhvien/${rfid}`, student),
  delete: (rfid) => api.delete(`/sinhvien/${rfid}`),
  checkExists: (rfid) => api.get(`/sinhvien/exists/${rfid}`),
};

// Attendance API
export const attendanceAPI = {
  getAll: () => api.get('/attendance'),
  getToday: () => api.get('/attendance/today'),
  getByFilters: (ngay, ca, maSinhVien) => {
    const params = new URLSearchParams();
    if (ngay) params.append('ngay', ngay);
    if (ca) params.append('ca', ca);
    if (maSinhVien) params.append('maSinhVien', maSinhVien);
    return api.get(`/attendance/filter?${params.toString()}`);
  },
  getByStudent: (maSinhVien) => api.get(`/attendance/student/${maSinhVien}`),
  processRfid: (rfid) => api.post('/attendance/rfid', { rfid }),
  getUnprocessedRfids: () => api.get('/attendance/unprocessed-rfids'),
  markProcessed: (id) => api.put(`/attendance/mark-processed/${id}`),
};

export default api;
