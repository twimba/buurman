#!/usr/bin/env node
/**
 * Locale key-parity gate.
 *
 * `en` is the source of truth. Every other locale must define the same key
 * paths in every namespace: no missing keys (which render as raw key names to
 * the user) and no stale extra keys (dead translations that hide renames).
 *
 * Plurals are compared by BASE key, not by exact suffix, because locales do not
 * share plural categories. Polish needs one/few/many/other, so `pl` legitimately
 * carries `_few` and `_many` variants that `en` does not have -- an exact-match
 * gate would reject correct Polish. Conversely a locale that only copies `en`'s
 * two forms renders grammatically wrong text: in Polish, "2 units" selects the
 * `few` category, and without a `_few` key i18next falls back to the wrong form.
 *
 * Required categories come from `Intl.PluralRules` (Node's ICU data) rather than
 * a hand-maintained table, so they stay correct as CLDR evolves.
 *
 * Usage:
 *   node scripts/check-locale-parity.mjs            # report and exit 1 on drift
 *   node scripts/check-locale-parity.mjs --json     # machine-readable
 */
import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const LOCALES_DIR = join(
  dirname(fileURLToPath(import.meta.url)),
  '..',
  'public',
  'locales'
);
const SOURCE_LOCALE = 'en';
const PLURAL_SUFFIXES = ['zero', 'one', 'two', 'few', 'many', 'other'];
const jsonOutput = process.argv.includes('--json');

function leafKeys(value, prefix = '', out = []) {
  if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
    for (const [k, v] of Object.entries(value)) {
      leafKeys(v, prefix ? `${prefix}.${k}` : k, out);
    }
  } else {
    out.push(prefix);
  }
  return out;
}

function readNamespace(locale, namespace) {
  const path = join(LOCALES_DIR, locale, namespace);
  try {
    return JSON.parse(readFileSync(path, 'utf8'));
  } catch (error) {
    if (error.code === 'ENOENT') {
      return null;
    }
    throw new Error(
      `${locale}/${namespace} is not valid JSON: ${error.message}`,
      {
        cause: error,
      }
    );
  }
}

/** Split `foo.bar_one` into { base: 'foo.bar', category: 'one' }; plain keys get category null. */
function splitPlural(key) {
  const underscore = key.lastIndexOf('_');
  if (underscore > 0) {
    const candidate = key.slice(underscore + 1);
    if (PLURAL_SUFFIXES.includes(candidate)) {
      return { base: key.slice(0, underscore), category: candidate };
    }
  }
  return { base: key, category: null };
}

/**
 * Group leaf keys by base key. A base is treated as plural only when the SOURCE
 * locale gives it suffixed variants and no plain leaf of the same name -- so a
 * key that merely happens to end in `_other` is not mistaken for a plural.
 */
function groupKeys(keys) {
  const plain = new Set();
  const plural = new Map();
  for (const key of keys) {
    const { base, category } = splitPlural(key);
    if (category === null) {
      plain.add(base);
    } else {
      if (!plural.has(base)) {
        plural.set(base, new Set());
      }
      plural.get(base).add(category);
    }
  }
  for (const base of plural.keys()) {
    if (plain.has(base)) {
      // Ambiguous: both `foo` and `foo_one` exist. Treat as plain, compare exactly.
      plural.delete(base);
    }
  }
  return { plain, plural };
}

const locales = readdirSync(LOCALES_DIR)
  .filter((entry) => statSync(join(LOCALES_DIR, entry)).isDirectory())
  .sort();
const targets = locales.filter((locale) => locale !== SOURCE_LOCALE);
const namespaces = readdirSync(join(LOCALES_DIR, SOURCE_LOCALE))
  .filter((file) => file.endsWith('.json'))
  .sort();

const requiredCategories = new Map(
  targets.map((locale) => [
    locale,
    new Set(new Intl.PluralRules(locale).resolvedOptions().pluralCategories),
  ])
);

const problems = [];

for (const namespace of namespaces) {
  const source = readNamespace(SOURCE_LOCALE, namespace);
  if (source === null) {
    problems.push({
      locale: SOURCE_LOCALE,
      namespace,
      kind: 'unreadable',
      keys: [],
    });
    continue;
  }
  const sourceGroups = groupKeys(leafKeys(source));

  for (const locale of targets) {
    const translated = readNamespace(locale, namespace);
    if (translated === null) {
      problems.push({
        locale,
        namespace,
        kind: 'missing-file',
        keys: [
          `(all ${sourceGroups.plain.size + sourceGroups.plural.size} keys)`,
        ],
      });
      continue;
    }
    const targetGroups = groupKeys(leafKeys(translated));

    const missing = [];
    const extra = [];
    const missingPluralForms = [];

    for (const base of sourceGroups.plain) {
      if (!targetGroups.plain.has(base)) {
        missing.push(base);
      }
    }
    for (const base of targetGroups.plain) {
      if (!sourceGroups.plain.has(base) && !sourceGroups.plural.has(base)) {
        extra.push(base);
      }
    }

    for (const [base, sourceCats] of sourceGroups.plural) {
      const targetCats = targetGroups.plural.get(base);
      if (targetCats === undefined) {
        if (targetGroups.plain.has(base)) {
          continue; // present, just not pluralised -- not a parity gap
        }
        missing.push(`${base}_*`);
        continue;
      }
      // Require the categories this locale's CLDR rules can actually select,
      // but only for keys the source itself pluralises in more than one form.
      // Where `en` defines a single form, demand no more than that one.
      if (sourceCats.size > 1) {
        for (const category of requiredCategories.get(locale)) {
          if (!targetCats.has(category)) {
            missingPluralForms.push(`${base}_${category}`);
          }
        }
      }
    }
    for (const base of targetGroups.plural.keys()) {
      if (!sourceGroups.plural.has(base) && !sourceGroups.plain.has(base)) {
        extra.push(`${base}_*`);
      }
    }

    if (missing.length > 0) {
      problems.push({
        locale,
        namespace,
        kind: 'missing-keys',
        keys: missing.sort(),
      });
    }
    if (missingPluralForms.length > 0) {
      problems.push({
        locale,
        namespace,
        kind: 'missing-plural-forms',
        keys: missingPluralForms.sort(),
      });
    }
    if (extra.length > 0) {
      problems.push({
        locale,
        namespace,
        kind: 'extra-keys',
        keys: extra.sort(),
      });
    }
  }
}

if (jsonOutput) {
  console.log(JSON.stringify({ locales, namespaces, problems }, null, 2));
} else {
  console.log(
    `Locale parity: ${namespaces.length} namespaces x ${targets.length} target locales ` +
      `against "${SOURCE_LOCALE}".`
  );
  for (const [locale, cats] of requiredCategories) {
    if (cats.size > 2) {
      console.log(
        `  ${locale} requires plural categories: ${[...cats].sort().join(', ')}`
      );
    }
  }
  if (problems.length === 0) {
    console.log(
      'OK — every locale defines the source key set with its own plural categories.'
    );
  } else {
    for (const { locale, namespace, kind, keys } of problems) {
      const shown = keys.slice(0, 15);
      const suffix =
        keys.length > shown.length
          ? ` … and ${keys.length - shown.length} more`
          : '';
      console.error(`\n${locale}/${namespace} — ${kind} (${keys.length}):`);
      console.error(`  ${shown.join('\n  ')}${suffix}`);
    }
    console.error(`\n${problems.length} parity problem(s).`);
  }
}

process.exit(problems.length === 0 ? 0 : 1);
