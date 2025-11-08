import { isToday, formatDate, formatDateTime } from '../dateUtils';

describe('dateUtils', () => {
  describe('isToday', () => {
    it('should return true for today\'s date', () => {
      const today = new Date().toISOString();
      expect(isToday(today)).toBe(true);
    });

    it('should return false for yesterday\'s date', () => {
      const yesterday = new Date();
      yesterday.setDate(yesterday.getDate() - 1);
      expect(isToday(yesterday.toISOString())).toBe(false);
    });

    it('should return false for tomorrow\'s date', () => {
      const tomorrow = new Date();
      tomorrow.setDate(tomorrow.getDate() + 1);
      expect(isToday(tomorrow.toISOString())).toBe(false);
    });

    it('should return false for empty string', () => {
      expect(isToday('')).toBe(false);
    });

    it('should return false for null', () => {
      expect(isToday(null)).toBe(false);
    });

    it('should return false for invalid date string', () => {
      expect(isToday('invalid-date')).toBe(false);
    });

    it('should handle ISO date strings', () => {
      const todayStr = new Date().toISOString();
      expect(isToday(todayStr)).toBe(true);
    });
  });

  describe('formatDate', () => {
    it('should format date correctly', () => {
      const date = '2025-01-15T10:30:00.000Z';
      const formatted = formatDate(date);
      expect(formatted).toBe('2025.01.15');
    });

    it('should return empty string for empty input', () => {
      expect(formatDate('')).toBe('');
    });

    it('should return empty string for null', () => {
      expect(formatDate(null)).toBe('');
    });
  });

  describe('formatDateTime', () => {
    it('should format date time correctly', () => {
      const date = '2025-01-15T10:30:00.000Z';
      const formatted = formatDateTime(date);
      expect(formatted).toMatch(/2025-01-15 \d{2}:\d{2}:\d{2}/);
    });

    it('should return empty string for empty input', () => {
      expect(formatDateTime('')).toBe('');
    });

    it('should return empty string for null', () => {
      expect(formatDateTime(null)).toBe('');
    });
  });
});

