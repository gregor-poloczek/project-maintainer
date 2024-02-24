import { createReducer, on } from '@ngrx/store';
import * as mainActions from './main.actions';

export interface MainState {
  connected: boolean;
}

export const initialState: MainState = {
  connected: false,
};

export const mainReducer = createReducer(
  initialState,
  on(mainActions.connectionLost, (state) => {
    return { ...state, connected: false };
  }),
  on(mainActions.connectionEstablished, (state) => {
    return { ...state, connected: true };
  }),
);
