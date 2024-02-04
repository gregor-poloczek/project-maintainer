import { createAction, props } from '@ngrx/store';
import { API } from './API';

export const triggerOperation = createAction(
  '[PROJECTS] trigger operation',
  props<{
    fqpn: API.FQPN;
    operation: API.ProjectOperation;
  }>(),
);

export const triggerOperationSuccess = createAction(
  '[PROJECTS] operation trigger success',
  props<{ fqpn: API.FQPN; operation: API.ProjectOperation }>(),
);

export const triggerOperationFailed = createAction(
  '[PROJECTS] operation trigger failed',
  props<{ fqpn: API.FQPN; operation: API.ProjectOperation; error: Error }>(),
);

export const loadProjects = createAction('[PROJECTS] Load Projects');
export const loadProject = createAction(
  '[PROJECTS] Load Project',
  props<{ fqpn: API.FQPN }>(),
);
export const loadedProjects = createAction(
  '[PROJECTS] Projects loaded',
  props<{ projects: API.ProjectResource[] }>(),
);
export const loadedProject = createAction(
  '[PROJECTS] Project loaded',
  props<{ project: API.ProjectResource }>(),
);
export const loadProjectsFailed = createAction(
  '[PROJECTS] Load failed',
  props<{ error: Error }>(),
);
