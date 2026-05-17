import { Component, ErrorInfo, ReactNode } from 'react';
import { RefreshCw } from 'lucide-react';
import i18n from '@/i18n';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

class ErrorBoundary extends Component<Props, State> {
  static displayName = 'ErrorBoundary';

  public state: State = {
    hasError: false,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    // TODO: Send to Sentry when integrated: Sentry.captureException(error, { extra: errorInfo });
    if (import.meta.env.DEV) {
      console.error('Uncaught error:', error, errorInfo);
    }
  }

  public render() {
    if (this.state.hasError) {
      const t = (key: string) => i18n.t(key, { ns: 'common' });

      return (
        <div className="flex items-center justify-center min-h-[100dvh] bg-background">
          <div className="bg-surface-card p-8 rounded-lg shadow-md max-w-md">
            <h1 className="text-2xl font-bold mb-4 text-error-text">
              {t('errorBoundary.title')}
            </h1>
            <p className="text-text-secondary mb-4">
              {t('errorBoundary.description')}
            </p>
            {this.state.error && import.meta.env.DEV && (
              <details className="mb-4">
                <summary className="cursor-pointer text-sm text-text-secondary">
                  {t('errorBoundary.details')}
                </summary>
                <pre className="mt-2 text-xs bg-surface-inset p-2 rounded overflow-auto">
                  {this.state.error.message}
                </pre>
              </details>
            )}
            {!import.meta.env.DEV && (
              <p className="text-sm text-text-secondary mb-4">
                {t('errorBoundary.contactSupport')}
              </p>
            )}
            <button
              onClick={() => window.location.reload()}
              className="w-full bg-primary-500 text-white py-2 px-4 rounded hover:bg-primary-600 transition-colors flex items-center justify-center gap-2"
            >
              <RefreshCw className="h-4 w-4" />
              {t('errorBoundary.reload')}
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
