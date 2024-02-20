import { filter, Observable, Subject, timer } from 'rxjs';
import { Injectable } from '@angular/core';
import { API } from '../API';
import { Store } from '@ngrx/store';
import { AppState } from '../store/AppState';
import * as projectsActions from '../store/projects.actions';

@Injectable({
  providedIn: 'root',
})
export class EventSourceService {
  private eventSource: EventSource | null = null;
  private connected: boolean = false;

  private readonly projectOperationProgress =
    new Subject<API.ProjectOperationProgress>();

  private readonly operationProgressByFQPN = new Map<
    string,
    Subject<API.ProjectOperationProgress>
  >();

  constructor(private readonly store: Store<AppState>) {}

  public getMessageStream(): Observable<API.ProjectOperationProgress> {
    return this.projectOperationProgress.asObservable();
  }

  public getProjectOperationProgress(
    fqpn: API.FQPN,
  ): Subject<API.ProjectOperationProgress> {
    let result = this.operationProgressByFQPN.get(fqpn);

    if (!result) {
      result = new Subject();
      this.projectOperationProgress
        .pipe(filter((pop) => pop.fqpn === fqpn))
        .subscribe((pop) => result!.next(pop));
      this.operationProgressByFQPN.set(fqpn, result);
    }
    return result!;
  }

  public close(): void {
    this.eventSource?.close();
    this.connected = false;
  }

  public init(): void {
    this.eventSource = new EventSource(
      'http://localhost:8080/v1/projects/updates',
    );

    this.eventSource.onopen = () => {
      console.log('Event stream connection to backend lost established.');
      this.connected = true;
      this.store.dispatch(projectsActions.connectionEstablished());
    };

    this.eventSource.onerror = (error: any) => {
      let message = this.connected
        ? 'Event stream connection to backend lost, reconnecting ...'
        : 'Event stream connection to backend not established, reconnecting ...';

      console.log(message, error);
      try {
        this.eventSource?.close();
        if (this.connected) {
          this.store.dispatch(projectsActions.connectionLost());
        } else {
          this.store.dispatch(projectsActions.connectionFailed());
        }
      } finally {
        this.connected = false;
        this.eventSource = null;
        timer(2000).subscribe(() => this.init());
      }
    };

    this.eventSource.addEventListener('message', (event: MessageEvent) => {
      const progress = JSON.parse(event.data) as API.ProjectOperationProgress;
      this.projectOperationProgress.next(progress);
    });
  }
}
