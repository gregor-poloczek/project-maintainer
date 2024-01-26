import { API } from './API';
import { createReducer, on } from '@ngrx/store';
import * as projectActions from './projects.actions';

export const initialState: API.ProjectResource[] = [];

export const projectsReducer = createReducer(
  initialState,
  on(projectActions.load, (state) => {
    console.log('load...');
    return state;
  }),
  on(projectActions.loaded, (state, payload) => {
    console.log('loaded...', payload);
    return payload.projects;
  }),
);
