import React, { createContext, useContext, useState } from 'react';
import NotificationModal from '../components/Notification/NotificationModal';

const NotificationContext = createContext();

export const useNotification = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotification must be used within a NotificationProvider');
  }
  return context;
};

export const NotificationProvider = ({ children }) => {
  const [notification, setNotification] = useState({
    open: false,
    message: '',
    type: 'info',
  });

  const showNotification = (message, type = 'info') => {
    setNotification({
      open: true,
      message,
      type,
    });

    // Tự động đóng sau 3 giây
    setTimeout(() => {
      closeNotification();
    }, 3000);
  };

  const closeNotification = () => {
    setNotification({
      open: false,
      message: '',
      type: 'info',
    });
  };

  const notify = {
    success: (message) => showNotification(message, 'success'),
    error: (message) => showNotification(message, 'error'),
    warning: (message) => showNotification(message, 'warning'),
    info: (message) => showNotification(message, 'info'),
  };

  const value = {
    notify,
  };

  return (
    <NotificationContext.Provider value={value}>
      {children}
      <NotificationModal
        open={notification.open}
        message={notification.message}
        type={notification.type}
        onClose={closeNotification}
      />
    </NotificationContext.Provider>
  );
};

