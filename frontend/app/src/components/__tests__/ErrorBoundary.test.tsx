import { render, screen } from '@testing-library/react';
import ErrorBoundary from '../ErrorBoundary';

function ThrowingComponent({ message }: { message?: string }) {
  throw new Error(message ?? 'Test error');
}

describe('ErrorBoundary', () => {
  it('renders children when no error occurs', () => {
    render(
      <ErrorBoundary>
        <div>Hello World</div>
      </ErrorBoundary>
    );

    expect(screen.getByText('Hello World')).toBeInTheDocument();
  });

  it('shows error screen when a child throws', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    render(
      <ErrorBoundary>
        <ThrowingComponent />
      </ErrorBoundary>
    );

    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
    expect(
      screen.getByText('An error occurred while loading the application.')
    ).toBeInTheDocument();

    consoleSpy.mockRestore();
  });

  it('shows error details in DEV mode (vitest default)', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    render(
      <ErrorBoundary>
        <ThrowingComponent message="secret internal error" />
      </ErrorBoundary>
    );

    // import.meta.env.DEV is true in vitest, so error details are shown
    expect(screen.getByText('Error details')).toBeInTheDocument();
    expect(screen.getByText('secret internal error')).toBeInTheDocument();

    consoleSpy.mockRestore();
  });

  it('shows "Reload Page" button', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    render(
      <ErrorBoundary>
        <ThrowingComponent />
      </ErrorBoundary>
    );

    expect(
      screen.getByRole('button', { name: /reload page/i })
    ).toBeInTheDocument();

    consoleSpy.mockRestore();
  });

  it('hides "contact support" message in DEV mode', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    render(
      <ErrorBoundary>
        <ThrowingComponent />
      </ErrorBoundary>
    );

    // In DEV mode (vitest default), the "contact support" message is hidden
    expect(
      screen.queryByText(/please contact support/i)
    ).not.toBeInTheDocument();

    consoleSpy.mockRestore();
  });
});
