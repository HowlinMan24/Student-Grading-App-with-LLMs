import {Component, OnInit} from '@angular/core';
import {User} from "../../interface/user.interface";
import {FormsModule, NgForm} from "@angular/forms";
import {AuthService} from "../../service/auth.service";
import {Router} from "@angular/router";

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    FormsModule
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent implements OnInit {
  user: User;

  constructor(private authService: AuthService, private router: Router) {
  }

  ngOnInit() {
    this.user = {name: '', email: '', password: '', role: 'STUDENT'};
  }

  register(form: NgForm) {
    if (form.valid) {
      this.authService.register(this.user).subscribe(
        () => {
          this.router.navigateByUrl("/login");
        },
        error => {
          console.error('Registration failed', error);
        }
      );
    }
  }
}
