import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-project-label',
  standalone: true,
  imports: [],
  templateUrl: './project-label.component.html',
  styleUrl: './project-label.component.scss',
})
export class ProjectLabelComponent {
  @Input() label!: string;
  @Input() highlight: RegExp | null = null;

  get firstSegment() {
    return this.label.split(':')[0];
  }

  get text(): string {
    return this.highlight
      ? this.label.replaceAll(
          this.highlight,
          '<span class="highlighted">$1</span>',
        )
      : this.label;
  }

  get cssClass(): Record<string, boolean> {
    return {
      [this.firstSegment]: true,
      'highlighting-active': !!this.highlight,
    };
  }
}
