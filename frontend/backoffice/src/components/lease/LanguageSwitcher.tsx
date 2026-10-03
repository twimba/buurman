import { LANGUAGES, LANGUAGE_LABELS } from '../../lib/leaseClauseTable';
import type { DocumentLanguage } from '../../generated/models';

interface LanguageSwitcherProps {
  value: DocumentLanguage;
  onChange: (language: DocumentLanguage) => void;
  className: string;
}

export const LanguageSwitcher = ({
  value,
  onChange,
  className,
}: LanguageSwitcherProps) => (
  <label className="block">
    <span className="mb-1 block text-sm font-medium text-text-secondary">
      Text language
    </span>
    <select
      value={value}
      onChange={(e) => onChange(e.target.value as DocumentLanguage)}
      className={className}
    >
      {LANGUAGES.map((lang) => (
        <option key={lang} value={lang}>
          {lang.toUpperCase()} {LANGUAGE_LABELS[lang]}
        </option>
      ))}
    </select>
  </label>
);
