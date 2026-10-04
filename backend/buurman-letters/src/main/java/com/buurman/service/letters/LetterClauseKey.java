package com.buurman.service.letters;

/**
 * One ordered legal-clause section within a letter: a heading and its body, both message keys. Both
 * are required — {@link LetterExporterHelper#resolveLegalClauses} resolves {@code titleKey}
 * unconditionally, so a {@code null} here would NPE at render time rather than render titleless.
 */
record LetterClauseKey(String titleKey, String bodyKey) {}
