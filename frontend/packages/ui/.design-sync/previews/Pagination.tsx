import { Pagination } from '@buurman/ui';

const noop = () => undefined;

export function FirstPage() {
  return (
    <Pagination
      page={0}
      totalPages={10}
      totalElements={248}
      size={25}
      onPageChange={noop}
      onSizeChange={noop}
    />
  );
}

export function MiddlePage() {
  return (
    <Pagination
      page={5}
      totalPages={10}
      totalElements={248}
      size={25}
      onPageChange={noop}
      onSizeChange={noop}
    />
  );
}

export function LastPage() {
  return (
    <Pagination
      page={9}
      totalPages={10}
      totalElements={248}
      size={25}
      onPageChange={noop}
      onSizeChange={noop}
    />
  );
}

export function SmallSet() {
  return (
    <Pagination
      page={1}
      totalPages={3}
      totalElements={64}
      size={25}
      onPageChange={noop}
      onSizeChange={noop}
    />
  );
}
