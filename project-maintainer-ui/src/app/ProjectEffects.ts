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
      ofType(projectActions.loadProjects),
      switchMap(() =>
        this.http
          .get<API.ProjectResource[]>('http://localhost:8080/v1/projects/')
          .pipe(
            map((projects) => projectActions.loadedProjects({ projects })),
            catchError((error) =>
              of(projectActions.loadProjectsFailed({ error })),
            ),
          ),
      ),
    ),
  );
  loadProject$ = createEffect(() =>
    this.actions$.pipe(
      ofType(projectActions.loadProject),
      switchMap((a) =>
        this.http
          .get<API.ProjectResource>(
            'http://localhost:8080/v1/projects/' + a.fqpn,
          )
          .pipe(
            map((project) => projectActions.loadedProject({ project })),
            catchError((error) =>
              of(projectActions.loadProjectsFailed({ error })),
            ),
          ),
      ),
    ),
  );
}
