import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface KnowledgeBaseIngestResponse {
    status: string;
    message: string;
}

@Injectable({
    providedIn: 'root'
})
export class KnowledgeBaseService {
    private http = inject(HttpClient);
    private apiUrl = `${environment.apiUrl}/rag`;

    ingestDocument(file: File, category: string, title: string, version: string): Observable<KnowledgeBaseIngestResponse> {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('category', category);
        formData.append('title', title);
        formData.append('version', version);

        return this.http.post<KnowledgeBaseIngestResponse>(`${this.apiUrl}/ingest`, formData);
    }
}
