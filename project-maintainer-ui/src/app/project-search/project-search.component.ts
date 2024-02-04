import { Component, Inject, PLATFORM_ID } from '@angular/core';
import { ProjectLabelComponent } from '../project-label/project-label.component';
import {
  CommonModule,
  isPlatformBrowser,
  NgForOf,
  NgIf,
} from '@angular/common';
import { API } from '../API';
import { FormsModule } from '@angular/forms';
import { map, Observable, Subscription } from 'rxjs';
import * as projectActions from './../store/projects.actions';
import { Store } from '@ngrx/store';
import { AppState } from '../store/AppState';

@Component({
  selector: 'app-project-search',
  standalone: true,
  imports: [ProjectLabelComponent, NgForOf, FormsModule, NgIf, CommonModule],
  templateUrl: './project-search.component.html',
  styleUrl: './project-search.component.scss',
})
export class ProjectSearchComponent {
  public searchString: string = '';
  public projects: API.ProjectResource[] = [];
  public searchRegExp: RegExp | null = null;
  private subscription!: Subscription;
  private projects$: Observable<API.ProjectResource[]>;
  public filteredProjects$: Observable<API.ProjectResource[]>;

  public constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private store: Store<AppState>,
  ) {
    this.projects$ = this.store.select('projects');

    this.filteredProjects$ = this.projects$.pipe(
      map((projects) =>
        projects.filter(
          (p) =>
            p.metaData.labels.length == 0 ||
            this.filteredLabels(p.metaData.labels).length > 0,
        ),
      ),
    );
  }

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    this.projects$.subscribe((p) => {
      for (const project of p) {
        this.store.dispatch(
          projectActions.triggerOperation({
            fqpn: project.fqpn,
            operation: 'analyze',
          }),
        );
      }
    });
  }

  ngOnDestroy() {
    this.subscription?.unsubscribe();
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
