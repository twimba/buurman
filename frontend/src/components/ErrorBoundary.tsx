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
        <div className="flex items-center justify-center min-h-screen bg-background dark:bg-gray-900">
          <div className="bg-white dark:bg-gray-800 p-8 rounded-lg shadow-md dark:shadow-gray-900 max-w-md">
            <h1 className="text-2xl font-bold mb-4 text-red-600 dark:text-red-400">
              Something went wrong
            </h1>
            <p className="text-gray-700 dark:text-gray-300 mb-4">
              An error occurred while loading the application.
            </p>
            {this.state.error && (
              <details className="mb-4">
                <summary className="cursor-pointer text-sm text-gray-600 dark:text-gray-400">
                  Error details
                </summary>
                <pre className="mt-2 text-xs bg-gray-100 dark:bg-gray-700 dark:text-gray-300 p-2 rounded overflow-auto">
                  {this.state.error.message}
                </pre>
              </details>
            )}
            <button
              onClick={() => window.location.reload()}
              className="w-full bg-blue-600 text-white py-2 px-4 rounded hover:bg-blue-700 dark:hover:bg-blue-600 transition-colors flex items-center justify-center gap-2"
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
