import { createAction, props } from '@ngrx/store';
import { API } from '../API';

export const toggleProject = createAction(
  'git::toggleProject',
  props<{ fqpn: API.FQPN }>(),
);
