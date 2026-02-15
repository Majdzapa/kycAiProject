import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ChatRequest, ChatResponse, Conversation } from '../models/chat.model';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private apiUrl = `${environment.apiUrl}/chat`;

  constructor(private http: HttpClient) {}

  sendMessage(
    message: string, 
    sessionId?: string,
    hasActiveApplication: boolean = false,
    currentStatus: string = '',
    documentsSubmitted: string[] = []
  ): Observable<ChatResponse> {
    const request: ChatRequest = {
      message,
      hasActiveApplication,
      currentStatus,
      documentsSubmitted
    };

    let headers = new HttpHeaders();
    if (sessionId) {
      headers = headers.set('X-Session-Id', sessionId);
    }

    return this.http.post<ChatResponse>(`${this.apiUrl}/message`, request, { headers });
  }

  deleteHistory(sessionId: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/history/${sessionId}`);
  }

  getHistory(sessionId: string): Observable<Conversation> {
    return this.http.get<Conversation>(`${this.apiUrl}/history/${sessionId}`);
  }
}
