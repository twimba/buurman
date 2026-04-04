import { cn } from '../utils/cn';

interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  error?: boolean;
  ref?: React.Ref<HTMLTextAreaElement>;
}

export function Textarea({ error, className, ref, ...props }: TextareaProps) {
  return (
    <textarea
      ref={ref}
      className={cn(
        'w-full rounded-md border bg-surface-card px-3 py-2 text-sm text-text-primary placeholder:text-text-muted',
        'transition-colors duration-150',
        'focus-ring',
        error
          ? 'border-error-border focus-visible:shadow-ring-error'
          : 'border-border-default',
        props.disabled && 'cursor-not-allowed bg-neutral-50 text-text-disabled',
        className
      )}
      {...props}
    />
  );
}

Textarea.displayName = 'Textarea';
