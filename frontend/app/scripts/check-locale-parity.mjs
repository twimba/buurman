#!/usr/bin/env node
/**
 * Locale key-parity gate.
 *
 * `en` is the source of truth. Every other locale must define exactly the same
 * key paths in every namespace: no missing keys (which render as raw key names
 * to the user) and no stale extra keys (dead translations that hide renames).
 *
 * A missing translation is not a cosmetic bug here -- Buurman serves landlords
 * across 13 locales, and an untranslated key surfaces as `units.term.one`
 * literally in the UI.
 *
 * Usage:
 *   node scripts/check-locale-parity.mjs            # report and exit 1 on drift
 *   node scripts/check-locale-parity.mjs --json     # machine-readable
 */
import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const LOCALES_DIR = join(dirname(fileURLToPath(import.meta.url)), '..', 'public', 'locales');
const SOURCE_LOCALE = 'en';
const jsonOutput = process.argv.includes('--json');

/** Collect every leaf key path, so nested objects are compared structurally. */
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
    throw new Error(`${locale}/${namespace} is not valid JSON: ${error.message}`);
  }
}

const locales = readdirSync(LOCALES_DIR)
  .filter((entry) => statSync(join(LOCALES_DIR, entry)).isDirectory())
  .sort();
const targets = locales.filter((locale) => locale !== SOURCE_LOCALE);
const namespaces = readdirSync(join(LOCALES_DIR, SOURCE_LOCALE))
  .filter((file) => file.endsWith('.json'))
  .sort();

const problems = [];

for (const namespace of namespaces) {
  const source = readNamespace(SOURCE_LOCALE, namespace);
  if (source === null) {
    problems.push({ locale: SOURCE_LOCALE, namespace, kind: 'unreadable', keys: [] });
    continue;
  }
  const sourceKeys = new Set(leafKeys(source));

  for (const locale of targets) {
    const translated = readNamespace(locale, namespace);
    if (translated === null) {
      problems.push({
        locale,
        namespace,
        kind: 'missing-file',
        keys: [`(all ${sourceKeys.size} keys)`],
      });
      continue;
    }
    const translatedKeys = new Set(leafKeys(translated));

    const missing = [...sourceKeys].filter((key) => !translatedKeys.has(key)).sort();
    const extra = [...translatedKeys].filter((key) => !sourceKeys.has(key)).sort();

    if (missing.length > 0) {
      problems.push({ locale, namespace, kind: 'missing-keys', keys: missing });
    }
    if (extra.length > 0) {
      problems.push({ locale, namespace, kind: 'extra-keys', keys: extra });
    }
  }
}

if (jsonOutput) {
  console.log(JSON.stringify({ locales, namespaces, problems }, null, 2));
} else {
  console.log(
    `Locale parity: ${namespaces.length} namespaces x ${targets.length} target locales ` +
      `against "${SOURCE_LOCALE}".`,
  );
  if (problems.length === 0) {
    console.log('OK — every locale defines exactly the source key set.');
  } else {
    for (const { locale, namespace, kind, keys } of problems) {
      const shown = keys.slice(0, 15);
      const suffix = keys.length > shown.length ? ` … and ${keys.length - shown.length} more` : '';
      console.error(`\n${locale}/${namespace} — ${kind} (${keys.length}):`);
      console.error(`  ${shown.join('\n  ')}${suffix}`);
    }
    console.error(`\n${problems.length} parity problem(s).`);
  }
}

process.exit(problems.length === 0 ? 0 : 1);
