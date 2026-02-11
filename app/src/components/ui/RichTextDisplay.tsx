import DOMPurify from 'dompurify';

interface RichTextDisplayProps {
  html: string;
  className?: string;
}

/** Renders sanitized HTML content inline. Use for displaying rich text notes. */
export const RichTextDisplay = ({
  html,
  className = '',
}: RichTextDisplayProps) => {
  const sanitized = DOMPurify.sanitize(html);
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
  return doc.body.textContent || '';
};
