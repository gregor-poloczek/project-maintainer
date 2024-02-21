import { Component, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterLink, RouterOutlet } from '@angular/router';
import { Store } from '@ngrx/store';
import { API } from './API';
import { filter, Observable, tap } from 'rxjs';
import * as projectActions from './store/projects.actions';
import { EventSourceService } from './service/EventSourceService';
import { AppState } from './store/AppState';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  public projects$: Observable<API.ProjectResource[]>;
  public connected$: Observable<boolean>;

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private store: Store<AppState>,
    private eventSourceService: EventSourceService,
  ) {
    this.projects$ = this.store.select('projects');
    this.connected$ = this.store.select('main', 'connected');
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
  }

  ngOnDestroy() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    this.eventSourceService.close();
  }
}
