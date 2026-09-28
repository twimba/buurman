import { describe, expect, it } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';

/**
 * Every key the app asks for must exist in the English bundles.
 *
 * A key missing everywhere does not fall back to English — there is nothing to fall back to, so
 * i18next renders the key itself and the user sees "common:invitation.youreInvited" on the page.
 * An inline defaultValue hides that, but then the string is English in all thirteen languages and
 * can never be translated. Both are caught here.
 *
 * Only literal keys are checked; `t(\`prefix.${x}\`)` is invisible to any static rule.
 */
const LOCALES_DIR = path.resolve(
  import.meta.dirname,
  '../../../public/locales'
);
const SRC_DIR = path.resolve(import.meta.dirname, '../..');
const PLURAL_SUFFIX = /_(zero|one|two|few|many|other)$/;

type Json = Record<string, unknown>;

const flatten = (value: Json, prefix = ''): string[] =>
  Object.entries(value).flatMap(([key, child]) =>
    child !== null && typeof child === 'object' && !Array.isArray(child)
      ? flatten(child as Json, `${prefix}${key}.`)
      : [`${prefix}${key}`]
  );

const englishKeysByNamespace = (): Record<string, Set<string>> => {
  const byNamespace: Record<string, Set<string>> = {};
  for (const file of fs.readdirSync(path.join(LOCALES_DIR, 'en'))) {
    if (!file.endsWith('.json')) {
      continue;
    }
    const parsed = JSON.parse(
      fs.readFileSync(path.join(LOCALES_DIR, 'en', file), 'utf8')
    ) as Json;
    byNamespace[file.replace(/\.json$/, '')] = new Set(
      flatten(parsed).map((key) => key.replace(PLURAL_SUFFIX, ''))
    );
  }
  return byNamespace;
};

const sourceFiles = (dir: string): string[] =>
  fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      return entry.name === 'generated' ? [] : sourceFiles(full);
    }
    return /\.tsx?$/.test(entry.name) ? [full] : [];
  });

describe('translation keys used in code', () => {
  const byNamespace = englishKeysByNamespace();
  const anyNamespace = new Set(
    Object.values(byNamespace).flatMap((keys) => [...keys])
  );
  const files = sourceFiles(SRC_DIR);

  it('discovers the source tree it is meant to check', () => {
    expect(files.length).toBeGreaterThan(100);
    expect(Object.keys(byNamespace).length).toBeGreaterThanOrEqual(10);
  });

  it('are all defined in the English bundles', () => {
    const missing: string[] = [];

    for (const file of files) {
      const source = fs.readFileSync(file, 'utf8');
      for (const match of source.matchAll(
        /\bt\(\s*['"]([a-zA-Z0-9_.:-]+)['"]/g
      )) {
        const raw = match[1];
        const [namespace, bare] = raw.includes(':')
          ? raw.split(':')
          : [null, raw];
        const key = bare.replace(PLURAL_SUFFIX, '');
        if (key.endsWith('.')) {
          continue; // t('prefix.' + something) — not statically checkable
        }
        const known = namespace
          ? byNamespace[namespace]?.has(key)
          : anyNamespace.has(key);
        if (!known) {
          missing.push(`${path.relative(SRC_DIR, file)}  ${raw}`);
        }
      }
    }

    expect([...new Set(missing)].sort()).toEqual([]);
  });
});
