import React, { useState } from 'react';
import { useHistory } from 'react-router-dom';
import axios from 'axios';
import './Login.css';

function Login() {
  const [activeTab, setActiveTab] = useState('login');
  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [emailVerificationRequired, setEmailVerificationRequired] = useState(false);
  const history = useHistory();

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setEmailVerificationRequired(false);

    try {
      const response = await axios.post('/api/users/login', {
        userId,
        password
      });
      console.log('로그인 성공:', response.data);
      history.push('/posts');
    } catch (err) {
      // 이메일 미인증 에러 처리 (403 Forbidden)
      if (err.response?.status === 403 && err.response?.data?.emailVerificationRequired) {
        setEmailVerificationRequired(true);
        setError(err.response.data.error || '이메일 인증이 필요합니다.');
      } else {
        setError(err.response?.data?.error || '로그인에 실패했습니다.');
      }
    }
  };

  const handleResendEmail = () => {
    history.push('/resend-verification');
  };

  const handleTabChange = (tab) => {
    if (tab === 'signup') {
      history.push('/signup');
    } else {
      setActiveTab(tab);
    }
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <div className="login-header">
          <h2>게시판 시스템</h2>
        </div>

        <div className="tab-list">
          <button
            className={`tab-button ${activeTab === 'login' ? 'active' : ''}`}
            onClick={() => handleTabChange('login')}
          >
            로그인
          </button>
          <button
            className={`tab-button ${activeTab === 'signup' ? 'active' : ''}`}
            onClick={() => handleTabChange('signup')}
          >
            회원가입
          </button>
        </div>

        <form onSubmit={handleLogin}>
          <div className="form-group">
            <label>이메일</label>
            <input
              type="text"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              placeholder="email@example.com"
              required
            />
          </div>

          <div className="form-group">
            <label>비밀번호</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
            />
          </div>

          {error && (
            <div className={`error-message ${emailVerificationRequired ? 'email-verification-error' : ''}`}>
              {error}
              {emailVerificationRequired && (
                <button onClick={handleResendEmail} className="resend-link">
                  인증 이메일 재발송
                </button>
              )}
            </div>
          )}

          <button type="submit" className="login-button">
            <svg className="button-icon" width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M8 2L14 8L8 14M14 8H2" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
            로그인
          </button>
        </form>
      </div>
    </div>
  );
}

export default Login;
