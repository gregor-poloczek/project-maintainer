import { initialState } from './projects.reducer';

export interface AppState {
  projects: typeof initialState;
}
