import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {User} from "../interface/user.interface";
import {USER_API} from "../utils/consts";

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = USER_API;

  constructor(private http: HttpClient) {
  }

  getAllProducts(): Observable<User[]> {
    return this.http.get<User[]>(this.apiUrl);
  }

  getProductById(id: number): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/${id}`);
  }

  createProduct(product: User): Observable<User> {
    return this.http.post<User>(this.apiUrl, product);
  }

  updateProduct(id: number, product: User): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/${id}`, product);
  }

  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
