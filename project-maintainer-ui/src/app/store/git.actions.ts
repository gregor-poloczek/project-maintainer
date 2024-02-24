import { createAction, props } from '@ngrx/store';
import { API } from '../API';

export const toggleProject = createAction(
  'git::toggleProject',
  props<{ fqpn: API.FQPN }>(),
);
export const executeOperation = createAction(
  'git::executeOperation',
  props<{ operation: API.ProjectOperation }>(),
);
