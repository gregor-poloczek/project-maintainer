import { Component, Input } from '@angular/core';
import { ProjectListItem } from '../ProjectListItem';
import { ProjectOverviewListItemComponent } from '../project-overview-list-item/project-overview-list-item.component';
import { NgForOf } from '@angular/common';

@Component({
  selector: 'app-project-overview-list',
  standalone: true,
  imports: [ProjectOverviewListItemComponent, NgForOf],
  templateUrl: './project-overview-list.component.html',
  styleUrl: './project-overview-list.component.scss',
})
export class ProjectOverviewListComponent {
  @Input()
  items!: ProjectListItem[];
}
