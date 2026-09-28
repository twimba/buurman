import { useTranslation } from 'react-i18next';
import { Smartphone } from 'lucide-react';

interface MessageBodyPreviewProps {
  channel: string;
  body: string;
}

/**
 * The stored message as the recipient received it — an HTML email in a frame, an SMS as a phone
 * bubble.
 *
 * <p>Shared by the landlord's timeline preview and the admin delivery log so there is one iframe
 * policy in the codebase rather than two. The sandbox below is the only security-relevant markup
 * in either surface; a second copy is a second policy, and whoever changes one will not find the
 * other.
 */
export const MessageBodyPreview = ({
  channel,
  body,
}: MessageBodyPreviewProps) => {
  const { t } = useTranslation('common');

  if (channel === 'SMS') {
    return (
      <div className="flex justify-center">
        <div className="w-[300px] rounded-2xl bg-neutral-900 dark:bg-neutral-950 p-4 shadow-inner">
          <div className="flex items-center justify-center gap-1.5 mb-3 text-[10px] text-text-secondary">
            <Smartphone className="h-3 w-3" />
            {t('communications.preview.smsLabel')}
          </div>
          <div className="flex justify-start">
            <div className="relative max-w-[240px] bg-neutral-100 dark:bg-neutral-700 rounded-2xl rounded-bl-sm px-3.5 py-2.5">
              <p className="text-sm text-text-primary whitespace-pre-wrap break-words leading-relaxed">
                {body}
              </p>
            </div>
          </div>
          {/*
           * No timestamp footer. This used to read "Delivered" unconditionally, which contradicted
           * the real status shown a few pixels above on every failed SMS — and the user believes
           * the thing next to the message over the badge.
           */}
        </div>
      </div>
    );
  }

  return (
    <iframe
      /*
       * sandbox="" — opaque origin, every capability off, matching the tenant-facing precedent in
       * settings/ReminderPreviewModal. The admin log previously used allow-same-origin purely so
       * it could read contentDocument.body.scrollHeight to auto-size. That is the one token that
       * must never meet allow-scripts, and a later "make the preview interactive" change would
       * read as a one-token diff on a line that already looks deliberately permissive. A fixed
       * height with internal scrolling costs nothing by comparison.
       *
       * It is not merely theoretical: templates/email/team-invitation.html renders inviterName
       * with th:utext, so at least one stored body in this system can contain author-supplied
       * HTML. That type carries no payment or contract link and so cannot reach the landlord
       * endpoint — but that is a property of the data, not an enforced constraint, which is
       * exactly why the sandbox must not depend on it.
       */
      sandbox=""
      srcDoc={body}
      referrerPolicy="no-referrer"
      loading="lazy"
      title={t('communications.preview.frameTitle')}
      className="w-full h-[55dvh] md:h-[26rem] rounded-md border border-border-default bg-white"
    />
  );
};
