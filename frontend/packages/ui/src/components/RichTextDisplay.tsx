import DOMPurify from 'dompurify';

interface RichTextDisplayProps {
  /** Primary prop name for the HTML content to render. */
  content?: string;
  /** Alias for `content` — accepts HTML string. Use one or the other. */
  html?: string;
  className?: string;
}

/** Renders sanitized HTML content inline. Use for displaying rich text notes. */
export const RichTextDisplay = ({
  content,
  html,
  className = '',
}: RichTextDisplayProps) => {
  const raw = content ?? html;

  if (!raw || raw.trim() === '') {
    return null;
  }

  // Content is sanitized via DOMPurify before rendering
  const sanitized = DOMPurify.sanitize(raw);

  return (
    <div
      className={`rich-text-display ${className}`}
      dangerouslySetInnerHTML={{ __html: sanitized }}
    />
  );
};

/** Strips HTML tags and returns plain text, useful for truncated previews. */
export const stripHtml = (html: string): string => {
  const doc = new DOMParser().parseFromString(
    DOMPurify.sanitize(html),
    'text/html'
  );
  return doc.body.textContent ?? '';
};
