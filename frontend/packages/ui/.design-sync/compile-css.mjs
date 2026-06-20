// Compiles the package's Tailwind v4 utilities + @theme tokens into a single
// static stylesheet for design-sync's cfg.cssEntry. Mirrors app/src/index.css.
import postcss from 'postcss';
import tailwind from '@tailwindcss/postcss';
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const input = join(here, 'tw-input.css');
const outDir = join(here, '.cache');
mkdirSync(outDir, { recursive: true });
const out = join(outDir, 'ui-compiled.css');

const css = readFileSync(input, 'utf8');
const result = await postcss([tailwind()]).process(css, { from: input, to: out });
writeFileSync(out, result.css);
console.error(`compiled ${result.css.length} bytes → ${out}`);
