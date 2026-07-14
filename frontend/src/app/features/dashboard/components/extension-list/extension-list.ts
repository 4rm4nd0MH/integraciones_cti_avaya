import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { ExtensionDto } from '../../../../core/models/cti.models';

@Component({
  selector: 'app-extension-list',
  imports: [DatePipe],
  templateUrl: './extension-list.html',
  styleUrl: './extension-list.scss'
})
export class ExtensionList {
  readonly extensions = input<ExtensionDto[]>([]);

  protected statusClass(status: string): string {
    return status.toLowerCase().replaceAll('_', '-');
  }
}
