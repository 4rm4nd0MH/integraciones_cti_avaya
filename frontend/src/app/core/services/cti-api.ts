import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AgentDto, CallDto, CtiSnapshotDto, ExtensionDto, HealthResponse } from '../models/cti.models';

@Injectable({ providedIn: 'root' })
export class CtiApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  health() {
    return this.http.get<HealthResponse>(`${this.baseUrl}/health`);
  }

  activeCalls() {
    return this.http.get<CallDto[]>(`${this.baseUrl}/calls/active`);
  }

  agents() {
    return this.http.get<AgentDto[]>(`${this.baseUrl}/agents`);
  }

  extensions() {
    return this.http.get<ExtensionDto[]>(`${this.baseUrl}/extensions`);
  }

  snapshot() {
    return this.http.get<CtiSnapshotDto>(`${this.baseUrl}/cti/snapshot`);
  }
}
