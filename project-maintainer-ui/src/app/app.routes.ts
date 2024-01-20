import { Routes } from '@angular/router';
import { ProjectOverviewComponent } from './project-overview/project-overview.component';
import { ProjectSearchComponent } from './project-search/project-search.component';

export const routes: Routes = [
  {
    path: '',
    component: ProjectOverviewComponent,
  },
  {
    path: 'search',
    component: ProjectSearchComponent,
  },
];
