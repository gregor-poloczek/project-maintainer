import { Component, Inject, NgZone, PLATFORM_ID } from '@angular/core';
import { ProjectLabelComponent } from '../project-label/project-label.component';
import { isPlatformBrowser, NgForOf, NgIf } from '@angular/common';
import { API } from '../API';
import axios from 'axios';
import { FormsModule } from '@angular/forms';
import ProjectResource = API.ProjectResource;

@Component({
  selector: 'app-project-search',
  standalone: true,
  imports: [ProjectLabelComponent, NgForOf, FormsModule, NgIf],
  templateUrl: './project-search.component.html',
  styleUrl: './project-search.component.scss',
})
export class ProjectSearchComponent {
  private updatesEventSource!: EventSource;
  public searchString: string = '';
  public projects: API.ProjectResource[] = [];
  public searchRegExp: RegExp | null = null;

  public constructor(
    private zone: NgZone,
    @Inject(PLATFORM_ID) private platformId: Object,
  ) {}

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.updatesEventSource = new EventSource(
      'http://localhost:8080/v1/projects/updates',
    );
    this.updatesEventSource.onerror = (e) => {
      console.error(e);
    };
    this.updatesEventSource.onmessage = (event) => {
      const progress = JSON.parse(event.data) as API.ProjectOperationProgress;
      if (progress.operation === 'analyse' && progress.state === 'SUCCEEDED') {
        let project = (progress as API.CompletedProjectOperationProgress)
          .project;
        this.zone.run(() => {
          this.projects = [
            project,
            ...this.projects.filter((p) => p.fqpn !== progress.fpqn),
          ].sort((p1, p2) => p1.fqpn.localeCompare(p2.fqpn));
        });
      }
    };

    (async () => {
      this.projects = await this.getProjects();
      this.projects.map((p) =>
        axios.post(
          `http://localhost:8080/v1/projects/${p.fqpn}/operations/analyse`,
        ),
      );
    })();
  }

  ngOnDestroy() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    this.updatesEventSource.close();
  }

  private async getProjects(): Promise<API.ProjectResource[]> {
    const projects = await axios
      .get<API.ProjectResource[]>('http://localhost:8080/v1/projects/')
      .then((r) => r.data);

    return projects.sort((p1, p2) => p1.fqpn.localeCompare(p2.fqpn));
  }

  showLabel(label: string): boolean {
    return !this.searchRegExp || !!label.toLowerCase().match(this.searchRegExp);
  }

  searchStringChanged($event: Event) {
    try {
      this.searchRegExp = this.searchString
        ? new RegExp(`(${this.searchString})`, 'g')
        : null;
    } catch (e) {
      console.error(e);
      this.searchRegExp = null;
    }
  }

  get filteredProjects(): ProjectResource[] {
    return this.projects.filter(
      (p) =>
        p.metaData.labels.length == 0 ||
        this.filteredLabels(p.metaData.labels).length > 0,
    );
  }

  filteredLabels(labels: string[]): string[] {
    return labels.filter((l) => this.showLabel(l));
  }

  getFrameworks(project: API.ProjectResource): string[] {
    return project.metaData.labels
      .filter((l) => l.startsWith('framework'))
      .map((l) => l.replace(/^framework:/, ''))
      .map((l) => l.replace(/:.*$/, ''));
  }
}
