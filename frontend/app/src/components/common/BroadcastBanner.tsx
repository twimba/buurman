import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import DOMPurify from 'dompurify';
import { Info, AlertTriangle, AlertCircle, X } from 'lucide-react';
import {
  useActiveBroadcasts,
  usePublicBroadcasts,
  useDismissBroadcast,
} from '../../hooks/useBroadcasts';
import type { BroadcastMessage } from '../../types/broadcast';

const severityConfig = {
  INFO: {
    bg: 'bg-info-bg',
    border: 'border-info-border',
    text: 'text-info-text',
    icon: Info,
    iconColor: 'text-info-text',
  },
  WARNING: {
    bg: 'bg-warning-bg',
    border: 'border-warning-border',
    text: 'text-warning-text',
    icon: AlertTriangle,
    iconColor: 'text-warning-text',
  },
  CRITICAL: {
    bg: 'bg-error-bg',
    border: 'border-error-border',
    text: 'text-error-text',
    icon: AlertCircle,
    iconColor: 'text-error-text',
  },
} as const;

const BroadcastItem = ({
  message,
  onDismiss,
}: {
  message: BroadcastMessage;
  onDismiss: (identifier: string) => void;
}) => {
  const { t } = useTranslation('common');
  const config =
    severityConfig[message.severity as keyof typeof severityConfig] ??
    severityConfig.INFO;
  const Icon = config.icon;

  return (
    <div
      className={`${config.bg} ${config.border} border rounded-lg px-4 py-3 flex items-start gap-3`}
    >
      <Icon className={`h-5 w-5 ${config.iconColor} shrink-0 mt-0.5`} />
      <div className={`flex-1 min-w-0 ${config.text}`}>
        <p className="text-sm font-semibold">{message.title}</p>
        <div
          className="text-sm mt-0.5 prose prose-sm max-w-none [&>p]:m-0"
          dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(message.body) }}
        />
      </div>
      <button
        type="button"
        onClick={() => onDismiss(message.identifier)}
        className={`${config.text} opacity-60 hover:opacity-100 transition-opacity shrink-0 p-0.5`}
        aria-label={t('accessibility.dismiss')}
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
