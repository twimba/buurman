import { Construction } from 'lucide-react';

interface WorkInProgressProps {
  pageName?: string;
  message?: string;
}

export const WorkInProgress = ({
  pageName,
  message = "Rome wasn't built in a day... and neither was this page!",
}: WorkInProgressProps) => {
  return (
    <div className="flex items-center justify-center min-h-[calc(100vh-4rem)]">
      <div className="text-center max-w-md px-6">
        {/* Icon */}
        <div className="mb-8 flex justify-center">
          <div className="relative">
            <div className="absolute inset-0 bg-blue-100 rounded-full blur-2xl opacity-50 animate-pulse" />
            <Construction
              className="h-32 w-32 text-blue-600 relative"
              strokeWidth={1.5}
            />
          </div>
        </div>

        {/* Title */}
        <h1 className="text-3xl font-bold text-gray-900 mb-4">
          {pageName ? `${pageName} Coming Soon` : 'Coming Soon'}
        </h1>

        {/* Message */}
        <p className="text-lg text-gray-600 mb-2">{message}</p>

        {/* Subtext */}
        <p className="text-sm text-gray-500">
          We're working hard to bring you this feature. Stay tuned!
        </p>

        {/* Decorative dots */}
        <div className="mt-8 flex justify-center gap-2">
          <div
            className="h-2 w-2 rounded-full bg-blue-600 animate-bounce"
            style={{ animationDelay: '0ms' }}
          />
          <div
            className="h-2 w-2 rounded-full bg-blue-600 animate-bounce"
            style={{ animationDelay: '150ms' }}
          />
          <div
            className="h-2 w-2 rounded-full bg-blue-600 animate-bounce"
            style={{ animationDelay: '300ms' }}
          />
        </div>
      </div>
    </div>
  );
};
