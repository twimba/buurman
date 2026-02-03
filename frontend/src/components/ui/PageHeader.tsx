import { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';

interface PageHeaderProps {
  /** Main title of the page */
  title: string;
  /** Subtitle or identifier (e.g., "#ABC123") */
  subtitle?: string;
  /** Additional description line */
  description?: string;
  /** Back navigation path */
  backTo: string;
  /** Status badge or other inline element after title */
  badge?: ReactNode;
  /** Avatar or icon to display before title */
  avatar?: ReactNode;
  /** Action buttons for the header */
  actions?: ReactNode;
}

export const PageHeader = ({
  title,
  subtitle,
  description,
  backTo,
  badge,
  avatar,
  actions,
}: PageHeaderProps) => {
  const navigate = useNavigate();

  return (
    <div className="bg-white border-b border-gray-100 -mx-4 px-4 py-4 mb-6">
      <div className="max-w-6xl mx-auto">
        <div className="flex items-center justify-between gap-6">
          {/* Left side: Back button + content */}
          <div className="flex items-center gap-3 min-w-0 flex-1">
            <button
              onClick={() => navigate(backTo)}
              className="
                flex-shrink-0
                p-2 -ml-2
                text-gray-400
                hover:text-gray-600
                hover:bg-gray-100
                rounded-lg
                transition-colors
              "
              aria-label="Go back"
            >
              <ArrowLeft className="h-5 w-5" />
            </button>

            {avatar && (
              <div className="flex-shrink-0">
                {avatar}
              </div>
            )}

            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-3 flex-wrap">
                <h1 className="text-lg font-semibold text-gray-900 truncate">
                  {title}
                </h1>
                {badge}
              </div>
              {(subtitle || description) && (
                <div className="flex items-center gap-2 mt-0.5">
                  {subtitle && (
                    <span className="text-sm text-gray-500 font-medium">
                      {subtitle}
                    </span>
                  )}
                  {subtitle && description && (
                    <span className="text-gray-300">·</span>
                  )}
                  {description && (
                    <span className="text-sm text-gray-500">
                      {description}
                    </span>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* Right side: Action buttons */}
          {actions && (
            <div className="flex-shrink-0 flex items-center gap-2">
              {actions}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
