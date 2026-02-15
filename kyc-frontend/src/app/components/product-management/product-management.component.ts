import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ProductService, Product } from '../../services/product.service';

@Component({
    selector: 'app-product-management',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatCardModule,
        MatButtonModule,
        MatInputModule,
        MatSelectModule,
        MatTableModule,
        MatIconModule,
        MatSnackBarModule
    ],
    templateUrl: './product-management.component.html',
    styleUrls: ['./product-management.component.css']
})
export class ProductManagementComponent implements OnInit {
    private fb = inject(FormBuilder);
    private productService = inject(ProductService);
    private snackBar = inject(MatSnackBar);

    products = signal<Product[]>([]);
    displayedColumns = ['name', 'type', 'riskLevel', 'riskScore', 'actions'];

    productForm = this.fb.group({
        name: ['', Validators.required],
        type: ['SAVINGS_ACCOUNT', Validators.required],
        baseRiskLevel: ['LOW', Validators.required],
        riskScore: [null as number | null]
    });

    productTypes = [
        'SAVINGS_ACCOUNT', 'SALARY_ACCOUNT', 'INVESTMENT_ACCOUNT',
        'BROKERAGE', 'CRYPTO_TRADING', 'INTERNATIONAL_WIRE',
        'PREPAID_CARD', 'LOAN', 'CREDIT_CARD'
    ];

    riskLevels = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

    ngOnInit() {
        this.loadProducts();
    }

    loadProducts() {
        this.productService.getAllProducts().subscribe({
            next: (data) => this.products.set(data),
            error: (err) => this.snackBar.open('Error loading products', 'Close', { duration: 3000 })
        });
    }

    onSubmit() {
        if (this.productForm.valid) {
            const newProduct: Product = this.productForm.value as Product;

            this.productService.createProduct(newProduct).subscribe({
                next: (created) => {
                    this.products.update(list => [...list, created]);
                    this.productForm.reset({ type: 'SAVINGS_ACCOUNT', baseRiskLevel: 'LOW' });
                    this.snackBar.open('Product created successfully', 'Close', { duration: 3000 });
                },
                error: (err) => this.snackBar.open('Failed to create product', 'Close', { duration: 3000 })
            });
        }
    }

    deleteProduct(id: string) {
        if (confirm('Are you sure you want to delete this product?')) {
            this.productService.deleteProduct(id).subscribe({
                next: () => {
                    this.products.update(list => list.filter(p => p.id !== id));
                    this.snackBar.open('Product deleted', 'Close', { duration: 3000 });
                },
                error: (err) => this.snackBar.open('Failed to delete product', 'Close', { duration: 3000 })
            });
        }
    }
}
