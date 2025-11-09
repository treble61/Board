import React, { useEffect, useState } from 'react';
import { useHistory, useLocation } from 'react-router-dom';
import axios from 'axios';
import './VerifyEmail.css';

function VerifyEmail() {
  const [status, setStatus] = useState('loading'); // loading, success, error
  const [message, setMessage] = useState('');
  const [userId, setUserId] = useState('');
  const history = useHistory();
  const location = useLocation();

  useEffect(() => {
    const verifyEmail = async () => {
      // URL 쿼리 파라미터에서 토큰 추출
      const params = new URLSearchParams(location.search);
      const token = params.get('token');

      if (!token) {
        setStatus('error');
        setMessage('유효하지 않은 인증 링크입니다.');
        return;
      }

      try {
        const response = await axios.get(`/api/users/verify-email?token=${token}`);
        setStatus('success');
        setMessage(response.data.message);
        setUserId(response.data.userId);
      } catch (err) {
        setStatus('error');

        // HTTP 상태 코드에 따른 에러 메시지 처리
        if (err.response) {
          switch (err.response.status) {
            case 410: // Gone - 토큰 만료
              setMessage(err.response.data.error || '인증 토큰이 만료되었습니다.');
              break;
            case 409: // Conflict - 이미 사용됨
              setMessage(err.response.data.error || '이미 인증된 이메일입니다.');
              break;
            case 429: // Too Many Requests
              setMessage(err.response.data.error || '너무 많은 인증 시도가 있었습니다.');
              break;
            default:
              setMessage(err.response.data.error || '이메일 인증에 실패했습니다.');
          }
        } else {
          setMessage('이메일 인증 중 오류가 발생했습니다.');
        }
      }
    };

    verifyEmail();
  }, [location.search]);

  const handleGoToLogin = () => {
    history.push('/');
  };

  const handleResendEmail = () => {
    history.push('/resend-verification');
  };

  return (
    <div className="verify-email-container">
      <div className="verify-email-box">
        <div className="verify-email-header">
          <h2>이메일 인증</h2>
        </div>

        <div className="verify-email-content">
          {status === 'loading' && (
            <div className="status-loading">
              <div className="spinner"></div>
              <p>이메일 인증 중입니다...</p>
            </div>
          )}

          {status === 'success' && (
            <div className="status-success">
              <div className="success-icon">
                <svg width="64" height="64" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="32" cy="32" r="30" stroke="#10b981" strokeWidth="4"/>
                  <path d="M20 32L28 40L44 24" stroke="#10b981" strokeWidth="4" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
              </div>
              <h3>인증 완료!</h3>
              <p className="message">{message}</p>
              {userId && <p className="user-info">사용자 ID: <strong>{userId}</strong></p>}
              <button onClick={handleGoToLogin} className="btn-primary">
                로그인하러 가기
              </button>
            </div>
          )}

          {status === 'error' && (
            <div className="status-error">
              <div className="error-icon">
                <svg width="64" height="64" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="32" cy="32" r="30" stroke="#ef4444" strokeWidth="4"/>
                  <path d="M24 24L40 40M40 24L24 40" stroke="#ef4444" strokeWidth="4" strokeLinecap="round"/>
                </svg>
              </div>
              <h3>인증 실패</h3>
              <p className="message">{message}</p>
              <div className="error-actions">
                <button onClick={handleResendEmail} className="btn-secondary">
                  인증 이메일 재발송
                </button>
                <button onClick={handleGoToLogin} className="btn-link">
                  로그인 페이지로
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default VerifyEmail;
