package com.buurman.service.letters;

/** One ordered legal-clause section within a letter: a heading and its body, both message keys. */
record LetterClauseKey(String titleKey, String bodyKey) {}
