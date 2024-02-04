import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ProjectListItem } from '../ProjectListItem';
import { EventSourceService } from '../EventSourceService';
import {
  BehaviorSubject,
  combineLatest,
  filter,
  map,
  Observable,
  startWith,
  switchMap,
} from 'rxjs';
import { API } from '../API';
import { CommonModule } from '@angular/common';
import moment from 'moment';

@Component({
  selector: 'app-project-overview-list-item',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './project-overview-list-item.component.html',
  styleUrl: './project-overview-list-item.component.scss',
})
export class ProjectOverviewListItemComponent {
  @Input() item!: ProjectListItem;
  @Input() selected: boolean = false;
  @Output() onClick = new EventEmitter<API.FQPN>();

  public statusLine$!: Observable<string>;
  public item$!: BehaviorSubject<ProjectListItem>;

  constructor(private eventSourceService: EventSourceService) {}

  ngOnChanges(changes: Partial<ProjectOverviewListItemComponent>) {
    if (changes.item) {
      if (this.item$) {
        this.item$.next(changes.item as ProjectListItem);
        return;
      }
      this.item$ = new BehaviorSubject<ProjectListItem>(this.item);

      const operationProgress$ = this.item$.pipe(
        switchMap((p) =>
          this.eventSourceService.getProjectOperationProgress(p.fpqn),
        ),
        filter((pop) => pop.operation.startsWith('git::')),
      );

      const workingCopyInfo$: Observable<string> = this.item$.pipe(
        map((i) => {
          if (!i.project.git.workingCopy) {
            return 'not cloned';
          }
          const { latestCommit } = i.project.git.workingCopy;
          if (latestCommit) {
            return `${moment(latestCommit.timestamp).fromNow()} - ${latestCommit.message}`;
          }

          return 'no commits';
        }),
      );

      this.statusLine$ = combineLatest([
        this.item$,
        workingCopyInfo$,
        operationProgress$.pipe(startWith(null)),
      ]).pipe(
        map(([item, wci, pop]) => {
          if (!pop) {
            return wci;
          }

          const { message, progress, operation, state } = pop;
          if (state === API.OperationState.SUCCEEDED) {
            return wci;
          }

          let result = `${operation}`;
          if (message) {
            result += ` (${message})`;
          }
          if (progress != -1) {
            result += ` ${Math.ceil(progress * 100)}%`;
          }
          return result;
        }),
      );
    }
  }

  onClickSelf(): void {
    this.onClick.emit(this.item.fpqn);
  }
}
