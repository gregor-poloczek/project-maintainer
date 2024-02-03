import { Component, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterLink, RouterOutlet } from '@angular/router';
import { Store } from '@ngrx/store';
import { API } from './API';
import { filter, map, Observable, tap } from 'rxjs';
import * as projectActions from './projects.actions';
import { EventSourceService } from './EventSourceService';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  public projects$: Observable<API.ProjectResource[]>;
  public items$: Observable<{ fqpn: string; name: string }[]>;

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private store: Store<{ projects: API.ProjectResource[] }>,
    private eventSourceService: EventSourceService,
  ) {
    this.projects$ = this.store.select('projects');

    this.items$ = this.projects$.pipe(
      map((projects) =>
        projects.map((p) => ({
          fqpn: p.fqpn,
          name: p.metaData.name
            .split('-')
            .map((part) => part[0].toUpperCase() + part.substring(1))
            .join(' '),
        })),
      ),
    );
  }

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    // TODO erst intialisieren, wenn projekte geladen sind
    this.eventSourceService.init();

    this.eventSourceService
      .getMessageStream()
      .pipe(
        filter((pop) =>
          [API.OperationState.SUCCEEDED, API.OperationState.FAILED].includes(
            pop.state,
          ),
        ),
        tap((pop) =>
          this.store.dispatch(projectActions.loadProject({ fqpn: pop.fqpn })),
        ),
      )
      .subscribe();

    this.store.dispatch(projectActions.loadProjects());
  }

  ngOnDestroy() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    this.eventSourceService.close();
  }
}
