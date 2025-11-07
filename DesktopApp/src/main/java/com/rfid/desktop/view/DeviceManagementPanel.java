package com.rfid.desktop.view;

import com.rfid.desktop.model.Device;
import com.rfid.desktop.model.RfidEvent;
import com.rfid.desktop.service.ApplicationContext;
import com.rfid.desktop.service.AttendanceService;
import com.rfid.desktop.service.DeviceService;
import com.rfid.desktop.view.components.PaginationPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DeviceManagementPanel extends javax.swing.JPanel {

    private static final int PAGE_SIZE = 10;

    private final AttendanceService attendanceService;
    private final DeviceService deviceService;

    private final JTable rfidTable = new JTable();
    private final RfidTableModel rfidTableModel = new RfidTableModel();
    private final PaginationPanel rfidPagination = new PaginationPanel();
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"Tất cả", "Đã đăng ký", "Chưa đăng ký"});
    private final JButton btnTogglePolling = new JButton("Quét RFID");
    private final JButton btnRefreshRfids = new JButton("Làm mới");

    private final JTable deviceTable = new JTable();
    private final DeviceTableModel deviceTableModel = new DeviceTableModel();
    private final JTextField txtMaThietBi = new JTextField();
    private final JTextField txtPhongHoc = new JTextField();
    private final JButton btnSaveDevice = new JButton("Lưu thiết bị");
    private final JButton btnRefreshDevices = new JButton("Làm mới");
    private final JButton btnDeleteDevice = new JButton("Xóa thiết bị");

    private final JLabelStatus statusLabel = new JLabelStatus();

    private final Timer pollingTimer;
    private boolean polling;
    private List<RfidEvent> allRfidEvents = new ArrayList<>();

    public DeviceManagementPanel(ApplicationContext context) {
        this.attendanceService = context.getAttendanceService();
        this.deviceService = context.getDeviceService();

        initComponents();
        configureComponents();
        attachListeners();

        pollingTimer = new Timer(1200, e -> loadRfids());
        loadRfids();
        loadDevices();
    }

    private void initComponents() {
        setBackground(new Color(250, 250, 252));
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Quét RFID", buildRfidPanel());
        tabs.addTab("Thiết bị", buildDevicePanel());

        add(tabs, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private javax.swing.JPanel buildRfidPanel() {
        javax.swing.JPanel panel = new javax.swing.JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        javax.swing.JPanel toolbar = new javax.swing.JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.setOpaque(false);
        toolbar.add(statusFilter);
        toolbar.add(btnTogglePolling);
        toolbar.add(btnRefreshRfids);

        rfidTable.setModel(rfidTableModel);
        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(rfidTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("RFID chưa xử lý"));

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(rfidPagination, BorderLayout.SOUTH);
        return panel;
    }

    private javax.swing.JPanel buildDevicePanel() {
        javax.swing.JPanel panel = new javax.swing.JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        javax.swing.JPanel form = new javax.swing.JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        form.setBorder(BorderFactory.createTitledBorder("Đăng ký thiết bị"));
        txtMaThietBi.setColumns(14);
        txtPhongHoc.setColumns(14);
        form.add(new javax.swing.JLabel("Mã thiết bị:"));
        form.add(txtMaThietBi);
        form.add(new javax.swing.JLabel("Phòng học:"));
        form.add(txtPhongHoc);
        form.add(btnSaveDevice);
        form.add(btnRefreshDevices);
        form.add(btnDeleteDevice);

        deviceTable.setModel(deviceTableModel);
        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(deviceTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Thiết bị đã đăng ký"));

        panel.add(form, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void configureComponents() {
        rfidTable.setFillsViewportHeight(true);
        rfidTable.setRowHeight(26);
        rfidTable.setAutoCreateRowSorter(true);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        rfidTable.setDefaultRenderer(Integer.class, center);

        rfidPagination.setPageSize(PAGE_SIZE);
        rfidPagination.setPageChangeListener(this::showRfidPage);

        deviceTable.setFillsViewportHeight(true);
        deviceTable.setRowHeight(26);
        deviceTable.setAutoCreateRowSorter(true);

        statusFilter.setPreferredSize(new Dimension(180, 28));
    }

    private void attachListeners() {
        btnTogglePolling.addActionListener(e -> {
            polling = !polling;
            if (polling) {
                btnTogglePolling.setText("Dừng quét");
                pollingTimer.start();
                setStatus("Đang quét RFID...");
            } else {
                btnTogglePolling.setText("Quét RFID");
                pollingTimer.stop();
                setStatus("Đã dừng quét RFID");
            }
        });

        btnRefreshRfids.addActionListener(e -> loadRfids());
        statusFilter.addActionListener(e -> {
            rfidPagination.reset(filteredRfidEvents().size());
            showRfidPage(1);
        });

        btnSaveDevice.addActionListener(e -> saveDevice());
        btnRefreshDevices.addActionListener(e -> loadDevices());
        btnDeleteDevice.addActionListener(e -> deleteSelectedDevice());

        JPopupMenu menu = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem("Copy RFID");
        JMenuItem markItem = new JMenuItem("Đánh dấu đã xử lý");
        menu.add(copyItem);
        menu.add(markItem);
        copyItem.addActionListener(e -> {
            RfidEvent selected = getSelectedRfid();
            if (selected != null) {
                copyRfid(selected.getRfid());
            }
        });
        markItem.addActionListener(e -> {
            RfidEvent selected = getSelectedRfid();
            if (selected != null) {
                markProcessed(selected.getId());
            }
        });
        rfidTable.setComponentPopupMenu(menu);
    }

    private void loadRfids() {
        new SwingWorker<List<RfidEvent>, Void>() {
            @Override
            protected List<RfidEvent> doInBackground() {
                try {
                    return attendanceService.getUnprocessedRfids();
                } catch (IOException e) {
                    return List.of();
                }
            }

            @Override
            protected void done() {
                try {
                    allRfidEvents = new ArrayList<>(get());
                    rfidPagination.reset(filteredRfidEvents().size());
                    showRfidPage(rfidPagination.getCurrentPage());
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void loadDevices() {
        new SwingWorker<List<Device>, Void>() {
            @Override
            protected List<Device> doInBackground() {
                try {
                    return deviceService.getAll();
                } catch (IOException e) {
                    return List.of();
                }
            }

            @Override
            protected void done() {
                try {
                    deviceTableModel.setData(get());
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private List<RfidEvent> filteredRfidEvents() {
        String filter = (String) statusFilter.getSelectedItem();
        List<RfidEvent> filtered = new ArrayList<>();
        for (RfidEvent event : allRfidEvents) {
            boolean processed = event.getProcessed() != null && event.getProcessed();
            if ("Đã đăng ký".equals(filter) && !processed) {
                continue;
            }
            if ("Chưa đăng ký".equals(filter) && processed) {
                continue;
            }
            filtered.add(event);
        }
        return filtered;
    }

    private void showRfidPage(int page) {
        List<RfidEvent> list = filteredRfidEvents();
        if (list.isEmpty()) {
            rfidTableModel.setData(List.of());
            rfidPagination.setCurrentPage(1, false);
            return;
        }
        int pageSize = rfidPagination.getPageSize();
        int total = list.size();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        page = Math.max(1, Math.min(totalPages, page));
        rfidPagination.setCurrentPage(page, false);

        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        rfidTableModel.setData(list.subList(from, to));
        setStatus(String.format("RFID chưa xử lý: %d (trang %d/%d)", list.size(), page, totalPages));
    }

    private void saveDevice() {
        String ma = txtMaThietBi.getText().trim();
        String phong = txtPhongHoc.getText().trim();
        if (ma.isEmpty() || phong.isEmpty()) {
            setStatus("Vui lòng nhập đủ mã thiết bị và phòng học");
            return;
        }

        Device device = new Device();
        device.setMaThietBi(ma);
        device.setPhongHoc(phong);

        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    deviceService.create(device);
                } catch (IOException ex) {
                    error = ex;
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    setStatus("Lỗi: " + error.getMessage());
                } else {
                    setStatus("Đã lưu thiết bị");
                    txtMaThietBi.setText("");
                    txtPhongHoc.setText("");
                    loadDevices();
                }
            }
        }.execute();
    }

    private void markProcessed(long id) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                attendanceService.markProcessed(id);
                return null;
            }

            @Override
            protected void done() {
                loadRfids();
            }
        }.execute();
    }

    private void copyRfid(String rfid) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(rfid), null);
        setStatus("Đã copy RFID " + rfid);
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private RfidEvent getSelectedRfid() {
        int viewRow = rfidTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = rfidTable.convertRowIndexToModel(viewRow);
        return rfidTableModel.getEvent(modelRow);
    }

    private void deleteSelectedDevice() {
        int viewRow = deviceTable.getSelectedRow();
        if (viewRow < 0) {
            setStatus("Vui lòng chọn thiết bị");
            return;
        }
        int modelRow = deviceTable.convertRowIndexToModel(viewRow);
        Device device = deviceTableModel.getDevice(modelRow);
        if (device == null) {
            return;
        }

        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    deviceService.delete(device.getMaThietBi());
                } catch (IOException ex) {
                    error = ex;
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    setStatus("Không thể xóa thiết bị: " + error.getMessage());
                } else {
                    setStatus("Đã xóa thiết bị " + device.getMaThietBi());
                    loadDevices();
                }
            }
        }.execute();
    }

    private class RfidTableModel extends AbstractTableModel {

        private final String[] columns = {"ID", "RFID", "Mã SV", "Tên sinh viên", "Thời gian", "Trạng thái", ""};
        private List<RfidEvent> data = new ArrayList<>();
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        void setData(List<RfidEvent> data) {
            this.data = new ArrayList<>(data);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return data.size();
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
            RfidEvent event = data.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> event.getId();
                case 1 -> event.getRfid();
                case 2 -> event.getMaSinhVien();
                case 3 -> event.getTenSinhVien();
                case 4 -> event.getCreatedAt() != null ? formatter.format(event.getCreatedAt()) : "";
                case 5 -> Boolean.TRUE.equals(event.getProcessed()) ? "Đã đăng ký" : "Chưa đăng ký";
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return Long.class;
            }
            return super.getColumnClass(columnIndex);
        }

        RfidEvent getEvent(int rowIndex) {
            if (rowIndex < 0 || rowIndex >= data.size()) {
                return null;
            }
            return data.get(rowIndex);
        }
    }

    private class DeviceTableModel extends AbstractTableModel {

        private final String[] columns = {"Mã thiết bị", "Phòng học"};
        private List<Device> data = new ArrayList<>();

        void setData(List<Device> data) {
            this.data = new ArrayList<>(data);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return data.size();
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
            Device device = data.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> device.getMaThietBi();
                case 1 -> device.getPhongHoc();
                default -> "";
            };
        }

        Device getDevice(int rowIndex) {
            if (rowIndex < 0 || rowIndex >= data.size()) {
                return null;
            }
            return data.get(rowIndex);
        }
    }

    private class JLabelStatus extends javax.swing.JLabel {
        JLabelStatus() {
            setForeground(new Color(90, 90, 90));
            setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        }
    }
}

