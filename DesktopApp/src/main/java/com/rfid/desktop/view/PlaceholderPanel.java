package com.rfid.desktop.view;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;

/**
 * Simple placeholder for screens that are not yet implemented.
 */
public class PlaceholderPanel extends JPanel {

    public PlaceholderPanel(String message) {
        setLayout(new BorderLayout());
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
    }
}

