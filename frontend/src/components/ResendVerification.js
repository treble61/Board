import React, { useState } from 'react';
import { useHistory } from 'react-router-dom';
import axios from 'axios';
import './ResendVerification.css';

function ResendVerification() {
  const [email, setEmail] = useState('');
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const history = useHistory();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess(false);
    setLoading(true);

    try {
      const response = await axios.post('/api/users/resend-verification', {
        email
      });

      setSuccess(true);
      setEmail(''); // 폼 초기화
    } catch (err) {
      if (err.response?.status === 429) {
        setError('너무 많은 재발송 요청이 있었습니다. 1시간 후 다시 시도해주세요.');
      } else {
        setError(err.response?.data?.error || '인증 이메일 재발송에 실패했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleGoToLogin = () => {
    history.push('/');
  };

  return (
    <div className="resend-container">
      <div className="resend-box">
        <div className="resend-header">
          <button onClick={() => history.goBack()} className="back-button">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12.5 15L7.5 10L12.5 5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </button>
          <h2>인증 이메일 재발송</h2>
        </div>

        <div className="resend-content">
          {success ? (
            <div className="success-container">
              <div className="success-icon">
                <svg width="64" height="64" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="32" cy="32" r="30" stroke="#10b981" strokeWidth="4"/>
                  <path d="M20 32L28 40L44 24" stroke="#10b981" strokeWidth="4" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
              </div>
              <h3>이메일 재발송 완료!</h3>
              <p className="success-message">
                인증 이메일이 재전송되었습니다.<br/>
                이메일을 확인해주세요.
              </p>
              <div className="success-actions">
                <button onClick={handleGoToLogin} className="btn-primary">
                  로그인 페이지로
                </button>
                <button onClick={() => setSuccess(false)} className="btn-link">
                  다른 이메일로 재발송
                </button>
              </div>
            </div>
          ) : (
            <div className="form-container">
              <div className="info-box">
                <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="10" cy="10" r="8" stroke="#667eea" strokeWidth="1.5"/>
                  <path d="M10 10V14M10 6V7" stroke="#667eea" strokeWidth="1.5" strokeLinecap="round"/>
                </svg>
                <p>
                  회원가입 시 입력한 이메일 주소를 입력하시면<br/>
                  인증 이메일을 다시 발송해드립니다.
                </p>
              </div>

              <form onSubmit={handleSubmit}>
                <div className="form-group">
                  <label htmlFor="email">이메일</label>
                  <input
                    type="email"
                    id="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="email@example.com"
                    required
                    disabled={loading}
                  />
                </div>

                {error && <div className="error-message">{error}</div>}

                <button type="submit" className="submit-button" disabled={loading}>
                  {loading ? (
                    <>
                      <div className="spinner-small"></div>
                      전송 중...
                    </>
                  ) : (
                    <>
                      <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M2 3L14 8L2 13V3Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                      </svg>
                      인증 이메일 재발송
                    </>
                  )}
                </button>
              </form>

              <div className="footer-links">
                <button onClick={handleGoToLogin} className="footer-link">
                  로그인 페이지로 돌아가기
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default ResendVerification;
