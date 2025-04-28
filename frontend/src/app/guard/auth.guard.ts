import {CanActivate, Router} from "@angular/router";
import {Injectable} from "@angular/core";
import {AuthService} from "../service/auth.service";
import {map, Observable, take} from "rxjs";

@Injectable({
  providedIn: 'root',
})
export class AuthGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {
  }

  canActivate(): Observable<boolean> {
    return this.authService.isLoggedIn$.pipe(
      take(1),
      map(isLoggedIn => {
        if (!isLoggedIn) {
          return true;
        }
        this.router.navigateByUrl('/chat');
        return false;
      })
    );
  }
}
