import { createReducer, on } from '@ngrx/store';
import * as projectActions from './projects.actions';
import { replaceElement } from './utils';
import { API } from '../API';

export interface GitState {
  projects: API.ProjectResource[];
}

export const initialState: GitState = {
  projects: [],
};

export const gitReducer = createReducer(
  initialState,
  on(projectActions.loadProjectsSuccess, (state, payload) => {
    return { ...state, projects: payload.projects };
  }),

  on(projectActions.loadProjectSuccess, (state, payload) => {
    return replaceElement(
      state,
      'projects',
      payload.project,
      (p) => p.fqpn === payload.project.fqpn,
    );
  }),
);
