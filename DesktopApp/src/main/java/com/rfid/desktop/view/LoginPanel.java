package com.rfid.desktop.view;

import com.rfid.desktop.model.AuthResponse;
import com.rfid.desktop.model.UserAccount;
import com.rfid.desktop.service.AuthService;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

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

    private BufferedImage backgroundImage;
    private BufferedImage logoImage;

    public LoginPanel(AuthService authService, LoginListener listener) {
        this.authService = authService;
        this.listener = listener;

        // Load images
        loadImages();

        setLayout(new BorderLayout());
        setOpaque(false); // Make panel transparent so background shows through

        // Left panel with logo and text
        JPanel leftPanel = createLeftPanel();

        // Right panel with login form
        JPanel rightPanel = createRightPanel();

        // Add panels
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        int width = getWidth();
        int height = getHeight();
        
        if (backgroundImage != null && width > 0 && height > 0) {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Scale image to cover entire panel (fill mode - may crop edges but covers entire screen)
            double imageAspect = (double) backgroundImage.getWidth() / backgroundImage.getHeight();
            double panelAspect = (double) width / height;
            
            int drawWidth, drawHeight, x, y;
            if (panelAspect > imageAspect) {
                // Panel is wider, scale to cover width (will crop top/bottom)
                drawWidth = width;
                drawHeight = (int) (width / imageAspect);
                x = 0;
                y = (height - drawHeight) / 2;
            } else {
                // Panel is taller, scale to cover height (will crop left/right)
                drawHeight = height;
                drawWidth = (int) (height * imageAspect);
                x = (width - drawWidth) / 2;
                y = 0;
            }
            
            // Draw background image to cover entire panel
            g2d.drawImage(backgroundImage, x, y, drawWidth, drawHeight, null);
            
            // Add semi-transparent overlay for better text visibility
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillRect(0, 0, width, height);
        } else {
            // Fallback: solid dark background if image not loaded
            g2d.setColor(new Color(50, 50, 50));
            g2d.fillRect(0, 0, width, height);
        }
    }

    private void loadImages() {
        try {
            // Load background image
            InputStream bgStream = getClass().getClassLoader().getResourceAsStream("image/ta1-background.jpg");
            if (bgStream != null) {
                backgroundImage = ImageIO.read(bgStream);
                bgStream.close();
            } else {
                System.err.println("Warning: Could not load background image: image/ta1-background.jpg");
            }

            // Load logo - try .jpg first, then .png as fallback
            InputStream logoStream = getClass().getClassLoader().getResourceAsStream("image/logo.jpg");
            if (logoStream == null) {
                // Try .png as fallback
                logoStream = getClass().getClassLoader().getResourceAsStream("image/logo.png");
            }
            if (logoStream != null) {
                logoImage = ImageIO.read(logoStream);
                logoStream.close();
                System.out.println("Logo loaded successfully");
            } else {
                System.err.println("Warning: Could not load logo image: image/logo.jpg or image/logo.png");
            }
        } catch (IOException e) {
            System.err.println("Error loading images: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setPreferredSize(new Dimension(600, 0));
        leftPanel.setLayout(new GridBagLayout());
        leftPanel.setOpaque(false); // Transparent so background shows through

        // Create a container panel for centered content
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 40, 20, 40);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Logo with circular clipping
        if (logoImage != null) {
            int logoSize = 200; // Increased logo size
            final int finalLogoSize = logoSize;
            
            JLabel logoLabel = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (logoImage != null) {
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        
                        int width = getWidth();
                        int height = getHeight();
                        int size = Math.min(width, height);
                        int x = (width - size) / 2;
                        int y = (height - size) / 2;
                        
                        // Create circular clip
                        Shape circle = new Ellipse2D.Double(x, y, size, size);
                        g2d.setClip(circle);
                        
                        // Scale and draw logo
                        Image scaledLogo = logoImage.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                        g2d.drawImage(scaledLogo, x, y, null);
                        
                        g2d.dispose();
                    }
                }
            };
            logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
            logoLabel.setVerticalAlignment(SwingConstants.CENTER);
            logoLabel.setPreferredSize(new Dimension(finalLogoSize + 20, finalLogoSize + 20));
            logoLabel.setOpaque(false);
            contentPanel.add(logoLabel, gbc);
        }

        // School name
        gbc.gridy++;
        gbc.insets = new Insets(30, 40, 10, 40);
        JLabel schoolName = new JLabel("HỌC VIỆN KỸ THUẬT MẬT MÃ", SwingConstants.CENTER);
        schoolName.setFont(new Font("Segoe UI", Font.BOLD, 28));
        schoolName.setForeground(Color.WHITE);
        contentPanel.add(schoolName, gbc);

        // Subtitle
        gbc.gridy++;
        gbc.insets = new Insets(0, 40, 20, 40);
        JLabel subtitle = new JLabel("Hệ thống điểm danh sinh viên RFID", SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 28));
        subtitle.setForeground(new Color(240, 240, 240));
        contentPanel.add(subtitle, gbc);

        // Add content panel to left panel with constraints to center it
        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.gridx = 0;
        mainGbc.gridy = 0;
        mainGbc.weightx = 1.0;
        mainGbc.weighty = 1.0;
        mainGbc.anchor = GridBagConstraints.CENTER;
        mainGbc.fill = GridBagConstraints.NONE;
        leftPanel.add(contentPanel, mainGbc);

        return leftPanel;
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setOpaque(false); // Transparent so background shows through
        rightPanel.setBorder(new EmptyBorder(40, 60, 40, 60));

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

        JLabel header = new JLabel("ĐĂNG NHẬP", JLabel.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 18));
        formPanel.add(header, gbc);

        gbc.gridy++;
        gbc.gridwidth = 2;
        JLabel subHeader = new JLabel("", JLabel.CENTER);
        subHeader.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subHeader.setForeground(new Color(100, 100, 100));
        formPanel.add(subHeader, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(15, 8, 8, 8);
        JLabel usernameLabel = new JLabel("Tên đăng nhập");
        usernameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formPanel.add(usernameLabel, gbc);

        gbc.gridx = 1;
        usernameField.setPreferredSize(new Dimension(200, 30));
        formPanel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel passwordLabel = new JLabel("Mật khẩu");
        passwordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        passwordField.setPreferredSize(new Dimension(200, 30));
        formPanel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(8, 8, 8, 8);
        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        formPanel.add(statusLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(15, 8, 8, 8);
        loginButton.setPreferredSize(new Dimension(200, 35));
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setBackground(new Color(33, 150, 83));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setOpaque(true);
        loginButton.addActionListener(e -> performLogin());
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(loginButton, gbc);

        rightPanel.add(formPanel);
        return rightPanel;
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
                    // Clear form and status before switching to app
                    resetForm();
                    // Call listener to switch to app after UI updates
                    SwingUtilities.invokeLater(() -> {
                        listener.onLoginSuccess(response.getUser());
                    });
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

    public void resetForm() {
        usernameField.setText("");
        passwordField.setText("");
        statusLabel.setText(" ");
        statusLabel.setForeground(Color.DARK_GRAY);
        usernameField.setEnabled(true);
        passwordField.setEnabled(true);
        loginButton.setEnabled(true);
    }
}

