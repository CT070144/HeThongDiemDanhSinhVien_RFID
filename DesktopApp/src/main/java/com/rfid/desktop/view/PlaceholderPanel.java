package com.rfid.desktop.view;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;

/**
 * Simple placeholder for screens that are not yet implemented.
 */
public class PlaceholderPanel extends JPanel {

    public enum Status {
        DEFAULT, SUCCESS, WARNING, ERROR
    }

    public PlaceholderPanel(String message) {
        this(message, Status.DEFAULT);
    }

    public PlaceholderPanel(String message, Status status) {
        setLayout(new BorderLayout());
        setBackground(new java.awt.Color(250, 250, 252));
        
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        
        // Set color based on status
        switch (status) {
            case SUCCESS:
                label.setForeground(new Color(33, 150, 83)); // Green
                break;
            case WARNING:
                label.setForeground(new Color(255, 193, 7)); // Orange/Yellow
                break;
            case ERROR:
                label.setForeground(new Color(220, 53, 69)); // Red
                break;
            default:
                label.setForeground(new Color(90, 90, 90)); // Gray
                break;
        }
        
        add(label, BorderLayout.CENTER);
    }
}

