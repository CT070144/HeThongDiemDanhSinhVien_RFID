package com.rfid.desktop.view;

import com.rfid.desktop.model.UserAccount;
import com.rfid.desktop.service.ApplicationContext;
import com.rfid.desktop.service.AuthService;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.HashMap;
import java.util.Map;

/**
 * Primary window of the desktop application. It coordinates navigation between
 * screens and owns the shared {@link ApplicationContext}.
 */
public class MainFrame extends JFrame implements LoginPanel.LoginListener, NavigationPanel.NavigationListener {

    public static final String SCREEN_DASHBOARD = "dashboard";
    public static final String SCREEN_STUDENTS = "students";
    public static final String SCREEN_ATTENDANCE = "attendance";
    public static final String SCREEN_CLASSES = "classes";
    public static final String SCREEN_DEVICES = "devices";

    private final ApplicationContext context = new ApplicationContext();
    private final AuthService authService = context.getAuthService();

    private final CardLayout rootLayout = new CardLayout();
    private final JPanel rootPanel = new JPanel(rootLayout);

    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(contentLayout);

    private final Map<String, JPanel> loadedScreens = new HashMap<>();

    private NavigationPanel navigationPanel;
    private JPanel appContainer;

    public MainFrame() {
        super("RFID Attendance Desktop");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 820);
        setLocationRelativeTo(null);

        LoginPanel loginPanel = new LoginPanel(authService, this);
        rootPanel.add(loginPanel, "login");
        setContentPane(rootPanel);
        rootLayout.show(rootPanel, "login");
    }

    @Override
    public void onLoginSuccess(UserAccount user) {
        SwingUtilities.invokeLater(() -> {
            setTitle("RFID Attendance Desktop - " + user.getFullName());
            if (appContainer == null) {
                appContainer = new JPanel(new BorderLayout());
                appContainer.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

                navigationPanel = new NavigationPanel(user, this);
                appContainer.add(navigationPanel, BorderLayout.WEST);
                appContainer.add(contentPanel, BorderLayout.CENTER);

                rootPanel.add(appContainer, "app");
            }

            navigateTo(SCREEN_DASHBOARD);
            rootLayout.show(rootPanel, "app");
        });
    }

    @Override
    public void onLogoutRequested() {
        authService.logout();
        context.getWebSocketService().disconnect();
        SwingUtilities.invokeLater(() -> {
            loadedScreens.clear();
            contentPanel.removeAll();
            setTitle("RFID Attendance Desktop");
            rootLayout.show(rootPanel, "login");
        });
    }

    @Override
    public void navigateTo(String screenId) {
        SwingUtilities.invokeLater(() -> {
            if (!loadedScreens.containsKey(screenId)) {
                JPanel panel = createScreen(screenId);
                if (panel != null) {
                    loadedScreens.put(screenId, panel);
                    contentPanel.add(panel, screenId);
                }
            }
            if (navigationPanel != null) {
                navigationPanel.setActive(screenId);
            }
            contentLayout.show(contentPanel, screenId);
        });
    }

    private JPanel createScreen(String screenId) {
        return switch (screenId) {
            case SCREEN_DASHBOARD -> new DashboardPanel(context);
            case SCREEN_STUDENTS -> new StudentManagementPanel(context);
            case SCREEN_ATTENDANCE -> new AttendanceHistoryPanel(context);
            case SCREEN_CLASSES -> new ClassManagementPanel(context);
            case SCREEN_DEVICES -> new DeviceManagementPanel(context);
            default -> null;
        };
    }
}

