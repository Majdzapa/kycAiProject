import { Injectable, signal, computed } from '@angular/core';
import { KycDocument, KycStatus } from '../models/kyc.model';

export interface KycState {
    submissions: KycDocument[];
    currentSubmission: KycDocument | null;
    currentKycStatus: KycStatus | null;
    loading: boolean;
    submitting: boolean;
    error: string | null;
}

@Injectable({
    providedIn: 'root'
})
export class KycStateService {
    private _kycState = signal<KycState>({
        submissions: [],
        currentSubmission: null,
        currentKycStatus: null,
        loading: false,
        submitting: false,
        error: null
    });

    readonly submissions = computed(() => this._kycState().submissions);
    readonly currentSubmission = computed(() => this._kycState().currentSubmission);
    readonly currentKycStatus = computed(() => this._kycState().currentKycStatus);
    readonly loading = computed(() => this._kycState().loading);
    readonly submitting = computed(() => this._kycState().submitting);
    readonly error = computed(() => this._kycState().error);

    // Computed values
    readonly hasSubmissions = computed(() => this._kycState().submissions.length > 0);
    readonly overallStatus = computed(() => this._kycState().currentKycStatus?.overallStatus || 'INCOMPLETE');

    constructor() { }

    // Actions
    setLoading(loading: boolean): void {
        this._kycState.update(state => ({ ...state, loading }));
    }

    setSubmitting(submitting: boolean): void {
        this._kycState.update(state => ({ ...state, submitting }));
    }

    setError(error: string | null): void {
        this._kycState.update(state => ({ ...state, error }));
    }

    setSubmissions(submissions: KycDocument[]): void {
        this._kycState.update(state => ({
            ...state,
            submissions,
            loading: false,
            error: null
        }));
    }

    setCurrentSubmission(submission: KycDocument | null): void {
        this._kycState.update(state => ({
            ...state,
            currentSubmission: submission,
            loading: false,
            error: null
        }));
    }

    setCurrentKycStatus(status: KycStatus | null): void {
        this._kycState.update(state => ({
            ...state,
            currentKycStatus: status,
            loading: false,
            error: null
        }));
    }

    submitKycSuccess(response: any): void {
        this._kycState.update(state => ({
            ...state,
            currentKycStatus: response.kycStatus,
            submitting: false,
            error: null
        }));
    }

    updateSubmission(submission: KycDocument): void {
        this._kycState.update(state => ({
            ...state,
            submissions: state.submissions.map(s =>
                s.id === submission.id ? submission : s
            ),
            currentSubmission: state.currentSubmission?.id === submission.id
                ? submission
                : state.currentSubmission,
            loading: false,
            error: null
        }));
    }

    submitKycFailure(error: string): void {
        this._kycState.update(state => ({
            ...state,
            submitting: false,
            error
        }));
    }

    loadSubmissionsSuccess(submissions: KycDocument[]): void {
        this.setSubmissions(submissions);
    }

    loadSubmissionsFailure(error: string): void {
        this._kycState.update(state => ({
            ...state,
            loading: false,
            error
        }));
    }

    loadSubmissionSuccess(status: KycStatus): void {
        this.setCurrentKycStatus(status);
    }

    loadSubmissionFailure(error: string): void {
        this._kycState.update(state => ({
            ...state,
            loading: false,
            error
        }));
    }

    clearError(): void {
        this.setError(null);
    }

    clearCurrentSubmission(): void {
        this.setCurrentSubmission(null);
    }

    reset(): void {
        this._kycState.set({
            submissions: [],
            currentSubmission: null,
            currentKycStatus: null,
            loading: false,
            submitting: false,
            error: null
        });
    }
}
