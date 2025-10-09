import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor để thêm JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor để xử lý lỗi authentication
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token hết hạn hoặc không hợp lệ
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

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
  getByFilters: (ngay, ca, maSinhVien, phongHoc) => {
    const params = new URLSearchParams();
    if (ngay) params.append('ngay', ngay);
    if (ca) params.append('ca', ca);
    if (maSinhVien) params.append('maSinhVien', maSinhVien);
    if (phongHoc) params.append('phongHoc', phongHoc);
    return api.get(`/attendance/filter?${params.toString()}`);
  },
  getByStudent: (maSinhVien) => api.get(`/attendance/student/${maSinhVien}`),
  processRfid: (rfid, maThietBi) => api.post('/attendance/rfid', { rfid, maThietBi }),
  getUnprocessedRfids: () => api.get('/attendance/unprocessed-rfids'),
  markProcessed: (id) => api.put(`/attendance/mark-processed/${id}`),
};

// Device API
export const deviceAPI = {
  getAll: () => api.get('/thietbi'),
  getOne: (maThietBi) => api.get(`/thietbi/${maThietBi}`),
  create: (data) => api.post('/thietbi', data),
  update: (maThietBi, data) => api.put(`/thietbi/${maThietBi}`, data),
  delete: (maThietBi) => api.delete(`/thietbi/${maThietBi}`),
};

export default api;
