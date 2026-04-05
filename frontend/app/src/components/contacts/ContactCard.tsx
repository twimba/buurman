import { useTranslation } from 'react-i18next';
import {
  ContactListItemResponse,
  TAG_COLORS,
  ContactType,
} from '@/types/contact';
import {
  Mail,
  Phone,
  FileText,
  Calendar,
  Wallet,
  CheckCircle2,
  Shield,
} from 'lucide-react';
import { useFormatDate } from '@/hooks/useFormatDate';
import { useNavigate } from 'react-router-dom';
import { Avatar } from '@/components/common/Avatar';
import { StatusBadge } from '@buurman/ui';
import type { BadgeColorVariant } from '@buurman/ui';

const CONTACT_TYPE_COLORS: Record<ContactType, BadgeColorVariant> = {
  INDIVIDUAL: 'blue',
  COMPANY: 'amber',
  SERVICE_PROVIDER: 'green',
};

const MAX_VISIBLE_TAGS = 3;

const formatCurrency = (amount: number, currency: string) =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(amount);

interface ContactCardProps {
  contact: ContactListItemResponse;
}

export const ContactCard = ({ contact }: ContactCardProps) => {
  const { t } = useTranslation('tenants');
  const navigate = useNavigate();
  const { formatRelative } = useFormatDate();
  const tags = contact.tags ?? [];
  const visibleTags = tags.slice(0, MAX_VISIBLE_TAGS);
  const overflowCount = tags.length - MAX_VISIBLE_TAGS;
  const balance = contact.balanceSummary;

  return (
    <div
      className="bg-surface-card rounded-lg shadow-sm hover:shadow-md transition-shadow cursor-pointer p-6"
      onClick={() => navigate(`/contacts/${contact.identifier}`)}
    >
      {/* Header: Avatar + Name + Type badge */}
      <div className="flex items-center gap-3 mb-3">
        <Avatar
          firstName={contact.firstName ?? contact.displayName}
          lastName={contact.lastName}
          photoUrl={contact.mainPhotoThumbnailUrl}
          size="md"
        />
        <div className="min-w-0 flex-1">
          <h3 className="text-lg font-semibold text-text-primary truncate">
            {contact.displayName}
          </h3>
          <div className="flex items-center gap-2 mt-0.5">
            <StatusBadge
              label={t(
                `enums.contactTypes.${contact.contactType}`,
                contact.contactType
              )}
              color={CONTACT_TYPE_COLORS[contact.contactType] ?? 'gray'}
              size="sm"
            />
          </div>
        </div>
      </div>

      {/* Tags */}
      {tags.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mb-3">
          {visibleTags.map((tag) => (
            <StatusBadge
              key={tag}
              label={t(`enums.contactTags.${tag}`, tag)}
              color={TAG_COLORS[tag] ?? 'gray'}
              size="sm"
            />
          ))}
          {overflowCount > 0 && (
            <span className="text-xs text-text-muted px-1.5 py-0.5">
              +{overflowCount}
            </span>
          )}
        </div>
      )}

      {/* Contact Info */}
      <div className="flex items-center gap-3 mb-4">
        {contact.email && (
          <a
            href={`mailto:${contact.email}`}
            className="inline-flex items-center gap-1.5 text-sm text-text-secondary hover:text-primary-500 transition-colors"
            onClick={(e) => e.stopPropagation()}
            title={contact.email}
          >
            <Mail className="h-4 w-4" />
            <span className="truncate max-w-[160px]">{contact.email}</span>
          </a>
        )}
        {contact.phone && (
          <a
            href={`tel:${contact.phone}`}
            className="inline-flex items-center gap-1.5 text-sm text-text-secondary hover:text-primary-500 transition-colors"
            onClick={(e) => e.stopPropagation()}
            title={contact.phone}
          >
            <Phone className="h-4 w-4" />
            <span>{contact.phone}</span>
          </a>
        )}
      </div>

      {/* Balance Indicator */}
      {balance && balance.status === 'ALL_PAID' && (
        <div
          className="flex items-center justify-between gap-2 px-3 py-2 mb-2 rounded bg-success-bg text-success-text"
          role="status"
          aria-label="All payments are up to date"
        >
          <div className="flex items-center gap-2">
            <CheckCircle2 className="h-4 w-4" />
            <span className="text-sm font-medium">{t('card.allPaid')}</span>
          </div>
        </div>
      )}
      {balance &&
        (balance.status === 'PENDING' || balance.status === 'OVERDUE') && (
          <div
            className={`flex items-center justify-between gap-2 px-3 py-2 mb-2 rounded ${
              balance.status === 'OVERDUE'
                ? 'bg-error-bg text-error-text'
                : 'bg-warning-bg text-warning-text'
            }`}
            role="status"
            aria-label={`Outstanding balance: ${formatCurrency(balance.outstandingAmount, balance.currency)}, ${balance.outstandingPaymentCount} payment${balance.outstandingPaymentCount !== 1 ? 's' : ''} ${balance.status === 'OVERDUE' ? 'overdue' : 'pending'}`}
          >
            <div className="flex items-center gap-2">
              <Wallet className="h-4 w-4" />
              <span className="text-sm font-medium">
                {formatCurrency(balance.outstandingAmount, balance.currency)}
              </span>
            </div>
            <span className="text-xs">
              {t('card.payment', { count: balance.outstandingPaymentCount })}{' '}
              {balance.status === 'OVERDUE'
                ? t('card.overdue')
                : t('card.pending')}
            </span>
          </div>
        )}
      {balance &&
        balance.guaranteedAmount != null &&
        balance.guaranteedAmount > 0 && (
          <div
            className="flex items-center justify-between gap-2 px-3 py-2 mb-2 rounded bg-info-bg text-info-text"
            role="status"
            aria-label={`Guarantees ${formatCurrency(balance.guaranteedAmount, balance.currency)}`}
          >
            <div className="flex items-center gap-2">
              <Shield className="h-4 w-4" />
              <span className="text-sm font-medium">
                {t('card.guarantees', {
                  amount: formatCurrency(
                    balance.guaranteedAmount,
                    balance.currency
                  ),
                })}
              </span>
            </div>
            <span className="text-xs">
              {t('card.payment', {
                count: balance.guaranteedPaymentCount ?? 0,
              })}
            </span>
          </div>
        )}

      {/* Footer: Active Contracts + Created Date */}
      <div className="flex items-center justify-between gap-2 text-text-secondary px-3 py-2 bg-surface-inset rounded">
        <div className="flex items-center gap-2">
          <FileText className="h-4 w-4" />
          <span className="text-sm">
            {contact.activeContractCount > 0
              ? t('card.activeContracts', {
                  count: contact.activeContractCount,
                })
              : t('card.noActiveContracts')}
          </span>
        </div>
        <div
          className="flex items-center gap-1.5 text-text-muted"
          title={new Date(contact.createdAt).toLocaleDateString()}
        >
          <Calendar className="h-3.5 w-3.5" />
          <span className="text-xs">{formatRelative(contact.createdAt)}</span>
        </div>
      </div>
    </div>
  );
};
