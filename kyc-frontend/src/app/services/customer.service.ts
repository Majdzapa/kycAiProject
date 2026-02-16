import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Customer, CustomerFullProfile } from '../models/customer.model';
import { Observable, tap } from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class CustomerService {
    private apiUrl = `${environment.apiUrl}/api/v1/admin/customers`;

    // Signal-based state management
    customers = signal<Customer[]>([]);
    loading = signal<boolean>(false);
    error = signal<string | null>(null);

    constructor(private http: HttpClient) { }

    loadCustomers(): Observable<Customer[]> {
        this.loading.set(true);
        return this.http.get<Customer[]>(this.apiUrl).pipe(
            tap({
                next: (data) => {
                    this.customers.set(data);
                    this.loading.set(false);
                    this.error.set(null);
                },
                error: (err) => {
                    this.error.set('Failed to load customers');
                    this.loading.set(false);
                }
            })
        );
    }

    getCustomerById(id: string): Observable<Customer> {
        return this.http.get<Customer>(`${this.apiUrl}/${id}`);
    }

    getCustomerFullProfile(id: string): Observable<CustomerFullProfile> {
        return this.http.get<CustomerFullProfile>(`${this.apiUrl}/${id}/full-profile`);
    }

    createCustomer(customer: Customer): Observable<Customer> {
        return this.http.post<Customer>(this.apiUrl, customer).pipe(
            tap(() => this.loadCustomers().subscribe())
        );
    }

    updateCustomer(id: string, customer: Customer): Observable<Customer> {
        return this.http.put<Customer>(`${this.apiUrl}/${id}`, customer).pipe(
            tap(() => this.loadCustomers().subscribe())
        );
    }

    deleteCustomer(id: string): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
            tap(() => this.loadCustomers().subscribe())
        );
    }
}
