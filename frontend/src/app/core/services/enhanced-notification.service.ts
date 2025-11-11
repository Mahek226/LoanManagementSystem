import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface Notification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  duration?: number;
  persistent?: boolean;
  action?: {
    label: string;
    callback: () => void;
  };
  timestamp: Date;
}

@Injectable({
  providedIn: 'root'
})
export class EnhancedNotificationService {
  private notifications$ = new BehaviorSubject<Notification[]>([]);
  private soundEnabled = true;

  constructor() {
    // Load sound preference from localStorage
    const soundPref = localStorage.getItem('notificationSound');
    this.soundEnabled = soundPref !== 'false';
  }

  getNotifications(): Observable<Notification[]> {
    return this.notifications$.asObservable();
  }

  show(notification: Omit<Notification, 'id' | 'timestamp'>): void {
    const newNotification: Notification = {
      ...notification,
      id: this.generateId(),
      timestamp: new Date(),
      duration: notification.duration || 5000
    };

    const currentNotifications = this.notifications$.value;
    this.notifications$.next([...currentNotifications, newNotification]);

    // Play sound
    this.playNotificationSound(notification.type);

    // Auto-remove notification if not persistent
    if (!notification.persistent) {
      setTimeout(() => {
        this.remove(newNotification.id);
      }, newNotification.duration);
    }
  }

  success(title: string, message: string, options?: Partial<Notification>): void {
    this.show({
      type: 'success',
      title,
      message,
      ...options
    });
  }

  error(title: string, message: string, options?: Partial<Notification>): void {
    this.show({
      type: 'error',
      title,
      message,
      persistent: true, // Errors are persistent by default
      ...options
    });
  }

  warning(title: string, message: string, options?: Partial<Notification>): void {
    this.show({
      type: 'warning',
      title,
      message,
      ...options
    });
  }

  info(title: string, message: string, options?: Partial<Notification>): void {
    this.show({
      type: 'info',
      title,
      message,
      ...options
    });
  }

  remove(id: string): void {
    const currentNotifications = this.notifications$.value;
    this.notifications$.next(currentNotifications.filter(n => n.id !== id));
  }

  clear(): void {
    this.notifications$.next([]);
  }

  toggleSound(): void {
    this.soundEnabled = !this.soundEnabled;
    localStorage.setItem('notificationSound', this.soundEnabled.toString());
  }

  isSoundEnabled(): boolean {
    return this.soundEnabled;
  }

  private generateId(): string {
    return Math.random().toString(36).substr(2, 9);
  }

  private playNotificationSound(type: string): void {
    if (!this.soundEnabled) return;

    try {
      const audio = new Audio();
      
      // Different sounds for different notification types
      switch (type) {
        case 'success':
          // Create a success sound using Web Audio API
          this.createTone(800, 0.1, 'sine');
          setTimeout(() => this.createTone(1000, 0.1, 'sine'), 100);
          break;
        case 'error':
          // Create an error sound
          this.createTone(300, 0.2, 'sawtooth');
          break;
        case 'warning':
          // Create a warning sound
          this.createTone(600, 0.15, 'triangle');
          break;
        case 'info':
          // Create an info sound
          this.createTone(500, 0.1, 'sine');
          break;
      }
    } catch (error) {
      console.warn('Could not play notification sound:', error);
    }
  }

  private createTone(frequency: number, duration: number, type: OscillatorType): void {
    try {
      const audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
      const oscillator = audioContext.createOscillator();
      const gainNode = audioContext.createGain();

      oscillator.connect(gainNode);
      gainNode.connect(audioContext.destination);

      oscillator.frequency.value = frequency;
      oscillator.type = type;

      gainNode.gain.setValueAtTime(0.1, audioContext.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + duration);

      oscillator.start(audioContext.currentTime);
      oscillator.stop(audioContext.currentTime + duration);
    } catch (error) {
      console.warn('Web Audio API not supported:', error);
    }
  }
}
