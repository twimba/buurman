import { APIProvider, Map, AdvancedMarker } from '@vis.gl/react-google-maps';
import { MapPin } from 'lucide-react';
import { env } from '../../config/env';

interface PropertyMapProps {
  street: string;
  city: string;
  latitude?: number | null;
  longitude?: number | null;
}

export const PropertyMap = ({
  street,
  city,
  latitude,
  longitude,
}: PropertyMapProps) => {
  const apiKey = env('VITE_GOOGLE_MAPS_API_KEY');

  if (!apiKey) {
    return (
      <div className="bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg p-8 text-center">
        <MapPin className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] dark:text-[#6b7194] dark:text-[#8b90a8] mx-auto mb-3" />
        <p className="text-[#6b7194] dark:text-[#8b90a8] font-medium mb-1">
          Map Preview Unavailable
        </p>
        <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] dark:text-[#5c6180]">
          Configure VITE_GOOGLE_MAPS_API_KEY to enable maps
        </p>
        <div className="mt-3 text-xs text-[#9ca0b8] dark:text-[#5c6180] bg-[#f1f3f9] dark:bg-[#1e2130] rounded p-2 font-mono">
          {street}, {city}
        </div>
      </div>
    );
  }

  if (latitude == null || longitude == null) {
    return (
      <div className="bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg p-8 text-center">
        <MapPin className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] dark:text-[#6b7194] dark:text-[#8b90a8] mx-auto mb-3" />
        <p className="text-[#6b7194] dark:text-[#8b90a8] font-medium mb-1">
          Location Not Found
        </p>
        <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] dark:text-[#5c6180]">
          Could not find this address on the map
        </p>
        <div className="mt-3 text-xs text-[#9ca0b8] dark:text-[#5c6180] bg-[#f1f3f9] dark:bg-[#1e2130] rounded p-2 font-mono">
          {street}, {city}
        </div>
      </div>
    );
  }

  const coordinates = { lat: latitude, lng: longitude };

  return (
    <div className="w-full h-96 rounded-lg overflow-hidden border border-[#e2e6f0] dark:border-[#2a2e3f]">
      <APIProvider apiKey={apiKey}>
        <Map
          center={coordinates}
          zoom={15}
          mapId="property-map"
          gestureHandling="cooperative"
          disableDefaultUI={false}
        >
          <AdvancedMarker position={coordinates} title={`${street}, ${city}`} />
        </Map>
      </APIProvider>
    </div>
  );
};
