#!/usr/bin/env node
/**
 * Orval `afterAllFilesWrite` hook: makes a handful of generated identifier types nominal
 * (branded) instead of plain `string` aliases.
 *
 * Why: every generated identifier type (UnitIdentifier, PropertyIdentifier, ...) is generated as
 * a structural `export type X = string`. That means any string-typed identifier is assignable to
 * any other — passing a PropertyIdentifier where a UnitIdentifier is expected compiles cleanly,
 * eslint is happy, and the break only shows up at runtime as a 404. See BUUR-106 item 2: the WWS
 * pre-fill hook did exactly this for months without a single type error.
 *
 * This patches the *generated* model files after every `orval` run (both the `app` and
 * `backoffice` projects), so the fix survives `clean: true` wiping the output directory.
 */

import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));

// { generated models directory, [{ file, typeName }] }
//
// Deliberately UnitIdentifier only, not PropertyIdentifier: PropertyIdentifier is threaded
// through most of the property domain (financials, occupancy, dashboards, ...), all as plain
// `string` route params today. Branding it too would force a cast at dozens of unrelated call
// sites outside this wave's scope for no WWS benefit. Branding UnitIdentifier alone already
// catches the actual bug class: a plain string (including a PropertyIdentifier, since it stays a
// `string`) can no longer satisfy a UnitIdentifier-typed parameter without an explicit cast.
const TARGETS = [
  {
    dir: join(__dirname, '..', 'app', 'src', 'generated', 'models'),
    types: [{ file: 'unitIdentifier.ts', typeName: 'UnitIdentifier' }],
  },
  {
    dir: join(__dirname, '..', 'backoffice', 'src', 'generated', 'models'),
    types: [{ file: 'unitIdentifier.ts', typeName: 'UnitIdentifier' }],
  },
];

function brandFile(dir, file, typeName) {
  const path = join(dir, file);
  if (!existsSync(path)) {
    return;
  }
  const source = readFileSync(path, 'utf8');
  const plainAlias = `export type ${typeName} = string;`;
  if (!source.includes(plainAlias)) {
    // Already branded (re-run), or the generated shape changed upstream — leave it alone rather
    // than silently no-op-ing on a shape we don't recognize.
    return;
  }
  const branded = `export type ${typeName} = string & { readonly __brand: '${typeName}' };`;
  writeFileSync(path, source.replace(plainAlias, branded));
  // eslint-disable-next-line no-console
  console.log(`[brand-identifiers] branded ${typeName} in ${path}`);
}

for (const { dir, types } of TARGETS) {
  for (const { file, typeName } of types) {
    brandFile(dir, file, typeName);
  }
}
