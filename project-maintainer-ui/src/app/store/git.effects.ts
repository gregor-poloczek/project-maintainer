import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import * as gitActions from './git.actions';
import * as projectActions from './projects.actions';

import { mergeMap, Observable, withLatestFrom } from 'rxjs';
import { API } from '../API';
import { AppState } from './AppState';
import { Store } from '@ngrx/store';

@Injectable()
export class GitEffects {
  private selectedProjects$: Observable<Set<API.FQPN>>;

  constructor(
    private readonly actions$: Actions,
    private readonly http: HttpClient,
    private readonly store: Store<AppState>,
  ) {
    this.onExecuteOperation$ = createEffect(() =>
      this.actions$.pipe(
        ofType(gitActions.executeOperation),
        withLatestFrom(this.store.select('git', 'selectedProjects')),
        mergeMap(([action, selectedProjects]) => {
          return Array.from(selectedProjects).map((fqpn) =>
            projectActions.triggerOperation({
              fqpn,
              operation: action.operation,
            }),
          );
        }),
      ),
    );
  }

  onExecuteOperation$: Observable<any>;
}
