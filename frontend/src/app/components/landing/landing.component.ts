import {Component} from '@angular/core';
import {RouterLink} from "@angular/router";
import {NgClass, NgForOf} from "@angular/common";

@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html',
  standalone: true,
  imports: [RouterLink, NgForOf, NgClass],
  styleUrls: ['./landing.component.scss']
})
export class LandingComponent {
  stats = [
    { label: 'Practice freely', value: '24/7' },
    { label: 'AI response time', value: '<2s' },
    { label: 'Exam help refused', value: '100%' },
  ];

  steps = [
    {
      title: 'Submit your work',
      description: 'Type your answer or upload a photo of your handwritten work. EduBot accepts both text and images.'
    },
    {
      title: 'AI analyses it',
      description: 'The AI evaluates your submission against common rubric criteria, identifying strengths and gaps in your understanding.'
    },
    {
      title: 'Receive feedback',
      description: 'Get a detailed breakdown with an estimated score and actionable suggestions to improve before the real assessment.'
    }
  ];
}
