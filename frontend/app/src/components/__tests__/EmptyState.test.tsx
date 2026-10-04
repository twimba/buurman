import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { EmptyState } from '@buurman/ui';

const iconWrapper = () =>
  screen.getByTestId('icon').parentElement as HTMLElement;

describe('EmptyState tone', () => {
  const renderWith = (tone?: 'neutral' | 'info' | 'error') =>
    render(
      <EmptyState icon={<svg data-testid="icon" />} title="Title" tone={tone} />
    );

  it('defaults to the muted neutral icon colour', () => {
    renderWith();
    expect(iconWrapper()).toHaveClass('text-text-muted');
  });

  it('uses the info icon colour', () => {
    renderWith('info');
    expect(iconWrapper()).toHaveClass('text-info-text');
    expect(iconWrapper()).not.toHaveClass('text-text-muted');
  });

  it('uses the error icon colour', () => {
    renderWith('error');
    expect(iconWrapper()).toHaveClass('text-error-text');
  });
});
