import { API } from './API';
import { createReducer, on } from '@ngrx/store';
import * as projectActions from './projects.actions';

export const initialState: API.ProjectResource[] = [];

export const projectsReducer = createReducer(
  initialState,
  on(projectActions.loadProjects, (state) => {
    console.log('load...');
    return state;
  }),

  on(projectActions.loadedProjects, (state, payload) => {
    console.log('loaded...', payload);
    return payload.projects;
  }),

  on(projectActions.loadedProject, (state, payload) => {
    let result = [...state];

    const index = result.findIndex((p) => p.fqpn === payload.project.fqpn);
    result[index] = payload.project;
    return result;
  }),

  on(projectActions.loadedProjects, (state, payload) => {
    console.log('loaded...', payload);
    return payload.projects;
  }),
);
