import { createAction, props } from '@ngrx/store';
import { API } from '../API';

export const connectionLost = createAction('main::connectionLost');
export const connectionFailed = createAction('main::connectionFailed');
export const connectionEstablished = createAction(
  'main::connectionEstablished',
);

export const triggerOperation = createAction(
  'main::triggerOperation',
  props<{
    fqpn: API.FQPN;
    operation: API.ProjectOperation;
  }>(),
);

export const triggerOperationSuccess = createAction(
  'main::triggerOperationSuccess',
  props<{ fqpn: API.FQPN; operation: API.ProjectOperation }>(),
);

export const triggerOperationFailed = createAction(
  'main::triggerOperationFailed',
  props<{ fqpn: API.FQPN; operation: API.ProjectOperation; error: Error }>(),
);

// TODO make obsolete
export const loadProjects = createAction('main::loadProjects');
export const loadProject = createAction(
  'main::loadProject',
  props<{ fqpn: API.FQPN }>(),
);
export const loadProjectsSuccess = createAction(
  'main::loadProjectsSuccess',
  props<{ projects: API.ProjectResource[] }>(),
);
export const loadProjectSuccess = createAction(
  'main::loadProjectSuccess',
  props<{ project: API.ProjectResource }>(),
);
export const loadProjectsFailed = createAction(
  'main::loadProjectsFailed',
  props<{ error: Error }>(),
);
export const loadProjectFailed = createAction(
  'main::loadProjectFailed',
  props<{ error: Error }>(),
);

export const projectOperationProgressUpdated = createAction(
  'main::projectOperationProgressUpdated',
  props<{ progress: API.ProjectOperationProgress }>(),
);
