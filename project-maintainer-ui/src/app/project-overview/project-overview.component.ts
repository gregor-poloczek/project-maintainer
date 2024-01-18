import { Component, Inject, NgZone, PLATFORM_ID } from '@angular/core';
import { API } from '../API';
import { ProjectListItem } from '../ProjectListItem';
import axios from 'axios';
import { isPlatformBrowser } from '@angular/common';
import { ProjectOverviewListComponent } from '../project-overview-list/project-overview-list.component';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import OperationState = API.OperationState;

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
  private updatesEventSource!: EventSource;

  projects: ProjectListItem[] = [];

  public constructor(
    private zone: NgZone,
    @Inject(PLATFORM_ID) private platformId: Object,
  ) {}

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
      .filter((p) => p.selected)
      .map((p) => p.fpqn)
      .map((p) => this.executeOperation(p, operation));
  }

  private executeOperation(project: string, operation: string) {
    axios.post(
      `http://localhost:8080/v1/projects/${project}/operations/${operation}`,
    );
  }

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    (async () => {
      this.projects = await this.getProjects();
    })();

    this.updatesEventSource = new EventSource(
      'http://localhost:8080/v1/projects/updates',
    );
    this.updatesEventSource.onmessage = (event) => {
      const progress = JSON.parse(event.data) as API.ProjectOperationProgress;

      let project = this.projects.find((p) => p.fpqn === progress.fpqn)!;

      this.zone.run(() => {
        if (
          progress.state == OperationState.FAILED ||
          progress.state == OperationState.SUCCEEDED
        ) {
          // TODO refactor
          const oldProject = project;
          project = new ProjectListItem(
            (progress as API.CompletedProjectOperationProgress).project,
          );
          project.selected = oldProject.selected;
          this.projects = [
            ...this.projects.filter((p) => p.fpqn !== progress.fpqn),
            project,
          ].sort((p1, p2) => p1.fpqn.localeCompare(p2.fpqn));
        }
        project.operationProgress = progress;
      });
    };
  }

  ngOnDestroy() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    this.updatesEventSource.close();
  }

  private async getProjects(): Promise<ProjectListItem[]> {
    const projects = await axios
      .get<API.ProjectResource[]>('http://localhost:8080/v1/projects/')
      .then((r) => r.data);

    return projects
      .sort((p1, p2) => p1.fqpn.localeCompare(p2.fqpn))
      .map((p) => new ProjectListItem(p));
  }
}
