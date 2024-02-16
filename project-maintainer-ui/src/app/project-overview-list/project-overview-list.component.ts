import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ProjectOverviewListItemComponent } from '../project-overview-list-item/project-overview-list-item.component';
import { NgForOf } from '@angular/common';
import { API } from '../API';

@Component({
  selector: 'app-project-overview-list',
  standalone: true,
  imports: [ProjectOverviewListItemComponent, NgForOf],
  templateUrl: './project-overview-list.component.html',
  styleUrl: './project-overview-list.component.scss',
})
export class ProjectOverviewListComponent {
  @Input()
  projects!: API.ProjectResource[];
  @Output()
  onSelectionChanged = new EventEmitter<Set<API.FQPN>>();
  private selectedItems = new Set<API.FQPN>();

  public onItemClick(fqpn: API.FQPN) {
    if (!this.selectedItems.delete(fqpn)) {
      this.selectedItems.add(fqpn);
    }
    this.onSelectionChanged.emit(this.selectedItems);
  }

  public isSelected(fqpn: API.FQPN) {
    return this.selectedItems.has(fqpn);
  }
}
