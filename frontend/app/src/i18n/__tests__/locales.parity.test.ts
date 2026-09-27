import { describe, expect, it } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { SUPPORTED_LANGUAGES } from '../../config/languages';
import { LanguageCode } from '../../generated/models';

const LOCALES_DIR = path.resolve(import.meta.dirname, '../../../public/locales');
const BASE_LANGUAGE = 'en';

type Json = Record<string, unknown>;

const readNamespace = (language: string, namespace: string): Json =>
  JSON.parse(fs.readFileSync(path.join(LOCALES_DIR, language, namespace), 'utf8')) as Json;

const flatten = (value: Json, prefix = ''): string[] =>
  Object.entries(value).flatMap(([key, child]) =>
    child !== null && typeof child === 'object' && !Array.isArray(child)
      ? flatten(child as Json, `${prefix}${key}.`)
      : [`${prefix}${key}`]
  );

// i18next appends a CLDR plural category to keys that interpolate a count. Which categories a
// language needs differs — Polish needs one/few/many/other where English needs one/other — so the
// suffix is stripped before comparing keys and checked separately below.
const PLURAL_SUFFIX = /_(zero|one|two|few|many|other)$/;
const stripPlural = (key: string) => key.replace(PLURAL_SUFFIX, '');

const namespaces = fs
  .readdirSync(path.join(LOCALES_DIR, BASE_LANGUAGE))
  .filter((name) => name.endsWith('.json'))
  .sort();

// Every CLDR category the language defines. A suffix outside this set is a typo or a dead key.
const definedCategories = (language: string): Set<string> =>
  new Set(new Intl.PluralRules(language).resolvedOptions().pluralCategories);

/**
 * The categories a real count in this product can actually select, plus `other` as the catch-all.
 *
 * Deriving this from Intl rather than listing languages keeps the rule generic. It also keeps it
 * honest: French, Spanish, Portuguese and Italian define a `many` category, but it only selects at
 * 1,000,000+, which no count of properties or payments reaches — demanding it would mean 240
 * near-duplicate strings for no reader. Polish `few` (2-4) and `many` (5+) select constantly, so
 * Polish genuinely needs them, and this rule asks for them without naming Polish anywhere.
 */
const requiredCategories = (language: string): string[] => {
  const rules = new Intl.PluralRules(language);
  const reachable = new Set<string>(['other']);
  for (let count = 0; count <= 200; count += 1) {
    reachable.add(rules.select(count));
  }
  return [...reachable].sort();
};

const suffixesFor = (keys: string[], base: string): Set<string> =>
  new Set(
    keys
      .filter((key) => PLURAL_SUFFIX.test(key) && stripPlural(key) === base)
      .map((key) => key.slice(base.length + 1))
  );

describe('locale bundles', () => {
  // Spec S1: the backend's DocumentLanguages.ORDERED reaches the frontend through the OpenAPI
  // LanguageCode enum, which Orval generates. Asserting against it is what actually stops the
  // list drifting across stacks — comparing the locale directories to SUPPORTED_LANGUAGES alone
  // would pass happily while the backend had gained a language the frontend never learned about.
  it('matches the language enum generated from the backend contract', () => {
    expect([...SUPPORTED_LANGUAGES].sort()).toEqual(Object.values(LanguageCode).sort());
  });

  it('discovers the namespaces it is meant to check', () => {
    expect(namespaces.length).toBeGreaterThanOrEqual(10);
  });

  it('ships a directory for every supported language and nothing else', () => {
    const directories = fs
      .readdirSync(LOCALES_DIR, { withFileTypes: true })
      .filter((entry) => entry.isDirectory())
      .map((entry) => entry.name)
      .sort();

    expect(directories).toEqual([...SUPPORTED_LANGUAGES].sort());
  });

  describe.each(SUPPORTED_LANGUAGES.filter((language) => language !== BASE_LANGUAGE))(
    '%s',
    (language) => {
      it.each(namespaces)('%s carries every key', (namespace) => {
        const base = new Set(flatten(readNamespace(BASE_LANGUAGE, namespace)).map(stripPlural));
        const translated = new Set(
          flatten(readNamespace(language, namespace)).map(stripPlural)
        );

        expect([...base].filter((key) => !translated.has(key))).toEqual([]);
        expect([...translated].filter((key) => !base.has(key))).toEqual([]);
      });
    }
  );

  it.each(SUPPORTED_LANGUAGES)(
    '%s can resolve every plural key at every count it will be asked for',
    (language) => {
      const required = requiredCategories(language);
      const defined = definedCategories(language);

      for (const namespace of namespaces) {
        const keys = flatten(readNamespace(language, namespace));
        const present = new Set(keys);
        const pluralBases = new Set(
          keys.filter((key) => PLURAL_SUFFIX.test(key)).map(stripPlural)
        );

        for (const base of pluralBases) {
          const supplied = suffixesFor(keys, base);

          // A suffix the language has no such category for is dead weight and never renders.
          expect(
            [...supplied].filter((category) => !defined.has(category)),
            `${language}/${namespace} ${base} has a suffix ${language} does not define`
          ).toEqual([]);

          // i18next falls back to the unsuffixed key for any category it cannot find, so a base
          // key makes every count resolvable. Without one, a missing category renders the raw
          // key to the user — "Delete 1 documents" is the good case, the key itself is the bad one.
          if (!present.has(base)) {
            expect(
              required.filter((category) => !supplied.has(category)),
              `${language}/${namespace} ${base} has no fallback key and is missing categories`
            ).toEqual([]);
          }
        }
      }
    }
  );

  // A blanket "every {{count}} key must have plural forms" rule was tried here and removed: it
  // cannot tell "{{count}} properties found" (needs plural agreement) from "Your Teams
  // ({{count}})" (a parenthetical tally that never agrees), and it flagged 50 keys in all 13
  // languages on that basis. Which of those need real plural forms is a per-key judgement for a
  // native reviewer, so it lives in docs/i18n-review.md instead of a rule that would be silenced.
});
