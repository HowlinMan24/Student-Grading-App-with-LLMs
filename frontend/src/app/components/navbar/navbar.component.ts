import {Component, OnInit} from '@angular/core';
import {AuthService} from "../../service/auth.service";
import {Router, RouterLink} from "@angular/router";
import {AsyncPipe, NgIf} from "@angular/common";

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    AsyncPipe,
    RouterLink,
    NgIf
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent implements OnInit {
  userName = '';

  constructor(public authService: AuthService, private router: Router) {
  }

  ngOnInit() {
    this.authService.user$.subscribe(user => {
      this.userName = user ? user.name : '';
    });
  }

  logout() {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}
