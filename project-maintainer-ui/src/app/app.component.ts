import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import axios from 'axios';
import { MatListModule } from '@angular/material/list';

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

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, MatListModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  title = 'project-maintainer-ui';
  projects: ProjectResource[] = [];

  ngOnInit() {
    this.getProjects().then((r) => (this.projects = r.data));
  }

  getProjects() {
    return axios.get<ProjectResource[]>('http://localhost:8080/v1/projects/');
  }
}
