export type CallStatus = 'RINGING' | 'IN_CALL' | 'ON_HOLD' | 'TRANSFERRING';
export type AgentStatus = 'AVAILABLE' | 'BUSY' | 'ON_HOLD';
export type ExtensionStatus = 'IDLE' | 'RINGING' | 'BUSY' | 'ON_HOLD';

export interface HealthResponse {
  status: 'UP' | string;
}

export interface CallDto {
  callId: string;
  extension: string;
  agentId: string;
  phoneNumber: string;
  status: CallStatus;
  startedAt: string;
  updatedAt: string;
}

export interface AgentDto {
  agentId: string;
  status: AgentStatus;
  updatedAt: string;
}

export interface ExtensionDto {
  extension: string;
  status: ExtensionStatus;
  updatedAt: string;
}

export interface CtiSnapshotDto {
  activeCalls: CallDto[];
  agents: AgentDto[];
  extensions: ExtensionDto[];
  ctiConnected: boolean;
  lastHeartbeatAt: string | null;
  generatedAt: string;
}
