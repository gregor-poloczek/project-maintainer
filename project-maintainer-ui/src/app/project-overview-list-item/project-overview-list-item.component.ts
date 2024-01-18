import { Component, Input } from '@angular/core';
import { ProjectListItem } from '../ProjectListItem';

@Component({
  selector: 'app-project-overview-list-item',
  standalone: true,
  imports: [],
  templateUrl: './project-overview-list-item.component.html',
  styleUrl: './project-overview-list-item.component.scss',
})
export class ProjectOverviewListItemComponent {
  @Input() item!: ProjectListItem;

  onItemClick(): void {
    this.item.selected = !this.item.selected;
  }
}
