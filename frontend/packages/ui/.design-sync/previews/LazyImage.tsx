import { LazyImage } from '@buurman/ui';

const HOUSE =
  'https://images.unsplash.com/photo-1568605114967-8130f3a36994?w=400&q=80';
const APARTMENT =
  'https://images.unsplash.com/photo-1554995207-c18c203602cb?w=400&q=80';

export function PropertyPhoto() {
  return (
    <div className="max-w-sm">
      <LazyImage
        src={HOUSE}
        alt="Prinsengracht 263"
        rootMargin="2000px 0px 2000px 0px"
        wrapperClassName="aspect-video rounded-lg overflow-hidden border border-border-default"
        className="h-full w-full object-cover"
      />
    </div>
  );
}

export function PhotoGrid() {
  return (
    <div className="grid grid-cols-2 gap-3 max-w-md">
      <LazyImage
        src={HOUSE}
        alt="Canal house"
        rootMargin="2000px 0px 2000px 0px"
        wrapperClassName="aspect-square rounded-lg overflow-hidden border border-border-default"
        className="h-full w-full object-cover"
      />
      <LazyImage
        src={APARTMENT}
        alt="Apartment block"
        rootMargin="2000px 0px 2000px 0px"
        wrapperClassName="aspect-square rounded-lg overflow-hidden border border-border-default"
        className="h-full w-full object-cover"
      />
    </div>
  );
}

export function PortraitCard() {
  return (
    <div className="max-w-[200px] bg-surface-card border border-border-default rounded-lg overflow-hidden">
      <LazyImage
        src={APARTMENT}
        alt="Herengracht 412"
        rootMargin="2000px 0px 2000px 0px"
        wrapperClassName="aspect-[3/4] overflow-hidden"
        className="h-full w-full object-cover"
      />
      <div className="p-3">
        <p className="font-medium text-text-primary truncate">Herengracht 412</p>
        <p className="text-sm text-text-secondary">Amsterdam · 2 units</p>
      </div>
    </div>
  );
}
