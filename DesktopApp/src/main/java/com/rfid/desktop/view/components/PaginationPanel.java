package com.rfid.desktop.view.components;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.FlowLayout;

public class PaginationPanel extends JPanel {

    public interface PageChangeListener {
        void onPageChanged(int pageIndex);
    }

    private final JButton btnPrevious;
    private final JButton btnNext;
    private final JLabel lblInfo;

    private PageChangeListener listener;
    private int page = 1;
    private int pageSize = 10;
    private int totalItems = 0;
    public PaginationPanel() {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 4));

        btnPrevious = new JButton("Trước");
        btnNext = new JButton("Sau");
        lblInfo = new JLabel("Trang 1/1", SwingConstants.CENTER);

        btnPrevious.addActionListener(e -> goToPage(page - 1));
        btnNext.addActionListener(e -> goToPage(page + 1));

        add(btnPrevious);
        add(lblInfo);
        add(btnNext);

        updateControls();
    }

    public void setPageSize(int pageSize) {
        if (pageSize <= 0) {
            pageSize = 10;
        }
        this.pageSize = pageSize;
        int totalPages = getTotalPages();
        if (page > totalPages && totalPages > 0) {
            setCurrentPage(totalPages, false);
        } else {
            updateControls();
        }
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = Math.max(0, totalItems);
        int totalPages = getTotalPages();
        if (totalPages == 0) {
            setCurrentPage(1, false);
        } else if (page > totalPages) {
            setCurrentPage(totalPages, false);
        } else {
            updateControls();
        }
    }

    public void reset(int totalItems) {
        this.totalItems = Math.max(0, totalItems);
        setCurrentPage(this.totalItems > 0 ? 1 : 1, false);
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setPageChangeListener(PageChangeListener listener) {
        this.listener = listener;
    }

    public int getCurrentPage() {
        return page;
    }

    public void setCurrentPage(int page, boolean fireEvent) {
        int totalPages = getTotalPages();
        if (totalPages == 0) {
            page = 1;
        } else {
            page = Math.max(1, Math.min(totalPages, page));
        }
        if (this.page != page) {
            this.page = page;
            updateControls();
            if (fireEvent && listener != null) {
                listener.onPageChanged(this.page);
            }
        } else {
            updateControls();
        }
    }

    public int getOffset() {
        return (getCurrentPage() - 1) * pageSize;
    }

    private void goToPage(int requestedPage) {
        setCurrentPage(requestedPage, true);
    }

    private void updateControls() {
        int totalPages = getTotalPages();

        if (totalPages == 0) {
            lblInfo.setText("Không có dữ liệu");
            btnPrevious.setEnabled(false);
            btnNext.setEnabled(false);
        } else {
            lblInfo.setText(String.format("Trang %d/%d", page, totalPages));
            btnPrevious.setEnabled(page > 1);
            btnNext.setEnabled(page < totalPages);
        }
    }

    private int getTotalPages() {
        if (totalItems == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalItems / pageSize);
    }
}

