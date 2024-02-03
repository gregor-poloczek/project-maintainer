import { Component, Input, SimpleChanges } from '@angular/core';
import { ProjectListItem } from '../ProjectListItem';
import { EventSourceService } from '../EventSourceService';
import { map, Observable, Subject } from 'rxjs';
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

  ngOnChanges(changes: SimpleChanges) {
    if ('item' in changes) {
      this.operationProgress$ =
        this.eventSourceService.getProjectOperationProgress(
          this.item.project.fqpn,
        );

      this.statusLine$ = this.operationProgress$.pipe(
        map((pop) => {
          const { message, progress, operation } = pop;
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
