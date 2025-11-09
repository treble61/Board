import React, { useState } from 'react';
import { useHistory } from 'react-router-dom';
import axios from 'axios';
import './Signup.css';

function Signup() {
  const [activeTab, setActiveTab] = useState('signup');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [name, setName] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const history = useHistory();

  const handleSignup = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess(false);

    if (password !== passwordConfirm) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }

    try {
      const response = await axios.post('/api/users/signup', {
        userId: email,  // 이메일을 userId로 사용
        password,
        passwordConfirm,
        name,
        email
      });

      // 회원가입 성공 - 이메일 인증 메시지 표시
      setSuccess(true);
      setSuccessMessage(response.data.message);

      // 폼 초기화
      setEmail('');
      setPassword('');
      setPasswordConfirm('');
      setName('');
    } catch (err) {
      setError(err.response?.data?.error || '회원가입에 실패했습니다.');
    }
  };

  const handleResendEmail = () => {
    history.push('/resend-verification');
  };

  const handleTabChange = (tab) => {
    if (tab === 'login') {
      history.push('/');
    } else {
      setActiveTab(tab);
    }
  };

  return (
    <div className="signup-container">
      <div className="signup-box">
        <div className="signup-header">
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

        {success ? (
          <div className="success-container">
            <div className="success-icon">
              <svg width="64" height="64" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="32" cy="32" r="30" stroke="#10b981" strokeWidth="4"/>
                <path d="M20 32L28 40L44 24" stroke="#10b981" strokeWidth="4" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </div>
            <h3 className="success-title">회원가입 완료!</h3>
            <p className="success-message">{successMessage}</p>
            <div className="success-info">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M10 1L3 3.5L10 6L17 3.5L10 1Z" stroke="#667eea" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                <path d="M3 13.5V6.5L10 9V16L3 13.5Z" stroke="#667eea" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                <path d="M10 9L17 6.5V13.5L10 16V9Z" stroke="#667eea" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
              <span>이메일을 확인하여 인증을 완료해주세요.</span>
            </div>
            <div className="success-actions">
              <button onClick={() => history.push('/')} className="btn-primary">
                로그인 페이지로
              </button>
              <button onClick={handleResendEmail} className="btn-link">
                이메일을 받지 못하셨나요?
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSignup}>
            <div className="form-group">
              <label>이메일</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="email@example.com"
                required
              />
            </div>

            <div className="form-group">
              <label>이름</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="홍길동"
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

            <div className="form-group">
              <label>비밀번호 확인</label>
              <input
                type="password"
                value={passwordConfirm}
                onChange={(e) => setPasswordConfirm(e.target.value)}
                placeholder="••••••••"
                required
              />
            </div>

            {error && <div className="error-message">{error}</div>}

            <button type="submit" className="signup-button">
              <svg className="button-icon" width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M8 2L14 8L8 14M14 8H2" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
              회원가입
            </button>
          </form>
        )}
      </div>
    </div>
  );
}

export default Signup;
