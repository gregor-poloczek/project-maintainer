import { Component } from '@angular/core';
import { API } from '../API';
import { ProjectListItem } from '../ProjectListItem';
import axios from 'axios';
import { ProjectOverviewListComponent } from '../project-overview-list/project-overview-list.component';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Observable } from 'rxjs';
import { Store } from '@ngrx/store';

@Component({
  selector: 'app-project-overview',
  standalone: true,
  imports: [
    ProjectOverviewListComponent,
    MatButtonModule,
    MatToolbarModule,
    MatTooltipModule,
  ],
  templateUrl: './project-overview.component.html',
  styleUrl: './project-overview.component.scss',
})
export class ProjectOverviewComponent {
  public projects: ProjectListItem[] = [];
  private projects$: Observable<API.ProjectResource[]>;
  private selectedProjects = new Set<API.FQPN>();

  onSelectionChanged(selected: Set<API.FQPN>) {
    this.selectedProjects = new Set(selected);
  }

  public constructor(
    private store: Store<{ projects: API.ProjectResource[] }>,
  ) {
    this.projects$ = this.store.select('projects');

    this.projects$.subscribe((projects) => {
      this.projects = projects.map((p) => new ProjectListItem(p));
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
    this.executeOperations('analyse');
  }

  private executeOperations(operation: string) {
    this.projects
      .filter((p) => this.selectedProjects.has(p.fpqn))
      .map((p) => p.fpqn)
      .map((p) => this.executeOperation(p, operation));
  }

  private executeOperation(project: string, operation: string) {
    // TODO replace with http module
    axios.post(
      `http://localhost:8080/v1/projects/${project}/operations/${operation}`,
    );
  }
}
