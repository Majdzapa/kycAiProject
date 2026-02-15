import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  KycDocument,
  KycSubmissionResponse,
  KycStatus,
  DocumentType,
  LegalBasis
} from '../models/kyc.model';
import { KycStateService } from './kyc-state.service';

@Injectable({
  providedIn: 'root'
})
export class KycService {
  private http = inject(HttpClient);
  private kycState = inject(KycStateService);
  private apiUrl = `${environment.apiUrl}/kyc`;

  submitDocument(
    customerId: string,
    file: File,
    docType: DocumentType,
    legalBasis: LegalBasis = LegalBasis.LEGAL_OBLIGATION
  ): Observable<KycSubmissionResponse> {
    const formData = new FormData();
    formData.append('document', file);
    formData.append('docType', docType);
    formData.append('legalBasis', legalBasis);

    const headers = new HttpHeaders({
      'X-Customer-Id': customerId,
      'X-Consent-Token': 'consent-granted'
    });

    this.kycState.setSubmitting(true);
    return this.http.post<KycSubmissionResponse>(`${this.apiUrl}/submit`, formData, { headers }).pipe(
      tap({
        next: (response) => {
          this.kycState.submitKycSuccess(response as any);
        },
        error: (error) => {
          this.kycState.submitKycFailure(error.error?.message || 'Submission failed');
        }
      })
    );
  }

  getKycStatus(customerId: string): Observable<KycStatus> {
    this.kycState.setLoading(true);
    return this.http.get<KycStatus>(`${this.apiUrl}/status/${customerId}`).pipe(
      tap({
        next: (status) => {
          this.kycState.loadSubmissionSuccess(status as any);
        },
        error: (error) => {
          this.kycState.loadSubmissionFailure(error.error?.message || 'Failed to load status');
        }
      })
    );
  }

  getCustomerDocuments(customerId: string): Observable<KycDocument[]> {
    return this.http.get<KycDocument[]>(`${this.apiUrl}/documents/${customerId}`);
  }

  ingestKnowledgeDocument(
    file: File,
    category: string,
    title: string,
    version: string
  ): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('category', category);
    formData.append('title', title);
    formData.append('version', version);

    return this.http.post(`${this.apiUrl}/ingest-knowledge`, formData);
  }

  searchKnowledge(query: string, maxResults: number = 5): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/knowledge/search`, {
      params: { query, maxResults: maxResults.toString() }
    });
  }

  assessRisk(customerId: string, riskData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/risk-assessment/${customerId}`, riskData);
  }
}
