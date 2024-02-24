import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import { provideStore } from '@ngrx/store';
import { provideHttpClient } from '@angular/common/http';
import { provideEffects } from '@ngrx/effects';
import { ProjectsEffects } from './store/projects.effects';
import { searchReducer } from './store/search.reducer';
import { provideStoreDevtools } from '@ngrx/store-devtools';
import { mainReducer } from './store/main.reducer';
import { gitReducer } from './store/git.reducer';
import { GitEffects } from './store/git.effects';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideClientHydration(),
    provideStore({
      git: gitReducer,
      search: searchReducer,
      main: mainReducer,
    }),
    provideHttpClient(),
    provideEffects(ProjectsEffects, GitEffects),
    provideStoreDevtools(),
  ],
};
