import { Component } from '@angular/core';
import { API } from '../API';
import { ProjectOverviewListComponent } from '../project-overview-list/project-overview-list.component';
import { Observable } from 'rxjs';
import { Store } from '@ngrx/store';
import { AppState } from '../store/AppState';
import * as projectsActions from '../store/projects.actions';
import { ToolbarComponent } from '../toolbar/toolbar.component';

@Component({
  selector: 'app-project-overview',
  standalone: true,
  imports: [ProjectOverviewListComponent, ToolbarComponent],
  templateUrl: './project-overview.component.html',
  styleUrl: './project-overview.component.scss',
})
export class ProjectOverviewComponent {
  public projects: API.ProjectResource[] = [];
  private projects$: Observable<API.ProjectResource[]>;
  private selectedProjects = new Set<API.FQPN>();

  public constructor(private store: Store<AppState>) {
    this.projects$ = this.store.select('git', 'projects');

    this.projects$.subscribe((projects) => {
      this.projects = projects;
    });
  }

  public onCloneButtonClick(): void {
    this.executeOperations('clone');
  }

  public onWipeButtonClick(): void {
    this.executeOperations('wipe');
  }

  public onPullButtonClick(): void {
    this.executeOperations('pull');
  }

  public onAnalyseButtonClick(): void {
    this.executeOperations('analyze');
  }

  private executeOperations(operation: API.ProjectOperation) {
    this.projects
      .filter((p) => this.selectedProjects.has(p.fqpn))
      .map((p) => p.fqpn)
      .map((p) => this.executeOperation(p, operation));
  }

  private executeOperation(fqpn: API.FQPN, operation: API.ProjectOperation) {
    this.store.dispatch(projectsActions.triggerOperation({ fqpn, operation }));
  }

  onSelectionChanged(selected: Set<API.FQPN>) {
    this.selectedProjects = new Set(selected);
  }
}
