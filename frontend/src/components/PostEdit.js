import React, { useState, useEffect } from 'react';
import { useHistory, useParams } from 'react-router-dom';
import axios from 'axios';
import './PostEdit.css';

function PostEdit() {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('free');
  const [isPublic, setIsPublic] = useState(true);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [existingFiles, setExistingFiles] = useState([]);
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [isDragging, setIsDragging] = useState(false);
  const [uploadedImages, setUploadedImages] = useState([]);
  const contentEditableRef = React.useRef(null);
  const history = useHistory();
  const { id } = useParams();

  useEffect(() => {
    checkAuth();
    fetchPost();
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
      const post = response.data;
      setTitle(post.title);
      setContent(post.content);
      setCategory(post.isNotice ? 'notice' : 'free');

      // contentEditable에 초기 내용 설정
      setTimeout(() => {
        if (contentEditableRef.current) {
          contentEditableRef.current.textContent = post.content;
        }
      }, 0);

      setLoading(false);
    } catch (err) {
      console.error('게시글 로딩 실패:', err);
      alert('게시글을 불러올 수 없습니다.');
      history.push('/posts');
    }
  };

  const fetchFiles = async () => {
    try {
      const response = await axios.get(`/api/files/post/${id}`);
      setExistingFiles(response.data);
    } catch (err) {
      console.error('첨부파일 로딩 실패:', err);
    }
  };

  const insertImageIntoEditor = (file) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const imageUrl = e.target.result;

      // contentEditable div에 이미지 삽입
      const img = document.createElement('img');
      img.src = imageUrl;
      img.style.maxWidth = '100%';
      img.style.height = 'auto';
      img.style.display = 'block';
      img.style.margin = '10px 0';
      img.setAttribute('data-file-name', file.name);

      const editor = contentEditableRef.current;
      if (editor) {
        // 현재 커서 위치에 삽입
        const selection = window.getSelection();
        if (selection.rangeCount > 0) {
          const range = selection.getRangeAt(0);
          range.deleteContents();
          range.insertNode(img);

          // 이미지 뒤에 개행 추가
          const br = document.createElement('br');
          range.collapse(false);
          range.insertNode(br);

          // 커서를 이미지 뒤로 이동
          range.setStartAfter(br);
          range.collapse(true);
          selection.removeAllRanges();
          selection.addRange(range);
        } else {
          editor.appendChild(img);
        }
      }

      // 파일 저장
      setUploadedImages(prev => [...prev, { file, dataUrl: imageUrl }]);
    };
    reader.readAsDataURL(file);
  };

  const handleFileSelect = (e) => {
    const files = Array.from(e.target.files);

    for (let file of files) {
      if (file.size > 10 * 1024 * 1024) {
        alert(`파일 "${file.name}"의 크기가 10MB를 초과합니다.`);
        return;
      }
    }

    // 이미지 파일은 에디터에 삽입
    files.forEach(file => {
      if (file.type.startsWith('image/')) {
        insertImageIntoEditor(file);
      }
    });

    setSelectedFiles([...selectedFiles, ...files]);
  };

  const removeFile = (index) => {
    setSelectedFiles(selectedFiles.filter((_, i) => i !== index));
  };

  const deleteExistingFile = async (fileId) => {
    if (!window.confirm('파일을 삭제하시겠습니까?')) {
      return;
    }

    try {
      await axios.delete(`/api/files/${fileId}`);
      fetchFiles();
    } catch (err) {
      alert('파일 삭제에 실패했습니다.');
    }
  };

  // 드래그 앤 드롭 핸들러
  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    const files = Array.from(e.dataTransfer.files);

    // 이미지 파일만 필터링
    const imageFiles = files.filter(file => file.type.startsWith('image/'));

    if (imageFiles.length === 0) {
      alert('이미지 파일만 첨부할 수 있습니다.');
      return;
    }

    // 파일 크기 체크 (각 파일 10MB 제한)
    for (let file of imageFiles) {
      if (file.size > 10 * 1024 * 1024) {
        alert(`파일 "${file.name}"의 크기가 10MB를 초과합니다.`);
        return;
      }
    }

    // 이미지를 에디터에 삽입
    imageFiles.forEach(file => insertImageIntoEditor(file));

    setSelectedFiles([...selectedFiles, ...imageFiles]);
  };

  // 붙여넣기 핸들러
  const handlePaste = (e) => {
    const items = e.clipboardData.items;

    for (let i = 0; i < items.length; i++) {
      if (items[i].type.indexOf('image') !== -1) {
        e.preventDefault();
        const file = items[i].getAsFile();

        if (file.size > 10 * 1024 * 1024) {
          alert(`파일 크기가 10MB를 초과합니다.`);
          return;
        }

        // 이미지를 에디터에 삽입
        insertImageIntoEditor(file);
        setSelectedFiles([...selectedFiles, file]);
      }
    }
  };

  // contentEditable의 내용이 변경될 때
  const handleContentChange = () => {
    const editor = contentEditableRef.current;
    if (editor) {
      const text = editor.innerText;
      setContent(text);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // contentEditable에서 HTML 추출 (이미지 포함)
    const editor = contentEditableRef.current;
    const contentHTML = editor ? editor.innerHTML : '';
    const contentText = editor ? editor.innerText.trim() : '';

    if (!title.trim() || !contentText) {
      alert('제목과 내용을 입력해주세요.');
      return;
    }

    try {
      // 게시글 수정 (HTML 저장하여 이미지 base64 포함)
      await axios.put(`/api/posts/${id}`, {
        title,
        content: contentHTML,
        isNotice: category === 'notice'
      });

      // 새 첨부파일 업로드만 수행 (selectedFiles만 업로드, uploadedImages는 제외)
      if (selectedFiles.length > 0) {
        for (let file of selectedFiles) {
          const formData = new FormData();
          formData.append('file', file);
          formData.append('postId', id);

          try {
            await axios.post('/api/files/upload', formData, {
              headers: {
                'Content-Type': 'multipart/form-data'
              }
            });
          } catch (fileErr) {
            console.error('파일 업로드 실패:', fileErr);
          }
        }
      }

      alert('게시글이 수정되었습니다.');
      history.push(`/posts/${id}`);
    } catch (err) {
      alert(err.response?.data?.error || '수정에 실패했습니다.');
    }
  };

  const getInitial = (name) => {
    return name ? name.charAt(0) : '?';
  };

  if (loading) {
    return <div>로딩 중...</div>;
  }

  return (
    <div className="post-edit-container">
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

        {/* Form Card */}
        <div className="form-card">
          <div className="form-header">
            <h2 className="form-title">게시글 수정</h2>
            <p className="form-subtitle">게시글 정보를 입력해주세요</p>
          </div>

          <form onSubmit={handleSubmit}>
            {/* Author Field */}
            <div className="form-group">
              <label>작성자</label>
              <input
                type="text"
                value={user?.userName || '홍길동'}
                disabled
                className="disabled-input"
              />
              <p className="field-hint">로그인한 사용자 이름이 자동으로 표시됩니다</p>
            </div>

            {/* Category Field */}
            <div className="form-group">
              <label>
                카테고리 <span className="required">*</span>
              </label>
              <div className="select-wrapper">
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  required
                >
                  <option value="free">자유게시판</option>
                  <option value="question">질문답변</option>
                  <option value="review">후기</option>
                </select>
                <svg className="select-icon" width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path d="M4 6l4 4 4-4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
              </div>
              <p className="field-hint">공지사항은 관리자만 작성할 수 있습니다</p>
            </div>

            {/* Title Field */}
            <div className="form-group">
              <label>
                제목 <span className="required">*</span>
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="제목을 입력하세요"
                required
              />
            </div>

            {/* Content Field */}
            <div className="form-group">
              <label>
                내용 <span className="required">*</span>
              </label>
              <div
                className={`textarea-wrapper ${isDragging ? 'dragging' : ''}`}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
              >
                <div
                  ref={contentEditableRef}
                  contentEditable
                  className="content-editor"
                  onInput={handleContentChange}
                  onPaste={handlePaste}
                  data-placeholder="내용을 입력하세요 (이미지를 붙여넣거나 드래그하여 첨부할 수 있습니다)"
                  suppressContentEditableWarning
                />
                {isDragging && (
                  <div className="drag-overlay">
                    <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
                      <path d="M24 16v16M16 24h16" stroke="currentColor" strokeWidth="3" strokeLinecap="round"/>
                    </svg>
                    <p>이미지를 여기에 놓으세요</p>
                  </div>
                )}
              </div>
              <p className="char-count">현재 {content.length}자</p>
            </div>

            {/* File Upload */}
            <div className="form-group">
              <label>첨부파일</label>

              {/* Existing Files */}
              {existingFiles.length > 0 && (
                <div className="selected-files-list">
                  {existingFiles.map((file) => (
                    <div key={file.fileId} className="selected-file-item">
                      <span className="file-name">{file.originalFilename}</span>
                      <span className="file-size">
                        ({(file.fileSize / 1024).toFixed(1)} KB)
                      </span>
                      <button
                        type="button"
                        className="file-remove-btn"
                        onClick={() => deleteExistingFile(file.fileId)}
                      >
                        ×
                      </button>
                    </div>
                  ))}
                </div>
              )}

              <input
                type="file"
                id="file-input"
                multiple
                onChange={handleFileSelect}
                style={{ display: 'none' }}
              />
              <button
                type="button"
                className="file-select-btn"
                onClick={() => document.getElementById('file-input').click()}
              >
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path d="M8 3v10M3 8h10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
                </svg>
                파일 선택
              </button>
              <p className="field-hint">최대 10MB, 여러 파일 선택 가능</p>

              {/* New Selected Files List */}
              {selectedFiles.length > 0 && (
                <div className="selected-files-list">
                  {selectedFiles.map((file, index) => (
                    <div key={index} className="selected-file-item">
                      {file.type.startsWith('image/') && (
                        <div className="file-preview">
                          <img
                            src={URL.createObjectURL(file)}
                            alt={file.name}
                            onLoad={(e) => URL.revokeObjectURL(e.target.src)}
                          />
                        </div>
                      )}
                      <span className="file-name">{file.name} (새 파일)</span>
                      <span className="file-size">
                        ({(file.size / 1024).toFixed(1)} KB)
                      </span>
                      <button
                        type="button"
                        className="file-remove-btn"
                        onClick={() => removeFile(index)}
                      >
                        ×
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Public Setting */}
            <div className="form-group">
              <label>
                공개 설정 <span className="required">*</span>
              </label>
              <div className="radio-group">
                <label className="radio-label">
                  <input
                    type="radio"
                    name="isPublic"
                    checked={isPublic}
                    onChange={() => setIsPublic(true)}
                  />
                  <span className="radio-custom"></span>
                  <span className="radio-text">공개 - 모든 사용자에게 보입니다</span>
                </label>
                <label className="radio-label">
                  <input
                    type="radio"
                    name="isPublic"
                    checked={!isPublic}
                    onChange={() => setIsPublic(false)}
                  />
                  <span className="radio-custom"></span>
                  <span className="radio-text">비공개 - 작성자만 볼 수 있습니다</span>
                </label>
              </div>
            </div>

            {/* Form Actions */}
            <div className="form-actions">
              <button type="submit" className="submit-btn">
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path d="M8 2L14 8L8 14M14 8H2" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
                수정하기
              </button>
              <button type="button" className="cancel-btn" onClick={() => history.push(`/posts/${id}`)}>
                취소
              </button>
            </div>
          </form>
        </div>
      </main>
    </div>
  );
}

export default PostEdit;
