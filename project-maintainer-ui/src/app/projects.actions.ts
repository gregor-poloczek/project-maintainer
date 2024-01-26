import { createAction, props } from '@ngrx/store';
import { API } from './API';

const refresh = createAction('[PROJECTS] Refresh');
export const load = createAction('[PROJECTS] Load');
export const loaded = createAction(
  '[PROJECTS] Loaded',
  props<{ projects: API.ProjectResource[] }>(),
);
export const loadFailed = createAction(
  '[PROJECTS] Load failed',
  props<{ error: Error }>(),
);
