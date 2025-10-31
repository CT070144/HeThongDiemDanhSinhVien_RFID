export const formatTime = (timeStr) => {
    if (!timeStr) return '-';
    if (typeof timeStr === 'string') {
      // If includes fractional seconds, trim them
      if (timeStr.includes('.')) {
        return timeStr.split('.')[0];
      }
      // If already HH:mm:ss, keep first 8 chars
      if (/^\d{2}:\d{2}:\d{2}/.test(timeStr)) {
        return timeStr.substring(0, 8);
      }
      // If it's an ISO or date-like string, try to format
      const d = new Date(timeStr);
      if (!isNaN(d.getTime())) {
        return d.toLocaleTimeString('vi-VN', { hour12: false });
      }
    }
    return String(timeStr);
  };