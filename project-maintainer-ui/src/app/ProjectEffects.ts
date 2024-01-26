import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import * as projectActions from './projects.actions';

import { catchError, map, of, switchMap } from 'rxjs';
import { API } from './API';

@Injectable()
export class ProjectEffects {
  constructor(
    private readonly http: HttpClient,
    private readonly actions$: Actions,
  ) {}

  loadProjects$ = createEffect(() =>
    this.actions$.pipe(
      ofType(projectActions.load),
      switchMap(() =>
        this.http
          .get<API.ProjectResource[]>('http://localhost:8080/v1/projects/')
          .pipe(
            map((projects) => projectActions.loaded({ projects })),
            catchError((error) => of(projectActions.loadFailed({ error }))),
          ),
      ),
    ),
  );
}
