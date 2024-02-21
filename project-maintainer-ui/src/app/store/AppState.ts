import { initialState as projects } from './projects.reducer';
import { initialState as search } from './search.reducer';
import { initialState as main } from './main.reducer';

export interface AppState {
  projects: typeof projects;
  search: typeof search;
  main: typeof main;
}
