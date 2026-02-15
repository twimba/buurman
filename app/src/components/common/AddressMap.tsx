import { useEffect, useState } from 'react';
import { APIProvider, Map, AdvancedMarker } from '@vis.gl/react-google-maps';
import { MapPin } from 'lucide-react';
import { env } from '../../config/env';

interface AddressMapProps {
  street: string;
  city: string;
  postalCode: string;
  country: string;
  latitude?: number | null;
  longitude?: number | null;
  onCoordinatesChange?: (lat: number, lng: number) => void;
  height?: string;
}

interface Coordinates {
  lat: number;
  lng: number;
}

export const AddressMap = ({
  street,
  city,
  postalCode,
  country,
  latitude,
  longitude,
  onCoordinatesChange,
  height = 'h-96',
}: AddressMapProps) => {
  const [coordinates, setCoordinates] = useState<Coordinates | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const apiKey = env('VITE_GOOGLE_MAPS_API_KEY');

  useEffect(() => {
    // If coordinates are already provided, use them directly
    if (
      latitude !== null &&
      latitude !== undefined &&
      longitude !== null &&
      longitude !== undefined
    ) {
      setCoordinates({ lat: latitude, lng: longitude });
      setLoading(false);
      setError(null);
      return;
    }

    const geocodeAddress = async () => {
      if (!apiKey) {
        setError('Google Maps API key not configured');
        setLoading(false);
        return;
      }

      const address = `${street}, ${city}, ${postalCode}, ${country}`;

      try {
        // Use Google Geocoding API
        const response = await fetch(
          `https://maps.googleapis.com/maps/api/geocode/json?address=${encodeURIComponent(address)}&key=${apiKey}`
        );

        const data = await response.json();

        if (data.status === 'OK' && data.results.length > 0) {
          const location = data.results[0].geometry.location;
          const newCoords = { lat: location.lat, lng: location.lng };
          setCoordinates(newCoords);
          setError(null);

          // Notify parent component of new coordinates
          if (onCoordinatesChange) {
            onCoordinatesChange(newCoords.lat, newCoords.lng);
          }
        } else {
          setError('Could not find location');
        }
      } catch {
        setError('Failed to geocode address');
      } finally {
        setLoading(false);
      }
    };

    geocodeAddress();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [street, city, postalCode, country, latitude, longitude, apiKey]); // onCoordinatesChange excluded to prevent infinite loop

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
          {street}, {city}, {postalCode}, {country}
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg p-8 text-center">
        <div className="animate-pulse">
          <MapPin className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] dark:text-[#6b7194] dark:text-[#8b90a8] mx-auto mb-3" />
          <p className="text-[#6b7194] dark:text-[#8b90a8]">Loading map...</p>
        </div>
      </div>
    );
  }

  if (error || !coordinates) {
    return (
      <div className="bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg p-8 text-center">
        <MapPin className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] dark:text-[#6b7194] dark:text-[#8b90a8] mx-auto mb-3" />
        <p className="text-[#6b7194] dark:text-[#8b90a8] font-medium mb-1">
          Location Not Found
        </p>
        <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] dark:text-[#5c6180]">
          {error || 'Could not find this address on the map'}
        </p>
        <div className="mt-3 text-xs text-[#9ca0b8] dark:text-[#5c6180] bg-[#f1f3f9] dark:bg-[#1e2130] rounded p-2 font-mono">
          {street}, {city}, {postalCode}, {country}
        </div>
      </div>
    );
  }

  return (
    <div
      className={`w-full ${height} rounded-lg overflow-hidden border border-[#e2e6f0] dark:border-[#2a2e3f]`}
    >
      <APIProvider apiKey={apiKey}>
        <Map
          center={coordinates}
          zoom={15}
          mapId="address-map"
          gestureHandling="cooperative"
          disableDefaultUI={false}
        >
          <AdvancedMarker position={coordinates} title={`${street}, ${city}`} />
        </Map>
      </APIProvider>
    </div>
  );
};
