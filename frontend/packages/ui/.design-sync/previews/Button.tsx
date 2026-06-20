import { Button } from '@buurman/ui';
import { Plus, ArrowRight, Trash2 } from 'lucide-react';

export function Variants() {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <Button variant="primary">Add property</Button>
      <Button variant="secondary">Cancel</Button>
      <Button variant="success">Approve</Button>
      <Button variant="danger">Delete</Button>
      <Button variant="ghost">Dismiss</Button>
    </div>
  );
}

export function Sizes() {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <Button size="sm">Small</Button>
      <Button size="md">Medium</Button>
      <Button size="lg">Large</Button>
    </div>
  );
}

export function WithIcons() {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <Button leftIcon={<Plus />}>New tenant</Button>
      <Button variant="secondary" rightIcon={<ArrowRight />}>
        Continue
      </Button>
      <Button variant="danger" leftIcon={<Trash2 />}>
        Remove
      </Button>
    </div>
  );
}

export function States() {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <Button isLoading>Saving…</Button>
      <Button disabled>Disabled</Button>
      <Button variant="secondary" disabled>
        Unavailable
      </Button>
    </div>
  );
}
