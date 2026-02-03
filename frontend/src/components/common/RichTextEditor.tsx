import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import DOMPurify from 'dompurify';
import { Bold, Italic, List, ListOrdered, Undo, Redo } from 'lucide-react';

interface RichTextEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  readOnly?: boolean;
}

export const RichTextEditor = ({
  value,
  onChange,
  placeholder = 'Enter text...',
  readOnly = false,
}: RichTextEditorProps) => {
  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        heading: false,
        code: false,
        codeBlock: false,
        blockquote: false,
        horizontalRule: false,
      }),
    ],
    content: value || '',
    editable: !readOnly,
    onUpdate: ({ editor }) => {
      const html = editor.getHTML();
      const sanitized = DOMPurify.sanitize(html);
      onChange(sanitized);
    },
  });

  if (!editor) {
    return null;
  }

  if (readOnly) {
    return (
      <div className="prose dark:prose-invert max-w-none">
        <EditorContent editor={editor} />
      </div>
    );
  }

  return (
    <div className="border border-gray-300 dark:border-gray-600 rounded-md overflow-hidden">
      {/* Toolbar */}
      <div className="flex gap-1 p-2 bg-gray-50 dark:bg-gray-700 border-b border-gray-300 dark:border-gray-600">
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleBold().run()}
          className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-600 ${
            editor.isActive('bold') ? 'bg-gray-300 dark:bg-gray-600' : ''
          }`}
          title="Bold"
        >
          <Bold className="h-4 w-4" />
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleItalic().run()}
          className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-600 ${
            editor.isActive('italic') ? 'bg-gray-300 dark:bg-gray-600' : ''
          }`}
          title="Italic"
        >
          <Italic className="h-4 w-4" />
        </button>
        <div className="w-px bg-gray-300 dark:bg-gray-600 mx-1" />
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleBulletList().run()}
          className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-600 ${
            editor.isActive('bulletList') ? 'bg-gray-300 dark:bg-gray-600' : ''
          }`}
          title="Bullet List"
        >
          <List className="h-4 w-4" />
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleOrderedList().run()}
          className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-600 ${
            editor.isActive('orderedList') ? 'bg-gray-300 dark:bg-gray-600' : ''
          }`}
          title="Numbered List"
        >
          <ListOrdered className="h-4 w-4" />
        </button>
        <div className="w-px bg-gray-300 dark:bg-gray-600 mx-1" />
        <button
          type="button"
          onClick={() => editor.chain().focus().undo().run()}
          className="p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-600"
          disabled={!editor.can().undo()}
          title="Undo"
        >
          <Undo className="h-4 w-4" />
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().redo().run()}
          className="p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-600"
          disabled={!editor.can().redo()}
          title="Redo"
        >
          <Redo className="h-4 w-4" />
        </button>
      </div>

      {/* Editor Content */}
      <div className="p-3 min-h-[150px] prose dark:prose-invert max-w-none">
        <EditorContent editor={editor} placeholder={placeholder} />
      </div>
    </div>
  );
};
