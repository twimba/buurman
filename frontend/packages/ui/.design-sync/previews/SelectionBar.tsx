import { SelectionBar } from '@buurman/ui';
import { CheckCircle, Trash2 } from 'lucide-react';

// `transform` establishes a containing block so the `position: fixed` bar
// resolves against this wrapper (and is captured in-frame) instead of
// anchoring to the page viewport bottom.
export function Active() {
  return (
    <div
      className="relative w-full"
      style={{ transform: 'translateZ(0)', height: 200 }}
    >
      <SelectionBar
        open={true}
        count={3}
        onCancel={() => {}}
        actions={[
          { label: 'Mark paid', icon: CheckCircle, onClick: () => {} },
          {
            label: 'Delete',
            icon: Trash2,
            tone: 'danger',
            onClick: () => {},
          },
        ]}
      />
    </div>
  );
}
