package com.rfid.desktop.view;

import com.rfid.desktop.chart.AttendanceChartFactory;
import com.rfid.desktop.model.AttendanceRecord;
import com.rfid.desktop.model.RfidEvent;
import com.rfid.desktop.model.Student;
import com.rfid.desktop.service.ApplicationContext;
import com.rfid.desktop.service.AttendanceService;
import com.rfid.desktop.service.StudentService;
import com.rfid.desktop.view.components.PaginationPanel;
import com.rfid.desktop.websocket.WebSocketService;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DashboardPanel extends javax.swing.JPanel implements WebSocketService.AttendanceUpdateListener {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int ROWS_PER_PAGE = 10;

    private final ApplicationContext context;
    private final StudentService studentService;
    private final AttendanceService attendanceService;
    private final WebSocketService webSocketService;

    private final AttendanceTableModel tableModel = new AttendanceTableModel();
    private final PaginationPanel paginationPanel = new PaginationPanel();
    private final AtomicReference<SwingWorker<?, ?>> currentWorker = new AtomicReference<>();

    private List<AttendanceRecord> allAttendance = new ArrayList<>();
    private boolean listenerRegistered;

    public DashboardPanel(ApplicationContext context) {
        this.context = context;
        this.studentService = context.getStudentService();
        this.attendanceService = context.getAttendanceService();
        this.webSocketService = context.getWebSocketService();

        initComponents();
        configureComponents();
        attachListeners();
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

    private void configureComponents() {
        tblTodayAttendance.setModel(tableModel);
        tblTodayAttendance.setFillsViewportHeight(true);
        tblTodayAttendance.setRowHeight(28);
        tblTodayAttendance.setAutoCreateRowSorter(true);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tblTodayAttendance.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        tblTodayAttendance.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        tblTodayAttendance.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        paginationPanel.setPageSize(ROWS_PER_PAGE);
        paginationPanel.setPageChangeListener(this::showPage);
    }

    private void attachListeners() {
        btnRefresh.addActionListener(e -> reloadData());
    }

    private void reloadData() {
        setLoadingState(true);

        SwingWorker<DashboardData, Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected DashboardData doInBackground() {
                try {
                    DashboardData data = new DashboardData();
                    data.students = studentService.getAll();
                    data.todayAttendance = attendanceService.getToday();
                    data.unprocessedRfids = attendanceService.getUnprocessedRfids();
                    return data;
                } catch (Exception ex) {
                    error = ex;
                    cancel(true);
                    return null;
                }
            }

            @Override
            protected void done() {
                setLoadingState(false);
                if (error != null) {
                    lblTotalStudentsValue.setText("-");
                    lblTodayAttendanceValue.setText("-");
                    lblUnprocessedValue.setText("-");
                    lblCurrentShiftValue.setText("-");
                    showPlaceholderCharts();
                    tableModel.setRecords(List.of());
                    paginationPanel.reset(0);
                    return;
                }
                try {
                    DashboardData data = get();
                    if (data == null) {
                        return;
                    }

                    lblTotalStudentsValue.setText(String.valueOf(data.students.size()));
                    lblTodayAttendanceValue.setText(String.valueOf(data.todayAttendance.size()));
                    long unprocessed = data.unprocessedRfids.stream()
                            .filter(event -> Boolean.FALSE.equals(event.getProcessed()))
                            .count();
                    lblUnprocessedValue.setText(String.valueOf(unprocessed));
                    lblCurrentShiftValue.setText(getCurrentCaName());

                    data.todayAttendance.sort(Comparator.comparing(AttendanceRecord::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())));

                    allAttendance = new ArrayList<>(data.todayAttendance);
                    paginationPanel.reset(allAttendance.size());
                    if (allAttendance.isEmpty()) {
                        tableModel.setRecords(List.of());
                        showPlaceholderCharts();
                    } else {
                        showPage(paginationPanel.getCurrentPage());
                        updateCharts(allAttendance);
                    }
                } catch (Exception ignored) {
                }
            }
        };

        SwingWorker<?, ?> previous = currentWorker.getAndSet(worker);
        if (previous != null) {
            previous.cancel(true);
        }
        worker.execute();
    }

    private void showPage(int page) {
        if (allAttendance.isEmpty()) {
            tableModel.setRecords(List.of());
            return;
        }
        int pageSize = paginationPanel.getPageSize();
        int totalItems = allAttendance.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (totalPages == 0) {
            tableModel.setRecords(List.of());
            return;
        }

        page = Math.max(1, Math.min(totalPages, page));
        paginationPanel.setCurrentPage(page, false);

        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalItems);
        List<AttendanceRecord> pageRecords = allAttendance.subList(fromIndex, toIndex);
        tableModel.setRecords(pageRecords);
    }

    private void updateCharts(List<AttendanceRecord> records) {
        if (records == null || records.isEmpty()) {
            showPlaceholderCharts();
            return;
        }

        Map<Integer, Long> byCa = records.stream()
                .collect(Collectors.groupingBy(r -> r.getCa() != null ? r.getCa() : 0, Collectors.counting()));

        Map<String, Long> statusMap = new HashMap<>();
        statusMap.put("Đúng giờ", 0L);
        statusMap.put("Muộn", 0L);
        statusMap.put("Đang học", 0L);
        statusMap.put("Đã ra về", 0L);
        statusMap.put("Ra về sớm", 0L);
        statusMap.put("Không điểm danh ra", 0L);

        Map<Integer, Long> hourly = new HashMap<>();

        for (AttendanceRecord record : records) {
            if (record.getGioVao() != null) {
                int hour = record.getGioVao().getHour();
                hourly.merge(hour, 1L, Long::sum);
            }

            if (record.getTinhTrangDiemDanh() != null) {
                switch (record.getTinhTrangDiemDanh().toUpperCase(Locale.ROOT)) {
                    case "DUNG_GIO" -> statusMap.merge("Đúng giờ", 1L, Long::sum);
                    case "MUON" -> statusMap.merge("Muộn", 1L, Long::sum);
                    default -> {
                    }
                }
            }

            if (record.getTrangThai() != null) {
                switch (record.getTrangThai().toUpperCase(Locale.ROOT)) {
                    case "DANG_HOC" -> statusMap.merge("Đang học", 1L, Long::sum);
                    case "DA_RA_VE" -> statusMap.merge("Đã ra về", 1L, Long::sum);
                    case "RA_VE_SOM" -> statusMap.merge("Ra về sớm", 1L, Long::sum);
                    case "KHONG_DIEM_DANH_RA" -> statusMap.merge("Không điểm danh ra", 1L, Long::sum);
                    default -> {
                    }
                }
            }
        }

        CategoryChart caChart = AttendanceChartFactory.buildAttendanceByCaChart(byCa);
        PieChart statusChart = AttendanceChartFactory.buildAttendanceStatusChart(statusMap);

        List<Long> hourlyData = new ArrayList<>(24);
        for (int hour = 0; hour < 24; hour++) {
            hourlyData.add(hourly.getOrDefault(hour, 0L));
        }
        XYChart hourChart = AttendanceChartFactory.buildAttendanceByHourChart(hourlyData);

        renderChart(pnlChartByShift, new XChartPanel<>(caChart));
        renderChart(pnlChartStatus, new XChartPanel<>(statusChart));
        renderChart(pnlChartHourly, new XChartPanel<>(hourChart));
    }

    private void renderChart(JPanel container, JPanel chartPanel) {
        container.removeAll();
        container.add(chartPanel, BorderLayout.CENTER);
        container.revalidate();
        container.repaint();
    }

    private void showPlaceholderCharts() {
        showPlaceholder(pnlChartByShift, "Không có dữ liệu");
        showPlaceholder(pnlChartStatus, "Không có dữ liệu");
        showPlaceholder(pnlChartHourly, "Không có dữ liệu");
    }

    private void showPlaceholder(JPanel container, String message) {
        container.removeAll();
        JLabel placeholder = new JLabel(message, SwingConstants.CENTER);
        placeholder.setForeground(new Color(120, 120, 120));
        container.add(placeholder, BorderLayout.CENTER);
        container.revalidate();
        container.repaint();
    }

    private void setLoadingState(boolean isLoading) {
        btnRefresh.setEnabled(!isLoading);
    }

    private String getCurrentCaName() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime time = now.toLocalTime();
        int minutes = time.getHour() * 60 + time.getMinute();
        if (minutes >= 420 && minutes < 570) {
            return "Ca 1 (07:00-09:30)";
        }
        if (minutes >= 570 && minutes < 720) {
            return "Ca 2 (09:30-12:00)";
        }
        if (minutes >= 750 && minutes < 900) {
            return "Ca 3 (12:30-15:00)";
        }
        if (minutes >= 900 && minutes < 1050) {
            return "Ca 4 (15:00-17:30)";
        }
        return "Ngoài giờ học";
    }

    private String resolveCaName(Integer ca) {
        if (ca == null) {
            return "-";
        }
        return switch (ca) {
            case 1 -> "Ca 1";
            case 2 -> "Ca 2";
            case 3 -> "Ca 3";
            case 4 -> "Ca 4";
            default -> "Ca " + ca;
        };
    }

    private String resolveTrangThai(AttendanceRecord record) {
        String trangThaiHoc = record.getTrangThai();
        String tinhTrang = record.getTinhTrangDiemDanh();

        String trangThaiText = translateTrangThai(trangThaiHoc);
        String tinhTrangText = translateTinhTrang(tinhTrang);

        if (!trangThaiText.isBlank() && !tinhTrangText.isBlank()) {
            return trangThaiText + " - " + tinhTrangText;
        }
        if (!trangThaiText.isBlank()) {
            return trangThaiText;
        }
        return tinhTrangText;
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

    private void initComponents() {

        headerPanel = new javax.swing.JPanel();
        lblTitle = new javax.swing.JLabel();
        btnRefresh = new javax.swing.JButton();
        statsPanel = new javax.swing.JPanel();
        cardTotalStudents = new javax.swing.JPanel();
        lblTotalStudentsTitle = new javax.swing.JLabel();
        lblTotalStudentsValue = new javax.swing.JLabel();
        cardTodayAttendance = new javax.swing.JPanel();
        lblTodayAttendanceTitle = new javax.swing.JLabel();
        lblTodayAttendanceValue = new javax.swing.JLabel();
        cardUnprocessed = new javax.swing.JPanel();
        lblUnprocessedTitle = new javax.swing.JLabel();
        lblUnprocessedValue = new javax.swing.JLabel();
        cardCurrentShift = new javax.swing.JPanel();
        lblCurrentShiftTitle = new javax.swing.JLabel();
        lblCurrentShiftValue = new javax.swing.JLabel();
        chartsContainer = new javax.swing.JPanel();
        pnlChartByShift = new javax.swing.JPanel();
        pnlChartStatus = new javax.swing.JPanel();
        pnlChartHourly = new javax.swing.JPanel();
        tableContainer = new javax.swing.JPanel();
        scrollToday = new javax.swing.JScrollPane();
        tblTodayAttendance = new javax.swing.JTable();

        setBackground(new java.awt.Color(250, 250, 252));

        headerPanel.setOpaque(false);

        lblTitle.setFont(new java.awt.Font("Segoe UI", 1, 20));
        lblTitle.setForeground(new java.awt.Color(33, 37, 41));
        lblTitle.setText("Dashboard - Hệ thống điểm danh RFID");

        btnRefresh.setText("Làm mới");

        javax.swing.GroupLayout headerPanelLayout = new javax.swing.GroupLayout(headerPanel);
        headerPanel.setLayout(headerPanelLayout);
        headerPanelLayout.setHorizontalGroup(
            headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(headerPanelLayout.createSequentialGroup()
                .addComponent(lblTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        headerPanelLayout.setVerticalGroup(
            headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(headerPanelLayout.createSequentialGroup()
                .addGroup(headerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTitle)
                    .addComponent(btnRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
        );

        statsPanel.setOpaque(false);

        configureStatCard(cardTotalStudents);
        lblTotalStudentsTitle.setFont(new java.awt.Font("Segoe UI", 0, 14));
        lblTotalStudentsTitle.setForeground(new java.awt.Color(73, 82, 91));
        lblTotalStudentsTitle.setText("Tổng số sinh viên");

        lblTotalStudentsValue.setFont(new java.awt.Font("Segoe UI", 1, 28));
        lblTotalStudentsValue.setForeground(new java.awt.Color(20, 112, 204));
        lblTotalStudentsValue.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTotalStudentsValue.setText("0");

        javax.swing.GroupLayout cardTotalStudentsLayout = new javax.swing.GroupLayout(cardTotalStudents);
        cardTotalStudents.setLayout(cardTotalStudentsLayout);
        cardTotalStudentsLayout.setHorizontalGroup(
            cardTotalStudentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblTotalStudentsTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(lblTotalStudentsValue, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
        );
        cardTotalStudentsLayout.setVerticalGroup(
            cardTotalStudentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cardTotalStudentsLayout.createSequentialGroup()
                .addComponent(lblTotalStudentsTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblTotalStudentsValue)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        configureStatCard(cardTodayAttendance);
        lblTodayAttendanceTitle.setFont(new java.awt.Font("Segoe UI", 0, 14));
        lblTodayAttendanceTitle.setForeground(new java.awt.Color(73, 82, 91));
        lblTodayAttendanceTitle.setText("Điểm danh hôm nay");

        lblTodayAttendanceValue.setFont(new java.awt.Font("Segoe UI", 1, 28));
        lblTodayAttendanceValue.setForeground(new java.awt.Color(27, 132, 50));
        lblTodayAttendanceValue.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTodayAttendanceValue.setText("0");

        javax.swing.GroupLayout cardTodayAttendanceLayout = new javax.swing.GroupLayout(cardTodayAttendance);
        cardTodayAttendance.setLayout(cardTodayAttendanceLayout);
        cardTodayAttendanceLayout.setHorizontalGroup(
            cardTodayAttendanceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblTodayAttendanceTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(lblTodayAttendanceValue, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
        );
        cardTodayAttendanceLayout.setVerticalGroup(
            cardTodayAttendanceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cardTodayAttendanceLayout.createSequentialGroup()
                .addComponent(lblTodayAttendanceTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblTodayAttendanceValue)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        configureStatCard(cardUnprocessed);
        lblUnprocessedTitle.setFont(new java.awt.Font("Segoe UI", 0, 14));
        lblUnprocessedTitle.setForeground(new java.awt.Color(73, 82, 91));
        lblUnprocessedTitle.setText("RFID chưa xử lý");

        lblUnprocessedValue.setFont(new java.awt.Font("Segoe UI", 1, 28));
        lblUnprocessedValue.setForeground(new java.awt.Color(204, 142, 0));
        lblUnprocessedValue.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblUnprocessedValue.setText("0");

        javax.swing.GroupLayout cardUnprocessedLayout = new javax.swing.GroupLayout(cardUnprocessed);
        cardUnprocessed.setLayout(cardUnprocessedLayout);
        cardUnprocessedLayout.setHorizontalGroup(
            cardUnprocessedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblUnprocessedTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(lblUnprocessedValue, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
        );
        cardUnprocessedLayout.setVerticalGroup(
            cardUnprocessedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cardUnprocessedLayout.createSequentialGroup()
                .addComponent(lblUnprocessedTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblUnprocessedValue)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        configureStatCard(cardCurrentShift);
        lblCurrentShiftTitle.setFont(new java.awt.Font("Segoe UI", 0, 14));
        lblCurrentShiftTitle.setForeground(new java.awt.Color(73, 82, 91));
        lblCurrentShiftTitle.setText("Ca hiện tại");

        lblCurrentShiftValue.setFont(new java.awt.Font("Segoe UI", 1, 28));
        lblCurrentShiftValue.setForeground(new java.awt.Color(0, 102, 204));
        lblCurrentShiftValue.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCurrentShiftValue.setText("Ngoài giờ học");

        javax.swing.GroupLayout cardCurrentShiftLayout = new javax.swing.GroupLayout(cardCurrentShift);
        cardCurrentShift.setLayout(cardCurrentShiftLayout);
        cardCurrentShiftLayout.setHorizontalGroup(
            cardCurrentShiftLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblCurrentShiftTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(lblCurrentShiftValue, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
        );
        cardCurrentShiftLayout.setVerticalGroup(
            cardCurrentShiftLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cardCurrentShiftLayout.createSequentialGroup()
                .addComponent(lblCurrentShiftTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblCurrentShiftValue)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout statsPanelLayout = new javax.swing.GroupLayout(statsPanel);
        statsPanel.setLayout(statsPanelLayout);
        statsPanelLayout.setHorizontalGroup(
            statsPanelLayout.createSequentialGroup()
                .addComponent(cardTotalStudents, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cardTodayAttendance, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cardUnprocessed, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cardCurrentShift, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        statsPanelLayout.setVerticalGroup(
            statsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(cardTotalStudents, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(cardTodayAttendance, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(cardUnprocessed, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(cardCurrentShift, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        chartsContainer.setOpaque(false);

        pnlChartByShift.setLayout(new java.awt.BorderLayout());
        pnlChartStatus.setLayout(new java.awt.BorderLayout());
        pnlChartHourly.setLayout(new java.awt.BorderLayout());

        pnlChartByShift.setBorder(javax.swing.BorderFactory.createTitledBorder("Thống kê điểm danh theo ca học"));
        pnlChartStatus.setBorder(javax.swing.BorderFactory.createTitledBorder("Phân bố trạng thái điểm danh"));
        pnlChartHourly.setBorder(javax.swing.BorderFactory.createTitledBorder("Xu hướng điểm danh theo giờ"));

        javax.swing.GroupLayout chartsContainerLayout = new javax.swing.GroupLayout(chartsContainer);
        chartsContainer.setLayout(chartsContainerLayout);
        chartsContainerLayout.setHorizontalGroup(
            chartsContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(pnlChartHourly, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(chartsContainerLayout.createSequentialGroup()
                    .addComponent(pnlChartByShift, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(pnlChartStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        chartsContainerLayout.setVerticalGroup(
            chartsContainerLayout.createSequentialGroup()
                .addGroup(chartsContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnlChartByShift, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pnlChartStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(pnlChartHourly, javax.swing.GroupLayout.PREFERRED_SIZE, 320, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        tableContainer.setBorder(javax.swing.BorderFactory.createTitledBorder("Điểm danh hôm nay"));

        scrollToday.setViewportView(tblTodayAttendance);

        javax.swing.GroupLayout tableContainerLayout = new javax.swing.GroupLayout(tableContainer);
        tableContainer.setLayout(tableContainerLayout);
        tableContainerLayout.setHorizontalGroup(
            tableContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(scrollToday, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
                .addComponent(paginationPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        tableContainerLayout.setVerticalGroup(
            tableContainerLayout.createSequentialGroup()
                .addComponent(scrollToday, javax.swing.GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(paginationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(headerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(statsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(chartsContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(tableContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(statsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(chartsContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(tableContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }

    private void configureStatCard(JPanel panel) {
        panel.setBackground(Color.WHITE);
        panel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new Color(230, 234, 238)),
                javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12)));
    }

    private static class DashboardData {
        List<Student> students;
        List<AttendanceRecord> todayAttendance;
        List<RfidEvent> unprocessedRfids;
    }

    private class AttendanceTableModel extends AbstractTableModel {
        private final String[] columns = {"RFID", "Mã SV", "Tên sinh viên", "Ca", "Giờ vào", "Giờ ra", "Trạng thái"};
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
                case 3 -> resolveCaName(record.getCa());
                case 4 -> record.getGioVao() != null ? TIME_FORMATTER.format(record.getGioVao()) : "";
                case 5 -> record.getGioRa() != null ? TIME_FORMATTER.format(record.getGioRa()) : "";
                case 6 -> resolveTrangThai(record);
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 3) {
                return String.class;
            }
            return super.getColumnClass(columnIndex);
        }
    }

    // Variables declaration - do not modify
    private javax.swing.JButton btnRefresh;
    private javax.swing.JPanel cardCurrentShift;
    private javax.swing.JPanel cardTodayAttendance;
    private javax.swing.JPanel cardTotalStudents;
    private javax.swing.JPanel cardUnprocessed;
    private javax.swing.JPanel chartsContainer;
    private javax.swing.JPanel headerPanel;
    private javax.swing.JLabel lblCurrentShiftTitle;
    private javax.swing.JLabel lblCurrentShiftValue;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JLabel lblTodayAttendanceTitle;
    private javax.swing.JLabel lblTodayAttendanceValue;
    private javax.swing.JLabel lblTotalStudentsTitle;
    private javax.swing.JLabel lblTotalStudentsValue;
    private javax.swing.JLabel lblUnprocessedTitle;
    private javax.swing.JLabel lblUnprocessedValue;
    private javax.swing.JPanel pnlChartByShift;
    private javax.swing.JPanel pnlChartHourly;
    private javax.swing.JPanel pnlChartStatus;
    private javax.swing.JScrollPane scrollToday;
    private javax.swing.JPanel statsPanel;
    private javax.swing.JPanel tableContainer;
    private javax.swing.JTable tblTodayAttendance;
    // End of variables declaration
}

