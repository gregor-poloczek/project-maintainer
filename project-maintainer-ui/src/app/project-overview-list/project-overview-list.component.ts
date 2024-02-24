import { Component, Input } from '@angular/core';
import { ProjectOverviewListItemComponent } from '../project-overview-list-item/project-overview-list-item.component';
import { CommonModule, NgForOf } from '@angular/common';
import { API } from '../API';
import { Store } from '@ngrx/store';
import { toggleProject } from '../store/git.actions';
import { AppState } from '../store/AppState';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-project-overview-list',
  standalone: true,
  imports: [ProjectOverviewListItemComponent, NgForOf, CommonModule],
  templateUrl: './project-overview-list.component.html',
  styleUrl: './project-overview-list.component.scss',
})
export class ProjectOverviewListComponent {
  @Input()
  public projects!: API.ProjectResource[];
  public selectedProjects$: Observable<Set<API.FQPN>>;

  constructor(private readonly store: Store<AppState>) {
    this.selectedProjects$ = this.store.select('git', 'selectedProjects');
  }

  public onItemClick(fqpn: API.FQPN) {
    this.store.dispatch(toggleProject({ fqpn }));
  }
}
