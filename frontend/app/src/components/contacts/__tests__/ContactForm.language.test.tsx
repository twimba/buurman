import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SUPPORTED_LANGUAGES } from '@/config/languages';
import { renderWithProviders } from '@/test/test-utils';
import { ContactForm } from '../ContactForm';

/**
 * Covers the language selector added for BUUR-109.
 *
 * Also guards the reason `SUPPORTED_LANGUAGES` lives in `src/config/` rather than `src/i18n/`:
 * vitest.config.ts aliases `@/i18n` to a test mock and Vite string aliases match by prefix, so an
 * `@/i18n/languages` import would silently resolve to that mock inside this very test.
 */
describe('ContactForm language selector', () => {
  const setup = () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    renderWithProviders(<ContactForm onSubmit={onSubmit} isLoading={false} />);
    return { onSubmit, user: userEvent.setup() };
  };

  it('offers every supported language plus a team-default option', () => {
    setup();

    const select = screen.getByLabelText(/language/i) as HTMLSelectElement;
    const values = [...select.options].map((option) => option.value);

    expect(values).toEqual(['', ...SUPPORTED_LANGUAGES]);
  });

  it('labels each option in its own language', () => {
    setup();

    const select = screen.getByLabelText(/language/i) as HTMLSelectElement;
    const byValue = Object.fromEntries(
      [...select.options].map((option) => [option.value, option.textContent])
    );

    // Intl.DisplayNames in the locale itself: Dutch reads "Nederlands", not "Dutch".
    expect(byValue.nl).toBe(
      new Intl.DisplayNames(['nl'], { type: 'language' }).of('nl') ?? 'nl'
    );
    expect(byValue.el).toBe(
      new Intl.DisplayNames(['el'], { type: 'language' }).of('el') ?? 'el'
    );
  });

  it('sends the chosen language on submit', async () => {
    const { onSubmit, user } = setup();

    await user.type(screen.getByPlaceholderText('Jan'), 'Jan');
    await user.selectOptions(screen.getByLabelText(/language/i), 'pt');
    await user.click(screen.getByRole('button', { name: /create contact/i }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalled());
    expect(onSubmit.mock.calls[0][0]).toMatchObject({
      preferredLanguage: 'pt',
    });
  });

  it('omits the language when the team default is never touched', async () => {
    const { onSubmit, user } = setup();

    await user.type(screen.getByPlaceholderText('Jan'), 'Jan');
    await user.click(screen.getByRole('button', { name: /create contact/i }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalled());
    expect(onSubmit.mock.calls[0][0].preferredLanguage).toBeUndefined();
  });

  it('omits the language when a choice is made and then taken back', async () => {
    const { onSubmit, user } = setup();
    const select = screen.getByLabelText(/language/i);

    await user.type(screen.getByPlaceholderText('Jan'), 'Jan');
    await user.selectOptions(select, 'pt');
    await user.selectOptions(select, ''); // back to "use the team default"
    await user.click(screen.getByRole('button', { name: /create contact/i }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalled());
    // Must be undefined, never ''. The generated LanguageCode enum has no empty member, so an
    // empty string fails the request — and only this path, where onChange actually fires with
    // an empty value, can catch that.
    expect(onSubmit.mock.calls[0][0].preferredLanguage).toBeUndefined();
  });
});
