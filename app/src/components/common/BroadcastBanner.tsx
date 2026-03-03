import { useState } from 'react';
import { Info, AlertTriangle, AlertCircle, X } from 'lucide-react';
import {
  useActiveBroadcasts,
  usePublicBroadcasts,
  useDismissBroadcast,
} from '../../hooks/useBroadcasts';
import type { BroadcastMessage } from '../../api/broadcasts';

const severityConfig = {
  INFO: {
    bg: 'bg-blue-50',
    border: 'border-blue-200',
    text: 'text-blue-800',
    icon: Info,
    iconColor: 'text-blue-500',
  },
  WARNING: {
    bg: 'bg-amber-50',
    border: 'border-amber-200',
    text: 'text-amber-800',
    icon: AlertTriangle,
    iconColor: 'text-amber-500',
  },
  CRITICAL: {
    bg: 'bg-red-50',
    border: 'border-red-200',
    text: 'text-red-800',
    icon: AlertCircle,
    iconColor: 'text-red-500',
  },
} as const;

const BroadcastItem = ({
  message,
  onDismiss,
}: {
  message: BroadcastMessage;
  onDismiss: (identifier: string) => void;
}) => {
  const config = severityConfig[message.severity];
  const Icon = config.icon;

  return (
    <div
      className={`${config.bg} ${config.border} border rounded-lg px-4 py-3 flex items-start gap-3`}
    >
      <Icon className={`h-5 w-5 ${config.iconColor} shrink-0 mt-0.5`} />
      <div className={`flex-1 min-w-0 ${config.text}`}>
        <p className="text-sm font-semibold">{message.title}</p>
        <p className="text-sm mt-0.5">{message.body}</p>
      </div>
      <button
        type="button"
        onClick={() => onDismiss(message.identifier)}
        className={`${config.text} opacity-60 hover:opacity-100 transition-opacity shrink-0 p-0.5`}
        aria-label="Dismiss message"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
};

export const AuthenticatedBroadcastBanner = () => {
  const { data: broadcasts } = useActiveBroadcasts();
  const dismissMutation = useDismissBroadcast();

  const handleDismiss = (identifier: string) => {
    dismissMutation.mutate(identifier);
  };

  if (!broadcasts?.length) {
    return null;
  }

  return (
    <div className="space-y-2 mb-4">
      {broadcasts.map((message) => (
        <BroadcastItem
          key={message.identifier}
          message={message}
          onDismiss={handleDismiss}
        />
      ))}
    </div>
  );
};

export const PublicBroadcastBanner = ({
  context,
}: {
  context: 'login' | 'register';
}) => {
  const { data: broadcasts } = usePublicBroadcasts(context);
  const [dismissed, setDismissed] = useState<Set<string>>(new Set());

  const handleDismiss = (identifier: string) => {
    setDismissed((prev) => new Set(prev).add(identifier));
  };

  const visible = broadcasts?.filter((b) => !dismissed.has(b.identifier));

  if (!visible?.length) {
    return null;
  }

  return (
    <div className="space-y-2 mb-4 w-full">
      {visible.map((message) => (
        <BroadcastItem
          key={message.identifier}
          message={message}
          onDismiss={handleDismiss}
        />
      ))}
    </div>
  );
};
