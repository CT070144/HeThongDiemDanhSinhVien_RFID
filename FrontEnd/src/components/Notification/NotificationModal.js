import React from 'react';
import './NotificationModal.css';

const typeConfig = {
  success: { icon: 'fas fa-check-circle', color: '#16a34a' },
  error: { icon: 'fas fa-times-circle', color: '#ef4444' },
  warning: { icon: 'fas fa-exclamation-triangle', color: '#f59e0b' },
  info: { icon: 'fas fa-info-circle', color: '#3b82f6' },
};

const NotificationModal = ({ open, message, type = 'info', onClose }) => {
  if (!open) return null;
  const cfg = typeConfig[type] || typeConfig.info;

  return (
    <div className="notify-overlay" onClick={onClose}>
      <div
        className="notify-modal"
        style={{ borderColor: cfg.color }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="notify-icon" style={{ color: cfg.color }}>
          <i className={cfg.icon}></i>
        </div>
        <div className="notify-message">{message}</div>
        <button className="notify-close" onClick={onClose} aria-label="Đóng thông báo">
          <i className="fas fa-times"></i>
        </button>
      </div>
    </div>
  );
};

export default NotificationModal;


