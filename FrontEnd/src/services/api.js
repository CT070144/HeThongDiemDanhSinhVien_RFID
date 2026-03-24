import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

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
  bulkUpdateRfid: (studentList) => api.post('/sinhvien/bulk-update-rfid', studentList),
};

// Attendance API
export const attendanceAPI = {
  getAll: () => api.get('/attendance'),
  getToday: () => api.get('/attendance/today'),
  getByDateRange: (startDate, endDate) => {
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    const query = params.toString();
    return api.get(`/attendance/range${query ? `?${query}` : ''}`);
  },
  getPaged: (params = {}) => {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && `${value}`.trim() !== '') {
        query.append(key, value);
      }
    });
    return api.get(`/attendance/paged?${query.toString()}`);
  },
  exportExcel: (params = {}) => {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && `${value}`.trim() !== '') {
        query.append(key, value);
      }
    });
    return api.get(`/attendance/export?${query.toString()}`, { responseType: 'blob' });
  },
  getByFilters: (ngay, ca, maSinhVien, phongHoc) => {
    const params = new URLSearchParams();
    if (ngay) params.append('ngay', ngay);
    if (ca) params.append('ca', ca);
    if (maSinhVien) params.append('maSinhVien', maSinhVien);
    if (phongHoc) params.append('phongHoc', phongHoc);
    return api.get(`/attendance/filter?${params.toString()}`);
  },
  getByStudent: (maSinhVien) => api.get(`/attendance/student/${maSinhVien}`),
  getByLopHocPhan: (maLopHocPhan) => api.get(`/attendance/by-lophocphan/${maLopHocPhan}`),
  processRfid: (rfid, maThietBi) => api.post('/attendance/rfid', { rfid, maThietBi }),
  getUnprocessedRfids: () => api.get('/attendance/unprocessed-rfids'),
  markProcessed: (id) => api.put(`/attendance/mark-processed/${id}`),
  syncStudentInfo: () => api.post('/attendance/sync-student-info'),
};

// Device API
export const deviceAPI = {
  getAll: () => api.get('/thietbi'),
  getOne: (maThietBi) => api.get(`/thietbi/${maThietBi}`),
  create: (data) => api.post('/thietbi', data),
  update: (maThietBi, data) => api.put(`/thietbi/${maThietBi}`, data),
  toggleStatus: (maThietBi) => api.patch(`/thietbi/${maThietBi}/toggle-status`),
  activate: (maThietBi) => api.patch(`/thietbi/${maThietBi}/activate`),
  deactivate: (maThietBi) => api.patch(`/thietbi/${maThietBi}/deactivate`),
  delete: (maThietBi) => api.delete(`/thietbi/${maThietBi}`),
  // API Key management
  getApiKeys: (maThietBi) => api.get(`/thietbi/${maThietBi}/api-keys`),
  createApiKey: (maThietBi, data) => api.post(`/thietbi/${maThietBi}/api-keys`, data),
  toggleApiKeyStatus: (id) => api.patch(`/thietbi/api-keys/${id}/toggle`),
  activateApiKey: (id) => api.patch(`/thietbi/api-keys/${id}/activate`),
  deactivateApiKey: (id) => api.patch(`/thietbi/api-keys/${id}/deactivate`),
  revokeApiKey: (id) => api.patch(`/thietbi/api-keys/${id}/revoke`),
  deleteApiKey: (id) => api.delete(`/thietbi/api-keys/${id}`),
};

// Room (PhongHoc) API
export const roomAPI = {
  getAll: (keyword) => api.get(`/phonghoc${keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''}`),
  getPaged: (page, size, keyword) => {
    const params = new URLSearchParams();
    params.append('page', page);
    params.append('size', size);
    if (keyword) params.append('keyword', keyword);
    return api.get(`/phonghoc/paged?${params.toString()}`);
  },
  getOne: (maPhong) => api.get(`/phonghoc/${maPhong}`),
  create: (data) => api.post('/phonghoc', data),
  update: (maPhong, data) => api.put(`/phonghoc/${maPhong}`, data),
  delete: (maPhong) => api.delete(`/phonghoc/${maPhong}`),
  getRoomsWithStatus: (toaNha, tang, ngay, ca) => {
    const params = new URLSearchParams();
    if (toaNha) params.append('toaNha', toaNha);
    if (tang) params.append('tang', tang);
    if (ngay) params.append('ngay', ngay);
    if (ca) params.append('ca', ca);
    return api.get(`/phonghoc/status?${params.toString()}`);
  },
  getRoomDetail: (maPhong, ngay, ca) => {
    const params = new URLSearchParams();
    if (ngay) params.append('ngay', ngay);
    if (ca) params.append('ca', ca);
    return api.get(`/phonghoc/${maPhong}/detail?${params.toString()}`);
  },
  getRoomSchedule: (toaNha, tang, ngay) => {
    const params = new URLSearchParams();
    if (toaNha) params.append('toaNha', toaNha);
    if (tang) params.append('tang', tang);
    if (ngay) params.append('ngay', ngay);
    return api.get(`/phonghoc/schedule?${params.toString()}`);
  },
};

// Ca làm (shift) API
export const caLamAPI = {
  getAll: () => api.get('/calam'),
  getByMaCa: (maCa) => api.get(`/calam/${maCa}`),
  create: (data) => api.post('/calam', data),
  update: (maCa, data) => api.put(`/calam/${maCa}`, data),
  delete: (maCa) => api.delete(`/calam/${maCa}`),
};

// Phong ban API
export const phongBanAPI = {
  getAll: () => api.get('/phongban'),
  getOne: (maPhongBan) => api.get(`/phongban/${maPhongBan}`),
  create: (data) => api.post('/phongban', data),
  update: (maPhongBan, data) => api.put(`/phongban/${maPhongBan}`, data),
  delete: (maPhongBan) => api.delete(`/phongban/${maPhongBan}`),
};

export default api;
