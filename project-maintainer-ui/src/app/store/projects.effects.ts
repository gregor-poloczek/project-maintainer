import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import * as projectActions from './projects.actions';
import * as mainActions from './main.actions';

import { catchError, filter, map, of, switchMap } from 'rxjs';
import { API } from '../API';

@Injectable()
export class ProjectsEffects {
  constructor(
    private readonly http: HttpClient,
    private readonly actions$: Actions,
  ) {}

  onConnectionEstablished$ = createEffect(() =>
    this.actions$.pipe(
      ofType(mainActions.connectionEstablished),
      map(() => projectActions.loadProjects()),
    ),
  );

  onProjectProgressUpdated = createEffect(() =>
    this.actions$.pipe(
      ofType(projectActions.projectOperationProgressUpdated),
      filter(({ progress }) =>
        [API.OperationState.SUCCEEDED, API.OperationState.FAILED].includes(
          progress.state,
        ),
      ),
      map(({ progress }) =>
        projectActions.loadProject({ fqpn: progress.fqpn }),
      ),
    ),
  );

  onTriggerOperation$ = createEffect(() =>
    this.actions$.pipe(
      ofType(projectActions.triggerOperation),
      switchMap((a) =>
        this.http
          .post<{}>(
            `http://localhost:8080/v1/projects/${a.fqpn}/operations/${a.operation}`,
            {},
          )
          .pipe(
            map(() =>
              projectActions.triggerOperationSuccess({
                fqpn: a.fqpn,
                operation: a.operation,
              }),
            ),
            catchError((error) =>
              of(
                projectActions.triggerOperationFailed({
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
      ofType(projectActions.loadProjects),
      switchMap(() =>
        this.http
          .get<API.ProjectResource[]>('http://localhost:8080/v1/projects/')
          .pipe(
            map((projects) => projectActions.loadProjectsSuccess({ projects })),
            catchError((error) =>
              of(projectActions.loadProjectsFailed({ error })),
            ),
          ),
      ),
    ),
  );

  onLoadProject$ = createEffect(() =>
    this.actions$.pipe(
      ofType(projectActions.loadProject),
      switchMap((a) =>
        this.http
          .get<API.ProjectResource>(
            'http://localhost:8080/v1/projects/' + a.fqpn,
          )
          .pipe(
            map((project) => projectActions.loadProjectSuccess({ project })),
            catchError((error) =>
              of(projectActions.loadProjectFailed({ error })),
            ),
          ),
      ),
    ),
  );
}
