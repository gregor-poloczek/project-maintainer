import { createReducer, on } from '@ngrx/store';
import * as mainActions from './main.actions';
import * as gitActions from './git.actions';
import { replaceElement } from './utils';
import { API } from '../API';

export interface GitState {
  projects: API.ProjectResource[];
  selectedProjects: Set<API.FQPN>;
}

export const initialState: GitState = {
  projects: [],
  selectedProjects: new Set(),
};

export const gitReducer = createReducer(
  initialState,
  on(gitActions.toggleProject, (state, payload) => {
    const selectedProjects = new Set(state.selectedProjects);
    if (selectedProjects.has(payload.fqpn)) {
      selectedProjects.delete(payload.fqpn);
    } else {
      selectedProjects.add(payload.fqpn);
    }
    return { ...state, selectedProjects };
  }),

  on(mainActions.loadProjectsSuccess, (state, payload) => {
    return { ...state, projects: payload.projects };
  }),

  on(mainActions.loadProjectSuccess, (state, payload) => {
    return replaceElement(
      state,
      'projects',
      payload.project,
      (p) => p.fqpn === payload.project.fqpn,
    );
  }),
);
