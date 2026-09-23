import { lazy, Suspense } from 'react';
import type { RichTextEditorProps } from './RichTextEditorImpl';

/**
 * Lazily-loaded rich text editor.
 *
 * The TipTap/ProseMirror bundle is ~118 KB gzipped — larger than React itself —
 * and is only reached from forms and modals, all of which already live in lazy
 * route chunks. Importing it eagerly through the package barrel would pull it
 * into the entry graph of every consumer, so the editor is split behind a
 * dynamic import while keeping the same public component API.
 */
const RichTextEditorImpl = lazy(() =>
  import('./RichTextEditorImpl').then((m) => ({ default: m.RichTextEditorImpl }))
);

/**
 * Mirrors the editor's real structure — bordered shell, toolbar strip, 150px
 * content area — so swapping in the loaded editor causes no layout shift.
 */
const EditorSkeleton = () => (
  <div
    className="overflow-hidden rounded-lg border border-border-default"
    aria-hidden="true"
  >
    <div className="flex items-center gap-1 border-b border-border-default bg-surface-inset px-2 py-1.5">
      {Array.from({ length: 6 }, (_, i) => (
        <span
          key={i}
          className="h-7 w-7 animate-pulse rounded bg-border-default/60"
        />
      ))}
    </div>
    <div className="min-h-[150px] px-4 py-3">
      <span className="block h-3 w-2/5 animate-pulse rounded bg-border-default/60" />
    </div>
  </div>
);

export const RichTextEditor = (props: RichTextEditorProps) => (
  <Suspense fallback={<EditorSkeleton />}>
    <RichTextEditorImpl {...props} />
  </Suspense>
);

export type { RichTextEditorProps };
