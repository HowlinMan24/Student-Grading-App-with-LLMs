import { AfterViewChecked, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ChatService } from '../../service/chat.service';
import { AuthService } from '../../service/auth.service';
import { Router } from '@angular/router';
import { interval, Subscription } from 'rxjs';
import { MarkdownPipe } from '../../pipe/markdown.pipe';

interface Message {
  role: 'student' | 'teacher';
  content: string;
  files?: { name: string }[];
}

interface Suggestion {
  label: string;
  prompt: string;
}

const STUDENT_SUGGESTIONS: Suggestion[] = [
  { label: 'Grade my answers', prompt: 'Please grade my answers and give detailed feedback on each problem.' },
  { label: 'Explain my mistakes', prompt: 'What did I do wrong? Please explain each mistake clearly.' },
  { label: 'Estimate my score', prompt: 'Estimate my total score based on the rubric shown in the test.' },
  { label: 'Ask a study question', prompt: 'Can you explain how to find the vertex of a parabola?' },
];

const TEACHER_SUGGESTIONS: Suggestion[] = [
  { label: "Grade this student's work", prompt: "Please grade this student's answers against the rubric and give detailed feedback on each problem." },
  { label: 'Summarize common mistakes', prompt: "Summarize the mistakes this student made and note any patterns worth addressing in class." },
  { label: 'Compute final score', prompt: "Compute this student's total score based on the rubric shown in the test." },
  { label: 'Ask a teaching question', prompt: "What's a good way to explain quadratic inequalities to students who are struggling?" },
];

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [FormsModule, CommonModule, MarkdownPipe],
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.scss'],
})
export class ChatComponent implements AfterViewChecked, OnDestroy, OnInit {
  messages: Message[] = [];
  prompt: string = '';
  loading: boolean = false;
  streaming: boolean = false;
  selectedFiles: File[] = [];
  userName = '';
  userRole = '';

  @ViewChild('chatWindow') chatWindow!: ElementRef<HTMLDivElement>;
  private streamSubscription: Subscription | null = null;
  private userScrolledUp = false;
  private currentFullResponse = '';
  private currentStreamMessage: Message | null = null;

  constructor(private chatService: ChatService, private authService: AuthService, private router: Router) {}

  ngOnInit() {
    this.authService.user$.subscribe(user => {
      this.userName = user ? user.name : '';
      this.userRole = user ? (user.role || '') : '';
    });
  }

  get isTeacher(): boolean {
    return this.userRole === 'TEACHER';
  }

  get suggestions(): Suggestion[] {
    return this.isTeacher ? TEACHER_SUGGESTIONS : STUDENT_SUGGESTIONS;
  }

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  onScroll() {
    const el = this.chatWindow?.nativeElement;
    if (!el) return;
    const distanceFromBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
    this.userScrolledUp = distanceFromBottom > 4;
  }

  onFilesSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;
    this.selectedFiles = [...this.selectedFiles, ...Array.from(input.files)];
    input.value = '';
  }

  removeFile(index: number) {
    this.selectedFiles = this.selectedFiles.filter((_, i) => i !== index);
  }

  logout() {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }

  clearChat() {
    this.streamSubscription?.unsubscribe();
    this.messages = [];
    this.selectedFiles = [];
    this.prompt = '';
    this.loading = false;
    this.streaming = false;
    this.userScrolledUp = false;
    this.currentStreamMessage = null;
  }

  onKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      if (this.streaming) {
        this.stopStreaming();
      } else {
        this.sendPrompt();
      }
    }
  }

  sendPrompt() {
    if (!this.prompt.trim() || this.loading) return;

    const userMessage: Message = {
      role: 'student',
      content: this.prompt,
      files: this.selectedFiles.map(f => ({ name: f.name }))
    };
    this.messages.push(userMessage);
    this.userScrolledUp = false;

    const files = this.selectedFiles;
    this.selectedFiles = [];
    this.prompt = '';
    this.loading = true;

    this.chatService.sendPrompt(userMessage.content, files).subscribe({
      next: (response) => {
        this.streamResponse(response);
      },
      error: (err) => {
        console.error('Chat error:', err.message);
        this.messages.push({ role: 'teacher', content: 'Sorry, something went wrong. Please try again.' });
        this.loading = false;
      },
    });
  }

  private streamResponse(fullResponse: string) {
    let currentLength = 0;
    const CHUNK_SIZE = 5;   // reveal several characters per tick...
    const TICK_MS = 25;     // ...at a lower tick rate, so change detection (and the
                             // markdown re-parse + forced auto-scroll it triggers)
                             // doesn't run so often that it fights the user's own scrolling.
    const responseMessage: Message = { role: 'teacher', content: '' };
    this.messages.push(responseMessage);
    this.currentFullResponse = fullResponse;
    this.currentStreamMessage = responseMessage;
    this.streaming = true;

    this.streamSubscription = interval(TICK_MS).subscribe(() => {
      if (currentLength < fullResponse.length) {
        currentLength = Math.min(currentLength + CHUNK_SIZE, fullResponse.length);
        responseMessage.content = fullResponse.slice(0, currentLength);
      } else {
        this.finishStreaming();
      }
    });
  }

  stopStreaming() {
    if (this.currentStreamMessage) {
      this.currentStreamMessage.content = this.currentFullResponse;
    }
    this.finishStreaming();
  }

  private finishStreaming() {
    this.streamSubscription?.unsubscribe();
    this.streamSubscription = null;
    this.loading = false;
    this.streaming = false;
    this.currentStreamMessage = null;
  }

  private scrollToBottom() {
    if (this.chatWindow && !this.userScrolledUp) {
      const el = this.chatWindow.nativeElement;
      el.scrollTop = el.scrollHeight;
    }
  }

  ngOnDestroy() {
    this.streamSubscription?.unsubscribe();
  }
}
