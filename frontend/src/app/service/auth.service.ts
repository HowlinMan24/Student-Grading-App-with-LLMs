import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {BehaviorSubject, Observable, tap} from 'rxjs';
import {User} from "../interface/user.interface";
import {AUTH_API} from "../utils/consts";

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = AUTH_API;
  private isLoggedInSubject = new BehaviorSubject<boolean>(this.hasToken());
  isLoggedIn$ = this.isLoggedInSubject.asObservable();
  private userSubject = new BehaviorSubject<User | null>(null);
  user$ = this.userSubject.asObservable();

  constructor(private http: HttpClient) {
    if (this.hasToken()) {
      this.loadCurrentUser();
    }
  }

  register(user: User): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, {
      name: user.name,
      email: user.email,
      password: user.password,
      role: user.role
    });
  }

  login(email: string, password: string): Observable<string> {
    return this.http.post(`${this.apiUrl}/login`, { email, password }, { responseType: 'text' })
      .pipe(
        tap(token => {
          localStorage.setItem('token', token);
          this.isLoggedInSubject.next(true);
          this.loadCurrentUser();
        })
      );
  }

  logout() {
    localStorage.removeItem('token');
    this.isLoggedInSubject.next(false);
    this.userSubject.next(null);
  }

  private hasToken(): boolean {
    return !!localStorage.getItem('token');
  }

  private loadCurrentUser() {
    const token = localStorage.getItem('token');
    if (token) {
      const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
      this.http.get<User>(`${this.apiUrl}/me`, { headers })
        .subscribe({
          next: (user) => {
            this.userSubject.next(user);
            this.isLoggedInSubject.next(true);
          },
          error: (err) => {
            console.error('Failed to load user:', err.status, err.statusText, err.message);
            console.error('Error details:', err);
            this.logout();
          }
        });
    } else {
      this.isLoggedInSubject.next(false);
      this.userSubject.next(null);
    }
  }
}
