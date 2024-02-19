import { createAction, props } from '@ngrx/store';

export const updateFilter = createAction(
  'search.updateFilter',
  props<{
    value: string;
  }>(),
);
