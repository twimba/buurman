import { Component, ErrorInfo, ReactNode } from 'react';
import { RefreshCw } from 'lucide-react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Uncaught error:', error, errorInfo);
  }

  public render() {
    if (this.state.hasError) {
      return (
        <div className="flex items-center justify-center min-h-screen bg-background dark:bg-[#0c0d14]">
          <div className="bg-white dark:bg-[#14161f] p-8 rounded-lg shadow-md max-w-md">
            <h1 className="text-2xl font-bold mb-4 text-red-600 dark:text-red-400">
              Something went wrong
            </h1>
            <p className="text-[#3d4463] dark:text-[#c4c8db] mb-4">
              An error occurred while loading the application.
            </p>
            {this.state.error && (
              <details className="mb-4">
                <summary className="cursor-pointer text-sm text-[#6b7194] dark:text-[#8b90a8]">
                  Error details
                </summary>
                <pre className="mt-2 text-xs bg-[#f1f3f9] dark:bg-[#1e2130] dark:text-[#c4c8db] p-2 rounded overflow-auto">
                  {this.state.error.message}
                </pre>
              </details>
            )}
            <button
              onClick={() => window.location.reload()}
              className="w-full bg-[#5c7cfa] text-white py-2 px-4 rounded hover:bg-[#4c6ef5] dark:hover:bg-[#5c7cfa] transition-colors flex items-center justify-center gap-2"
            >
              <RefreshCw className="h-4 w-4" />
              Reload Page
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
