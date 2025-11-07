package com.rfid.desktop.view;

import com.rfid.desktop.model.LopHocPhan;
import com.rfid.desktop.model.RfidEvent;
import com.rfid.desktop.model.Student;
import com.rfid.desktop.service.ApplicationContext;
import com.rfid.desktop.service.AttendanceService;
import com.rfid.desktop.service.LopHocPhanService;
import com.rfid.desktop.service.StudentService;
import com.rfid.desktop.view.components.PaginationPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileInputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class StudentManagementPanel extends JPanel {

    private static final int ROWS_PER_PAGE = 15;

    private final StudentService studentService;
    private final LopHocPhanService lopHocPhanService;
    private final AttendanceService attendanceService;

    private final StudentTableModel tableModel = new StudentTableModel();
    private final JTable table = new JTable(tableModel);
    private final PaginationPanel paginationPanel = new PaginationPanel();

    private final JTextField searchField = new JTextField(20);
 
    
    private final JButton refreshButton = new JButton("Làm mới");
    private final JButton addButton = new JButton("Thêm mới");
    private final JButton editButton = new JButton("Sửa");
    private final JButton deleteButton = new JButton("Xóa");
    private final JButton importButton = new JButton("Import RFID");

    private final JLabel statusLabel = new JLabel(" ");

    private List<Student> allStudents = new ArrayList<>();
    private List<Student> filteredStudents = new ArrayList<>();

    public StudentManagementPanel(ApplicationContext context) {
        this.studentService = context.getStudentService();
        this.lopHocPhanService = context.getLopHocPhanService();
        this.attendanceService = context.getAttendanceService();

        initComponents();
        configureComponents();
        attachListeners();

        reloadData();
      
    }

    private void initComponents() {
        setBackground(new java.awt.Color(250, 250, 252));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        javax.swing.JPanel headerPanel = new javax.swing.JPanel();
        headerPanel.setOpaque(false);
        javax.swing.JLabel lblTitle = new javax.swing.JLabel();
        lblTitle.setFont(new java.awt.Font("Segoe UI", 1, 20));
        lblTitle.setForeground(new java.awt.Color(33, 37, 41));
        lblTitle.setText("Quản lý sinh viên");

        javax.swing.GroupLayout headerLayout = new javax.swing.GroupLayout(headerPanel);
        headerPanel.setLayout(headerLayout);
        headerLayout.setHorizontalGroup(
            headerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(lblTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        headerLayout.setVerticalGroup(
            headerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(lblTitle)
        );

        javax.swing.JPanel filterPanel = new javax.swing.JPanel();
        filterPanel.setOpaque(false);
        javax.swing.JLabel lblSearch = new javax.swing.JLabel("Tìm kiếm:");
        

        javax.swing.GroupLayout filterLayout = new javax.swing.GroupLayout(filterPanel);
        filterPanel.setLayout(filterLayout);
        filterLayout.setHorizontalGroup(
            filterLayout.createSequentialGroup()
                .addComponent(lblSearch)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
         
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(refreshButton, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        filterLayout.setVerticalGroup(
            filterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(lblSearch)
                .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
            
                .addComponent(refreshButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        javax.swing.JPanel actionPanel = new javax.swing.JPanel();
        actionPanel.setOpaque(false);

        javax.swing.GroupLayout actionLayout = new javax.swing.GroupLayout(actionPanel);
        actionPanel.setLayout(actionLayout);
        actionLayout.setHorizontalGroup(
            actionLayout.createSequentialGroup()
                .addComponent(addButton, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(editButton, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(deleteButton, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(importButton, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE)
        );
        actionLayout.setVerticalGroup(
            actionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(addButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(editButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(deleteButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(importButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        javax.swing.JPanel controlsPanel = new javax.swing.JPanel();
        controlsPanel.setOpaque(false);
        javax.swing.GroupLayout controlsLayout = new javax.swing.GroupLayout(controlsPanel);
        controlsPanel.setLayout(controlsLayout);
        controlsLayout.setHorizontalGroup(
            controlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(filterPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(actionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        controlsLayout.setVerticalGroup(
            controlsLayout.createSequentialGroup()
                .addComponent(filterPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(actionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        javax.swing.JPanel tableContainer = new javax.swing.JPanel();
        tableContainer.setBorder(BorderFactory.createTitledBorder("Danh sách sinh viên"));
        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane();
        scrollPane.setViewportView(table);

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

        statusLabel.setBorder(new EmptyBorder(4, 0, 0, 0));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(headerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(controlsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(tableContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(statusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(controlsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(tableContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(statusLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
    }

    private void configureComponents() {
        table.setFillsViewportHeight(true);
        table.setRowHeight(26);
        table.setAutoCreateRowSorter(true);

        paginationPanel.setPageSize(ROWS_PER_PAGE);
        paginationPanel.setPageChangeListener(this::showPage);

        searchField.setColumns(25);
       

        statusLabel.setText(" ");
        statusLabel.setForeground(new java.awt.Color(90, 90, 90));
    }

    private void attachListeners() {
        refreshButton.addActionListener(e -> reloadData());
        addButton.addActionListener(e -> openFormDialog(null));
        editButton.addActionListener(e -> {
            Student selected = getSelectedStudent();
            if (selected != null) {
                openFormDialog(selected);
            }
        });
        deleteButton.addActionListener(e -> deleteSelectedStudent());
        importButton.addActionListener(e -> importStudents());
        searchField.addActionListener(e -> applyFilters());
       
    }

    private void showPage(int page) {
        if (filteredStudents.isEmpty()) {
            tableModel.setStudents(List.of());
            paginationPanel.setCurrentPage(1, false);
            return;
        }

        int pageSize = paginationPanel.getPageSize();
        int totalItems = filteredStudents.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        page = Math.max(1, Math.min(totalPages, page));
        paginationPanel.setCurrentPage(page, false);

        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalItems);
        List<Student> pageData = filteredStudents.subList(fromIndex, toIndex);
        tableModel.setStudents(pageData);
        setStatus(String.format("Đang hiển thị %d - %d trên tổng %d sinh viên", fromIndex + 1, toIndex, totalItems));
    }

    private void updateFilteredStudents(List<Student> students) {
        this.filteredStudents = new ArrayList<>(students);
        paginationPanel.reset(filteredStudents.size());
        if (filteredStudents.isEmpty()) {
            tableModel.setStudents(List.of());
            setStatus("Không tìm thấy sinh viên phù hợp");
        } else {
            showPage(paginationPanel.getCurrentPage());
        }
    }

    private void reloadData() {
        setStatus("Đang tải dữ liệu sinh viên...");
        new SwingWorker<List<Student>, Void>() {
            private Exception error;

            @Override
            protected List<Student> doInBackground() {
                try {
                    return studentService.getAll();
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
                    allStudents = get();
                    applyFilters();
                } catch (Exception e) {
                    setStatus("Lỗi: " + e.getMessage());
                }
            }
        }.execute();
    }

    

    private void applyFilters() {
        String keyword = searchField.getText().trim().toLowerCase();
       

        List<Student> filtered = allStudents.stream()
                .filter(student -> keyword.isBlank() ||
                        student.getMaSinhVien().toLowerCase().contains(keyword) ||
                        student.getTenSinhVien().toLowerCase().contains(keyword))
                .collect(Collectors.toList());

        
    }

    private void deleteSelectedStudent() {
        Student student = getSelectedStudent();
        if (student == null) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa sinh viên " + student.getTenSinhVien() + "?",
                "Xóa sinh viên",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        setStatus("Đang xóa sinh viên...");
        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    studentService.delete(student.getMaSinhVien());
                } catch (Exception ex) {
                    error = ex;
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    setStatus("Lỗi: " + error.getMessage());
                } else {
                    setStatus("Đã xóa sinh viên");
                    reloadData();
                }
            }
        }.execute();
    }

    private Student getSelectedStudent() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một sinh viên", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return tableModel.getStudent(modelRow);
    }

    private void openFormDialog(Student student) {
        StudentFormDialog dialog = new StudentFormDialog(student);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        dialog.getResult().ifPresent(result -> {
            setStatus("Đang lưu thông tin sinh viên...");
            new SwingWorker<Student, Void>() {
                private Exception error;

                @Override
                protected Student doInBackground() {
                    try {
                        if (student == null) {
                            return studentService.create(result);
                        }
                        return studentService.update(student.getMaSinhVien(), result);
                    } catch (Exception ex) {
                        error = ex;
                        return null;
                    }
                }

                @Override
                protected void done() {
                    if (error != null) {
                        setStatus("Lỗi: " + error.getMessage());
                    } else {
                        setStatus("Lưu thông tin thành công");
                        reloadData();
                    }
                }
            }.execute();
        });
    }

    private void importStudents() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog progressDialog = new JDialog(owner, "Đang import", true);
        progressDialog.setLayout(new BorderLayout(10, 10));
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressDialog.add(new JLabel("Đang xử lý file..."), BorderLayout.NORTH);
        progressDialog.add(progressBar, BorderLayout.CENTER);
        progressDialog.setSize(280, 120);
        progressDialog.setLocationRelativeTo(this);

        new SwingWorker<String, Void>() {
            private Exception error;

            @Override
            protected String doInBackground() {
                try (FileInputStream fis = new FileInputStream(file); Workbook workbook = new XSSFWorkbook(fis)) {
                    Sheet sheet = workbook.getSheetAt(0);
                    List<Student> students = new ArrayList<>();
                    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null) {
                            continue;
                        }
                        String maSinhVien = readCell(row, 0);
                        String tenSinhVien = readCell(row, 1);
                        String rfid = readCell(row, 2);
                        if (maSinhVien == null || tenSinhVien == null || rfid == null) {
                            continue;
                        }
                        Student student = new Student();
                        student.setMaSinhVien(maSinhVien);
                        student.setTenSinhVien(tenSinhVien);
                        student.setRfid(rfid);
                        students.add(student);
                    }
                    if (students.isEmpty()) {
                        throw new IllegalStateException("Dữ liệu trong file không hợp lệ");
                    }
                    studentService.bulkUpdate(students);
                    return "Đã import " + students.size() + " sinh viên";
                } catch (Exception ex) {
                    error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                if (error != null) {
                    setStatus("Lỗi import: " + error.getMessage());
                } else {
                    try {
                        setStatus(get());
                    } catch (Exception e) {
                        setStatus("Import thành công");
                    }
                    reloadData();
                }
            }
        }.execute();

        progressDialog.setVisible(true);
    }

    private String readCell(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        DataFormatter formatter = new DataFormatter();
        String value = formatter.formatCellValue(cell);
        return value != null ? value.trim() : null;
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private class StudentFormDialog extends JDialog {
        private final JTextField maSinhVienField = new JTextField(20);
        private final JTextField tenSinhVienField = new JTextField(20);
        private final JTextField rfidField = new JTextField(20);
        private final JButton scanButton = new JButton("Quét RFID");
        private final JLabel scanStatusLabel = new JLabel(" ");

        private final Timer scanTimer;
        private Optional<Student> result = Optional.empty();
        private final Student editing;

        StudentFormDialog(Student editing) {
            super((Frame) SwingUtilities.getWindowAncestor(StudentManagementPanel.this),
                    editing == null ? "Thêm sinh viên" : "Cập nhật sinh viên",
                    true);
            this.editing = editing;
            setLayout(new BorderLayout(10, 10));
            setSize(420, 320);

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 6, 6, 6);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridx = 0;
            gbc.gridy = 0;

            form.add(new JLabel("Mã sinh viên"), gbc);
            gbc.gridx = 1;
            maSinhVienField.setText(editing != null ? editing.getMaSinhVien() : "");
            maSinhVienField.setEnabled(editing == null);
            form.add(maSinhVienField, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            form.add(new JLabel("Tên sinh viên"), gbc);
            gbc.gridx = 1;
            tenSinhVienField.setText(editing != null ? editing.getTenSinhVien() : "");
            form.add(tenSinhVienField, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            form.add(new JLabel("RFID"), gbc);
            gbc.gridx = 1;
            form.add(rfidField, gbc);

            gbc.gridy++;
            gbc.gridx = 1;
            scanButton.addActionListener(e -> toggleScanning());
            JPanel scanPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            scanPanel.add(scanButton);
            scanPanel.add(Box.createHorizontalStrut(10));
            scanPanel.add(scanStatusLabel);
            form.add(scanPanel, gbc);

            add(form, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton cancel = new JButton("Hủy");
            JButton save = new JButton("Lưu");
            cancel.addActionListener(e -> dispose());
            save.addActionListener(e -> onSave(editing));
            buttons.add(cancel);
            buttons.add(save);
            add(buttons, BorderLayout.SOUTH);

            scanTimer = new Timer(1000, e -> pollLatestRfid());

            if (editing != null) {
                rfidField.setText(editing.getRfid());
            }
        }

        private void onSave(Student original) {
            if (maSinhVienField.getText().isBlank() || tenSinhVienField.getText().isBlank() || rfidField.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ thông tin", "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Student student = new Student();
            student.setMaSinhVien(maSinhVienField.getText().trim());
            student.setTenSinhVien(tenSinhVienField.getText().trim());
            student.setRfid(rfidField.getText().trim());
            result = Optional.of(student);
            dispose();
        }

        private void toggleScanning() {
            if (scanTimer.isRunning()) {
                scanTimer.stop();
                scanButton.setText("Quét RFID");
                scanStatusLabel.setText(" ");
            } else {
                scanTimer.start();
                scanButton.setText("Dừng quét");
                scanStatusLabel.setText("Đang quét...");
            }
        }

        private void pollLatestRfid() {
            new SwingWorker<RfidSelection, Void>() {
                @Override
                protected RfidSelection doInBackground() throws Exception {
                    List<RfidEvent> events = attendanceService.getUnprocessedRfids();
                    events = events.stream()
                            .filter(event -> Boolean.FALSE.equals(event.getProcessed()))
                            .sorted(Comparator.comparing(RfidEvent::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                            .toList();

                    RfidSelection selection = new RfidSelection();
                    for (RfidEvent event : events) {
                        String rfid = event.getRfid();
                        if (rfid == null) {
                            continue;
                        }
                        boolean exists = allStudents.stream()
                                .anyMatch(s -> s.getRfid() != null && s.getRfid().equalsIgnoreCase(rfid)
                                        && (editing == null || !s.getMaSinhVien().equals(editing.getMaSinhVien())));
                        if (exists) {
                            selection.message = "RFID " + rfid + " đã được đăng ký";
                            continue;
                        }
                        selection.event = event;
                        selection.message = "Đã quét RFID: " + rfid;
                        break;
                    }
                    return selection;
                }

                @Override
                protected void done() {
                    try {
                        RfidSelection selection = get();
                        if (selection == null) {
                            return;
                        }
                        if (selection.event != null) {
                            String rfid = selection.event.getRfid();
                            rfidField.setText(rfid);
                            scanStatusLabel.setText(selection.message);
                            scanTimer.stop();
                            scanButton.setText("Quét lại");
                            markEventProcessed(selection.event.getId());
                        } else if (selection.message != null) {
                            scanStatusLabel.setText(selection.message);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }.execute();
        }

        private void markEventProcessed(Long id) {
            if (id == null) {
                return;
            }
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    try {
                        attendanceService.markProcessed(id);
                    } catch (Exception ignored) {
                    }
                    return null;
                }
            }.execute();
        }

        Optional<Student> getResult() {
            return result;
        }

        @Override
        public void dispose() {
            if (scanTimer.isRunning()) {
                scanTimer.stop();
            }
            super.dispose();
        }
    }

    private static class RfidSelection {
        RfidEvent event;
        String message;
    }

    private static class StudentTableModel extends AbstractTableModel {
        private final String[] headers = {"Mã sinh viên", "RFID", "Tên sinh viên", "Ngày tạo"};
        private List<Student> students = new ArrayList<>();
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        void setStudents(List<Student> students) {
            this.students = new ArrayList<>(students);
            fireTableDataChanged();
        }

        Student getStudent(int rowIndex) {
            return students.get(rowIndex);
        }

        @Override
        public int getRowCount() {
            return students.size();
        }

        @Override
        public int getColumnCount() {
            return headers.length;
        }

        @Override
        public String getColumnName(int column) {
            return headers[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Student student = students.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> student.getMaSinhVien();
                case 1 -> student.getRfid();
                case 2 -> student.getTenSinhVien();
                case 3 -> student.getCreatedAt() != null ? student.getCreatedAt().format(formatter) : "";
                default -> "";
            };
        }
    }
}

