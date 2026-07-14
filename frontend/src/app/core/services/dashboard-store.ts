import { DestroyRef, NgZone, computed, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, forkJoin, of, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AgentDto, CallDto, CtiSnapshotDto, ExtensionDto } from '../models/cti.models';
import { CtiApi } from './cti-api';

@Injectable({ providedIn: 'root' })
export class DashboardStore {
  private readonly api = inject(CtiApi);
  private readonly zone = inject(NgZone);
  private readonly baseUrl = environment.apiBaseUrl;
  private eventSource?: EventSource;
  private started = false;

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly backendUp = signal(false);
  readonly ctiConnected = signal(false);
  readonly lastHeartbeatAt = signal<string | null>(null);
  readonly calls = signal<CallDto[]>([]);
  readonly agents = signal<AgentDto[]>([]);
  readonly extensions = signal<ExtensionDto[]>([]);
  readonly lastUpdatedAt = signal<string | null>(null);

  readonly activeCallCount = computed(() => this.calls().length);
  readonly busyAgentCount = computed(() => this.agents().filter((agent) => agent.status !== 'AVAILABLE').length);
  readonly busyExtensionCount = computed(() => this.extensions().filter((extension) => extension.status !== 'IDLE').length);

  start(destroyRef: DestroyRef): void {
    if (this.started) {
      return;
    }

    this.started = true;
    destroyRef.onDestroy(() => this.stop());
    this.loadInitialData(destroyRef);
    this.openRealtimeStream(destroyRef);
  }

  refresh(destroyRef: DestroyRef): void {
    this.loadInitialData(destroyRef);
  }

  stop(): void {
    this.eventSource?.close();
    this.eventSource = undefined;
    this.started = false;
  }

  private loadInitialData(destroyRef: DestroyRef): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      health: this.api.health(),
      calls: this.api.activeCalls(),
      agents: this.api.agents(),
      extensions: this.api.extensions()
    }).pipe(
      timeout(5000),
      catchError((error) => {
        this.backendUp.set(false);
        this.error.set('No fue posible conectar con el backend. Revisa que Spring Boot esté levantado.');
        console.error('Error cargando dashboard CTI', error);
        return of(null);
      }),
      takeUntilDestroyed(destroyRef)
    ).subscribe((response) => {
      this.loading.set(false);

      if (!response) {
        return;
      }

      this.backendUp.set(response.health.status === 'UP');
      this.calls.set(response.calls);
      this.agents.set(response.agents);
      this.extensions.set(response.extensions);
      this.lastUpdatedAt.set(new Date().toISOString());
    });
  }

  private openRealtimeStream(destroyRef: DestroyRef): void {
    this.zone.runOutsideAngular(() => {
      const source = new EventSource(`${this.baseUrl}/cti/events`);
      this.eventSource = source;

      source.addEventListener('cti-snapshot', (event) => {
        const message = event as MessageEvent<string>;
        this.zone.run(() => {
          try {
            this.applySnapshot(JSON.parse(message.data) as CtiSnapshotDto);
          } catch (error) {
            this.error.set('El backend envió un evento en vivo con formato inesperado.');
            console.error('Evento SSE inválido', error);
          }
        });
      });

      source.onerror = () => {
        this.zone.run(() => {
          this.ctiConnected.set(false);
          this.error.set('La conexión en tiempo real está caída o reconectando.');
        });
      };

      destroyRef.onDestroy(() => source.close());
    });
  }

  private applySnapshot(snapshot: CtiSnapshotDto): void {
    this.backendUp.set(true);
    this.ctiConnected.set(snapshot.ctiConnected);
    this.lastHeartbeatAt.set(snapshot.lastHeartbeatAt);
    this.calls.set(snapshot.activeCalls);
    this.agents.set(snapshot.agents);
    this.extensions.set(snapshot.extensions);
    this.lastUpdatedAt.set(snapshot.generatedAt);
    this.loading.set(false);
    this.error.set(null);
  }
}
