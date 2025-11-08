import React, { useState, useEffect } from 'react';
import { useHistory, useParams } from 'react-router-dom';
import axios from 'axios';
import DOMPurify from 'dompurify';
import './PostDetail.css';

function PostDetail() {
  const [post, setPost] = useState(null);
  const [user, setUser] = useState(null);
  const [comment, setComment] = useState('');
  const [comments, setComments] = useState([]);
  const [files, setFiles] = useState([]);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showEditMenu, setShowEditMenu] = useState(false);
  const [excelFile, setExcelFile] = useState(null);
  const [uploadingExcel, setUploadingExcel] = useState(false);
  const history = useHistory();
  const { id } = useParams();

  useEffect(() => {
    checkAuth();
    fetchPost();
    fetchComments();
    fetchFiles();
  }, [id]);

  const checkAuth = async () => {
    try {
      const response = await axios.get('/api/users/me');
      setUser(response.data);
    } catch (err) {
      history.push('/');
    }
  };

  const fetchPost = async () => {
    try {
      const response = await axios.get(`/api/posts/${id}`);
      setPost(response.data);
    } catch (err) {
      console.error('게시글 로딩 실패:', err);
    }
  };

  const fetchComments = async () => {
    try {
      const response = await axios.get(`/api/comments/post/${id}`);
      setComments(response.data);
    } catch (err) {
      console.error('댓글 로딩 실패:', err);
    }
  };

  const fetchFiles = async () => {
    try {
      const response = await axios.get(`/api/files/post/${id}`);
      setFiles(response.data);
    } catch (err) {
      console.error('첨부파일 로딩 실패:', err);
    }
  };

  const downloadFile = (fileId, fileName) => {
    const link = document.createElement('a');
    link.href = `/api/files/download/${fileId}`;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleExcelUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    // Check if file is Excel
    const fileName = file.name.toLowerCase();
    if (!fileName.endsWith('.xlsx') && !fileName.endsWith('.xls')) {
      alert('엑셀 파일만 업로드 가능합니다. (.xlsx, .xls)');
      return;
    }

    // Check file size (10MB)
    if (file.size > 10 * 1024 * 1024) {
      alert('파일 크기는 10MB를 초과할 수 없습니다.');
      return;
    }

    setUploadingExcel(true);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await axios.post(`/api/posts/${id}/excel`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      alert('엑셀 파일이 업로드되었습니다.');
      fetchPost(); // Refresh post data to get Excel info
      event.target.value = ''; // Reset file input
    } catch (err) {
      alert(err.response?.data?.error || '엑셀 파일 업로드에 실패했습니다.');
    } finally {
      setUploadingExcel(false);
    }
  };

  const handleExcelDownload = () => {
    const link = document.createElement('a');
    link.href = `/api/posts/${id}/excel/download`;
    link.download = post.excelFilename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleExcelDelete = async () => {
    if (!window.confirm('엑셀 파일을 삭제하시겠습니까?')) {
      return;
    }

    try {
      await axios.delete(`/api/posts/${id}/excel`);
      alert('엑셀 파일이 삭제되었습니다.');
      fetchPost(); // Refresh post data
    } catch (err) {
      alert(err.response?.data?.error || '엑셀 파일 삭제에 실패했습니다.');
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

  const handleCommentSubmit = async () => {
    if (!comment.trim()) {
      alert('댓글 내용을 입력해주세요.');
      return;
    }

    try {
      await axios.post('/api/comments', {
        postId: id,
        content: comment
      });
      setComment('');
      fetchComments(); // 댓글 목록 새로고침
    } catch (err) {
      alert(err.response?.data?.error || '댓글 작성에 실패했습니다.');
    }
  };

  const handleDeleteComment = async (commentId) => {
    if (!window.confirm('댓글을 삭제하시겠습니까?')) {
      return;
    }

    try {
      await axios.delete(`/api/comments/${commentId}`);
      fetchComments(); // 댓글 목록 새로고침
    } catch (err) {
      alert(err.response?.data?.error || '댓글 삭제에 실패했습니다.');
    }
  };

  const formatDateTime = (dateString) => {
    const date = new Date(dateString);
    return date.toISOString().replace('T', ' ').substring(0, 19);
  };

  const handleEdit = () => {
    history.push(`/posts/${id}/edit`);
  };

  const handleDeleteClick = () => {
    setShowEditMenu(false);
    setShowDeleteModal(true);
  };

  const handleDeleteConfirm = async () => {
    try {
      await axios.delete(`/api/posts/${id}`);
      alert('게시글이 삭제되었습니다.');
      history.push('/posts');
    } catch (err) {
      alert(err.response?.data?.error || '게시글 삭제에 실패했습니다.');
    }
  };

  if (!post) {
    return <div>로딩 중...</div>;
  }

  const category = getCategoryBadge(post.isNotice);

  return (
    <div className="post-detail-container">
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
          <div className="user-avatar">
            {getInitial(user?.userName || '홍길동')}
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="board-main">
        {/* Back Button */}
        <button className="back-button" onClick={() => history.push('/posts')}>
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M10 12L6 8l4-4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          목록으로
        </button>

        {/* Post Card */}
        <div className="post-card">
          {/* Badges */}
          <div className="post-badges">
            <span className={`badge ${category.className}`}>
              {category.text}
            </span>
            <span className="badge badge-status">공개</span>
          </div>

          {/* Title */}
          <h2 className="post-title">{post.title}</h2>

          {/* Meta Info */}
          <div className="post-meta">
            <div className="meta-item">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M8 8a3 3 0 100-6 3 3 0 000 6zM8 10c-3.5 0-6 2-6 4v1h12v-1c0-2-2.5-4-6-4z" fill="currentColor"/>
              </svg>
              <span>{post.authorName}</span>
            </div>
            <div className="meta-item">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <rect x="3" y="4" width="10" height="9" rx="1" stroke="currentColor" strokeWidth="1.5" fill="none"/>
                <path d="M3 6h10M6 2v2M10 2v2" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
              </svg>
              <span>{formatDate(post.createdAt)}</span>
            </div>
            <div className="meta-item">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M8 5c-2.5 0-4.5 1.5-6 3 1.5 1.5 3.5 3 6 3s4.5-1.5 6-3c-1.5-1.5-3.5-3-6-3z" stroke="currentColor" strokeWidth="1" fill="none"/>
                <circle cx="8" cy="8" r="1.5" fill="currentColor"/>
              </svg>
              <span>조회 {post.viewCount}</span>
            </div>
          </div>

          {/* Edit/Delete Buttons (작성자만 보임) */}
          {user && post.authorId === user.userId && (
            <div className="post-action-buttons">
              <button className="action-btn edit-btn" onClick={handleEdit}>
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path d="M11 2l3 3-8 8H3v-3l8-8z" stroke="currentColor" strokeWidth="1.33" strokeLinecap="round" strokeLinejoin="round"/>
                  <path d="M10.67 2.67l2.66 2.66" stroke="currentColor" strokeWidth="1.33" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
                수정
              </button>
              <button className="action-btn delete-btn" onClick={handleDeleteClick}>
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path d="M3 4h10M5 4V3a1 1 0 011-1h4a1 1 0 011 1v1M6 7v4M10 7v4M4 4h8v9a1 1 0 01-1 1H5a1 1 0 01-1-1V4z" stroke="currentColor" strokeWidth="1.33" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
                삭제
              </button>
            </div>
          )}

          {/* Divider */}
          <div className="divider"></div>

          {/* Content - HTML 렌더링 (이미지 base64 포함) */}
          <div className="post-content">
            <div className="content-text" dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(post.content) }} />
          </div>

          {/* Attachments (모든 첨부파일 - 이미지 포함) */}
          {files.length > 0 && (
            <div className="post-attachments">
              <h4 className="attachments-title">첨부파일 ({files.length})</h4>
              <div className="attachments-list">
                {files.map((file) => (
                  <div
                    key={file.fileId}
                    className="attachment-item"
                    onClick={() => downloadFile(file.fileId, file.originalFilename)}
                  >
                    <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                      <path d="M8 2v8M8 10l3-3M8 10l-3-3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                      <path d="M2 12v1a1 1 0 001 1h10a1 1 0 001-1v-1" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
                    </svg>
                    <span className="attachment-name">{file.originalFilename}</span>
                    <span className="attachment-size">
                      ({(file.fileSize / 1024).toFixed(1)} KB)
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Excel File Section (separate from regular attachments) */}
          <div className="post-excel-section">
            <h4 className="excel-title">엑셀 파일</h4>
            {post.excelFilename ? (
              <div className="excel-file-info">
                <div className="excel-file-item">
                  <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                    <rect x="4" y="2" width="12" height="16" rx="1" stroke="#217346" strokeWidth="1.5" fill="none"/>
                    <path d="M8 2v16M12 2v16M4 7h12M4 12h12" stroke="#217346" strokeWidth="1.5"/>
                  </svg>
                  <div className="excel-file-details">
                    <span className="excel-filename">{post.excelFilename}</span>
                    <span className="excel-filesize">
                      ({(post.excelFileSize / 1024).toFixed(1)} KB)
                    </span>
                  </div>
                </div>
                <div className="excel-actions">
                  <button className="excel-btn download-btn" onClick={handleExcelDownload}>
                    <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                      <path d="M8 2v8M8 10l3-3M8 10l-3-3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                      <path d="M2 12v1a1 1 0 001 1h10a1 1 0 001-1v-1" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
                    </svg>
                    다운로드
                  </button>
                  {user && post.authorId === user.userId && (
                    <button className="excel-btn delete-btn" onClick={handleExcelDelete}>
                      <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                        <path d="M3 4h10M5 4V3a1 1 0 011-1h4a1 1 0 011 1v1M6 7v4M10 7v4M4 4h8v9a1 1 0 01-1 1H5a1 1 0 01-1-1V4z" stroke="currentColor" strokeWidth="1.33" strokeLinecap="round" strokeLinejoin="round"/>
                      </svg>
                      삭제
                    </button>
                  )}
                </div>
              </div>
            ) : (
              user && post.authorId === user.userId && (
                <div className="excel-upload-area">
                  <input
                    type="file"
                    id="excel-upload"
                    accept=".xlsx,.xls"
                    onChange={handleExcelUpload}
                    style={{ display: 'none' }}
                  />
                  <label htmlFor="excel-upload" className={`excel-upload-btn ${uploadingExcel ? 'uploading' : ''}`}>
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                      <rect x="4" y="2" width="12" height="16" rx="1" stroke="currentColor" strokeWidth="1.5" fill="none"/>
                      <path d="M8 2v16M12 2v16M4 7h12M4 12h12" stroke="currentColor" strokeWidth="1.5"/>
                      <path d="M10 9v6M7 12l3-3 3 3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                    </svg>
                    {uploadingExcel ? '업로드 중...' : '엑셀 파일 업로드'}
                  </label>
                  <p className="excel-upload-hint">
                    .xlsx, .xls 파일만 가능 (최대 10MB)
                  </p>
                </div>
              )
            )}
          </div>
        </div>

        {/* Comments Section */}
        <div className="comments-section">
          <h3 className="comments-title">댓글 ({comments.length})</h3>

          {/* Comment Input */}
          <div className="comment-input-container">
            <textarea
              className="comment-input"
              placeholder="댓글을 입력하세요..."
              value={comment}
              onChange={(e) => setComment(e.target.value)}
            />
            <div className="comment-footer">
              <span className="comment-author">{user?.userName || '익명'}</span>
              <button className="comment-submit-btn" onClick={handleCommentSubmit}>
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path d="M2 8l12-6-6 12-2-6-4-0z" fill="currentColor"/>
                </svg>
                댓글 작성
              </button>
            </div>
          </div>

          {/* Comments List */}
          {comments.length === 0 ? (
            <div className="comments-empty">
              <p>댓글이 없습니다. 첫 댓글을 작성해보세요!</p>
            </div>
          ) : (
            <div className="comments-list">
              {comments.map((c) => (
                <div key={c.commentId} className="comment-item">
                  <div className="comment-header">
                    <div className="comment-author-info">
                      <div className="comment-avatar">
                        {getInitial(c.authorName)}
                      </div>
                      <span className="comment-author-name">{c.authorName}</span>
                      <span className="comment-date">{formatDateTime(c.createdAt)}</span>
                    </div>
                    {user && user.userId === c.authorId && (
                      <button
                        className="comment-delete-btn"
                        onClick={() => handleDeleteComment(c.commentId)}
                      >
                        삭제
                      </button>
                    )}
                  </div>
                  <div className="comment-content">
                    {c.content}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </main>

      {/* Delete Confirmation Modal */}
      {showDeleteModal && (
        <div className="modal-overlay" onClick={() => setShowDeleteModal(false)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">게시글을 삭제하시겠습니까?</h2>
              <p className="modal-description">이 작업은 되돌릴 수 없습니다. 게시글이 영구적으로 삭제됩니다.</p>
            </div>
            <div className="modal-footer">
              <button className="modal-button cancel-button" onClick={() => setShowDeleteModal(false)}>
                취소
              </button>
              <button className="modal-button delete-button" onClick={handleDeleteConfirm}>
                삭제
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default PostDetail;
