import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface LoadingState {
  key: string;
  message?: string;
  progress?: number;
  type: 'spinner' | 'progress' | 'skeleton';
}

@Injectable({
  providedIn: 'root'
})
export class LoadingService {
  private loadingStates = new Map<string, LoadingState>();
  private loadingStates$ = new BehaviorSubject<LoadingState[]>([]);

  constructor() {}

  getLoadingStates(): Observable<LoadingState[]> {
    return this.loadingStates$.asObservable();
  }

  isLoading(key?: string): Observable<boolean> {
    return new Observable(observer => {
      this.loadingStates$.subscribe(states => {
        if (key) {
          observer.next(states.some(state => state.key === key));
        } else {
          observer.next(states.length > 0);
        }
      });
    });
  }

  startLoading(key: string, message?: string, type: 'spinner' | 'progress' | 'skeleton' = 'spinner'): void {
    const loadingState: LoadingState = {
      key,
      message,
      type,
      progress: type === 'progress' ? 0 : undefined
    };

    this.loadingStates.set(key, loadingState);
    this.updateLoadingStates();
  }

  updateProgress(key: string, progress: number, message?: string): void {
    const existingState = this.loadingStates.get(key);
    if (existingState) {
      existingState.progress = Math.max(0, Math.min(100, progress));
      if (message) {
        existingState.message = message;
      }
      this.updateLoadingStates();
    }
  }

  stopLoading(key: string): void {
    this.loadingStates.delete(key);
    this.updateLoadingStates();
  }

  stopAllLoading(): void {
    this.loadingStates.clear();
    this.updateLoadingStates();
  }

  // Convenience methods for common loading scenarios
  startPageLoading(message: string = 'Loading page...'): void {
    this.startLoading('page', message, 'skeleton');
  }

  stopPageLoading(): void {
    this.stopLoading('page');
  }

  startFormSubmission(message: string = 'Submitting...'): void {
    this.startLoading('form-submit', message, 'spinner');
  }

  stopFormSubmission(): void {
    this.stopLoading('form-submit');
  }

  startDataFetch(key: string, message: string = 'Loading data...'): void {
    this.startLoading(`data-${key}`, message, 'spinner');
  }

  stopDataFetch(key: string): void {
    this.stopLoading(`data-${key}`);
  }

  startFileUpload(message: string = 'Uploading file...'): void {
    this.startLoading('file-upload', message, 'progress');
  }

  updateFileUploadProgress(progress: number, message?: string): void {
    this.updateProgress('file-upload', progress, message);
  }

  stopFileUpload(): void {
    this.stopLoading('file-upload');
  }

  private updateLoadingStates(): void {
    this.loadingStates$.next(Array.from(this.loadingStates.values()));
  }
}
