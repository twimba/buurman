import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { APIProvider, Map, Marker } from '@vis.gl/react-google-maps';
import { MapPin } from 'lucide-react';
import { env } from '@/config/env';

interface AddressMapProps {
  street: string;
  city: string;
  postalCode: string;
  countryCode: string;
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
  countryCode,
  latitude,
  longitude,
  onCoordinatesChange,
  height = 'h-96',
}: AddressMapProps) => {
  const { t } = useTranslation('properties');
  const [geocodedCoords, setGeocodedCoords] = useState<Coordinates | null>(
    null
  );
  const [geocodedLoading, setGeocodedLoading] = useState(true);
  const [geocodedError, setGeocodedError] = useState<string | null>(null);

  const apiKey = env('VITE_GOOGLE_MAPS_API_KEY');

  const hasProvidedCoords = latitude != null && longitude != null;
  const coordinates = hasProvidedCoords
    ? { lat: latitude as number, lng: longitude as number }
    : geocodedCoords;
  const loading = hasProvidedCoords ? false : geocodedLoading;
  const error = hasProvidedCoords ? null : geocodedError;

  useEffect(() => {
    if (hasProvidedCoords) {
      return;
    }

    const geocodeAddress = async () => {
      if (!apiKey) {
        setGeocodedError(t('map.apiKeyNotConfigured'));
        setGeocodedLoading(false);
        return;
      }

      const address = `${street}, ${city}, ${postalCode}, ${countryCode}`;

      try {
        // Use Google Geocoding API
        const response = await fetch(
          `https://maps.googleapis.com/maps/api/geocode/json?address=${encodeURIComponent(address)}&key=${apiKey}`
        );

        const data = await response.json();

        if (data.status === 'OK' && data.results.length > 0) {
          const location = data.results[0].geometry.location;
          const newCoords = { lat: location.lat, lng: location.lng };
          setGeocodedCoords(newCoords);
          setGeocodedError(null);

          // Notify parent component of new coordinates
          if (onCoordinatesChange) {
            onCoordinatesChange(newCoords.lat, newCoords.lng);
          }
        } else {
          setGeocodedError(t('map.locationNotFoundShort'));
        }
      } catch {
        setGeocodedError(t('map.geocodeFailed'));
      } finally {
        setGeocodedLoading(false);
      }
    };

    geocodeAddress();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [street, city, postalCode, countryCode, latitude, longitude, apiKey]); // onCoordinatesChange excluded to prevent infinite loop

  if (!apiKey) {
    return (
      <div className="bg-gray-50 rounded-lg p-8 text-center">
        <MapPin className="h-12 w-12 text-gray-300 mx-auto mb-3" />
        <p className="text-gray-600 font-medium mb-1">{t('map.unavailable')}</p>
        <p className="text-sm text-gray-500">{t('map.configureApiKey')}</p>
        <div className="mt-3 text-xs text-gray-400 bg-gray-100 rounded p-2 font-mono">
          {street}, {city}, {postalCode}, {countryCode}
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="bg-gray-50 rounded-lg p-8 text-center">
        <div className="animate-pulse">
          <MapPin className="h-12 w-12 text-gray-300 mx-auto mb-3" />
          <p className="text-gray-500">{t('map.loading')}</p>
        </div>
      </div>
    );
  }

  if (error || !coordinates) {
    return (
      <div className="bg-gray-50 rounded-lg p-8 text-center">
        <MapPin className="h-12 w-12 text-gray-300 mx-auto mb-3" />
        <p className="text-gray-600 font-medium mb-1">
          {t('map.locationNotFound')}
        </p>
        <p className="text-sm text-gray-500">
          {error || t('map.couldNotFind')}
        </p>
        <div className="mt-3 text-xs text-gray-400 bg-gray-100 rounded p-2 font-mono">
          {street}, {city}, {postalCode}, {countryCode}
        </div>
      </div>
    );
  }

  return (
    <div
      className={`w-full ${height} rounded-lg overflow-hidden border border-gray-200`}
    >
      <APIProvider apiKey={apiKey}>
        <Map
          center={coordinates}
          zoom={15}
          mapId="address-map"
          gestureHandling="cooperative"
          disableDefaultUI={false}
        >
          <Marker position={coordinates} title={`${street}, ${city}`} />
        </Map>
      </APIProvider>
    </div>
  );
};
