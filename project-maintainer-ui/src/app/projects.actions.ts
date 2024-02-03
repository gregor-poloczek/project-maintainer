import { createAction, props } from '@ngrx/store';
import { API } from './API';

const refresh = createAction('[PROJECTS] Refresh');
export const loadProjects = createAction('[PROJECTS] Load Projects');
export const loadProject = createAction(
  '[PROJECTS] Load Project',
  props<{ fqpn: string }>(),
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
