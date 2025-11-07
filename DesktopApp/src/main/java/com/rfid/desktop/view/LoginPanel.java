package com.rfid.desktop.view;

import com.rfid.desktop.model.AuthResponse;
import com.rfid.desktop.model.UserAccount;
import com.rfid.desktop.service.AuthService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class LoginPanel extends JPanel {

    public interface LoginListener {
        void onLoginSuccess(UserAccount user);

        void onLogoutRequested();
    }

    private final AuthService authService;
    private final LoginListener listener;

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JButton loginButton = new JButton("Đăng nhập");
    private final JLabel statusLabel = new JLabel(" ");

    public LoginPanel(AuthService authService, LoginListener listener) {
        this.authService = authService;
        this.listener = listener;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(40, 0, 40, 0));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xdddddd)),
                new EmptyBorder(30, 30, 30, 30)));
        formPanel.setPreferredSize(new Dimension(420, 320));
        formPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        JLabel header = new JLabel("Hệ thống điểm danh RFID", JLabel.CENTER);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 18f));
        formPanel.add(header, gbc);

        gbc.gridy++;
        gbc.gridwidth = 2;
        JLabel subHeader = new JLabel("Đăng nhập", JLabel.CENTER);
        subHeader.setFont(subHeader.getFont().deriveFont(Font.PLAIN, 14f));
        formPanel.add(subHeader, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Tên đăng nhập"), gbc);

        gbc.gridx = 1;
        formPanel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        formPanel.add(new JLabel("Mật khẩu"), gbc);

        gbc.gridx = 1;
        formPanel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        formPanel.add(statusLabel, gbc);

        gbc.gridy++;
        loginButton.addActionListener(e -> performLogin());
        formPanel.add(loginButton, gbc);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(new Color(240, 243, 246));
        centerWrapper.add(formPanel);

        add(centerWrapper, BorderLayout.CENTER);
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isBlank() || password.isBlank()) {
            showError("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu");
            return;
        }

        setLoading(true);
        statusLabel.setForeground(Color.DARK_GRAY);
        statusLabel.setText("Đang đăng nhập...");

        new SwingWorker<AuthResponse, Void>() {
            private Exception error;

            @Override
            protected AuthResponse doInBackground() {
                try {
                    return authService.login(username, password);
                } catch (Exception ex) {
                    error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                setLoading(false);
                if (error != null) {
                    showError(error.getMessage());
                    return;
                }

                try {
                    AuthResponse response = get();
                    if (response == null || response.getUser() == null) {
                        showError("Đăng nhập thất bại");
                        return;
                    }
                    statusLabel.setForeground(new Color(33, 150, 83));
                    statusLabel.setText("Đăng nhập thành công");
                    SwingUtilities.invokeLater(() -> listener.onLoginSuccess(response.getUser()));
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }
        }.execute();
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        usernameField.setEnabled(!loading);
        passwordField.setEnabled(!loading);
    }

    private void showError(String message) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(message);
    }
}

