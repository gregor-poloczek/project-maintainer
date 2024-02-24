import { createReducer, on } from '@ngrx/store';
import { API } from '../API';
import * as projectActions from './projects.actions';

export const initialState: API.ProjectResource[] = [];

export const projectsReducer = createReducer(
  initialState,
  on(projectActions.loadProjectsSuccess, (state, payload) => {
    return payload.projects;
  }),

  on(projectActions.loadProjectSuccess, (state, payload) => {
    let result = [...state];

    const index = result.findIndex((p) => p.fqpn === payload.project.fqpn);
    result[index] = payload.project;
    return result;
  }),

  on(projectActions.loadProjectsSuccess, (state, payload) => {
    console.log('loaded...', payload);
    return payload.projects;
  }),
);
