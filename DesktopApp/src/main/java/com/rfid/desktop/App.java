package com.rfid.desktop;

import com.formdev.flatlaf.FlatLightLaf;
import com.rfid.desktop.view.MainFrame;

import javax.swing.SwingUtilities;

/**
 * Application entry point for the RFID desktop client. This class initialises the
 * look and feel and launches the main Swing frame.
 */
public final class App {

    private App() {
        // Utility class
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

