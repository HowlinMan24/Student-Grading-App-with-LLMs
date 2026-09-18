import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { catchError, map, Observable, tap, throwError } from 'rxjs';
import { CHAT_API } from "../utils/consts";

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private apiUrl = CHAT_API;

  constructor(private http: HttpClient) {}

  sendPrompt(prompt: string, files: File[] = []): Observable<string> {
    const token = localStorage.getItem('token');
    if (!token) {
      console.error('No token found in localStorage');
      return throwError(() => new Error('Authentication token missing'));
    }

    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);

    const formData = new FormData();
    formData.append('prompt', prompt);
    files.forEach(file => formData.append('images', file, file.name));

    console.log(files)
    console.log(formData)

    return this.http.post<{ response: string }>(this.apiUrl, formData, { headers }).pipe(
      tap((res) => console.log('Chat response:', res)),
      catchError((err) => {
        console.error('Chat API error:', err.status, err.message, err.error);
        return throwError(() => new Error(err.error?.message || 'Failed to send prompt'));
      }),
      map((res) => res.response)
    );
  }
}
