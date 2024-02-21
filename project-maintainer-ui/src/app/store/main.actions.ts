import { createAction } from '@ngrx/store';

export const connectionLost = createAction('main::connectionLost');
export const connectionFailed = createAction('main::connectionFailed');
export const connectionEstablished = createAction(
  'main::connectionEstablished',
);
