/**
 * 날짜 유틸리티 함수들
 */

/**
 * 게시글 작성 날짜가 오늘인지 확인
 * @param {string} dateString - 날짜 문자열
 * @returns {boolean} - 오늘이면 true, 아니면 false
 */
export const isToday = (dateString) => {
  if (!dateString) return false;
  
  try {
    const today = new Date();
    const postDate = new Date(dateString);
    
    // 년, 월, 일을 문자열로 비교 (타임존 무관)
    const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    const postDateStr = `${postDate.getFullYear()}-${String(postDate.getMonth() + 1).padStart(2, '0')}-${String(postDate.getDate()).padStart(2, '0')}`;
    
    return todayStr === postDateStr;
  } catch (error) {
    return false;
  }
};

/**
 * 날짜 포맷팅
 * @param {string} dateString - 날짜 문자열
 * @returns {string} - 포맷팅된 날짜 (YYYY.MM.DD)
 */
export const formatDate = (dateString) => {
  if (!dateString) return '';
  
  const date = new Date(dateString);
  return date.toISOString().split('T')[0].replace(/-/g, '.');
};

/**
 * 날짜시간 포맷팅
 * @param {string} dateString - 날짜 문자열
 * @returns {string} - 포맷팅된 날짜시간 (YYYY-MM-DD HH:MM:SS)
 */
export const formatDateTime = (dateString) => {
  if (!dateString) return '';
  
  const date = new Date(dateString);
  return date.toISOString().replace('T', ' ').substring(0, 19);
};

