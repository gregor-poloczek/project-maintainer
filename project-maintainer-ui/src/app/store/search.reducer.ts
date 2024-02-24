import { createReducer, on } from '@ngrx/store';
import { API } from '../API';
import * as projectActions from './projects.actions';
import * as searchActions from './search.actions';
import { replaceElement } from './utils';
import ProjectResource = API.ProjectResource;

export interface SearchState {
  rawFilter: string;
  regExpFilter: RegExp | null;
  allProjects: ProjectResource[];
  foundProjects: ProjectResource[];
}

export const initialState: SearchState = {
  rawFilter: '',
  regExpFilter: null,
  allProjects: [],
  foundProjects: [],
};

function applyFilter(state: SearchState): SearchState {
  const { rawFilter, allProjects } = state;
  const regExpFilter = state.regExpFilter
    ? new RegExp(state.regExpFilter.source, state.regExpFilter.flags)
    : null;

  const foundProjects = allProjects.filter(
    (project) =>
      regExpFilter == null ||
      project.metaData.name.toLowerCase().match(regExpFilter) ||
      project.metaData.labels.some((l) => l.toLowerCase().match(regExpFilter)),
  );
  return { ...state, foundProjects };
}

export const searchReducer = createReducer(
  initialState,
  on(projectActions.loadProjectsSuccess, (state, payload) => {
    return applyFilter({
      ...state,
      allProjects: payload.projects,
    });
  }),
  on(projectActions.loadProjectSuccess, (state, payload) => {
    return applyFilter(
      replaceElement(
        state,
        'allProjects',
        payload.project,
        (p) => p.fqpn === payload.project.fqpn,
      ),
    );
  }),
  on(searchActions.updateFilter, (state, payload) => {
    const rawFilter = payload.value;

    let regExpFilter = null;
    try {
      regExpFilter = rawFilter ? new RegExp(`(${rawFilter})`, 'g') : null;
    } catch (e) {}

    return applyFilter({ ...state, rawFilter, regExpFilter });
  }),
);
