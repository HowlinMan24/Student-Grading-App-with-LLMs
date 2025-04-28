import {AfterViewChecked, Component, ElementRef, OnDestroy, ViewChild} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {ChatService} from '../../service/chat.service';
import {interval, Subscription} from 'rxjs';
import {MarkdownPipe} from "../../pipe/markdown.pipe";

interface Message {
  role: 'student' | 'teacher';
  content: string;
}

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [FormsModule, CommonModule, MarkdownPipe],
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.scss'],
})
export class ChatComponent implements AfterViewChecked, OnDestroy {
  messages: Message[] = [];
  prompt: string = '';
  loading: boolean = false;
  @ViewChild('chatWindow') chatWindow!: ElementRef<HTMLDivElement>;
  private streamSubscription: Subscription | null = null;

  constructor(private chatService: ChatService) {
  }

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  sendPrompt() {
    if (!this.prompt.trim() || this.loading) return;

    const userMessage: Message = {role: 'student', content: this.prompt};
    this.messages.push(userMessage);
    this.prompt = '';
    this.loading = true;

    this.chatService.sendPrompt(userMessage.content).subscribe({
      next: (response) => {
        this.streamResponse(response);
      },
      error: (err) => {
        console.error('Chat error:', err.message);
        this.messages.push({role: 'teacher', content: 'Sorry, something went wrong.'});
        this.loading = false;
      },
    });
  }

  private streamResponse(fullResponse: string) {
    let currentLength = 0;
    const responseMessage: Message = {role: 'teacher', content: ''};
    this.messages.push(responseMessage);

    this.streamSubscription = interval(50).subscribe(() => {
      if (currentLength < fullResponse.length) {
        responseMessage.content = fullResponse.slice(0, currentLength + 1);
        currentLength++;
        this.scrollToBottom();
      } else {
        this.streamSubscription?.unsubscribe();
        this.loading = false;
      }
    });
  }

  private scrollToBottom() {
    if (this.chatWindow) {
      const element = this.chatWindow.nativeElement;
      element.scrollTop = element.scrollHeight;
    }
  }

  ngOnDestroy() {
    this.streamSubscription?.unsubscribe();
  }
}
