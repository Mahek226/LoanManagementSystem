import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface OfficerNote {
  noteId: number;
  loanId: number;
  officerId: number;
  officerName: string;
  officerType: string; // LOAN_OFFICER, COMPLIANCE_OFFICER
  noteType: string; // GENERAL, DOCUMENT_REVIEW, FRAUD_CHECK, RISK_ASSESSMENT, DECISION
  content: string;
  isInternal: boolean; // Internal notes not visible to applicant
  createdAt: string;
  updatedAt?: string;
}

@Component({
  selector: 'app-officer-notes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './officer-notes.component.html',
  styleUrl: './officer-notes.component.css'
})
export class OfficerNotesComponent implements OnInit {
  @Input() loanId!: number;
  @Input() officerId!: number;
  @Input() officerName!: string;
  @Input() officerType: string = 'LOAN_OFFICER';
  @Input() canEdit: boolean = true;

  notes: OfficerNote[] = [];
  filteredNotes: OfficerNote[] = [];
  loading = false;
  error = '';
  success = '';

  // New note form
  newNoteContent = '';
  newNoteType = 'GENERAL';
  newNoteIsInternal = true;

  // Filter
  filterType = 'ALL';
  showInternalOnly = false;

  noteTypes = [
    { value: 'GENERAL', label: 'General Note' },
    { value: 'DOCUMENT_REVIEW', label: 'Document Review' },
    { value: 'FRAUD_CHECK', label: 'Fraud Check' },
    { value: 'RISK_ASSESSMENT', label: 'Risk Assessment' },
    { value: 'DECISION', label: 'Decision' }
  ];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    if (this.loanId) {
      this.loadNotes();
    }
  }

  loadNotes(): void {
    this.loading = true;
    this.error = '';

    this.http.get<OfficerNote[]>(`${environment.apiUrl}/loan-officer/loan/${this.loanId}/notes`)
      .subscribe({
        next: (notes) => {
          this.notes = notes.sort((a, b) => 
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
          );
          this.applyFilters();
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading notes:', err);
          this.error = 'Failed to load notes';
          this.loading = false;
        }
      });
  }

  addNote(): void {
    if (!this.newNoteContent.trim()) {
      this.error = 'Please enter note content';
      return;
    }

    const note: Partial<OfficerNote> = {
      loanId: this.loanId,
      officerId: this.officerId,
      officerName: this.officerName,
      officerType: this.officerType,
      noteType: this.newNoteType,
      content: this.newNoteContent.trim(),
      isInternal: this.newNoteIsInternal
    };

    this.loading = true;
    this.error = '';
    this.success = '';

    this.http.post<OfficerNote>(`${environment.apiUrl}/loan-officer/${this.officerId}/notes`, note)
      .subscribe({
        next: (createdNote) => {
          this.notes.unshift(createdNote);
          this.applyFilters();
          this.success = 'Note added successfully';
          this.resetForm();
          this.loading = false;
          setTimeout(() => this.success = '', 3000);
        },
        error: (err) => {
          console.error('Error adding note:', err);
          this.error = err.error?.message || 'Failed to add note';
          this.loading = false;
        }
      });
  }

  deleteNote(noteId: number): void {
    if (!confirm('Are you sure you want to delete this note?')) {
      return;
    }

    this.http.delete(`${environment.apiUrl}/loan-officer/notes/${noteId}`)
      .subscribe({
        next: () => {
          this.notes = this.notes.filter(n => n.noteId !== noteId);
          this.applyFilters();
          this.success = 'Note deleted successfully';
          setTimeout(() => this.success = '', 3000);
        },
        error: (err) => {
          console.error('Error deleting note:', err);
          this.error = 'Failed to delete note';
        }
      });
  }

  applyFilters(): void {
    this.filteredNotes = this.notes.filter(note => {
      const typeMatch = this.filterType === 'ALL' || note.noteType === this.filterType;
      const internalMatch = !this.showInternalOnly || note.isInternal;
      return typeMatch && internalMatch;
    });
  }

  resetForm(): void {
    this.newNoteContent = '';
    this.newNoteType = 'GENERAL';
    this.newNoteIsInternal = true;
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getNoteTypeColor(type: string): string {
    const colors: { [key: string]: string } = {
      'GENERAL': 'secondary',
      'DOCUMENT_REVIEW': 'info',
      'FRAUD_CHECK': 'warning',
      'RISK_ASSESSMENT': 'primary',
      'DECISION': 'success'
    };
    return colors[type] || 'secondary';
  }

  getNoteTypeIcon(type: string): string {
    const icons: { [key: string]: string } = {
      'GENERAL': 'bi-pencil-square',
      'DOCUMENT_REVIEW': 'bi-file-earmark-check',
      'FRAUD_CHECK': 'bi-shield-exclamation',
      'RISK_ASSESSMENT': 'bi-graph-up',
      'DECISION': 'bi-check-circle'
    };
    return icons[type] || 'bi-pencil-square';
  }
}
