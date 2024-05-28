import { SearchState } from './search.reducer';
import { GitState } from './git.reducer';
import { MainState } from './main.reducer';

export interface AppState {
  git: GitState;
  search: SearchState;
  main: MainState;
}
