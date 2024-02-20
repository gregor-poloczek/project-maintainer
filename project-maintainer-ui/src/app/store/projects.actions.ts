import { createAction, props } from '@ngrx/store';
import { API } from '../API';

export const connectionLost = createAction('projects::connectionLost');
export const connectionFailed = createAction('projects::connectionFailed');
export const connectionEstablished = createAction(
  'projects::connectionEstablished',
);

export const triggerOperation = createAction(
  'projects::triggerOperation',
  props<{
    fqpn: API.FQPN;
    operation: API.ProjectOperation;
  }>(),
);

export const triggerOperationSuccess = createAction(
  'projects::triggerOperationSuccess',
  props<{ fqpn: API.FQPN; operation: API.ProjectOperation }>(),
);

export const triggerOperationFailed = createAction(
  'projects::triggerOperationFailed',
  props<{ fqpn: API.FQPN; operation: API.ProjectOperation; error: Error }>(),
);

// TODO make obsolete
export const loadProjects = createAction('projects::loadProjects');
export const loadProject = createAction(
  'projects::loadProject',
  props<{ fqpn: API.FQPN }>(),
);
export const loadProjectsSuccess = createAction(
  'projects::loadProjectsSuccess',
  props<{ projects: API.ProjectResource[] }>(),
);
export const loadProjectSuccess = createAction(
  'projects::loadProjectSuccess',
  props<{ project: API.ProjectResource }>(),
);
export const loadProjectsFailed = createAction(
  'projects::loadProjectsFailed',
  props<{ error: Error }>(),
);
export const loadProjectFailed = createAction(
  'projects::loadProjectFailed',
  props<{ error: Error }>(),
);
