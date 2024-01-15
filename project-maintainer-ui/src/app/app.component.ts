import { Component, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import axios from 'axios';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import moment from 'moment';
import { MatButtonModule } from '@angular/material/button';

interface ProjectResource {
  fqpn: string;
  metaData: {
    owner: string;
    name: string;
  };
  git: {
    uri: string;
    provider: 'UNKNOWN' | 'GITHUB' | 'AWS_CODECOMMIT';
    workingCopy?: {
      latestCommit: {
        hash: string;
        timestamp: string;
        message: string;
      };
    };
  };
}

class ProjectListItem {
  name: string;
  commit: string | null;
  fpqn: string;
  operation: string | null = null;

  constructor(private projectResource: ProjectResource) {
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

enum OperationState {
  SCHEDULED = 'SCHEDULED',
  STARTED = 'STARTED',
  RUNNING = 'RUNNING',
  SUCCEEDED = 'SUCCEEDED',
  FAILED = 'FAILED',
}

interface OperationProgress {
  type: string;
  fpqn: string;
  operation: string;
  state: OperationState;
  progress: number;
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
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  title = 'project-maintainer-ui';
  projects: ProjectListItem[] = [];

  constructor(private zone: NgZone) {}

  onPullClick() {
    this.executeOperation('pull');
  }

  onCloneClick() {
    this.executeOperation('clone');
  }

  onWipeClick() {
    this.executeOperation('wipe');
  }

  private executeOperation(operation: string) {
    const eventSource = new EventSource(
      `http://localhost:8080/v1/projects/operations/${operation}`,
    );
    eventSource.onmessage = (event) => {
      const progress = JSON.parse(event.data) as OperationProgress;
      const project = this.projects.find((p) => p.fpqn === progress.fpqn);
      this.zone.run(() => {
        if (project) {
          project.operation = `${progress.operation} (${progress.state})`;
          if (progress.progress != -1) {
            project.operation += ` ${Math.ceil(progress.progress * 100)}%`;
          }
        }
      });
    };
    eventSource.onopen = (event) => {
      console.log('open:', event);
    };

    eventSource.onerror = (error) => {
      console.error('Fehler:', error);
      eventSource.close();
    };
  }

  ngOnInit() {
    (async () => {
      this.projects = await this.getProjects();
    })();
  }

  async getProjects(): Promise<ProjectListItem[]> {
    const projects = await axios
      .get<ProjectResource[]>('http://localhost:8080/v1/projects/')
      .then((r) => r.data);

    return projects
      .sort((p1, p2) => p1.fqpn.localeCompare(p2.fqpn))
      .map((p) => new ProjectListItem(p));
  }
}
