import type { RentComponentRequest } from '../generated/models';
import type { RentComponentFormItem } from '../types/contract';

/**
 * Convert form-shaped rent components (amount may be an empty string while editing) into API
 * requests: drop blank/non-positive rows and narrow `amount` to a number. Prevents a half-filled
 * component row from POSTing `amount: ''` (rejected by the `@minimum 0.01` backend validation).
 */
export const toRentComponentRequests = (
  components?: RentComponentFormItem[]
): RentComponentRequest[] | undefined =>
  components
    ?.filter((c) => typeof c.amount === 'number' && c.amount > 0)
    .map((c) => ({
      componentType: c.componentType,
      amount: c.amount as number,
      description: c.description,
    }));
