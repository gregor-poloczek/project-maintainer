import { Component, Input } from '@angular/core';
import { ProjectListItem } from '../ProjectListItem';
import { EventSourceService } from '../EventSourceService';
import {
  BehaviorSubject,
  combineLatest,
  map,
  Observable,
  startWith,
  Subject,
} from 'rxjs';
import { API } from '../API';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-project-overview-list-item',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './project-overview-list-item.component.html',
  styleUrl: './project-overview-list-item.component.scss',
})
export class ProjectOverviewListItemComponent {
  @Input() item!: ProjectListItem;

  private operationProgress$!: Subject<API.ProjectOperationProgress>;

  public statusLine$!: Observable<string>;
  public item$!: BehaviorSubject<ProjectListItem>;

  ngOnChanges(changes: Partial<ProjectOverviewListItemComponent>) {
    if (changes.item) {
      if (!this.item$) {
        this.item$ = new BehaviorSubject<ProjectListItem>(this.item);
      } else {
        this.item$.next(changes.item as ProjectListItem);
      }

      this.operationProgress$ =
        this.eventSourceService.getProjectOperationProgress(
          this.item.project.fqpn,
        );

      this.statusLine$ = combineLatest([
        this.item$,
        this.operationProgress$.pipe(startWith(null)),
      ]).pipe(
        map(([item, pop]) => {
          const commitMessage =
            item.project.git.workingCopy?.latestCommit.message;

          if (!pop) {
            return commitMessage || '';
          }

          const { message, progress, operation, state } = pop;

          if (state === API.OperationState.SUCCEEDED) {
            return commitMessage || '';
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

  constructor(private eventSourceService: EventSourceService) {}

  onItemClick(): void {
    this.item.selected = !this.item.selected;
  }
}
