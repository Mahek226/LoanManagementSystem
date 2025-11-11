import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject } from 'rxjs';

export type Theme = 'light' | 'dark' | 'system' | 'blue' | 'green' | 'purple';

export interface ThemeConfig {
  id: Theme;
  name: string;
  description: string;
  icon: string;
  colors: {
    primary: string;
    secondary: string;
    accent: string;
  };
}

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly THEME_KEY = 'lms-theme';
  private currentTheme = new BehaviorSubject<Theme>('system');
  
  theme$ = this.currentTheme.asObservable();

  private availableThemes: ThemeConfig[] = [
    {
      id: 'light',
      name: 'Light',
      description: 'Clean and bright interface',
      icon: 'fa-sun',
      colors: {
        primary: '#3b82f6',
        secondary: '#64748b',
        accent: '#10b981'
      }
    },
    {
      id: 'dark',
      name: 'Dark',
      description: 'Easy on the eyes',
      icon: 'fa-moon',
      colors: {
        primary: '#60a5fa',
        secondary: '#94a3b8',
        accent: '#34d399'
      }
    },
    {
      id: 'system',
      name: 'System',
      description: 'Follows your system preference',
      icon: 'fa-desktop',
      colors: {
        primary: '#3b82f6',
        secondary: '#64748b',
        accent: '#10b981'
      }
    },
    {
      id: 'blue',
      name: 'Ocean Blue',
      description: 'Professional blue theme',
      icon: 'fa-water',
      colors: {
        primary: '#0ea5e9',
        secondary: '#0284c7',
        accent: '#06b6d4'
      }
    },
    {
      id: 'green',
      name: 'Nature Green',
      description: 'Fresh and natural',
      icon: 'fa-leaf',
      colors: {
        primary: '#059669',
        secondary: '#047857',
        accent: '#10b981'
      }
    },
    {
      id: 'purple',
      name: 'Royal Purple',
      description: 'Elegant and sophisticated',
      icon: 'fa-crown',
      colors: {
        primary: '#7c3aed',
        secondary: '#6d28d9',
        accent: '#8b5cf6'
      }
    }
  ];

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {
    if (isPlatformBrowser(this.platformId)) {
      this.initializeTheme();
      this.watchSystemTheme();
    }
  }

  private initializeTheme(): void {
    const savedTheme = localStorage.getItem(this.THEME_KEY) as Theme;
    const theme = savedTheme || 'system';
    this.setTheme(theme);
  }

  setTheme(theme: Theme): void {
    this.currentTheme.next(theme);
    
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(this.THEME_KEY, theme);
      this.applyTheme(theme);
    }
  }

  private applyTheme(theme: Theme): void {
    const root = document.documentElement;
    
    // Remove existing theme classes
    root.classList.remove('light-theme', 'dark-theme', 'blue-theme', 'green-theme', 'purple-theme');
    
    if (theme === 'system') {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      root.classList.add(prefersDark ? 'dark-theme' : 'light-theme');
    } else {
      root.classList.add(`${theme}-theme`);
    }

    // Apply custom colors for themed variants
    const themeConfig = this.getThemeConfig(theme);
    if (themeConfig && theme !== 'system') {
      root.style.setProperty('--primary', themeConfig.colors.primary);
      root.style.setProperty('--primary-dark', this.darkenColor(themeConfig.colors.primary, 10));
      root.style.setProperty('--secondary', themeConfig.colors.secondary);
      root.style.setProperty('--accent', themeConfig.colors.accent);
    }
  }

  private watchSystemTheme(): void {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    mediaQuery.addEventListener('change', () => {
      if (this.currentTheme.value === 'system') {
        this.applyTheme('system');
      }
    });
  }

  getCurrentTheme(): Theme {
    return this.currentTheme.value;
  }

  toggleTheme(): void {
    const themes: Theme[] = ['light', 'dark', 'system'];
    const currentIndex = themes.indexOf(this.currentTheme.value);
    const nextIndex = (currentIndex + 1) % themes.length;
    this.setTheme(themes[nextIndex]);
  }

  getAvailableThemes(): ThemeConfig[] {
    return this.availableThemes;
  }

  getThemeConfig(themeId: Theme): ThemeConfig | undefined {
    return this.availableThemes.find(theme => theme.id === themeId);
  }

  getCurrentThemeConfig(): ThemeConfig | undefined {
    return this.getThemeConfig(this.currentTheme.value);
  }

  private darkenColor(color: string, percent: number): string {
    // Convert hex to RGB
    const hex = color.replace('#', '');
    const r = parseInt(hex.substr(0, 2), 16);
    const g = parseInt(hex.substr(2, 2), 16);
    const b = parseInt(hex.substr(4, 2), 16);

    // Darken by percentage
    const factor = (100 - percent) / 100;
    const newR = Math.round(r * factor);
    const newG = Math.round(g * factor);
    const newB = Math.round(b * factor);

    // Convert back to hex
    return `#${newR.toString(16).padStart(2, '0')}${newG.toString(16).padStart(2, '0')}${newB.toString(16).padStart(2, '0')}`;
  }

  // Animation utilities
  enableThemeTransition(): void {
    const root = document.documentElement;
    root.style.setProperty('--theme-transition', 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)');
  }

  disableThemeTransition(): void {
    const root = document.documentElement;
    root.style.removeProperty('--theme-transition');
  }

  // Accessibility
  getContrastRatio(color1: string, color2: string): number {
    const getLuminance = (color: string) => {
      const hex = color.replace('#', '');
      const r = parseInt(hex.substr(0, 2), 16) / 255;
      const g = parseInt(hex.substr(2, 2), 16) / 255;
      const b = parseInt(hex.substr(4, 2), 16) / 255;

      const sRGB = [r, g, b].map(c => {
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
      });

      return 0.2126 * sRGB[0] + 0.7152 * sRGB[1] + 0.0722 * sRGB[2];
    };

    const lum1 = getLuminance(color1);
    const lum2 = getLuminance(color2);
    const brightest = Math.max(lum1, lum2);
    const darkest = Math.min(lum1, lum2);

    return (brightest + 0.05) / (darkest + 0.05);
  }

  isHighContrast(): boolean {
    const themeConfig = this.getCurrentThemeConfig();
    if (!themeConfig) return false;

    const bgColor = this.currentTheme.value === 'dark' ? '#0f172a' : '#ffffff';
    const textColor = this.currentTheme.value === 'dark' ? '#f1f5f9' : '#1e293b';

    return this.getContrastRatio(bgColor, textColor) >= 7; // WCAG AAA standard
  }
}
