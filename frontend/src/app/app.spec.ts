import { computed, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { DashboardStore } from './core/services/dashboard-store';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        {
          provide: DashboardStore,
          useValue: {
            loading: signal(false),
            error: signal(null),
            backendUp: signal(true),
            ctiConnected: signal(true),
            lastHeartbeatAt: signal(null),
            calls: signal([]),
            agents: signal([]),
            extensions: signal([]),
            lastUpdatedAt: signal(null),
            activeCallCount: computed(() => 0),
            busyAgentCount: computed(() => 0),
            busyExtensionCount: computed(() => 0),
            start: () => undefined,
            refresh: () => undefined
          }
        }
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the dashboard title', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Dashboard de llamadas');
  });
});
