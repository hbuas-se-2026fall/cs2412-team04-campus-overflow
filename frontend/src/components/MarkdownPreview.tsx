import { useEffect, useRef } from 'react';
import { Marked, Renderer, type Tokens } from 'marked';
import hljs from 'highlight.js/lib/core';
// 只注册课程里真正会出现的语言，避免把整包 1MB 的 highlight.js 打进产物
import bash from 'highlight.js/lib/languages/bash';
import css from 'highlight.js/lib/languages/css';
import java from 'highlight.js/lib/languages/java';
import javascript from 'highlight.js/lib/languages/javascript';
import json from 'highlight.js/lib/languages/json';
import plaintext from 'highlight.js/lib/languages/plaintext';
import python from 'highlight.js/lib/languages/python';
import sql from 'highlight.js/lib/languages/sql';
import typescript from 'highlight.js/lib/languages/typescript';
import xml from 'highlight.js/lib/languages/xml';
import yaml from 'highlight.js/lib/languages/yaml';

hljs.registerLanguage('bash', bash);
hljs.registerLanguage('css', css);
hljs.registerLanguage('java', java);
hljs.registerLanguage('javascript', javascript);
hljs.registerLanguage('js', javascript);
hljs.registerLanguage('json', json);
hljs.registerLanguage('plaintext', plaintext);
hljs.registerLanguage('python', python);
hljs.registerLanguage('sql', sql);
hljs.registerLanguage('typescript', typescript);
hljs.registerLanguage('ts', typescript);
hljs.registerLanguage('xml', xml);
hljs.registerLanguage('html', xml);
hljs.registerLanguage('yaml', yaml);
hljs.registerLanguage('yml', yaml);

/**
 * Markdown 实时预览（T4-03）。
 *
 * 设计要点：
 * 1. **不信任输入**：`marked` 默认会把原始 HTML 直接透传，预览区因此可能被注入脚本。
 *    这里关掉原始 HTML（`renderer.html` 返回空串），并对链接加 `rel="noopener noreferrer"`。
 *    正式展示仍以服务端渲染的 `bodyHtml`（commonmark + OWASP Sanitizer）为准，
 *    预览只是编辑时的所见即所得。
 * 2. **代码高亮**：用 highlight.js 在渲染后对 `<pre><code class="language-xxx">` 逐块高亮，
 *    语言未知时走自动检测，高亮失败不影响内容展示。
 * 3. 没有新语法的降级：`marked` 解析失败的极端情况下退回纯文本显示，不白屏。
 */

/**
 * 类型完整的自定义 renderer：继承 marked 的 Renderer 以获得 this.parser 的类型与运行时绑定。
 * 用裸对象字面量时 this.parser 无类型（tsc 报 TS2339/TS7022），且 token 隐式 any。
 */
class PreviewRenderer extends Renderer {
  /** 关闭原始 HTML，避免预览区被注入 */
  override html(): string {
    return '';
  }

  override link({ href, title, tokens }: Tokens.Link): string {
    const text = this.parser.parseInline(tokens ?? []);
    const titleAttr = title ? ` title="${escapeAttr(title)}"` : '';
    return `<a href="${escapeAttr(href ?? '#')}" target="_blank" rel="noopener noreferrer"${titleAttr}>${text}</a>`;
  }
}

const marked = new Marked({
  gfm: true,
  breaks: true,
  async: false,
  renderer: new PreviewRenderer(),
});

function escapeAttr(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

/** 把 Markdown 源码转成 HTML 字符串（已做 HTML 转义与高亮） */
export function renderMarkdown(source: string): string {
  if (!source.trim()) return '';
  try {
    return marked.parse(source) as string;
  } catch {
    // 解析异常时退回转义后的纯文本，保证预览区始终有内容
    return `<pre>${escapeAttr(source)}</pre>`;
  }
}

interface Props {
  source: string;
  /** 空内容时的占位文案 */
  placeholder?: string;
  className?: string;
}

export function MarkdownPreview({ source, placeholder = '（没有内容可预览）', className = '' }: Props) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const root = ref.current;
    if (!root) return;
    // 对每个代码块做高亮；语言不认识时自动检测，失败就保持原样
    root.querySelectorAll<HTMLElement>('pre code').forEach((block) => {
      if (block.dataset.highlighted === 'yes') return;
      try {
        hljs.highlightElement(block);
        block.dataset.highlighted = 'yes';
      } catch {
        /* 高亮失败不影响内容 */
      }
    });
  }, [source]);

  if (!source.trim()) {
    return <div className={`md-preview md-preview-empty muted ${className}`}>{placeholder}</div>;
  }

  return (
    <div
      ref={ref}
      className={`md-preview ${className}`}
      // 内容由本文件的 renderMarkdown 生成：原始 HTML 已被丢弃，链接已加 rel
      dangerouslySetInnerHTML={{ __html: renderMarkdown(source) }}
    />
  );
}
