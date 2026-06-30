import { useEditor, EditorContent, Extension } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Highlight from '@tiptap/extension-highlight';
import Placeholder from '@tiptap/extension-placeholder';
import TextAlign from '@tiptap/extension-text-align';
import DOMPurify from 'dompurify';
import { useState, useCallback, useRef, useEffect } from 'react';
import {
  Bold,
  Italic,
  Underline as UnderlineIcon,
  Strikethrough,
  Highlighter,
  Link as LinkIcon,
  Unlink,
  List,
  ListOrdered,
  Quote,
  Code,
  CodeSquare,
  Minus,
  AlignLeft,
  AlignCenter,
  AlignRight,
  Heading1,
  Heading2,
  Heading3,
  Undo,
  Redo,
} from 'lucide-react';

interface RichTextEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  readOnly?: boolean;
  onSubmit?: () => void;
}

const ToolbarButton = ({
  onClick,
  isActive = false,
  disabled = false,
  title,
  children,
}: {
  onClick: () => void;
  isActive?: boolean;
  disabled?: boolean;
  title: string;
  children: React.ReactNode;
}) => (
  <button
    type="button"
    onClick={onClick}
    disabled={disabled}
    title={title}
    className={`p-1.5 rounded-md transition-colors disabled:opacity-30 disabled:cursor-not-allowed ${
      isActive
        ? 'bg-primary-100 dark:bg-primary-900/40 text-primary-700 dark:text-primary-300'
        : 'text-text-secondary hover:bg-neutral-100 dark:hover:bg-surface-inset'
    }`}
  >
    {children}
  </button>
);

const ToolbarDivider = () => (
  <div className="w-px h-6 self-center bg-border-default mx-0.5" />
);

export const RichTextEditor = ({
  value,
  onChange,
  placeholder = 'Start writing...',
  readOnly = false,
  onSubmit,
}: RichTextEditorProps) => {
  const [linkUrl, setLinkUrl] = useState('');
  const [showLinkInput, setShowLinkInput] = useState(false);
  const onSubmitRef = useRef(onSubmit);
  useEffect(() => {
    onSubmitRef.current = onSubmit;
  });

  const editor = useEditor({
    // Required for React 19 / StrictMode: defer ProseMirror view creation until after mount so the
    // keymap (Enter → new paragraph, etc.) binds to the live view instead of a discarded one.
    immediatelyRender: false,
    extensions: [
      StarterKit.configure({
        heading: { levels: [1, 2, 3] },
        // Link & Underline ship inside StarterKit v3 — configure Link here rather than re-adding the
        // standalone extensions; duplicate extensions corrupt the schema/keymap.
        link: {
          openOnClick: false,
          HTMLAttributes: {
            class:
              'text-primary-600 dark:text-primary-400 underline cursor-pointer',
          },
        },
      }),
      Highlight.configure({ multicolor: false }),
      Placeholder.configure({ placeholder }),
      TextAlign.configure({ types: ['heading', 'paragraph'] }),
      Extension.create({
        name: 'submitShortcut',
        addKeyboardShortcuts() {
          return {
            'Mod-Enter': () => {
              onSubmitRef.current?.();
              return !!onSubmitRef.current;
            },
          };
        },
      }),
    ],
    content: value ?? '',
    editable: !readOnly,
    onUpdate: ({ editor }) => {
      const html = editor.getHTML();
      const sanitized = DOMPurify.sanitize(html);
      onChange(sanitized);
    },
  });

  const setLink = useCallback(() => {
    if (!editor || !linkUrl) {
      return;
    }

    if (linkUrl === '') {
      editor.chain().focus().extendMarkRange('link').unsetLink().run();
    } else {
      editor
        .chain()
        .focus()
        .extendMarkRange('link')
        .setLink({ href: linkUrl })
        .run();
    }

    setLinkUrl('');
    setShowLinkInput(false);
  }, [editor, linkUrl]);

  const openLinkInput = useCallback(() => {
    if (!editor) {
      return;
    }
    const previousUrl = editor.getAttributes('link').href ?? '';
    setLinkUrl(previousUrl);
    setShowLinkInput(true);
  }, [editor]);

  if (!editor) {
    return null;
  }

  if (readOnly) {
    return (
      <div className="rich-text-display">
        <EditorContent editor={editor} />
      </div>
    );
  }

  return (
    <div className="border border-border-default rounded-lg overflow-hidden focus-within:border-primary-400 dark:focus-within:border-primary-600 focus-within:ring-1 focus-within:ring-primary-400/30 transition-colors">
      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-0.5 px-2 py-1.5 bg-surface-inset border-b border-border-default">
        {/* Headings */}
        <ToolbarButton
          onClick={() =>
            editor.chain().focus().toggleHeading({ level: 1 }).run()
          }
          isActive={editor.isActive('heading', { level: 1 })}
          title="Heading 1"
        >
          <Heading1 className="h-4 w-4" />
        </ToolbarButton>
        <ToolbarButton
          onClick={() =>
            editor.chain().focus().toggleHeading({ level: 2 }).run()
          }
          isActive={editor.isActive('heading', { level: 2 })}
          title="Heading 2"
        >
          <Heading2 className="h-4 w-4" />
        </ToolbarButton>
        <ToolbarButton
          onClick={() =>
            editor.chain().focus().toggleHeading({ level: 3 }).run()
          }
          isActive={editor.isActive('heading', { level: 3 })}
          title="Heading 3"
        >
          <Heading3 className="h-4 w-4" />
        </ToolbarButton>

        <ToolbarDivider />

        {/* Text formatting */}
        <ToolbarButton
          onClick={() => editor.chain().focus().toggleBold().run()}
          isActive={editor.isActive('bold')}
          title="Bold (Ctrl+B)"
        >
          <Bold className="h-4 w-4" />
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor.chain().focus().toggleItalic().run()}
          isActive={editor.isActive('italic')}
          title="Italic (Ctrl+I)"
        >
          <Italic className="h-4 w-4" />
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor.chain().focus().toggleUnderline().run()}
          isActive={editor.isActive('underline')}
          title="Underline (Ctrl+U)"
        >
          <UnderlineIcon className="h-4 w-4" />
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor.chain().focus().toggleStrike().run()}
          isActive={editor.isActive('strike')}
          title="Strikethrough"
        >
          <Strikethrough className="h-4 w-4" />
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor.chain().focus().toggleHighlight().run()}
          isActive={editor.isActive('highlight')}
          title="Highlight"
        >
          <Highlighter className="h-4 w-4" />
        </ToolbarButton>

        <ToolbarDivider />

        {/* Links */}
        <ToolbarButton
          onClick={openLinkInput}
          isActive={editor.isActive('link')}
          title="Add link"
        >
          <LinkIcon className="h-4 w-4" />
        </ToolbarButton>
        {editor.isActive('link') && (
          <ToolbarButton
            onClick={() => editor.chain().focus().unsetLink().run()}
            title="Remove link"
          >
            <Unlink className="h-4 w-4" />
          </ToolbarButton>
        )}

        <ToolbarDivider />

        {/* Lists & blocks */}
        <ToolbarButton
          onClick={() => editor.chain().focus().toggleBulletList().run()}
          isActive={editor.isActive('bulletList')}
          title="Bullet list"
        >
          <List className="h-4 w-4" />
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor.chain().focus().toggleOrderedList().run()}
          isActive={editor.isActive('orderedList')}
          title="Numbered list"
        >
          <ListOrdered className="h-4 w-4" />
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor.chain().focus().toggleBlockquote().run()}
          isActive={editor.isActive('blockquote')}
          title="Blockquote"
        >
          <Quote className="h-4 w-4" />
        </ToolbarButton>

        <ToolbarDivider />

        {/* Alignment */}
        <ToolbarButton
          onClick={() => editor.chain().focus().setTextAlign('left').run()}
          isActive={editor.isActive({ textAlign: 'left' })}
          title="Align left"
        >
          <AlignLeft className="h-4 w-4" />
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor.chain().focus().setTextAlign('center').run()}
          isActive={editor.isActive({ textAlign: 'center' })}
          title="Align center"
        >
          <AlignCenter className="h-4 w-4" />
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor.chain().focus().setTextAlign('right').run()}
          isActive={editor.isActive({ textAlign: 'right' })}
          title="Align right"
        >
          <AlignRight className="h-4 w-4" />
        </ToolbarButton>

        <ToolbarDivider />

        {/* Code & rule */}
        <ToolbarButton
          onClick={() => editor.chain().focus().toggleCode().run()}
          isActive={editor.isActive('code')}
          title="Inline code"
        >
          <Code className="h-4 w-4" />
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor.chain().focus().toggleCodeBlock().run()}
          isActive={editor.isActive('codeBlock')}
          title="Code block"
        >
          <CodeSquare className="h-4 w-4" />
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor.chain().focus().setHorizontalRule().run()}
          title="Horizontal rule"
        >
          <Minus className="h-4 w-4" />
        </ToolbarButton>

        <ToolbarDivider />

        {/* Undo/Redo */}
        <ToolbarButton
          onClick={() => editor.chain().focus().undo().run()}
          disabled={!editor.can().undo()}
          title="Undo (Ctrl+Z)"
        >
          <Undo className="h-4 w-4" />
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor.chain().focus().redo().run()}
          disabled={!editor.can().redo()}
          title="Redo (Ctrl+Shift+Z)"
        >
          <Redo className="h-4 w-4" />
        </ToolbarButton>
      </div>

      {/* Link input popover */}
      {showLinkInput && (
        <div className="flex items-center gap-2 px-3 py-2 bg-surface-inset border-b border-border-default">
          <LinkIcon className="h-4 w-4 text-text-secondary flex-shrink-0" />
          <input
            type="url"
            value={linkUrl}
            onChange={(e) => setLinkUrl(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault();
                setLink();
              }
              if (e.key === 'Escape') {
                setShowLinkInput(false);
              }
            }}
            placeholder="https://example.com"
            className="flex-1 text-sm bg-surface-card border border-border-default rounded px-2 py-1 outline-none focus:border-primary-400 dark:focus:border-primary-600"
            autoFocus
          />
          <button
            type="button"
            onClick={setLink}
            className="text-xs font-medium px-2.5 py-1 rounded bg-primary-600 text-white hover:bg-primary-700 transition-colors"
          >
            Apply
          </button>
          <button
            type="button"
            onClick={() => setShowLinkInput(false)}
            className="text-xs font-medium px-2.5 py-1 rounded text-text-secondary hover:bg-neutral-100 dark:hover:bg-surface-inset transition-colors"
          >
            Cancel
          </button>
        </div>
      )}

      {/* Editor content */}
      <div className="px-4 py-3 min-h-[150px]">
        <EditorContent editor={editor} />
      </div>
    </div>
  );
};
