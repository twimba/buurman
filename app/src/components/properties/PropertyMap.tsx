import { useEffect, useState } from 'react';
import { APIProvider, Map, Marker } from '@vis.gl/react-google-maps';
import { MapPin } from 'lucide-react';

interface PropertyMapProps {
  street: string;
  city: string;
  postalCode: string;
  country: string;
  latitude?: number | null;
  longitude?: number | null;
  onCoordinatesChange?: (lat: number, lng: number) => void;
}

interface Coordinates {
  lat: number;
  lng: number;
}

export const PropertyMap = ({
  street,
  city,
  postalCode,
  country,
  latitude,
  longitude,
  onCoordinatesChange,
}: PropertyMapProps) => {
  const [coordinates, setCoordinates] = useState<Coordinates | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY ?? '';

  useEffect(() => {
    // If coordinates are already provided, use them directly
    if (latitude != null && longitude != null) {
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
      <div className="bg-gray-50 rounded-lg p-8 text-center">
        <MapPin className="h-12 w-12 text-gray-300 mx-auto mb-3" />
        <p className="text-gray-600 font-medium mb-1">
          Map Preview Unavailable
        </p>
        <p className="text-sm text-gray-500">
          Configure VITE_GOOGLE_MAPS_API_KEY to enable maps
        </p>
        <div className="mt-3 text-xs text-gray-400 bg-gray-100 rounded p-2 font-mono">
          {street}, {city}, {postalCode}, {country}
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="bg-gray-50 rounded-lg p-8 text-center">
        <div className="animate-pulse">
          <MapPin className="h-12 w-12 text-gray-300 mx-auto mb-3" />
          <p className="text-gray-500">Loading map...</p>
        </div>
      </div>
    );
  }

  if (error || !coordinates) {
    return (
      <div className="bg-gray-50 rounded-lg p-8 text-center">
        <MapPin className="h-12 w-12 text-gray-300 mx-auto mb-3" />
        <p className="text-gray-600 font-medium mb-1">Location Not Found</p>
        <p className="text-sm text-gray-500">
          {error || 'Could not find this address on the map'}
        </p>
        <div className="mt-3 text-xs text-gray-400 bg-gray-100 rounded p-2 font-mono">
          {street}, {city}, {postalCode}, {country}
        </div>
      </div>
    );
  }

  return (
    <div className="w-full h-96 rounded-lg overflow-hidden border border-gray-200">
      <APIProvider apiKey={apiKey}>
        <Map
          center={coordinates}
          zoom={15}
          mapId="property-map"
          gestureHandling="cooperative"
          disableDefaultUI={false}
        >
          <Marker position={coordinates} title={`${street}, ${city}`} />
        </Map>
      </APIProvider>
    </div>
  );
};
