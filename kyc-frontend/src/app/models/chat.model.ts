export interface ChatMessage {
  id?: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp?: Date;
  suggestedActions?: string[];
  escalationNeeded?: boolean;
}

export interface ChatRequest {
  message: string;
  hasActiveApplication: boolean;
  currentStatus: string;
  documentsSubmitted: string[];
}

export interface ChatResponse {
  sessionId: string;
  response: string;
  suggestedActions: string[];
  escalationNeeded: boolean;
  escalationReason?: string;
  disclaimer?: string;
}

export interface Conversation {
  sessionId: string;
  messages: ChatMessage[];
  createdAt: Date;
}
