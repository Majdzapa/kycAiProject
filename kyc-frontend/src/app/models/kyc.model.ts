export enum DocumentType {
  ID_CARD = 'ID_CARD',
  PASSPORT = 'PASSPORT',
  DRIVERS_LICENSE = 'DRIVERS_LICENSE',
  PROOF_OF_ADDRESS = 'PROOF_OF_ADDRESS',
  UTILITY_BILL = 'UTILITY_BILL',
  BANK_STATEMENT = 'BANK_STATEMENT'
}

export enum VerificationStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  VERIFIED = 'VERIFIED',
  REJECTED = 'REJECTED',
  NEEDS_REVIEW = 'NEEDS_REVIEW',
  EXPIRED = 'EXPIRED'
}

export enum RiskLevel {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL'
}

export enum LegalBasis {
  CONSENT = 'CONSENT',
  LEGAL_OBLIGATION = 'LEGAL_OBLIGATION',
  CONTRACT = 'CONTRACT',
  LEGITIMATE_INTEREST = 'LEGITIMATE_INTEREST',
  VITAL_INTEREST = 'VITAL_INTEREST',
  PUBLIC_TASK = 'PUBLIC_TASK'
}

export interface KycDocument {
  id: string;
  customerId: string;
  documentType: DocumentType;
  verificationStatus: VerificationStatus;
  riskLevel?: RiskLevel;
  confidenceScore?: number;
  createdAt: string;
  processedAt?: string;
  findings?: string[];
}

export interface KycSubmissionRequest {
  document: File;
  docType: DocumentType;
  legalBasis?: LegalBasis;
}

export interface KycSubmissionResponse {
  status: string;
  message: string;
  documentId?: string;
  kycStatus?: KycStatus;
}

export interface KycStatus {
  documentStatus: string;
  riskLevel: string;
  confidenceScore: number;
  overallStatus: string;
  findings?: string[];
}

export interface RiskAssessment {
  customerId: string;
  riskLevel: RiskLevel;
  riskScore: number;
  riskFactors: RiskFactor[];
  recommendedActions: string[];
}

export interface RiskFactor {
  category: string;
  factor: string;
  severity: string;
  weight: number;
}
