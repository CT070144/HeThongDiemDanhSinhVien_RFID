package com.rfid.desktop.view;

import com.rfid.desktop.model.UserAccount;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.LinkedHashMap;
import java.util.Map;

public class NavigationPanel extends JPanel {

    public interface NavigationListener {
        void navigateTo(String screenId);

        void onLogoutRequested();
    }

    private final NavigationListener listener;
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    public NavigationPanel(UserAccount user, NavigationListener listener) {
        this.listener = listener;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(220, 0));
        setBorder(new EmptyBorder(16, 10, 16, 16));
        setBackground(Color.WHITE);

       

        

        addNavButton("Dashboard", MainFrame.SCREEN_DASHBOARD);
        addNavButton("Sinh viên", MainFrame.SCREEN_STUDENTS);
        addNavButton("Lịch sử điểm danh", MainFrame.SCREEN_ATTENDANCE);
        addNavButton("Lớp học phần", MainFrame.SCREEN_CLASSES);
        addNavButton("Thiết bị", MainFrame.SCREEN_DEVICES);

        add(Box.createVerticalGlue());
        JLabel nameLabel = new JLabel("Xin chào, " + user.getFullName());
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
        add(nameLabel);

        String roleText = user.getRoleDescription() != null ? user.getRoleDescription() : user.getRole();
        JLabel roleLabel = new JLabel(roleText != null ? roleText : "");
        roleLabel.setForeground(new Color(100, 100, 100));
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        roleLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
        add(roleLabel);
        add(Box.createVerticalStrut(20));   
        JButton logoutButton = new JButton("Đăng xuất");
        logoutButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        logoutButton.addActionListener(e -> listener.onLogoutRequested());
        logoutButton.setBackground(new Color(240, 77, 77));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 14));
        add(logoutButton);
    }

    private void addNavButton(String text, String screenId) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setFocusPainted(false);
        button.setBackground(new Color(242, 245, 249));
        button.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 12));
        button.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        button.addActionListener(e -> listener.navigateTo(screenId));
        add(button);
        add(Box.createVerticalStrut(8));
        navButtons.put(screenId, button);
    }

    public void setActive(String screenId) {
        navButtons.forEach((key, button) -> {
            boolean active = key.equals(screenId);
            button.setBackground(active ? new Color(214, 228, 255) : new Color(242, 245, 249));
        });
    }
}
