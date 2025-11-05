import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from './core/services/theme.service';
import { ToastComponent } from './shared/components/toast/toast.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ToastComponent],
  template: `
    <router-outlet></router-outlet>
    <app-toast></app-toast>
  `,
  styles: []
})
export class AppComponent implements OnInit {
  title = 'Loan Management System';

  constructor(private themeService: ThemeService) {}

  ngOnInit(): void {
    // Initialize theme service - this will apply the saved theme or system theme
  }
}
