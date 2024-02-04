import { filter, Observable, Subject } from 'rxjs';
import { Injectable } from '@angular/core';
import { API } from '../API';

@Injectable({
  providedIn: 'root',
})
export class EventSourceService {
  private eventSource!: EventSource;

  private readonly projectOperationProgress =
    new Subject<API.ProjectOperationProgress>();

  private readonly operationProgressByFQPN = new Map<
    string,
    Subject<API.ProjectOperationProgress>
  >();

  constructor() {}

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
  }

  public init(): void {
    this.eventSource = new EventSource(
      'http://localhost:8080/v1/projects/updates',
    );

    this.eventSource.addEventListener('message', (event: MessageEvent) => {
      const progress = JSON.parse(event.data) as API.ProjectOperationProgress;
      this.projectOperationProgress.next(progress);
    });

    this.eventSource.onerror = (error: any) => {
      this.eventSource.close();
      this.projectOperationProgress.error(error);
    };
  }
}
