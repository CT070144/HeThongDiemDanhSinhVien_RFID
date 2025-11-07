package com.rfid.desktop.view;

import com.rfid.desktop.model.LopHocPhan;
import com.rfid.desktop.model.PagedResult;
import com.rfid.desktop.model.Student;
import com.rfid.desktop.service.ApplicationContext;
import com.rfid.desktop.service.LopHocPhanService;
import com.rfid.desktop.service.StudentService;
import com.rfid.desktop.view.components.PaginationPanel;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ClassManagementPanel extends javax.swing.JPanel {

    private static final int PAGE_SIZE = 10;

    private final LopHocPhanService lopHocPhanService;
    private final StudentService studentService;

    private final JTextField txtSearch = new JTextField();
    private final JButton btnSearch = new JButton("Tìm kiếm");
    private final JButton btnRefresh = new JButton("Làm mới");
    private final JButton btnCreate = new JButton("Thêm lớp");
    private final JButton btnEdit = new JButton("Sửa");
    private final JButton btnDelete = new JButton("Xóa");
    private final JButton btnViewStudents = new JButton("Sinh viên");
    private final JLabel lblStatus = new JLabel(" ");

    private final JTable table = new JTable();
    private final ClassTableModel tableModel = new ClassTableModel();
    private final PaginationPanel paginationPanel = new PaginationPanel();

    private List<LopHocPhan> currentClasses = new ArrayList<>();
    private long totalElements;
    private int currentPage = 1;

    public ClassManagementPanel(ApplicationContext context) {
        this.lopHocPhanService = context.getLopHocPhanService();
        this.studentService = context.getStudentService();

        initComponents();
        configureComponents();
        attachListeners();
        loadClasses(1);
    }

    private void initComponents() {
        setBackground(new java.awt.Color(250, 250, 252));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        javax.swing.JPanel headerPanel = new javax.swing.JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        javax.swing.JLabel lblTitle = new javax.swing.JLabel("Quản lý lớp học phần");
        lblTitle.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 20));
        lblTitle.setForeground(new java.awt.Color(33, 37, 41));
        headerPanel.add(lblTitle, BorderLayout.WEST);

        javax.swing.JPanel toolbar = new javax.swing.JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        txtSearch.setPreferredSize(new Dimension(220, 30));
        toolbar.add(txtSearch);
        toolbar.add(btnSearch);
        toolbar.add(btnRefresh);
        toolbar.add(btnCreate);
        toolbar.add(btnEdit);
        toolbar.add(btnDelete);
        toolbar.add(btnViewStudents);

        table.setModel(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder("Danh sách lớp học phần"));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(headerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(toolbar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(paginationPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(toolbar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(paginationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
        table.setDefaultRenderer(Integer.class, centerRenderer);

        paginationPanel.setPageSize(PAGE_SIZE);
        paginationPanel.setPageChangeListener(this::loadClasses);

        lblStatus.setForeground(new java.awt.Color(90, 90, 90));
    }

    private void attachListeners() {
        btnSearch.addActionListener(e -> {
            currentPage = 1;
            loadClasses(currentPage);
        });
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            currentPage = 1;
            loadClasses(currentPage);
        });
        btnCreate.addActionListener(e -> openClassDialog(null));
        btnEdit.addActionListener(e -> {
            LopHocPhan selected = getSelectedClass();
            if (selected != null) {
                openClassDialog(selected);
            }
        });
        btnDelete.addActionListener(e -> deleteSelectedClass());
        btnViewStudents.addActionListener(e -> viewStudents());
    }

    private void loadClasses(int page) {
        currentPage = page;
        setStatus("Đang tải dữ liệu lớp học phần...");
        new SwingWorker<PagedResult<LopHocPhan>, Void>() {
            private Exception error;

            @Override
            protected PagedResult<LopHocPhan> doInBackground() {
                try {
                    return lopHocPhanService.getPaged(page - 1, PAGE_SIZE, txtSearch.getText().trim());
                } catch (IOException ex) {
                    error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    setStatus("Lỗi: " + error.getMessage());
                    return;
                }
                try {
                    PagedResult<LopHocPhan> result = get();
                    currentClasses = result != null ? result.getContent() : List.of();
                    tableModel.setData(currentClasses);
                    totalElements = result != null ? result.getTotalElements() : 0;
                    paginationPanel.reset((int) totalElements);
                    paginationPanel.setCurrentPage(page, false);
                    setStatus(String.format("Đang hiển thị trang %d/%d (Tổng %d lớp)",
                            page,
                            result != null ? result.getTotalPages() : 1,
                            totalElements));
                } catch (Exception ex) {
                    setStatus("Lỗi: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void openClassDialog(LopHocPhan lopHocPhan) {
        ClassDialog dialog = new ClassDialog(lopHocPhan);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadClasses(currentPage);
        }
    }

    private void deleteSelectedClass() {
        LopHocPhan lopHocPhan = getSelectedClass();
        if (lopHocPhan == null) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa lớp học phần " + lopHocPhan.getTenLopHocPhan() + "?",
                "Xóa lớp học phần",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        setStatus("Đang xóa lớp học phần...");
        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    lopHocPhanService.delete(lopHocPhan.getMaLopHocPhan());
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
                    setStatus("Đã xóa lớp học phần");
                    loadClasses(1);
                }
            }
        }.execute();
    }

    private void viewStudents() {
        LopHocPhan lopHocPhan = getSelectedClass();
        if (lopHocPhan == null) {
            return;
        }

        new SwingWorker<List<Student>, Void>() {
            private Exception error;

            @Override
            protected List<Student> doInBackground() {
                try {
                    return lopHocPhanService.getStudents(lopHocPhan.getMaLopHocPhan());
                } catch (IOException ex) {
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
                    List<Student> students = get();
                    StudentListDialog dialog = new StudentListDialog(lopHocPhan, students);
                    dialog.setLocationRelativeTo(ClassManagementPanel.this);
                    dialog.setVisible(true);
                } catch (Exception ex) {
                    setStatus("Lỗi: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private LopHocPhan getSelectedClass() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0 || viewRow >= currentClasses.size()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một lớp học phần", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return currentClasses.get(modelRow);
    }

    private void setStatus(String message) {
        lblStatus.setText(message);
    }

    private class ClassTableModel extends AbstractTableModel {

        private final String[] columns = {"Mã lớp", "Tên lớp", "Giảng viên", "Hình thức", "Phòng học", "Số SV"};
        private List<LopHocPhan> data = new ArrayList<>();

        void setData(List<LopHocPhan> data) {
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
            LopHocPhan lopHocPhan = data.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> lopHocPhan.getMaLopHocPhan();
                case 1 -> lopHocPhan.getTenLopHocPhan();
                case 2 -> lopHocPhan.getGiangVien();
                case 3 -> lopHocPhan.getHinhThucHoc();
                case 4 -> lopHocPhan.getPhongHoc();
                case 5 -> lopHocPhan.getSoSinhVien() != null ? lopHocPhan.getSoSinhVien() : 0;
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 5) {
                return Integer.class;
            }
            return String.class;
        }
    }

    private class ClassDialog extends JDialog {

        private final JTextField txtMa = new JTextField();
        private final JTextField txtTen = new JTextField();
        private final JTextField txtGiangVien = new JTextField();
        private final JTextField txtPhongHoc = new JTextField();
        private final JTextField txtHinhThuc = new JTextField();
        private boolean saved;
        private final LopHocPhan original;

        ClassDialog(LopHocPhan lopHocPhan) {
            super((Frame) SwingUtilities.getWindowAncestor(ClassManagementPanel.this), true);
            this.original = lopHocPhan;
            setTitle(lopHocPhan == null ? "Thêm lớp học phần" : "Cập nhật lớp học phần");
            setSize(420, 320);
            setLayout(new BorderLayout(10, 10));
            ((JPanel) getContentPane()).setBorder(new EmptyBorder(12, 12, 12, 12));

            javax.swing.JPanel form = new javax.swing.JPanel();
            form.setLayout(new java.awt.GridLayout(0, 1, 6, 6));

            txtMa.setColumns(20);
            txtTen.setColumns(20);

            if (lopHocPhan != null) {
                txtMa.setText(lopHocPhan.getMaLopHocPhan());
                txtMa.setEditable(false);
                txtTen.setText(lopHocPhan.getTenLopHocPhan());
                txtGiangVien.setText(lopHocPhan.getGiangVien());
                txtPhongHoc.setText(lopHocPhan.getPhongHoc());
                txtHinhThuc.setText(lopHocPhan.getHinhThucHoc());
            }

            form.add(labeledField("Mã lớp học phần", txtMa));
            form.add(labeledField("Tên lớp học phần", txtTen));
            form.add(labeledField("Giảng viên", txtGiangVien));
            form.add(labeledField("Phòng học", txtPhongHoc));
            form.add(labeledField("Hình thức học", txtHinhThuc));

            JButton btnSave = new JButton("Lưu");
            JButton btnCancel = new JButton("Hủy");

            javax.swing.JPanel actions = new javax.swing.JPanel(new FlowLayout(FlowLayout.RIGHT));
            actions.add(btnCancel);
            actions.add(btnSave);

            add(form, BorderLayout.CENTER);
            add(actions, BorderLayout.SOUTH);

            btnSave.addActionListener(e -> save());
            btnCancel.addActionListener(e -> dispose());
        }

        private JPanel labeledField(String label, JTextField field) {
            JPanel panel = new JPanel(new BorderLayout(6, 6));
            JLabel lbl = new JLabel(label);
            panel.add(lbl, BorderLayout.NORTH);
            panel.add(field, BorderLayout.CENTER);
            return panel;
        }

        private void save() {
            if (txtMa.getText().trim().isEmpty() || txtTen.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ mã và tên lớp", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                return;
            }

            LopHocPhan payload = new LopHocPhan();
            payload.setMaLopHocPhan(txtMa.getText().trim());
            payload.setTenLopHocPhan(txtTen.getText().trim());
            payload.setGiangVien(txtGiangVien.getText().trim());
            payload.setPhongHoc(txtPhongHoc.getText().trim());
            payload.setHinhThucHoc(txtHinhThuc.getText().trim());

            new SwingWorker<Void, Void>() {
                private Exception error;

                @Override
                protected Void doInBackground() {
                    try {
                        if (original == null) {
                            lopHocPhanService.create(payload);
                        } else {
                            lopHocPhanService.update(original.getMaLopHocPhan(), payload);
                        }
                    } catch (IOException ex) {
                        error = ex;
                    }
                    return null;
                }

                @Override
                protected void done() {
                    if (error != null) {
                        JOptionPane.showMessageDialog(ClassDialog.this,
                                "Lỗi: " + error.getMessage(),
                                "Thất bại",
                                JOptionPane.ERROR_MESSAGE);
                    } else {
                        saved = true;
                        dispose();
                    }
                }
            }.execute();
        }

        boolean isSaved() {
            return saved;
        }
    }

    private class StudentListDialog extends JDialog {

        private final JTable studentTable = new JTable();
        private final StudentTableModel studentTableModel = new StudentTableModel();
        private final PaginationPanel studentPagination = new PaginationPanel();
        private final List<Student> students;
        private final LopHocPhan lopHocPhan;

        StudentListDialog(LopHocPhan lopHocPhan, List<Student> students) {
            super((Frame) SwingUtilities.getWindowAncestor(ClassManagementPanel.this), true);
            this.students = new ArrayList<>(students);
            this.lopHocPhan = lopHocPhan;
            setTitle("Sinh viên - " + lopHocPhan.getTenLopHocPhan());
            setSize(560, 420);
            setLayout(new BorderLayout(10, 10));
            ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

            studentTable.setModel(studentTableModel);
            studentTable.setFillsViewportHeight(true);
            studentTable.setRowHeight(24);

            JScrollPane scroll = new JScrollPane(studentTable);
            add(scroll, BorderLayout.CENTER);
            add(studentPagination, BorderLayout.SOUTH);

            JButton btnExport = new JButton("Xuất Excel");
            btnExport.addActionListener(e -> exportStudents());
            add(btnExport, BorderLayout.NORTH);

            studentPagination.setPageSize(15);
            studentPagination.setPageChangeListener(this::showPage);
            studentPagination.reset(students.size());
            showPage(1);
        }

        private void showPage(int page) {
            if (students.isEmpty()) {
                studentTableModel.setData(List.of());
                return;
            }
            int pageSize = studentPagination.getPageSize();
            int total = students.size();
            int totalPages = (int) Math.ceil((double) total / pageSize);
            page = Math.max(1, Math.min(totalPages, page));
            studentPagination.setCurrentPage(page, false);

            int from = (page - 1) * pageSize;
            int to = Math.min(from + pageSize, total);
            studentTableModel.setData(students.subList(from, to));
        }

        private void exportStudents() {
            if (students.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Không có sinh viên để xuất", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("DanhSachSinhVien_" + lopHocPhan.getMaLopHocPhan() + ".xlsx"));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Students");
                int rowIndex = 0;
                Row header = sheet.createRow(rowIndex++);
                header.createCell(0).setCellValue("Mã sinh viên");
                header.createCell(1).setCellValue("Tên sinh viên");
                header.createCell(2).setCellValue("RFID");
                header.createCell(3).setCellValue("Ngày tạo");

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                for (Student student : students) {
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(student.getMaSinhVien());
                    row.createCell(1).setCellValue(student.getTenSinhVien());
                    row.createCell(2).setCellValue(student.getRfid());
                    row.createCell(3).setCellValue(student.getCreatedAt() != null ? formatter.format(student.getCreatedAt()) : "");
                }

                for (int i = 0; i < 4; i++) {
                    sheet.autoSizeColumn(i);
                }

                try (FileOutputStream fos = new FileOutputStream(chooser.getSelectedFile())) {
                    workbook.write(fos);
                }

                JOptionPane.showMessageDialog(this, "Đã xuất danh sách sinh viên", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Thất bại", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class StudentTableModel extends AbstractTableModel {

        private final String[] columns = {"Mã sinh viên", "Tên sinh viên", "RFID", "Ngày tạo"};
        private List<Student> data = new ArrayList<>();
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        void setData(List<Student> data) {
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
            Student student = data.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> student.getMaSinhVien();
                case 1 -> student.getTenSinhVien();
                case 2 -> student.getRfid();
                case 3 -> student.getCreatedAt() != null ? formatter.format(student.getCreatedAt()) : "";
                default -> "";
            };
        }
    }
}

