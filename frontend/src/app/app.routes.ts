import {Routes} from '@angular/router';
import {LoginComponent} from "./components/login/login.component";
import {RegisterComponent} from "./components/register/register.component";
import {ChatComponent} from "./components/chat/chat.component";
import {LandingComponent} from "./components/landing/landing.component";

export const routes: Routes = [
  {path: 'welcome', component: LandingComponent},
  {path: 'register', component: RegisterComponent},
  {path: 'login', component: LoginComponent},
  {path: 'chat', component: ChatComponent},
  {path: '', redirectTo: '/welcome', pathMatch: 'full'}
];
