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

export const executePull = createAction('git::executePull');
export const executeClone = createAction('git::executeClone');
export const executeWipe = createAction('git::executeWipe');
