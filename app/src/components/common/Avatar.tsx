interface AvatarProps {
  firstName: string;
  lastName?: string;
  photoUrl?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
}

const sizeClasses = {
  sm: 'h-8 w-8 text-xs',
  md: 'h-12 w-12 text-sm',
  lg: 'h-16 w-16 text-lg',
  xl: 'h-24 w-24 text-2xl',
};

export const Avatar = ({
  firstName,
  lastName,
  photoUrl,
  size = 'md',
  className = '',
}: AvatarProps) => {
  const getInitials = () => {
    const firstInitial = firstName?.charAt(0).toUpperCase() ?? '';
    const lastInitial = lastName?.charAt(0).toUpperCase() ?? '';
    return firstInitial + lastInitial;
  };

  const getBackgroundColor = () => {
    // Generate a consistent color based on the name
    const name = `${firstName}${lastName}`;
    const colors = [
      'bg-blue-500',
      'bg-green-500',
      'bg-purple-500',
      'bg-pink-500',
      'bg-indigo-500',
      'bg-yellow-500',
      'bg-red-500',
      'bg-teal-500',
    ];
    const index =
      name.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0) %
      colors.length;
    return colors[index];
  };

  if (photoUrl) {
    return (
      <img
        src={photoUrl}
        alt={`${firstName} ${lastName ?? ''}`}
        className={`${sizeClasses[size]} rounded-full object-cover ${className}`}
      />
    );
  }

  return (
    <div
      className={`${sizeClasses[size]} rounded-full flex items-center justify-center text-white font-semibold ${getBackgroundColor()} ${className}`}
    >
      {getInitials()}
    </div>
  );
};
