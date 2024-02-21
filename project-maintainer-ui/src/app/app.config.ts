import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import { provideStore } from '@ngrx/store';
import { projectsReducer } from './store/projects.reducer';
import { provideHttpClient } from '@angular/common/http';
import { provideEffects } from '@ngrx/effects';
import { ProjectsEffects } from './store/projects.effects';
import { searchReducer } from './store/search.reducer';
import { provideStoreDevtools } from '@ngrx/store-devtools';
import { mainReducer } from './store/main.reducer';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideClientHydration(),
    provideStore({
      projects: projectsReducer,
      search: searchReducer,
      main: mainReducer,
    }),
    provideHttpClient(),
    provideEffects(ProjectsEffects),
    provideStoreDevtools(),
  ],
};
