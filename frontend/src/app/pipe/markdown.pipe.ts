import {Pipe, PipeTransform} from '@angular/core';
import {marked} from 'marked';
import DOMPurify from 'dompurify';

@Pipe({
  name: 'markdown',
  standalone: true,
})
export class MarkdownPipe implements PipeTransform {
  transform(value: string): string {
    if (!value) return '';
    const markdown = marked(value, {breaks: true, async: false}) as string;
    return DOMPurify.sanitize(markdown);
  }
}
