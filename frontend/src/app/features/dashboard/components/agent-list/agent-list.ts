import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { AgentDto } from '../../../../core/models/cti.models';

@Component({
  selector: 'app-agent-list',
  imports: [DatePipe],
  templateUrl: './agent-list.html',
  styleUrl: './agent-list.scss'
})
export class AgentList {
  readonly agents = input<AgentDto[]>([]);

  protected statusClass(status: string): string {
    return status.toLowerCase().replaceAll('_', '-');
  }
}
