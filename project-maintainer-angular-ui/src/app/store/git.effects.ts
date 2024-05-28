import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import * as gitActions from './git.actions';
import * as mainActions from './main.actions';

import { map, mergeMap, Observable, withLatestFrom } from 'rxjs';
import { AppState } from './AppState';
import { Store } from '@ngrx/store';

@Injectable()
export class GitEffects {
  constructor(
    private readonly actions$: Actions,
    private readonly store: Store<AppState>,
  ) {
    this.onExecuteOperation$ = createEffect(() =>
      this.actions$.pipe(
        ofType(gitActions.executeOperation),
        withLatestFrom(this.store.select('git', 'selectedProjects')),
        mergeMap(([action, selectedProjects]) => {
          return Array.from(selectedProjects).map((fqpn) =>
            mainActions.triggerOperation({
              fqpn,
              operation: action.operation,
            }),
          );
        }),
      ),
    );
  }

  onExecuteOperation$: Observable<any>;
  onExecutePull$ = createEffect(() =>
    this.actions$.pipe(
      ofType(gitActions.executePull),
      map(() => gitActions.executeOperation({ operation: 'pull' })),
    ),
  );
  onExecuteClone$ = createEffect(() =>
    this.actions$.pipe(
      ofType(gitActions.executeClone),
      map(() => gitActions.executeOperation({ operation: 'clone' })),
    ),
  );
  onExecuteWipe$ = createEffect(() =>
    this.actions$.pipe(
      ofType(gitActions.executeWipe),
      map(() => gitActions.executeOperation({ operation: 'wipe' })),
    ),
  );
}
