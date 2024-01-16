import { Component, Inject, NgZone, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import axios from 'axios';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import moment from 'moment';
import { MatButtonModule } from '@angular/material/button';
import { API } from './API';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import OperationState = API.OperationState;

class ProjectListItem {
  name: string;
  commit: string | null;
  fpqn: string;
  operationProgress: API.ProjectOperationProgress | null = null;

  get cloned(): boolean {
    return !!this.projectResource.git.workingCopy;
  }

  get operationLabel(): string | null {
    if (!this.operationProgress || !this.operationInProgress) {
      return null;
    }
    const { message, progress, operation } = this.operationProgress;
    let result = `${operation}`;
    if (message) {
      result += ` (${message})`;
    }
    if (progress != -1) {
      result += ` ${Math.ceil(progress * 100)}%`;
    }
    return result;
  }

  get operationInProgress(): boolean {
    let b =
      this.operationProgress != null &&
      ![OperationState.SUCCEEDED, OperationState.FAILED].includes(
        this.operationProgress.state,
      );
    return b;
  }

  get statusLine(): string {
    if (this.operationInProgress) {
      return this.operationLabel as string;
    }
    if (!this.cloned) {
      return 'Not cloned';
    }
    let latestCommit = this.projectResource.git.workingCopy?.latestCommit;
    if (latestCommit) {
      return `${moment(latestCommit.timestamp).fromNow()} - ${latestCommit.message}`;
    }
    return '??undefined state??';
  }

  constructor(private projectResource: API.ProjectResource) {
    this.name = projectResource.metaData.name;
    this.fpqn = projectResource.fqpn;
    let latestCommit = projectResource.git.workingCopy?.latestCommit;
    if (latestCommit) {
      this.commit = `${moment(latestCommit.timestamp).fromNow()} - ${latestCommit.message}`;
    } else {
      this.commit = null;
    }
  }
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatToolbarModule,
    MatTooltipModule,
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  private updatesEventSource!: EventSource;

  title = 'project-maintainer-ui';
  projects: ProjectListItem[] = [];
  selectedProjects = new Set<string>();

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

  private executeOperations(operation: string) {
    Array.from(this.selectedProjects.keys()).map((p) =>
      this.executeOperation(p, operation),
    );
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
          project = new ProjectListItem(
            (progress as API.CompletedProjectOperationProgress).project,
          );
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

  public onProjectSelectionChanged(
    project: ProjectListItem,
    selected: boolean,
  ) {
    if (selected) {
      this.selectedProjects.add(project.fpqn);
    } else {
      this.selectedProjects.delete(project.fpqn);
    }
  }
}
