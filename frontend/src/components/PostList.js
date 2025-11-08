import React, { useState, useEffect } from 'react';
import { useHistory } from 'react-router-dom';
import axios from 'axios';
import './PostList.css';

function PostList() {
  const [posts, setPosts] = useState([]);
  const [user, setUser] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalCount, setTotalCount] = useState(0);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const pageSize = 20;
  const history = useHistory();

  useEffect(() => {
    checkAuth();
  }, []);

  useEffect(() => {
    // 검색어가 변경되면 1페이지로 이동
    if (currentPage !== 1) {
      setCurrentPage(1);
    } else {
      // 이미 1페이지면 바로 검색
      fetchPosts(1);
    }
  }, [searchQuery]);

  useEffect(() => {
    fetchPosts(currentPage);
  }, [currentPage]);

  const checkAuth = async () => {
    try {
      const response = await axios.get('/api/users/me');
      setUser(response.data);
    } catch (err) {
      history.push('/');
    }
  };

  const fetchPosts = async (page) => {
    try {
      let url = `/api/posts?page=${page}&size=${pageSize}`;
      if (searchQuery) {
        url += `&search=${encodeURIComponent(searchQuery)}`;
      }
      const response = await axios.get(url);
      setPosts(response.data.posts);
      setTotalPages(response.data.totalPages);
      setTotalCount(response.data.totalCount);
      setCurrentPage(response.data.currentPage);
    } catch (err) {
      console.error('게시글 로딩 실패:', err);
    }
  };

  const handleLogout = async () => {
    try {
      await axios.post('/api/users/logout');
      history.push('/');
    } catch (err) {
      console.error('로그아웃 실패:', err);
    }
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toISOString().split('T')[0].replace(/-/g, '.');
  };

  const getCategoryBadge = (isNotice) => {
    if (isNotice) {
      return { text: '공지사항', className: 'badge-notice' };
    }
    return { text: '자유게시판', className: 'badge-free' };
  };

  const getInitial = (name) => {
    return name ? name.charAt(0) : '?';
  };

  const isToday = (dateString) => {
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

  return (
    <div className="post-list-container">
      {/* Header */}
      <header className="board-header">
        <div className="header-left">
          <div className="logo-icon">📋</div>
          <div className="logo-text">
            <h1>온라인채널 운영팀 게시판</h1>
            <p>Online Channel Operations Board</p>
          </div>
        </div>
        <div className="header-right">
          <svg className="user-icon" width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M8 8a3 3 0 100-6 3 3 0 000 6zM8 10c-3.5 0-6 2-6 4v1h12v-1c0-2-2.5-4-6-4z" fill="currentColor"/>
          </svg>
          <span className="user-name">{user?.userName || '홍길동'}</span>
          <div className="user-avatar" onClick={() => setShowUserMenu(!showUserMenu)}>
            {getInitial(user?.userName || '홍길동')}
          </div>
          {showUserMenu && (
            <div className="user-menu-dropdown">
              <button className="menu-item" onClick={() => history.push('/change-password')}>
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path d="M11 7V5a3 3 0 00-6 0v2M4 7h8a1 1 0 011 1v5a1 1 0 01-1 1H4a1 1 0 01-1-1V8a1 1 0 011-1z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
                </svg>
                비밀번호 변경
              </button>
              <button className="menu-item" onClick={handleLogout}>
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path d="M6 14H3a1 1 0 01-1-1V3a1 1 0 011-1h3M11 11l3-3-3-3M14 8H6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
                로그아웃
              </button>
            </div>
          )}
        </div>
      </header>

      {/* Main Content */}
      <main className="board-main">
        <div className="content-header">
          <div className="title-section">
            <h2>게시판</h2>
            <p className="post-count">전체 {totalCount}개의 게시글</p>
          </div>
          <button className="write-button" onClick={() => history.push('/posts/new')}>
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M8 3v10M3 8h10" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
            </svg>
            <span>글쓰기</span>
          </button>
        </div>

        {/* Search */}
        <div className="search-container">
          <svg className="search-icon" width="16" height="16" viewBox="0 0 16 16" fill="none">
            <circle cx="7" cy="7" r="4" stroke="currentColor" strokeWidth="1.5"/>
            <path d="M10 10l3 3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
          </svg>
          <input
            type="text"
            className="search-input"
            placeholder="제목 또는 작성자로 검색"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        {/* Table - Desktop */}
        <div className="table-container posts-table">
          <table className="board-table">
            <thead>
              <tr>
                <th>번호</th>
                <th>카테고리</th>
                <th>제목</th>
                <th>작성자</th>
                <th>조회수</th>
                <th>작성일</th>
                <th>상태</th>
              </tr>
            </thead>
            <tbody>
              {posts.map((post, index) => {
                const category = getCategoryBadge(post.isNotice);
                const postNumber = post.isNotice ? '공지' : totalCount - ((currentPage - 1) * pageSize + index);
                return (
                  <tr
                    key={post.postId}
                    onClick={() => history.push(`/posts/${post.postId}`)}
                  >
                    <td>{postNumber}</td>
                    <td>
                      <span className={`badge ${category.className}`}>
                        {category.text}
                      </span>
                    </td>
                    <td className="title-cell">
                      <span className="title-text">{post.title}</span>
                      {/* {isToday(post.createdAt) && (
                        <span className="badge badge-new">New</span>
                      )} */}
                      {post.commentCount > 0 && (
                        <span className="comment-count">({post.commentCount})</span>
                      )}
                      {post.fileCount > 0 && (
                        <svg className="file-icon" width="14" height="14" viewBox="0 0 16 16" fill="none">
                          <path d="M9 2H4a1 1 0 00-1 1v10a1 1 0 001 1h8a1 1 0 001-1V7M9 2v5h4M9 2l4 5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                        </svg>
                      )}
                      {post.viewCount > 100 && (
                        <span className="badge badge-popular">인기</span>
                      )}
                    </td>
                    <td>{post.authorName}</td>
                    <td>
                      <div className="view-count">
                        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                          <path d="M8 5c-2.5 0-4.5 1.5-6 3 1.5 1.5 3.5 3 6 3s4.5-1.5 6-3c-1.5-1.5-3.5-3-6-3z" stroke="currentColor" strokeWidth="1"/>
                          <circle cx="8" cy="8" r="1.5" fill="currentColor"/>
                        </svg>
                        {post.viewCount}
                      </div>
                    </td>
                    <td>{formatDate(post.createdAt)}</td>
                    <td>
                      <span className="badge badge-status">공개</span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {/* Mobile Card List */}
        <div className="posts-list-mobile">
          {posts.map((post, index) => {
            const category = getCategoryBadge(post.isNotice);
            return (
              <div
                key={post.postId}
                className="post-item-mobile"
                onClick={() => history.push(`/posts/${post.postId}`)}
              >
                <div className="post-mobile-header">
                  <span className={`badge ${category.className}`}>
                    {category.text}
                  </span>
                  <span className="badge badge-status">공개</span>
                </div>
                <div className="post-mobile-title">
                  {post.title}
                  {post.commentCount > 0 && (
                    <span className="comment-count"> ({post.commentCount})</span>
                  )}
                </div>
                <div className="post-mobile-meta">
                  <span>{post.authorName}</span>
                  <span>•</span>
                  <span>{formatDate(post.createdAt)}</span>
                  <span>•</span>
                  <span>조회 {post.viewCount}</span>
                </div>
              </div>
            );
          })}
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="pagination">
            <button
              className="page-button"
              disabled={currentPage === 1}
              onClick={() => setCurrentPage(currentPage - 1)}
            >
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M10 12L6 8l4-4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
              Previous
            </button>

            {Array.from({ length: totalPages }, (_, i) => i + 1).map((pageNum) => {
              // 현재 페이지 주변 5개 페이지만 표시
              if (
                pageNum === 1 ||
                pageNum === totalPages ||
                (pageNum >= currentPage - 2 && pageNum <= currentPage + 2)
              ) {
                return (
                  <button
                    key={pageNum}
                    className={`page-number ${pageNum === currentPage ? 'active' : ''}`}
                    onClick={() => setCurrentPage(pageNum)}
                  >
                    {pageNum}
                  </button>
                );
              } else if (
                pageNum === currentPage - 3 ||
                pageNum === currentPage + 3
              ) {
                return <span key={pageNum} className="page-ellipsis">...</span>;
              }
              return null;
            })}

            <button
              className="page-button"
              disabled={currentPage === totalPages}
              onClick={() => setCurrentPage(currentPage + 1)}
            >
              Next
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M6 12l4-4-4-4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </button>
          </div>
        )}
      </main>
    </div>
  );
}

export default PostList;
