import { Component, Inject, PLATFORM_ID } from '@angular/core';
import { ProjectLabelComponent } from '../project-label/project-label.component';
import {
  CommonModule,
  isPlatformBrowser,
  NgForOf,
  NgIf,
} from '@angular/common';
import { API } from '../API';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, map, Observable, Subscription, take } from 'rxjs';
import * as projectActions from './../store/projects.actions';
import * as searchActions from './../store/search.actions';
import { Store } from '@ngrx/store';
import { AppState } from '../store/AppState';
import { ProjectItemComponent } from '../project-item/project-item.component';
import { ToolbarComponent } from '../toolbar/toolbar.component';

@Component({
  selector: 'app-project-search',
  standalone: true,
  imports: [
    ProjectLabelComponent,
    NgForOf,
    FormsModule,
    NgIf,
    CommonModule,
    ProjectItemComponent,
    ToolbarComponent,
    ReactiveFormsModule,
  ],
  templateUrl: './project-search.component.html',
  styleUrl: './project-search.component.scss',
})
export class ProjectSearchComponent {
  public projects: API.ProjectResource[] = [];
  public searchRegExp$: Observable<RegExp | null>;
  public searchRegExp: RegExp | null = null;
  private subscription!: Subscription;
  private projects$: Observable<API.ProjectResource[]>;
  public filteredProjects$: Observable<API.ProjectResource[]>;
  public searchField = new FormControl('');

  public constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private store: Store<AppState>,
  ) {
    this.projects$ = this.store
      .select('projects')
      .pipe(map((projects) => projects.filter((p) => !!p.git.workingCopy)));

    this.filteredProjects$ = this.store.select('search', 'foundProjects');
    this.searchRegExp$ = this.store.select('search', 'regExpFilter');

    this.searchRegExp$.subscribe((r) => {
      this.searchRegExp = !r ? null : new RegExp(r.source, r.flags);
    });
  }

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.searchField.valueChanges.pipe(debounceTime(300)).subscribe((value) => {
      this.store.dispatch(searchActions.updateFilter({ value: value || '' }));
    });

    // TODO make it work without take(2) - used to call this only once, when the projects are
    //  finally available
    this.projects$.pipe(take(2)).subscribe((p) => {
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
    try {
      return (
        !this.searchRegExp || !!label.toLowerCase().match(this.searchRegExp)
      );
    } catch (e) {
      debugger;
      throw e;
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
