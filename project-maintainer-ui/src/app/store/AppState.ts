import { initialState as projects } from './projects.reducer';
import { initialState as search } from './search.reducer';

export interface AppState {
  projects: typeof projects;
  search: typeof search;
}
