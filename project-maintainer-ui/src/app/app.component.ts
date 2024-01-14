import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import axios from 'axios';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import moment from 'moment';

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

  constructor(private projectResource: ProjectResource) {
    this.name = projectResource.metaData.name;
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
  imports: [CommonModule, RouterOutlet, MatListModule, MatIconModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  title = 'project-maintainer-ui';
  projects: ProjectListItem[] = [];

  ngOnInit() {
    (async () => {
      this.projects = await this.getProjects();
    })();
  }

  async getProjects(): Promise<ProjectListItem[]> {
    const projects = await axios
      .get<ProjectResource[]>('http://localhost:8080/v1/projects/')
      .then((r) => r.data);

    return projects.map((p) => new ProjectListItem(p));
  }
}
