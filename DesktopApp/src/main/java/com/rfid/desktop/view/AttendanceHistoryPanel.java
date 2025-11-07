package com.rfid.desktop.view;

import com.rfid.desktop.model.AttendanceRecord;
import com.rfid.desktop.model.LopHocPhan;
import com.rfid.desktop.model.Student;
import com.rfid.desktop.service.ApplicationContext;
import com.rfid.desktop.service.AttendanceService;
import com.rfid.desktop.service.LopHocPhanService;
import com.rfid.desktop.view.components.PaginationPanel;
import com.rfid.desktop.websocket.WebSocketService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Attendance history view mirroring the React dashboard functionality.
 */
public class AttendanceHistoryPanel extends javax.swing.JPanel implements WebSocketService.AttendanceUpdateListener {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int ROWS_PER_PAGE = 12;

    private final AttendanceService attendanceService;
    private final LopHocPhanService lopHocPhanService;
    private final WebSocketService webSocketService;

    private final AttendanceHistoryTableModel tableModel = new AttendanceHistoryTableModel();
    private final JTable table = new JTable(tableModel);
    private final PaginationPanel paginationPanel = new PaginationPanel();
    private final JTextField txtNgay = new JTextField();
    private final javax.swing.JComboBox<String> cboCa = new javax.swing.JComboBox<>(new String[]{"", "1", "2", "3", "4", "5"});
    private final javax.swing.JComboBox<LopHocPhan> cboLopHocPhan = new javax.swing.JComboBox<>();
    private final JTextField txtMaSinhVien = new JTextField();
    private final JTextField txtPhongHoc = new JTextField();
    private final JButton btnExport = new JButton("Xuất Excel");
    private final JButton btnClear = new JButton("Xóa bộ lọc");
    private final JButton btnReload = new JButton("Làm mới");
    private final JLabel lblStatus = new JLabel(" ");
    private final JLabel lblGeneralTotal = createStatLabel();
    private final JLabel lblGeneralOnTime = createStatLabel();
    private final JLabel lblGeneralLate = createStatLabel();
    private final JLabel lblGeneralDangHoc = createStatLabel();
    private final JLabel lblGeneralDaRaVe = createStatLabel();
    private final JLabel lblGeneralKhongRa = createStatLabel();
    private final JLabel lblWarning = new JLabel(" ");
    private final JPanel panelClassStats = new JPanel(new BorderLayout());

    private final AtomicReference<SwingWorker<?, ?>> currentWorker = new AtomicReference<>();

    private List<AttendanceRecord> allRecords = new ArrayList<>();
    private List<AttendanceRecord> filteredRecords = new ArrayList<>();
    private List<Student> studentsOfSelectedClass = new ArrayList<>();
    private boolean listenerRegistered;

    public AttendanceHistoryPanel(ApplicationContext context) {
        this.attendanceService = context.getAttendanceService();
        this.lopHocPhanService = context.getLopHocPhanService();
        this.webSocketService = context.getWebSocketService();

        initComponents();
        configureComponents();
        attachListeners();

        loadLopHocPhans();
        reloadData();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (!listenerRegistered) {
            webSocketService.addAttendanceListener(this);
            webSocketService.connect();
            listenerRegistered = true;
        }
    }

    @Override
    public void removeNotify() {
        if (listenerRegistered) {
            webSocketService.removeAttendanceListener(this);
            listenerRegistered = false;
        }
        super.removeNotify();
    }

    @Override
    public void onAttendanceUpdated(Object payload) {
        SwingUtilities.invokeLater(this::reloadData);
    }

    private void initComponents() {
        setBackground(new Color(250, 250, 252));
        setBorder(new javax.swing.border.EmptyBorder(10, 10, 10, 10));

        javax.swing.JPanel headerPanel = new javax.swing.JPanel();
        headerPanel.setOpaque(false);
        javax.swing.JLabel lblTitle = new javax.swing.JLabel("Lịch sử điểm danh");
        lblTitle.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 20));
        lblTitle.setForeground(new Color(33, 37, 41));

        javax.swing.GroupLayout headerLayout = new javax.swing.GroupLayout(headerPanel);
        headerPanel.setLayout(headerLayout);
        headerLayout.setHorizontalGroup(
            headerLayout.createSequentialGroup()
                .addComponent(lblTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnReload, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnExport, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        headerLayout.setVerticalGroup(
            headerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(lblTitle)
                .addComponent(btnExport, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(btnReload, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        javax.swing.JPanel filterRow1 = new javax.swing.JPanel();
        filterRow1.setOpaque(false);
        javax.swing.JLabel lblNgay = new javax.swing.JLabel("Ngày (yyyy-MM-dd):");
        javax.swing.JLabel lblCa = new javax.swing.JLabel("Ca học:");
        javax.swing.JLabel lblClass = new javax.swing.JLabel("Lớp học phần:");

        javax.swing.GroupLayout filterRow1Layout = new javax.swing.GroupLayout(filterRow1);
        filterRow1.setLayout(filterRow1Layout);
        filterRow1Layout.setHorizontalGroup(
            filterRow1Layout.createSequentialGroup()
                .addComponent(lblNgay)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtNgay, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblCa)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cboCa, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblClass)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cboLopHocPhan, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE)
        );
        filterRow1Layout.setVerticalGroup(
            filterRow1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(lblNgay)
                .addComponent(txtNgay, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(lblCa)
                .addComponent(cboCa, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(lblClass)
                .addComponent(cboLopHocPhan, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        javax.swing.JPanel filterRow2 = new javax.swing.JPanel();
        filterRow2.setOpaque(false);
        javax.swing.JLabel lblMaSinhVien = new javax.swing.JLabel("Mã sinh viên:");
        javax.swing.JLabel lblPhongHoc = new javax.swing.JLabel("Phòng học:");

        javax.swing.GroupLayout filterRow2Layout = new javax.swing.GroupLayout(filterRow2);
        filterRow2.setLayout(filterRow2Layout);
        filterRow2Layout.setHorizontalGroup(
            filterRow2Layout.createSequentialGroup()
                .addComponent(lblMaSinhVien)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtMaSinhVien, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblPhongHoc)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtPhongHoc, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnClear, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE)
        );
        filterRow2Layout.setVerticalGroup(
            filterRow2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(lblMaSinhVien)
                .addComponent(txtMaSinhVien, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(lblPhongHoc)
                .addComponent(txtPhongHoc, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(btnClear, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        javax.swing.JPanel filtersPanel = new javax.swing.JPanel();
        filtersPanel.setOpaque(false);
        javax.swing.GroupLayout filtersLayout = new javax.swing.GroupLayout(filtersPanel);
        filtersPanel.setLayout(filtersLayout);
        filtersLayout.setHorizontalGroup(
            filtersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(filterRow1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(filterRow2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblWarning, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        filtersLayout.setVerticalGroup(
            filtersLayout.createSequentialGroup()
                .addComponent(filterRow1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(filterRow2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblWarning, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        javax.swing.JPanel statsPanel = new javax.swing.JPanel();
        statsPanel.setOpaque(false);
        javax.swing.GroupLayout statsLayout = new javax.swing.GroupLayout(statsPanel);
        statsPanel.setLayout(statsLayout);
        statsLayout.setHorizontalGroup(
            statsLayout.createSequentialGroup()
                .addComponent(createStatCard("Tổng bản ghi", lblGeneralTotal))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(createStatCard("Đúng giờ", lblGeneralOnTime))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(createStatCard("Muộn", lblGeneralLate))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(createStatCard("Đang học", lblGeneralDangHoc))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(createStatCard("Đã ra về", lblGeneralDaRaVe))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(createStatCard("Không điểm danh ra", lblGeneralKhongRa))
        );
        statsLayout.setVerticalGroup(
            statsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(lblGeneralTotal.getParent(), javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblGeneralOnTime.getParent(), javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblGeneralLate.getParent(), javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblGeneralDangHoc.getParent(), javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblGeneralDaRaVe.getParent(), javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblGeneralKhongRa.getParent(), javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        panelClassStats.setOpaque(false);
        panelClassStats.setBorder(javax.swing.BorderFactory.createTitledBorder("Thống kê lớp học phần"));
        panelClassStats.setVisible(false);

        JScrollPane scrollPane = new JScrollPane(table);

        javax.swing.JPanel tableContainer = new javax.swing.JPanel();
        tableContainer.setBorder(javax.swing.BorderFactory.createTitledBorder("Bảng lịch sử điểm danh"));
        javax.swing.GroupLayout tableLayout = new javax.swing.GroupLayout(tableContainer);
        tableContainer.setLayout(tableLayout);
        tableLayout.setHorizontalGroup(
            tableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(paginationPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        tableLayout.setVerticalGroup(
            tableLayout.createSequentialGroup()
                .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(paginationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

    	javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(headerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(filtersPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(statsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(panelClassStats, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(tableContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(filtersPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(statsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(panelClassStats, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(tableContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
    }

    private void configureComponents() {
        table.setFillsViewportHeight(true);
        table.setRowHeight(26);
        table.setAutoCreateRowSorter(true);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);

        cboLopHocPhan.setPrototypeDisplayValue(new LopHocPhan());
        cboLopHocPhan.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof LopHocPhan lop) {
                    String text = lop.getTenLopHocPhan();
                    if (text == null || text.isBlank()) {
                        text = lop.getMaLopHocPhan();
                    }
                    setText(text != null ? text : "");
                } else if (value == null) {
                    setText("Tất cả lớp học phần");
                }
                return comp;
            }
        });

        paginationPanel.setPageSize(ROWS_PER_PAGE);
        paginationPanel.setPageChangeListener(this::showPage);

        lblWarning.setForeground(new Color(204, 102, 0));
        lblStatus.setForeground(new Color(90, 90, 90));

        panelClassStats.setVisible(false);
    }

    private void attachListeners() {
        Runnable filterAction = this::applyFilters;
        btnReload.addActionListener(e -> reloadData());
        btnExport.addActionListener(e -> exportToExcel());
        btnClear.addActionListener(e -> {
            txtNgay.setText("");
            cboCa.setSelectedIndex(0);
            cboLopHocPhan.setSelectedItem(null);
            txtMaSinhVien.setText("");
            txtPhongHoc.setText("");
            applyFilters();
        });

        txtNgay.addActionListener(e -> filterAction.run());
        txtMaSinhVien.addActionListener(e -> filterAction.run());
        txtPhongHoc.addActionListener(e -> filterAction.run());
        cboCa.addActionListener(e -> filterAction.run());
        cboLopHocPhan.addActionListener(e -> filterAction.run());
    }

    private void reloadData() {
        setStatus("Đang tải dữ liệu điểm danh...");

        SwingWorker<List<AttendanceRecord>, Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected List<AttendanceRecord> doInBackground() {
                try {
                    return attendanceService.getAll();
                } catch (Exception ex) {
                    error = ex;
                    return List.of();
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    setStatus("Lỗi: " + error.getMessage());
                    return;
                }
                try {
                    List<AttendanceRecord> records = get();
                    records.sort(Comparator.comparing(AttendanceRecord::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())));
                    allRecords = new ArrayList<>(records);
                    applyFilters();
                } catch (Exception ex) {
                    setStatus("Lỗi: " + ex.getMessage());
                }
            }
        };

        SwingWorker<?, ?> previous = currentWorker.getAndSet(worker);
        if (previous != null) {
            previous.cancel(true);
        }
        worker.execute();
    }

    private void loadLopHocPhans() {
        new SwingWorker<List<LopHocPhan>, Void>() {
            @Override
            protected List<LopHocPhan> doInBackground() throws Exception {
                return lopHocPhanService.getAll();
            }

            @Override
            protected void done() {
                try {
                    List<LopHocPhan> classes = get();
                    cboLopHocPhan.removeAllItems();
                    cboLopHocPhan.addItem(null);
                    for (LopHocPhan lop : classes) {
                        cboLopHocPhan.addItem(lop);
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void applyFilters() {
        String ngay = txtNgay.getText().trim();
        String ca = (String) cboCa.getSelectedItem();
        LopHocPhan selectedClass = (LopHocPhan) cboLopHocPhan.getSelectedItem();
        String maSinhVien = txtMaSinhVien.getText().trim().toLowerCase();
        String phongHoc = txtPhongHoc.getText().trim().toLowerCase();

        List<AttendanceRecord> filtered = new ArrayList<>(allRecords);

        if (!ngay.isBlank()) {
            filtered.removeIf(record -> record.getNgay() == null || !DATE_FORMATTER.format(record.getNgay()).equals(ngay));
        }

        if (ca != null && !ca.isBlank()) {
            filtered.removeIf(record -> record.getCa() == null || record.getCa() != Integer.parseInt(ca));
        }

        if (!maSinhVien.isBlank()) {
            filtered.removeIf(record -> record.getMaSinhVien() == null ||
                    !record.getMaSinhVien().toLowerCase().contains(maSinhVien));
        }

        if (!phongHoc.isBlank()) {
            filtered.removeIf(record -> record.getPhongHoc() == null ||
                    !record.getPhongHoc().toLowerCase().contains(phongHoc));
        }

        if (selectedClass != null) {
            if (ngay.isBlank() || ca == null || ca.isBlank()) {
                lblWarning.setText("⚠️ Khi lọc theo lớp học phần, vui lòng chọn cả Ngày và Ca học");
                panelClassStats.setVisible(false);
                filteredRecords = List.of();
                tableModel.setRecords(List.of());
                paginationPanel.reset(0);
                setStatus("Không thể lọc lớp học phần khi thiếu ngày hoặc ca");
                return;
            }

            lblWarning.setText(" ");
            new SwingWorker<List<Student>, Void>() {
                @Override
                protected List<Student> doInBackground() throws Exception {
                    return lopHocPhanService.getStudents(selectedClass.getMaLopHocPhan());
                }

                @Override
                protected void done() {
                    try {
                        studentsOfSelectedClass = get();
                        Set<String> studentIds = new HashSet<>();
                        for (Student student : studentsOfSelectedClass) {
                            studentIds.add(student.getMaSinhVien());
                        }
                        filtered.removeIf(record -> !studentIds.contains(record.getMaSinhVien()));
                        updateFilteredData(filtered, true);
                    } catch (Exception ex) {
                        setStatus("Lỗi khi lọc lớp học phần: " + ex.getMessage());
                        updateFilteredData(filtered, false);
                    }
                }
            }.execute();
        } else {
            lblWarning.setText(" ");
            studentsOfSelectedClass = List.of();
            updateFilteredData(filtered, false);
        }
    }

    private void updateFilteredData(List<AttendanceRecord> data, boolean classFilterActive) {
        data.sort(Comparator.comparing(AttendanceRecord::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        filteredRecords = new ArrayList<>(data);
        paginationPanel.reset(filteredRecords.size());

        if (filteredRecords.isEmpty()) {
            tableModel.setRecords(List.of());
            setStatus("Không có bản ghi nào phù hợp");
        } else {
            showPage(paginationPanel.getCurrentPage());
        }
        updateGeneralStats();
        updateClassStats(classFilterActive);
    }

    private void showPage(int page) {
        if (filteredRecords.isEmpty()) {
            tableModel.setRecords(List.of());
            paginationPanel.setCurrentPage(1, false);
            return;
        }
        int pageSize = paginationPanel.getPageSize();
        int totalItems = filteredRecords.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        page = Math.max(1, Math.min(totalPages, page));
        paginationPanel.setCurrentPage(page, false);

        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, totalItems);
        List<AttendanceRecord> pageData = filteredRecords.subList(from, to);
        tableModel.setRecords(pageData);
        setStatus(String.format("Đang hiển thị %d - %d / %d bản ghi", from + 1, to, totalItems));
    }

    private void updateGeneralStats() {
        List<AttendanceRecord> source = filteredRecords.isEmpty() ? allRecords : filteredRecords;
        lblGeneralTotal.setText(String.valueOf(source.size()));
        lblGeneralOnTime.setText(String.valueOf(countByTinhTrang(source, "DUNG_GIO")));
        lblGeneralLate.setText(String.valueOf(countByTinhTrang(source, "MUON")));
        lblGeneralDangHoc.setText(String.valueOf(countByTrangThai(source, "DANG_HOC")));
        lblGeneralDaRaVe.setText(String.valueOf(countByTrangThai(source, "DA_RA_VE")));
        lblGeneralKhongRa.setText(String.valueOf(countByTrangThai(source, "KHONG_DIEM_DANH_RA")));
    }

    private void updateClassStats(boolean active) {
        panelClassStats.removeAll();
        if (!active || studentsOfSelectedClass.isEmpty()) {
            panelClassStats.setVisible(false);
            return;
        }

        Set<String> attended = new HashSet<>();
        Set<String> late = new HashSet<>();
        Set<String> dangHoc = new HashSet<>();
        Set<String> daRaVe = new HashSet<>();
        Set<String> raVeSom = new HashSet<>();
        Set<String> khongRa = new HashSet<>();

        for (AttendanceRecord record : filteredRecords) {
            if (record.getMaSinhVien() == null) {
                continue;
            }
            attended.add(record.getMaSinhVien());
            if ("MUON".equalsIgnoreCase(record.getTinhTrangDiemDanh())) {
                late.add(record.getMaSinhVien());
            }
            String status = record.getTrangThai();
            if (status != null) {
                status = status.toUpperCase(Locale.ROOT);
                switch (status) {
                    case "DANG_HOC" -> dangHoc.add(record.getMaSinhVien());
                    case "DA_RA_VE" -> daRaVe.add(record.getMaSinhVien());
                    case "RA_VE_SOM" -> raVeSom.add(record.getMaSinhVien());
                    case "KHONG_DIEM_DANH_RA" -> khongRa.add(record.getMaSinhVien());
                    default -> {
                    }
                }
            }
        }

        int totalStudents = studentsOfSelectedClass.size();
        int present = attended.size();
        int absent = Math.max(0, totalStudents - present);

        javax.swing.JLabel lblInfo = new javax.swing.JLabel(String.format(
                "Tổng số: %d | Tham gia: %d | Vắng: %d | Muộn: %d | Đang học: %d | Đã ra về: %d | Không điểm danh ra: %d",
                totalStudents, present, absent, late.size(), dangHoc.size(), daRaVe.size(), khongRa.size()));
        lblInfo.setForeground(new Color(33, 37, 41));
        lblInfo.setBorder(new javax.swing.border.EmptyBorder(8, 12, 8, 12));

        panelClassStats.add(lblInfo, BorderLayout.CENTER);
        panelClassStats.setVisible(true);
        panelClassStats.revalidate();
    }

    private int countByTinhTrang(List<AttendanceRecord> records, String expected) {
        int count = 0;
        for (AttendanceRecord record : records) {
            if (expected.equalsIgnoreCase(record.getTinhTrangDiemDanh())) {
                count++;
            }
        }
        return count;
    }

    private int countByTrangThai(List<AttendanceRecord> records, String expected) {
        int count = 0;
        for (AttendanceRecord record : records) {
            if (expected.equalsIgnoreCase(record.getTrangThai())) {
                count++;
            }
        }
        return count;
    }

    private void exportToExcel() {
        LopHocPhan selectedClass = (LopHocPhan) cboLopHocPhan.getSelectedItem();
        String ngay = txtNgay.getText().trim();
        String ca = (String) cboCa.getSelectedItem();

        if (selectedClass != null && (ngay.isBlank() || ca == null || ca.isBlank())) {
            JOptionPane.showMessageDialog(this,
                    "Khi lọc theo lớp học phần, vui lòng chọn cả Ngày và Ca học trước khi xuất Excel",
                    "Thiếu thông tin",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("attendance-history.xlsx"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();

        List<AttendanceRecord> exportData = filteredRecords.isEmpty() ? allRecords : filteredRecords;

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Attendance");
            int rowIndex = 0;
            Row header = sheet.createRow(rowIndex++);
            String[] headers = {"RFID", "Mã SV", "Tên sinh viên", "Phòng học", "Ngày", "Ca", "Giờ vào", "Giờ ra", "Tình trạng", "Trạng thái"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
            }

            for (AttendanceRecord record : exportData) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(nullSafe(record.getRfid()));
                row.createCell(1).setCellValue(nullSafe(record.getMaSinhVien()));
                row.createCell(2).setCellValue(nullSafe(record.getTenSinhVien()));
                row.createCell(3).setCellValue(nullSafe(record.getPhongHoc()));
                row.createCell(4).setCellValue(record.getNgay() != null ? DATE_FORMATTER.format(record.getNgay()) : "");
                row.createCell(5).setCellValue(record.getCa() != null ? record.getCa() : 0);
                row.createCell(6).setCellValue(record.getGioVao() != null ? TIME_FORMATTER.format(record.getGioVao()) : "");
                row.createCell(7).setCellValue(record.getGioRa() != null ? TIME_FORMATTER.format(record.getGioRa()) : "");
                row.createCell(8).setCellValue(nullSafe(translateTinhTrang(record.getTinhTrangDiemDanh())));
                row.createCell(9).setCellValue(nullSafe(translateTrangThai(record.getTrangThai())));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }

            setStatus("Đã xuất Excel: " + file.getAbsolutePath());
        } catch (IOException ex) {
            setStatus("Xuất Excel thất bại: " + ex.getMessage());
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private void setStatus(String text) {
        lblStatus.setText(text);
    }

    private JLabel createStatLabel() {
        JLabel label = new JLabel("0", SwingConstants.CENTER);
        label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18));
        label.setForeground(new Color(52, 71, 103));
        return label;
    }

    private JPanel createStatCard(String title, JLabel valueLabel) {
        JPanel panel = new JPanel();
        panel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new Color(230, 234, 238)),
                javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        panel.setLayout(new BorderLayout());
        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        lblTitle.setForeground(new Color(90, 90, 90));
        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        return panel;
    }

    private String translateTrangThai(String value) {
        if (value == null) {
            return "";
        }
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "DANG_HOC" -> "Đang học";
            case "DA_RA_VE" -> "Đã ra về";
            case "RA_VE_SOM" -> "Ra về sớm";
            case "KHONG_DIEM_DANH_RA" -> "Không điểm danh ra";
            default -> value;
        };
    }

    private String translateTinhTrang(String value) {
        if (value == null) {
            return "";
        }
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "DUNG_GIO" -> "Đúng giờ";
            case "MUON" -> "Muộn";
            default -> value;
        };
    }

    private class AttendanceHistoryTableModel extends AbstractTableModel {

        private final String[] columns = {
                "RFID", "Mã SV", "Tên sinh viên", "Phòng học", "Ngày", "Ca", "Giờ vào", "Giờ ra", "Tình trạng", "Trạng thái"
        };

        private List<AttendanceRecord> records = new ArrayList<>();

        void setRecords(List<AttendanceRecord> records) {
            this.records = new ArrayList<>(records);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return records.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            AttendanceRecord record = records.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> record.getRfid();
                case 1 -> record.getMaSinhVien();
                case 2 -> record.getTenSinhVien();
                case 3 -> record.getPhongHoc();
                case 4 -> record.getNgay() != null ? DATE_FORMATTER.format(record.getNgay()) : "";
                case 5 -> record.getCa();
                case 6 -> record.getGioVao() != null ? TIME_FORMATTER.format(record.getGioVao()) : "";
                case 7 -> record.getGioRa() != null ? TIME_FORMATTER.format(record.getGioRa()) : "";
                case 8 -> translateTinhTrang(record.getTinhTrangDiemDanh());
                case 9 -> translateTrangThai(record.getTrangThai());
                default -> "";
            };
        }
    }
}

