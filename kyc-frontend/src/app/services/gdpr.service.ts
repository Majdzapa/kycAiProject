import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class GdprService {
  private apiUrl = `${environment.apiUrl}/gdpr`;

  constructor(private http: HttpClient) {}

  exportData(customerId: string, format: 'JSON' | 'XML' = 'JSON'): Observable<any> {
    return this.http.get(`${this.apiUrl}/export-data`, {
      params: { customerId, format },
      responseType: 'blob'
    });
  }

  deleteData(customerId: string, reason?: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/delete-data`, {
      params: { customerId },
      body: { reason, confirmDeletion: true }
    });
  }

  recordConsent(purpose: string, version: string, dataCategories: string[]): Observable<any> {
    return this.http.post(`${this.apiUrl}/consent`, {
      purpose,
      version,
      explicitConsent: true,
      dataCategories
    });
  }

  withdrawConsent(purpose: string, reason?: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/withdraw-consent`, {
      purpose,
      reason
    });
  }

  getPrivacyPolicy(): Observable<any> {
    return this.http.get(`${this.apiUrl}/privacy-policy`);
  }

  getProcessingActivities(): Observable<any> {
    return this.http.get(`${this.apiUrl}/processing-activities`);
  }
}
