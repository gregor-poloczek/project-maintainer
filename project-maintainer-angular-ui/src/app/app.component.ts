import { Component, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterLink, RouterOutlet } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { EventSourceService } from './service/EventSourceService';
import { AppState } from './store/AppState';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  public connected$: Observable<boolean>;

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private store: Store<AppState>,
    private eventSourceService: EventSourceService,
  ) {
    this.connected$ = this.store.select('main', 'connected');
  }

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    // TODO erst intialisieren, wenn projekte geladen sind
    this.eventSourceService.init();
  }

  ngOnDestroy() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    this.eventSourceService.close();
  }
}
