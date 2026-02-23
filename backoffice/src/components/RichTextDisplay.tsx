import DOMPurify from "dompurify";

interface RichTextDisplayProps {
  content: string;
  className?: string;
}

export const RichTextDisplay = ({
  content,
  className = "",
}: RichTextDisplayProps) => {
  if (!content || content.trim() === "") {
    return null;
  }

  const sanitized = DOMPurify.sanitize(content);

  return (
    <div
      className={`rich-text-display ${className}`}
      dangerouslySetInnerHTML={{ __html: sanitized }}
    />
  );
};
