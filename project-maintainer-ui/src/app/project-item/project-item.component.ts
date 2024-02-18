import { Component, Input } from '@angular/core';
import { AsyncPipe, NgIf } from '@angular/common';
import { API } from '../API';
import ProjectResource = API.ProjectResource;

@Component({
  selector: 'app-project-item',
  standalone: true,
  imports: [AsyncPipe, NgIf],
  templateUrl: './project-item.component.html',
  styleUrl: './project-item.component.scss',
})
export class ProjectItemComponent {
  @Input() project!: ProjectResource;
  @Input({ required: false }) textLine?: string | null;

  get cloned(): boolean {
    return !!this.project.git.workingCopy;
  }

  get readableName(): string {
    const parts = this.project.metaData.name.split('-');

    return parts.map((s) => s.charAt(0).toUpperCase() + s.slice(1)).join(' ');
  }
}
