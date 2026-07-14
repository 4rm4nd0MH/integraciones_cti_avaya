import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { CallDto } from '../../../../core/models/cti.models';

@Component({
  selector: 'app-call-table',
  imports: [DatePipe],
  templateUrl: './call-table.html',
  styleUrl: './call-table.scss'
})
export class CallTable {
  readonly calls = input<CallDto[]>([]);

  protected statusClass(status: string): string {
    return status.toLowerCase().replaceAll('_', '-');
  }
}
