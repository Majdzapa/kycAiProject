import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ChatService } from '../../services/chat.service';
import { ChatMessage, ChatResponse } from '../../models/chat.model';

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatChipsModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './chatbot.component.html',
  styleUrls: ['./chatbot.component.css']
})
export class ChatbotComponent implements OnInit {
  @ViewChild('messageContainer') messageContainer!: ElementRef;

  messages: ChatMessage[] = [];
  currentMessage = '';
  isTyping = false;
  sessionId?: string;

  constructor(private chatService: ChatService) { }

  ngOnInit(): void {
    // Add welcome message
    this.messages.push({
      role: 'assistant',
      content: 'Hello! I\'m your KYC support assistant. How can I help you today?',
      timestamp: new Date()
    });
  }

  sendMessage(message?: string): void {
    const text = message || this.currentMessage.trim();
    if (!text || this.isTyping) return;

    // Add user message
    this.messages.push({
      role: 'user',
      content: text,
      timestamp: new Date()
    });

    this.currentMessage = '';
    this.isTyping = true;
    this.scrollToBottom();

    // Call API
    this.chatService.sendMessage(
      text,
      this.sessionId,
      false, // hasActiveApplication
      '',    // currentStatus
      []     // documentsSubmitted
    ).subscribe({
      next: (response: ChatResponse) => {
        this.sessionId = response.sessionId;

        this.messages.push({
          role: 'assistant',
          content: response.response,
          timestamp: new Date(),
          suggestedActions: response.suggestedActions,
          escalationNeeded: response.escalationNeeded
        });

        this.isTyping = false;
        this.scrollToBottom();
      },
      error: (error) => {
        this.messages.push({
          role: 'assistant',
          content: 'I apologize, but I\'m having trouble responding right now. Please try again later.',
          timestamp: new Date()
        });
        this.isTyping = false;
        this.scrollToBottom();
      }
    });
  }

  scrollToBottom(): void {
    setTimeout(() => {
      if (this.messageContainer) {
        this.messageContainer.nativeElement.scrollTop =
          this.messageContainer.nativeElement.scrollHeight;
      }
    }, 100);
  }
}
