import * as XLSX from 'xlsx';

const getCaName = (ca) => {
  const caMap = {
    1: 'Ca 1 (07:00-09:25)',
    2: 'Ca 2 (09:35-12:00)',
    3: 'Ca 3 (12:30-14:55)',
    4: 'Ca 4 (15:05-17:30)',
    5: 'Ca 5 (18:00-20:30)'
  };
  return caMap[ca] || `Ca ${ca}`;
};

export function exportAttendanceToExcel({
  attendance,
  allFilteredAttendance,
  studentsInLop,
  filters,
  lopHocPhans,
  attendanceStats
}) {
  const wb = XLSX.utils.book_new();

  const headerInfo = [];

  if (filters.lopHocPhan) {
    const selectedLop = lopHocPhans.find(l => l.maLopHocPhan === filters.lopHocPhan);
    const caName = getCaName(parseInt(filters.ca));

    headerInfo.push(['BAN CƠ YẾU CHÍNH PHỦ', '', '', '', '', '', '', '', 'CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM']);
    headerInfo.push(['HỌC VIỆN KỸ THUẬT MẬT MÃ', '', '', '', '', '', '', '', 'Độc lập - Tự do - Hạnh phúc']);
    headerInfo.push([]);
    headerInfo.push([]);
    headerInfo.push(['', '', '', 'DANH SÁCH ĐIỂM DANH SINH VIÊN', '', '', '', '']);
    headerInfo.push(['', '', `Lớp: ${selectedLop?.tenLopHocPhan}`, '', '', '', '', '']);
    headerInfo.push(['', '', `Ngày: ${new Date(filters.ngay).toLocaleDateString('vi-VN')} - ${caName}`, '', '', '', '', '']);
    headerInfo.push(['', '', `Học kỳ 1 - Năm học 2025 - 2026`, '', '', '', '', '']);
    headerInfo.push([]);
    headerInfo.push(['STT', 'Mã sinh viên', 'Họ và tên', 'Điểm danh', 'Ghi chú', '', '', '']);
  } else {
    headerInfo.push(['BAN CƠ YẾU CHÍNH PHỦ', '', '', '', '', '', '', '', '', '', 'CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM']);
    headerInfo.push(['HỌC VIỆN KỸ THUẬT MẬT MÃ', '', '', '', '', '', '', '', '', '', 'Độc lập - Tự do - Hạnh phúc']);
    headerInfo.push([]);
    headerInfo.push([]);
    headerInfo.push(['', '', '', '', 'LỊCH SỬ ĐIỂM DANH SINH VIÊN', '', '', '', '', '', '']);
    headerInfo.push(['', '', '', '', `Xuất ngày: ${new Date().toLocaleDateString('vi-VN')}`, '', '', '', '', '', '']);
    headerInfo.push([]);
    headerInfo.push(['RFID', 'Mã sinh viên', 'Tên sinh viên', 'Phòng học', 'Ngày', 'Ca', 'Giờ vào', 'Giờ ra', 'Tình trạng điểm danh', 'Ghi chú', 'Thời gian tạo']);
  }

  let data = [];

  if (filters.lopHocPhan && studentsInLop && studentsInLop.length > 0) {
    data = studentsInLop.map((student, index) => {
      const attendanceRecord = (allFilteredAttendance || []).find(r => r.maSinhVien === student.maSinhVien);
      let diemDanh = 'v';
      let ghiChu = '';
      let rowStyle = null;

      if (attendanceRecord) {
        if (attendanceRecord.tinhTrangDiemDanh === 'muon' || attendanceRecord.tinhTrangDiemDanh === 'MUON') {
          diemDanh = 'M';
          rowStyle = { fill: { fgColor: { rgb: 'FFFFE0' } } };
        } else {
          diemDanh = 'x';
        }

        if (attendanceRecord.trangThai === 'RA_VE_SOM' || attendanceRecord.trangThai === 'ra_ve_som') {
          ghiChu = 'Ra về sớm';
        } else if (attendanceRecord.trangThai === 'KHONG_DIEM_DANH_RA' || attendanceRecord.trangThai === 'khong_diem_danh_ra') {
          ghiChu = 'Không điểm danh ra';
        }
      } else {
        diemDanh = 'v';
        rowStyle = { fill: { fgColor: { rgb: 'FFE6E6' } } };
      }

      return {
        data: [index + 1, student.maSinhVien, student.tenSinhVien, diemDanh, ghiChu],
        style: rowStyle
      };
    });
  } else {
    const source = (allFilteredAttendance && allFilteredAttendance.length ? allFilteredAttendance : attendance) || [];
    data = source.map(r => {
      let ghiChu = '';
      if (r.trangThai === 'RA_VE_SOM' || r.trangThai === 'ra_ve_som') {
        ghiChu = 'Ra về sớm';
      } else if (r.trangThai === 'KHONG_DIEM_DANH_RA' || r.trangThai === 'khong_diem_danh_ra') {
        ghiChu = 'Không điểm danh ra';
      }
      return [
        r.rfid,
        r.maSinhVien,
        r.tenSinhVien,
        r.phongHoc || '',
        r.ngay,
        r.ca,
        r.gioVao || '',
        r.gioRa || '',
        r.tinhTrangDiemDanh === 'dung_gio' ? 'Đúng giờ' : r.tinhTrangDiemDanh === 'muon' ? 'Muộn' : r.tinhTrangDiemDanh,
        ghiChu,
        new Date(r.createdAt).toLocaleString('vi-VN')
      ];
    });
  }

  let allData = [...headerInfo];
  if (filters.lopHocPhan && studentsInLop && studentsInLop.length > 0) {
    data.forEach(item => {
      allData.push(item.data);
    });
  } else {
    allData = [...allData, ...data];
  }

  let ws;
  if (filters.lopHocPhan && studentsInLop && studentsInLop.length > 0) {
    let htmlTable = '<table border="1" style="border-collapse: collapse;">';
    headerInfo.forEach(row => {
      htmlTable += '<tr>';
      row.forEach(cell => {
        htmlTable += `<td style="font-weight: bold; text-align: center; background-color: #f0f0f0; padding: 5px;">${cell || ''}</td>`;
      });
      htmlTable += '</tr>';
    });
    data.forEach(item => {
      const backgroundColor = item.style?.fill?.fgColor?.rgb === 'FFFFE0' ? '#FFFFE0' : 
                             item.style?.fill?.fgColor?.rgb === 'FFE6E6' ? '#FFE6E6' : '';
      htmlTable += '<tr>';
      item.data.forEach(cell => {
        const style = backgroundColor ? `style="background-color: ${backgroundColor}; padding: 5px;"` : 'style="padding: 5px;"';
        htmlTable += `<td ${style}>${cell}</td>`;
      });
      htmlTable += '</tr>';
    });
    if (attendanceStats && attendanceStats.totalStudents > 0) {
      htmlTable += '<tr><td colspan="5"></td></tr>';
      htmlTable += '<tr><td style="font-weight: bold;" colspan="5">THỐNG KÊ:</td></tr>';
      htmlTable += `<tr><td colspan="5">Tổng số sinh viên: ${attendanceStats.totalStudents}</td></tr>`;
      htmlTable += `<tr><td colspan="5">Số sinh viên tham gia: ${attendanceStats.attended}</td></tr>`;
      htmlTable += `<tr><td colspan="5">Số sinh viên vắng: ${attendanceStats.absent}</td></tr>`;
      htmlTable += `<tr><td colspan="5">Số sinh viên muộn: ${attendanceStats.late}</td></tr>`;
      htmlTable += `<tr><td colspan="5">Số sinh viên đang học: ${attendanceStats.dangHoc}</td></tr>`;
      htmlTable += `<tr><td colspan="5">Số sinh viên đã ra về: ${attendanceStats.daRaVe}</td></tr>`;
      htmlTable += `<tr><td colspan="5">Số sinh viên ra về sớm: ${attendanceStats.raVeSom}</td></tr>`;
      htmlTable += `<tr><td colspan="5">Số sinh viên không điểm danh ra: ${attendanceStats.khongDiemDanhRa}</td></tr>`;
    }
    htmlTable += '</table>';
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = htmlTable;
    ws = XLSX.utils.table_to_sheet(tempDiv.querySelector('table'));
  } else {
    ws = XLSX.utils.aoa_to_sheet(allData);
  }

  if (filters.lopHocPhan) {
    ws['!cols'] = [
      { width: 5 },
      { width: 15 },
      { width: 25 },
      { width: 12 }
    ];
  } else {
    ws['!cols'] = [
      { width: 20 },
      { width: 15 },
      { width: 25 },
      { width: 15 },
      { width: 12 },
      { width: 8 },
      { width: 12 },
      { width: 12 },
      { width: 20 },
      { width: 20 },
      { width: 20 }
    ];
  }

  XLSX.utils.book_append_sheet(wb, ws, 'Attendance');

  let filename = 'attendance.xlsx';
  if (filters.lopHocPhan) {
    const selectedLop = lopHocPhans.find(l => l.maLopHocPhan === filters.lopHocPhan);
    const dateStr = filters.ngay || new Date().toISOString().split('T')[0];
    filename = `DiemDanh_${selectedLop?.maLopHocPhan}_${dateStr}.xlsx`;
  } else {
    const dateStr = new Date().toISOString().split('T')[0];
    filename = `LichSuDiemDanh_${dateStr}.xlsx`;
  }

  XLSX.writeFile(wb, filename);
}


export function exportClassAttendanceMatrix({
  lopHocPhan,
  students,
  sessions,
  attendance
}) {
  const wb = XLSX.utils.book_new();

  // Build headers: STT, Mã SV, Tên SV, then one column per session "dd/MM/yyyy - Ca X", and summary
  const headers = ['STT', 'Mã sinh viên', 'Tên sinh viên'];
  const sessionHeaders = sessions.map(s => {
    const d = new Date(s.ngayHoc);
    const dateStr = d.toLocaleDateString('vi-VN');
    return dateStr;
  });
  headers.push(...sessionHeaders);
  headers.push('Tổng muộn');
  headers.push('Tổng vắng');
  headers.push('Tổng tham gia');

  // Index attendance by key: maSinhVien|ngay|ca
  const attIndex = new Map();
  (attendance || []).forEach(r => {
    const key = `${r.maSinhVien}|${r.ngay}|${r.ca}`;
    attIndex.set(key, r);
  });

  const totalSessions = sessions.length;
  const rows = [headers];

  students.forEach((sv, idx) => {
    const row = [idx + 1, sv.maSinhVien, sv.tenSinhVien];
    let participated = 0;
    let lateCount = 0;
    let absentCount = 0;
    sessions.forEach(s => {
      const key = `${sv.maSinhVien}|${s.ngayHoc}|${s.ca}`;
      const rec = attIndex.get(key);
      let cell = 'v';
      if (rec) {
        if (rec.tinhTrangDiemDanh === 'muon' || rec.tinhTrangDiemDanh === 'MUON') {
          cell = 'M';
          participated += 1; // Muộn vẫn tính tham gia
          lateCount += 1;
        } else {
          cell = 'x';
          participated += 1;
        }
      } else {
        absentCount += 1;
      }
      row.push(cell);
    });
    row.push(lateCount);
    row.push(absentCount);
    row.push(participated);
    rows.push(row);
  });

  const ws = XLSX.utils.aoa_to_sheet(rows);
  // Set column widths
  const cols = [
    { width: 5 },   // STT
    { width: 15 },  // Mã SV
    { width: 28 },  // Tên SV
    ...sessionHeaders.map(() => ({ width: 20 })),
    { width: 12 },  // Tổng muộn
    { width: 12 },  // Tổng vắng
    { width: 14 }   // Tổng tham gia
  ];
  ws['!cols'] = cols;

  XLSX.utils.book_append_sheet(wb, ws, (lopHocPhan || 'LopHocPhan'));

  const dateStr = new Date().toISOString().split('T')[0];
  const safeCode = (lopHocPhan || 'LHP').replace(/[^A-Za-z0-9_-]+/g, '-');
  const filename = `BangDiemDanh_${safeCode}_${dateStr}.xlsx`;
  XLSX.writeFile(wb, filename);
}


