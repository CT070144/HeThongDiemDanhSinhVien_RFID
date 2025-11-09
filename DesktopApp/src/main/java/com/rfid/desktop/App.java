package com.rfid.desktop;

import com.formdev.flatlaf.FlatLightLaf;
import com.rfid.desktop.view.MainFrame;

import javax.swing.SwingUtilities;

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

