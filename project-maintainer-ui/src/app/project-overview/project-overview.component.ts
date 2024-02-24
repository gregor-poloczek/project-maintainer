import { Component } from '@angular/core';
import { API } from '../API';
import { ProjectOverviewListComponent } from '../project-overview-list/project-overview-list.component';
import { Store } from '@ngrx/store';
import { AppState } from '../store/AppState';
import * as gitActions from '../store/git.actions';
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
  private selectedProjects: Set<API.FQPN> = new Set();

  public constructor(private store: Store<AppState>) {
    this.store.select('git', 'projects').subscribe((projects) => {
      this.projects = projects;
    });
    this.store
      .select('git', 'selectedProjects')
      .subscribe((selectedProjects) => {
        this.selectedProjects = selectedProjects;
      });
  }

  public onCloneButtonClick(): void {
    this.store.dispatch(gitActions.executeClone());
  }

  public onWipeButtonClick(): void {
    this.store.dispatch(gitActions.executeWipe());
  }

  public onPullButtonClick(): void {
    this.store.dispatch(gitActions.executePull());
  }

  public onAnalyseButtonClick(): void {
    this.executeOperations('analyze');
  }

  private executeOperations(operation: API.ProjectOperation) {
    this.store.dispatch(gitActions.executeOperation({ operation }));
  }
}
