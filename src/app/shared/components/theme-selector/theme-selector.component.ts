import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { ThemeService, Theme, ThemeConfig } from '../../../core/services/theme.service';

@Component({
  selector: 'app-theme-selector',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="theme-selector" [class.expanded]="isExpanded">
      
      <!-- Theme Toggle Button -->
      <button 
        class="theme-toggle-btn"
        (click)="toggleExpanded()"
        [title]="'Current theme: ' + (currentThemeConfig?.name || 'Unknown')"
      >
        <i class="fas" [ngClass]="currentThemeConfig?.icon || 'fa-palette'"></i>
        <span class="theme-name" *ngIf="showLabel">{{ currentThemeConfig?.name || 'Theme' }}</span>
        <i class="fas fa-chevron-down expand-icon" [class.rotated]="isExpanded"></i>
      </button>

      <!-- Theme Options Panel -->
      <div *ngIf="isExpanded" class="theme-panel slide-in-up">
        <div class="theme-panel-header">
          <h6 class="panel-title">
            <i class="fas fa-palette"></i>
            Choose Theme
          </h6>
          <button class="close-btn" (click)="closePanel()">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div class="theme-options">
          <div 
            *ngFor="let theme of availableThemes"
            class="theme-option"
            [class.active]="theme.id === currentTheme"
            (click)="selectTheme(theme.id)"
          >
            <div class="theme-preview">
              <div class="preview-colors">
                <div 
                  class="color-dot primary" 
                  [style.background-color]="theme.colors.primary"
                ></div>
                <div 
                  class="color-dot secondary" 
                  [style.background-color]="theme.colors.secondary"
                ></div>
                <div 
                  class="color-dot accent" 
                  [style.background-color]="theme.colors.accent"
                ></div>
              </div>
              <div class="theme-icon">
                <i class="fas" [ngClass]="theme.icon"></i>
              </div>
            </div>
            
            <div class="theme-info">
              <div class="theme-name">{{ theme.name }}</div>
              <div class="theme-description">{{ theme.description }}</div>
            </div>
            
            <div class="theme-status">
              <i *ngIf="theme.id === currentTheme" class="fas fa-check active-indicator"></i>
            </div>
          </div>
        </div>

        <!-- Theme Features -->
        <div class="theme-features">
          <div class="feature-item">
            <label class="feature-toggle">
              <input 
                type="checkbox" 
                [checked]="animationsEnabled"
                (change)="toggleAnimations($event)"
              />
              <span class="toggle-slider"></span>
              <span class="feature-label">
                <i class="fas fa-magic"></i>
                Smooth Animations
              </span>
            </label>
          </div>
          
          <div class="feature-item">
            <label class="feature-toggle">
              <input 
                type="checkbox" 
                [checked]="highContrastMode"
                (change)="toggleHighContrast($event)"
              />
              <span class="toggle-slider"></span>
              <span class="feature-label">
                <i class="fas fa-eye"></i>
                High Contrast
              </span>
            </label>
          </div>
          
          <div class="feature-item">
            <label class="feature-toggle">
              <input 
                type="checkbox" 
                [checked]="reducedMotion"
                (change)="toggleReducedMotion($event)"
              />
              <span class="toggle-slider"></span>
              <span class="feature-label">
                <i class="fas fa-universal-access"></i>
                Reduced Motion
              </span>
            </label>
          </div>
        </div>

        <!-- Quick Actions -->
        <div class="quick-actions">
          <button class="action-btn" (click)="resetToDefault()">
            <i class="fas fa-undo"></i>
            Reset to Default
          </button>
          <button class="action-btn" (click)="exportThemeSettings()">
            <i class="fas fa-download"></i>
            Export Settings
          </button>
        </div>
      </div>

      <!-- Backdrop -->
      <div 
        *ngIf="isExpanded" 
        class="theme-backdrop"
        (click)="closePanel()"
      ></div>
    </div>
  `,
  styles: [`
    .theme-selector {
      position: relative;
      display: inline-block;
    }

    .theme-toggle-btn {
      display: flex;
      align-items: center;
      gap: 8px;
      background: var(--bg-primary);
      border: 1px solid var(--border-color);
      border-radius: 8px;
      padding: 8px 12px;
      cursor: pointer;
      transition: all 0.3s ease;
      color: var(--text-primary);
      font-size: 14px;
    }

    .theme-toggle-btn:hover {
      background: var(--bg-secondary);
      border-color: var(--primary);
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .expand-icon {
      font-size: 12px;
      transition: transform 0.3s ease;
    }

    .expand-icon.rotated {
      transform: rotate(180deg);
    }

    .theme-name {
      font-weight: 500;
    }

    /* Theme Panel */
    .theme-panel {
      position: absolute;
      top: 100%;
      right: 0;
      background: var(--bg-primary);
      border: 1px solid var(--border-color);
      border-radius: 12px;
      box-shadow: var(--shadow-lg);
      z-index: 1000;
      min-width: 320px;
      max-width: 400px;
      margin-top: 8px;
      overflow: hidden;
    }

    .theme-panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 20px;
      border-bottom: 1px solid var(--border-color);
      background: var(--bg-secondary);
    }

    .panel-title {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary);
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .close-btn {
      background: none;
      border: none;
      color: var(--text-muted);
      cursor: pointer;
      padding: 4px;
      border-radius: 4px;
      transition: all 0.2s ease;
    }

    .close-btn:hover {
      background: var(--bg-tertiary);
      color: var(--text-primary);
    }

    /* Theme Options */
    .theme-options {
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-height: 300px;
      overflow-y: auto;
    }

    .theme-option {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s ease;
      border: 2px solid transparent;
    }

    .theme-option:hover {
      background: var(--bg-secondary);
    }

    .theme-option.active {
      background: rgba(59, 130, 246, 0.1);
      border-color: var(--primary);
    }

    .theme-preview {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;
      flex-shrink: 0;
    }

    .preview-colors {
      display: flex;
      gap: 2px;
    }

    .color-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      border: 1px solid rgba(0, 0, 0, 0.1);
    }

    .theme-icon {
      font-size: 16px;
      color: var(--text-secondary);
    }

    .theme-info {
      flex: 1;
      min-width: 0;
    }

    .theme-option .theme-name {
      font-size: 14px;
      font-weight: 500;
      color: var(--text-primary);
      margin-bottom: 2px;
    }

    .theme-description {
      font-size: 12px;
      color: var(--text-secondary);
      line-height: 1.4;
    }

    .theme-status {
      flex-shrink: 0;
    }

    .active-indicator {
      color: var(--primary);
      font-size: 16px;
    }

    /* Theme Features */
    .theme-features {
      padding: 16px;
      border-top: 1px solid var(--border-color);
      background: var(--bg-secondary);
    }

    .feature-item {
      margin-bottom: 12px;
    }

    .feature-item:last-child {
      margin-bottom: 0;
    }

    .feature-toggle {
      display: flex;
      align-items: center;
      gap: 12px;
      cursor: pointer;
      font-size: 14px;
    }

    .feature-toggle input[type="checkbox"] {
      display: none;
    }

    .toggle-slider {
      position: relative;
      width: 40px;
      height: 20px;
      background: var(--border-color);
      border-radius: 20px;
      transition: all 0.3s ease;
    }

    .toggle-slider::before {
      content: '';
      position: absolute;
      top: 2px;
      left: 2px;
      width: 16px;
      height: 16px;
      background: white;
      border-radius: 50%;
      transition: all 0.3s ease;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    }

    .feature-toggle input:checked + .toggle-slider {
      background: var(--primary);
    }

    .feature-toggle input:checked + .toggle-slider::before {
      transform: translateX(20px);
    }

    .feature-label {
      display: flex;
      align-items: center;
      gap: 8px;
      color: var(--text-primary);
      font-weight: 500;
    }

    /* Quick Actions */
    .quick-actions {
      padding: 16px;
      border-top: 1px solid var(--border-color);
      display: flex;
      gap: 8px;
    }

    .action-btn {
      flex: 1;
      padding: 8px 12px;
      background: var(--bg-secondary);
      border: 1px solid var(--border-color);
      border-radius: 6px;
      color: var(--text-primary);
      font-size: 12px;
      cursor: pointer;
      transition: all 0.2s ease;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
    }

    .action-btn:hover {
      background: var(--bg-tertiary);
      border-color: var(--primary);
    }

    /* Backdrop */
    .theme-backdrop {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      z-index: 999;
    }

    /* Animations */
    .slide-in-up {
      animation: slideInUp 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }

    @keyframes slideInUp {
      from {
        opacity: 0;
        transform: translateY(10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    /* Mobile responsiveness */
    @media (max-width: 768px) {
      .theme-panel {
        right: -20px;
        left: -20px;
        min-width: auto;
        max-width: none;
        width: calc(100vw - 40px);
      }

      .theme-options {
        max-height: 250px;
      }

      .quick-actions {
        flex-direction: column;
      }
    }
  `]
})
export class ThemeSelectorComponent implements OnInit, OnDestroy {
  currentTheme: Theme = 'system';
  currentThemeConfig?: ThemeConfig;
  availableThemes: ThemeConfig[] = [];
  isExpanded = false;
  showLabel = true;
  
  // Feature toggles
  animationsEnabled = true;
  highContrastMode = false;
  reducedMotion = false;

  private subscription?: Subscription;

  constructor(private themeService: ThemeService) {}

  ngOnInit(): void {
    this.availableThemes = this.themeService.getAvailableThemes();
    this.currentTheme = this.themeService.getCurrentTheme();
    this.currentThemeConfig = this.themeService.getCurrentThemeConfig();

    this.subscription = this.themeService.theme$.subscribe(theme => {
      this.currentTheme = theme;
      this.currentThemeConfig = this.themeService.getCurrentThemeConfig();
    });

    // Load feature preferences
    this.loadFeaturePreferences();
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  toggleExpanded(): void {
    this.isExpanded = !this.isExpanded;
  }

  closePanel(): void {
    this.isExpanded = false;
  }

  selectTheme(themeId: Theme): void {
    this.themeService.setTheme(themeId);
    this.closePanel();
  }

  toggleAnimations(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.animationsEnabled = target.checked;
    
    if (this.animationsEnabled) {
      this.themeService.enableThemeTransition();
    } else {
      this.themeService.disableThemeTransition();
    }
    
    localStorage.setItem('animations-enabled', this.animationsEnabled.toString());
  }

  toggleHighContrast(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.highContrastMode = target.checked;
    
    const root = document.documentElement;
    if (this.highContrastMode) {
      root.classList.add('high-contrast');
    } else {
      root.classList.remove('high-contrast');
    }
    
    localStorage.setItem('high-contrast', this.highContrastMode.toString());
  }

  toggleReducedMotion(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.reducedMotion = target.checked;
    
    const root = document.documentElement;
    if (this.reducedMotion) {
      root.classList.add('reduced-motion');
    } else {
      root.classList.remove('reduced-motion');
    }
    
    localStorage.setItem('reduced-motion', this.reducedMotion.toString());
  }

  resetToDefault(): void {
    this.themeService.setTheme('system');
    this.animationsEnabled = true;
    this.highContrastMode = false;
    this.reducedMotion = false;
    
    // Clear localStorage
    localStorage.removeItem('animations-enabled');
    localStorage.removeItem('high-contrast');
    localStorage.removeItem('reduced-motion');
    
    // Apply defaults
    this.themeService.enableThemeTransition();
    document.documentElement.classList.remove('high-contrast', 'reduced-motion');
    
    this.closePanel();
  }

  exportThemeSettings(): void {
    const settings = {
      theme: this.currentTheme,
      animationsEnabled: this.animationsEnabled,
      highContrastMode: this.highContrastMode,
      reducedMotion: this.reducedMotion,
      exportDate: new Date().toISOString()
    };

    const blob = new Blob([JSON.stringify(settings, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'fraudshield-theme-settings.json';
    link.click();
    URL.revokeObjectURL(url);
  }

  private loadFeaturePreferences(): void {
    // Load animations preference
    const animationsPref = localStorage.getItem('animations-enabled');
    if (animationsPref !== null) {
      this.animationsEnabled = animationsPref === 'true';
      if (!this.animationsEnabled) {
        this.themeService.disableThemeTransition();
      }
    }

    // Load high contrast preference
    const contrastPref = localStorage.getItem('high-contrast');
    if (contrastPref !== null) {
      this.highContrastMode = contrastPref === 'true';
      if (this.highContrastMode) {
        document.documentElement.classList.add('high-contrast');
      }
    }

    // Load reduced motion preference
    const motionPref = localStorage.getItem('reduced-motion');
    if (motionPref !== null) {
      this.reducedMotion = motionPref === 'true';
      if (this.reducedMotion) {
        document.documentElement.classList.add('reduced-motion');
      }
    }
  }
}
