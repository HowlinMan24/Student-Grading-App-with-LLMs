import {Component} from '@angular/core';
import {FormsModule, NgForm} from "@angular/forms";
import {AuthService} from "../../service/auth.service";
import {Router} from "@angular/router";

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    FormsModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  email: string = '';
  password: string = '';

  constructor(private authService: AuthService, private router: Router) {
  }

  login(form: NgForm) {
    if (form.valid) {
      this.authService.login(this.email, this.password).subscribe(
        token => {
          localStorage.setItem('token', token);
          this.router.navigateByUrl('/chat');
        },
        error => {
          console.error('Login failed', error);
        }
      );
    }
  }
}
