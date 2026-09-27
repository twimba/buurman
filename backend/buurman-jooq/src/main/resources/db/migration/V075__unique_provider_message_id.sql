-- A provider message id is unique only within its own provider, so the pair
-- (provider_message_id, channel) is what identifies a delivery. Lookups already
-- filter on both; this stops one provider's id being recorded twice, which would
-- make every open, click and status webhook update several rows at once.
--
-- Verified safe before writing: the duplicate check below returned zero rows in
-- production.
--   SELECT channel, provider_message_id, count(*)
--   FROM notifications
--   WHERE provider_message_id IS NOT NULL
--   GROUP BY 1, 2 HAVING count(*) > 1;
--
-- provider_message_id leads the index so it also serves the lookups the dropped
-- index served, as a prefix. Partial, so the NULLs on unsent rows stay out of it.
DROP INDEX IF EXISTS idx_notifications_provider_msg;

CREATE UNIQUE INDEX uq_notifications_provider_msg ON notifications (provider_message_id, channel)
WHERE
    provider_message_id IS NOT NULL;
