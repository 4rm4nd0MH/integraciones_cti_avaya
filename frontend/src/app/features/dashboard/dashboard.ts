import { Component, DestroyRef, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { DashboardStore } from '../../core/services/dashboard-store';
import { AgentList } from './components/agent-list/agent-list';
import { CallTable } from './components/call-table/call-table';
import { ExtensionList } from './components/extension-list/extension-list';

@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, AgentList, CallTable, ExtensionList],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard {
  protected readonly store = inject(DashboardStore);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.store.start(this.destroyRef);
  }

  protected refresh(): void {
    this.store.refresh(this.destroyRef);
  }
}
