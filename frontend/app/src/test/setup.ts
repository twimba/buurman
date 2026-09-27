import '@testing-library/jest-dom/vitest';
import './i18n.mock';

// jsdom does not implement matchMedia. Several components (useIsMobile,
// MobileFormStepperProvider) call it at module- or mount-time, so without a stub any test
// that imports them throws before rendering anything.
if (typeof window !== 'undefined' && !window.matchMedia) {
  window.matchMedia = (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  });
}
