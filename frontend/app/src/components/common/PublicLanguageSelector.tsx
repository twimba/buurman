import React from 'react';
import { Globe } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { supportedLanguages } from '../../context/LocaleContext';

interface Props {
  onChange?: (lang: string) => void;
}

const PublicLanguageSelector: React.FC<Props> = ({ onChange }) => {
  const { i18n } = useTranslation();

  const handleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const lang = e.target.value;
    i18n.changeLanguage(lang);
    localStorage.setItem('buurman-language', lang);
    onChange?.(lang);
  };

  return (
    <div className="inline-flex items-center gap-1.5 text-text-secondary hover:text-text-primary transition-colors">
      <Globe className="h-3.5 w-3.5 shrink-0" />
      <select
        value={i18n.language}
        onChange={handleChange}
        className="text-sm bg-transparent border-none focus:ring-0 focus:outline-none cursor-pointer text-text-secondary hover:text-text-primary transition-colors"
      >
        {supportedLanguages.map((lang) => (
          <option key={lang.value} value={lang.value}>
            {lang.label}
          </option>
        ))}
      </select>
    </div>
  );
};

export default PublicLanguageSelector;
