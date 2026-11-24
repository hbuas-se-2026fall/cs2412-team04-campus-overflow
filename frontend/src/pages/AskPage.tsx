import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import { MarkdownPreview } from '../components/MarkdownPreview';

type BodyMode = 'edit' | 'preview' | 'split';

export function AskPage() {
  const { me } = useAuth();
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [tags, setTags] = useState('');
  const [courseId, setCourseId] = useState<string>('');
  const [error, setError] = useState('');
  const [mode, setMode] = useState<BodyMode>('edit');

  /**
   * FE-03：US-02.feature 与 user-guide「所属课程（可选）」都允许不选课程发布公开问题，
   * 因此这里不再用 hasCourse 硬性拦截；仍提示「未加入课程」以便用户知道可以先去加课。
   */
  const hasCourse = (me?.courses?.length ?? 0) > 0;
  /** 标题与正文都填了才允许点击，减少无意义的 422 */
  const canSubmit = title.trim().length > 0 && body.trim().length > 0;

  const submit = async () => {
    try {
      const { id } = await api.ask({
        courseId: courseId ? Number(courseId) : undefined,
        title,
        body,
        tags: tags.split(/[,，\s]+/).filter(Boolean),
      });
      navigate(`/questions/${id}`);
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <div className="form-page">
      <h1>提出一个问题</h1>
      <label>
        标题（10–150 字，尽量具体）
        <input value={title} onChange={(e) => setTitle(e.target.value)} />
      </label>

      <div className="md-field">
        <div className="md-field-head">
          <span>正文（Markdown，至少 20 字：说明你尝试过什么、卡在哪里）</span>
          {/* 编辑 / 预览 / 分屏：实时预览正文效果与代码高亮 */}
          <div className="tabs tabs-sm">
            <button type="button" className={mode === 'edit' ? 'active' : ''} onClick={() => setMode('edit')}>
              编辑
            </button>
            <button
              type="button"
              className={mode === 'preview' ? 'active' : ''}
              onClick={() => setMode('preview')}
            >
              预览
            </button>
            <button
              type="button"
              className={mode === 'split' ? 'active' : ''}
              onClick={() => setMode('split')}
            >
              分屏
            </button>
          </div>
        </div>

        <div className={`md-field-body ${mode === 'split' ? 'is-split' : ''}`}>
          {mode !== 'preview' && (
            <textarea
              rows={14}
              value={body}
              onChange={(e) => setBody(e.target.value)}
              placeholder={'支持 Markdown：**加粗**、`行内代码`、列表、以及 ```java 代码块```'}
            />
          )}
          {mode !== 'edit' && (
            <MarkdownPreview source={body} placeholder="左侧输入后，这里实时显示渲染效果和代码高亮" />
          )}
        </div>
      </div>

      <label>
        标签（1–5 个，用空格或逗号分隔）
        <input value={tags} onChange={(e) => setTags(e.target.value)} placeholder="数据结构 红黑树" />
      </label>
      <label>
        所属课程（可选）
        <select value={courseId} onChange={(e) => setCourseId(e.target.value)}>
          <option value="">不关联课程</option>
          {me?.courses.map((c) => (
            <option key={c.id} value={c.id}>
              {c.code} {c.name}
            </option>
          ))}
        </select>
      </label>
      {error && <p className="error">{error}</p>}
      {!hasCourse && (
        <p className="muted">
          你还没有加入任何课程，可以先到
          <a href="/courses">课程页</a>
          加入，加入后提问会自动关联课程；也可以直接发布公开问题。
        </p>
      )}
      <button className="primary" onClick={submit} disabled={!canSubmit}>
        发布问题
      </button>
    </div>
  );
}
