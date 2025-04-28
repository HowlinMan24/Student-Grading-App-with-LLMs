import {Component} from '@angular/core';
import {RouterLink} from "@angular/router";
import {NgClass, NgForOf} from "@angular/common";

@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html',
  standalone: true,
  imports: [
    RouterLink,
    NgForOf,
    NgClass
  ],
  styleUrls: ['./landing.component.scss']
})
export class LandingComponent {
  heroImage = {
    src: 'https://images.unsplash.com/photo-1524178232363-1fb2b075b655?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80',
    alt: 'Students learning in a classroom',
  };
  features = [
    {
      title: 'Task Evaluation',
      description: 'Get instant feedback on assignments with AI-driven insights.',
      image: {
        src: 'https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?ixlib=rb-4.0.3&auto=format&fit=crop&w=400&q=80',
        alt: 'Task Evaluation Dashboard',
      },
    },
    {
      title: 'Real-Time Collaboration',
      description: 'Work with peers and mentors seamlessly.',
      image: {
        src: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?ixlib=rb-4.0.3&auto=format&fit=crop&w=400&q=80',
        alt: 'Team Collaboration',
      },
    },
    {
      title: 'AI-Powered Assistance',
      description: 'Chat with EduBot for personalized guidance.',
      image: {
        src: 'https://images.unsplash.com/photo-1531746790731-6c087fecd65a?ixlib=rb-4.0.3&auto=format&fit=crop&w=400&q=80',
        alt: 'AI Chat Interface',
      },
    },
  ];
}
