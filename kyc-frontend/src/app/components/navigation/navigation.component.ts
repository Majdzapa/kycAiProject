import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { AuthStateService } from '../../services/auth-state.service';
import { AuthService } from '../../services/auth.service';

@Component({
    selector: 'app-navigation',
    standalone: true,
    imports: [
        CommonModule,
        RouterModule,
        MatToolbarModule,
        MatButtonModule,
        MatIconModule,
        MatMenuModule,
        MatSidenavModule,
        MatListModule,
        MatDividerModule
    ],
    templateUrl: './navigation.component.html',
    styleUrls: ['./navigation.component.css']
})
export class NavigationComponent {
    authState = inject(AuthStateService);
    private authService = inject(AuthService);

    sidenavOpened = true;

    toggleSidenav() {
        this.sidenavOpened = !this.sidenavOpened;
    }

    logout() {
        this.authService.logout();
    }
}
