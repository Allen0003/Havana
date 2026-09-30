import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/lobby/lobby').then(m => m.LobbyComponent),
  },
  {
    path: 'game',
    loadComponent: () =>
      import('./features/game/game').then(m => m.GameComponent),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
