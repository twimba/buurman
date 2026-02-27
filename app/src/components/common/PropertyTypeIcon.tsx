import { Home } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import {
  PROPERTY_CATEGORY_ICONS,
  PROPERTY_TYPE_ICONS,
} from '@/utils/propertyIcons';
import type { PropertyCategory } from '@/types/property';

interface PropertyTypeIconProps {
  type?: string | null;
  category?: PropertyCategory | string | null;
  size?: number;
  className?: string;
}

export const PropertyTypeIcon = ({
  type,
  category,
  size = 16,
  className,
}: PropertyTypeIconProps) => {
  let Icon: LucideIcon = Home;
  if (type && PROPERTY_TYPE_ICONS[type]) {
    Icon = PROPERTY_TYPE_ICONS[type];
  } else if (
    category &&
    PROPERTY_CATEGORY_ICONS[category as PropertyCategory]
  ) {
    Icon = PROPERTY_CATEGORY_ICONS[category as PropertyCategory];
  }
  return <Icon size={size} className={className} />;
};
