import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import * as mainActions from './main.actions';

import { catchError, filter, map, of, switchMap } from 'rxjs';
import { API } from '../API';

@Injectable()
export class MainEffects {
  constructor(
    private readonly http: HttpClient,
    private readonly actions$: Actions,
  ) {}

  onConnectionEstablished$ = createEffect(() =>
    this.actions$.pipe(
      ofType(mainActions.connectionEstablished),
      map(() => mainActions.loadProjects()),
    ),
  );

  onProjectProgressUpdated = createEffect(() =>
    this.actions$.pipe(
      ofType(mainActions.projectOperationProgressUpdated),
      filter(({ progress }) =>
        [API.OperationState.SUCCEEDED, API.OperationState.FAILED].includes(
          progress.state,
        ),
      ),
      map(({ progress }) => mainActions.loadProject({ fqpn: progress.fqpn })),
    ),
  );

  onTriggerOperation$ = createEffect(() =>
    this.actions$.pipe(
      ofType(mainActions.triggerOperation),
      switchMap((a) =>
        this.http
          .post<{}>(
            `http://localhost:8080/v1/projects/${a.fqpn}/operations/${a.operation}`,
            {},
          )
          .pipe(
            map(() =>
              mainActions.triggerOperationSuccess({
                fqpn: a.fqpn,
                operation: a.operation,
              }),
            ),
            catchError((error) =>
              of(
                mainActions.triggerOperationFailed({
                  fqpn: a.fqpn,
                  operation: a.operation,
                  error,
                }),
              ),
            ),
          ),
      ),
    ),
  );

  onLoadProjects$ = createEffect(() =>
    this.actions$.pipe(
      ofType(mainActions.loadProjects),
      switchMap(() =>
        this.http
          .get<API.ProjectResource[]>('http://localhost:8080/v1/projects/')
          .pipe(
            map((projects) => mainActions.loadProjectsSuccess({ projects })),
            catchError((error) =>
              of(mainActions.loadProjectsFailed({ error })),
            ),
          ),
      ),
    ),
  );

  onLoadProject$ = createEffect(() =>
    this.actions$.pipe(
      ofType(mainActions.loadProject),
      switchMap((a) =>
        this.http
          .get<API.ProjectResource>(
            'http://localhost:8080/v1/projects/' + a.fqpn,
          )
          .pipe(
            map((project) => mainActions.loadProjectSuccess({ project })),
            catchError((error) => of(mainActions.loadProjectFailed({ error }))),
          ),
      ),
    ),
  );
}
