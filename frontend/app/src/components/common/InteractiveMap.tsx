/// <reference types="@types/google.maps" />
import { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import {
  APIProvider,
  Map,
  AdvancedMarker,
  MapMouseEvent,
} from '@vis.gl/react-google-maps';
import { Loader2, MapPin, MousePointerClick } from 'lucide-react';
import { env } from '../../config/env';

// Approximate center coordinates for common countries
const COUNTRY_CENTERS: Record<string, { lat: number; lng: number }> = {
  NL: { lat: 52.13, lng: 5.29 },
  DE: { lat: 51.16, lng: 10.45 },
  BE: { lat: 50.5, lng: 4.47 },
  FR: { lat: 46.23, lng: 2.21 },
  GB: { lat: 55.38, lng: -3.44 },
  US: { lat: 37.09, lng: -95.71 },
  PT: { lat: 39.4, lng: -8.22 },
  ES: { lat: 40.46, lng: -3.75 },
  IT: { lat: 41.87, lng: 12.57 },
  AT: { lat: 47.52, lng: 14.55 },
  CH: { lat: 46.82, lng: 8.23 },
  LU: { lat: 49.82, lng: 6.13 },
  IE: { lat: 53.14, lng: -7.69 },
  DK: { lat: 56.26, lng: 9.5 },
  SE: { lat: 60.13, lng: 18.64 },
  NO: { lat: 60.47, lng: 8.47 },
  FI: { lat: 61.92, lng: 25.75 },
  PL: { lat: 51.92, lng: 19.15 },
  CZ: { lat: 49.82, lng: 15.47 },
  GR: { lat: 39.07, lng: 21.82 },
  BR: { lat: -14.24, lng: -51.93 },
  CA: { lat: 56.13, lng: -106.35 },
  AU: { lat: -25.27, lng: 133.78 },
};

const DEFAULT_CENTER = { lat: 50.0, lng: 10.0 }; // Europe center

interface InteractiveMapProps {
  street: string;
  city: string;
  latitude?: number | null;
  longitude?: number | null;
  geocodeAccuracy?: string | null;
  isGeocoding?: boolean;
  onLocationChange?: (lat: number, lng: number) => void;
  height?: string;
  defaultCountryCode?: string;
}

const placeholderCls =
  'bg-surface-page rounded-lg flex flex-col items-center justify-center text-center';

function getAccuracyMessageKey(
  accuracy: string | null | undefined
): string | null {
  switch (accuracy) {
    case 'ROOFTOP':
    case 'RANGE_INTERPOLATED':
      return 'map.accuracy.rooftop';
    case 'MANUAL':
      return 'map.accuracy.manual';
    case 'CITY':
    case 'GEOMETRIC_CENTER':
    case 'APPROXIMATE':
      return 'map.accuracy.approximate';
    case 'COUNTRY':
      return 'map.accuracy.country';
    default:
      return null;
  }
}

function getZoomForAccuracy(accuracy: string | null | undefined): number {
  switch (accuracy) {
    case 'ROOFTOP':
    case 'RANGE_INTERPOLATED':
    case 'MANUAL':
      return 15;
    case 'CITY':
    case 'GEOMETRIC_CENTER':
    case 'APPROXIMATE':
      return 12;
    case 'COUNTRY':
      return 5;
    default:
      return 15;
  }
}

function getCountryCenter(countryCode?: string): { lat: number; lng: number } {
  if (!countryCode) {
    return DEFAULT_CENTER;
  }
  return COUNTRY_CENTERS[countryCode.toUpperCase()] ?? DEFAULT_CENTER;
}

function parseGoogleMapsChannel(value: string): number | undefined {
  if (!value) {
    return undefined;
  }

  const channel = Number(value);
  if (!Number.isInteger(channel) || channel < 0 || channel > 999) {
    return undefined;
  }

  return channel;
}

export const InteractiveMap = ({
  street,
  city,
  latitude,
  longitude,
  geocodeAccuracy,
  isGeocoding = false,
  onLocationChange,
  height = 'h-96',
  defaultCountryCode,
}: InteractiveMapProps) => {
  const { t } = useTranslation('properties');
  const apiKey = env('VITE_GOOGLE_MAPS_API_KEY');
  const mapsChannel = parseGoogleMapsChannel(env('VITE_GOOGLE_MAPS_CHANNEL'));
  const [clickToPlaceActive, setClickToPlaceActive] = useState(false);
  const [mapsApiLoadError, setMapsApiLoadError] = useState(false);
  const isInteractive = !!onLocationChange;

  const handleMapsApiError = useCallback((error: unknown) => {
    console.error('Failed to load Google Maps API', error);
    setMapsApiLoadError(true);
  }, []);

  const apiProviderProps =
    mapsChannel == null
      ? { apiKey, onError: handleMapsApiError }
      : { apiKey, onError: handleMapsApiError, channel: mapsChannel };

  const handleDragEnd = useCallback(
    (event: google.maps.MapMouseEvent) => {
      if (!onLocationChange || !event.latLng) {
        return;
      }
      onLocationChange(event.latLng.lat(), event.latLng.lng());
    },
    [onLocationChange]
  );

  const handleMapClick = useCallback(
    (event: MapMouseEvent) => {
      if (!onLocationChange || !clickToPlaceActive) {
        return;
      }
      const detail = event.detail;
      if (detail.latLng) {
        onLocationChange(detail.latLng.lat, detail.latLng.lng);
        setClickToPlaceActive(false);
      }
    },
    [onLocationChange, clickToPlaceActive]
  );

  // No API key
  if (!apiKey) {
    return (
      <div className={`${height} ${placeholderCls}`}>
        <MapPin className="h-12 w-12 text-text-disabled mb-3" />
        <p className="text-text-secondary font-medium mb-1">
          {t('map.unavailable')}
        </p>
        <p className="text-sm text-text-secondary">
          {t('map.configureApiKey')}
        </p>
        <div className="mt-3 text-xs text-text-muted bg-surface-inset rounded p-2 font-mono">
          {street}, {city}
        </div>
      </div>
    );
  }

  // Geocoding in progress
  if (isGeocoding) {
    return (
      <div className={`${height} ${placeholderCls}`}>
        <Loader2 className="h-10 w-10 text-primary-500 animate-spin mb-3" />
        <p className="text-text-secondary font-medium mb-1">
          {t('map.findingLocation')}
        </p>
        <div className="mt-3 text-xs text-text-muted bg-surface-inset rounded p-2 font-mono">
          {street}, {city}
        </div>
      </div>
    );
  }

  // Maps API failed to load
  if (mapsApiLoadError) {
    return (
      <div className={`${height} ${placeholderCls}`}>
        <MapPin className="h-12 w-12 text-text-disabled mb-3" />
        <p className="text-text-secondary font-medium mb-1">
          {t('map.unavailable')}
        </p>
        <p className="text-sm text-text-secondary">
          {t('map.couldNotLoadMaps')}
        </p>
        <div className="mt-3 text-xs text-text-muted bg-surface-inset rounded p-2 font-mono">
          {street}, {city}
        </div>
      </div>
    );
  }

  // Click-to-place mode (no coordinates, user activated manual placement)
  if (clickToPlaceActive && latitude == null && longitude == null) {
    const center = getCountryCenter(defaultCountryCode);
    return (
      <div className="space-y-2">
        <div
          className={`w-full ${height} rounded-lg overflow-hidden border border-border-default `}
        >
          <APIProvider {...apiProviderProps}>
            <Map
              center={center}
              zoom={5}
              mapId="interactive-map"
              gestureHandling="cooperative"
              disableDefaultUI={false}
              onClick={handleMapClick}
            />
          </APIProvider>
        </div>
        <p className="text-sm text-primary-500 dark:text-primary-300 text-center">
          {t('map.clickToPlace')}
        </p>
      </div>
    );
  }

  // No coordinates, interactive mode — offer click-to-place
  if ((latitude == null || longitude == null) && isInteractive) {
    return (
      <div className={`${height} ${placeholderCls}`}>
        <MapPin className="h-12 w-12 text-text-disabled mb-3" />
        <p className="text-text-secondary font-medium mb-1">
          {t('map.locationNotFound')}
        </p>
        <p className="text-sm text-text-secondary mb-3">
          {t('map.couldNotFind')}
        </p>
        <button
          type="button"
          onClick={() => setClickToPlaceActive(true)}
          className="inline-flex items-center gap-2 px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors text-sm font-medium"
        >
          <MousePointerClick className="h-4 w-4" />
          {t('map.placePinManually')}
        </button>
      </div>
    );
  }

  // No coordinates, read-only mode
  if (latitude == null || longitude == null) {
    return (
      <div className={`${height} ${placeholderCls}`}>
        <MapPin className="h-12 w-12 text-text-disabled mb-3" />
        <p className="text-text-secondary font-medium mb-1">
          {t('map.locationNotFound')}
        </p>
        <p className="text-sm text-text-secondary">{t('map.couldNotFind')}</p>
        <div className="mt-3 text-xs text-text-muted bg-surface-inset rounded p-2 font-mono">
          {street}, {city}
        </div>
      </div>
    );
  }

  // Has coordinates — render map with marker
  const coordinates = { lat: latitude, lng: longitude };
  const zoom = getZoomForAccuracy(geocodeAccuracy);
  const accuracyMessageKey = isInteractive
    ? getAccuracyMessageKey(geocodeAccuracy)
    : null;
  const accuracyMessage = accuracyMessageKey ? t(accuracyMessageKey) : null;

  return (
    <div className="space-y-2">
      <div
        className={`w-full ${height} rounded-lg overflow-hidden border border-border-default `}
      >
        <APIProvider {...apiProviderProps}>
          <Map
            center={coordinates}
            zoom={zoom}
            mapId="interactive-map"
            gestureHandling="cooperative"
            disableDefaultUI={false}
          >
            <AdvancedMarker
              position={coordinates}
              title={`${street}, ${city}`}
              draggable={isInteractive}
              onDragEnd={isInteractive ? handleDragEnd : undefined}
            />
          </Map>
        </APIProvider>
      </div>
      {accuracyMessage && (
        <p className="text-sm text-text-secondary text-center">
          {accuracyMessage}
        </p>
      )}
    </div>
  );
};
