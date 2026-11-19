import { Component } from '@angular/core';
import { AccountComponent } from './account.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [AccountComponent],
  template: `<app-account />`,
})
export class AppComponent {}
