export namespace API {
  export type FQPN = string;

  export enum OperationState {
    SCHEDULED = 'SCHEDULED',
    STARTED = 'STARTED',
    RUNNING = 'RUNNING',
    SUCCEEDED = 'SUCCEEDED',
    FAILED = 'FAILED',
  }

  export interface ProjectOperationProgress {
    type: string;
    fqpn: FQPN;
    operation: string;
    message: string | null;
    state: OperationState;
    progress: number;
  }

  export interface CompletedProjectOperationProgress
    extends ProjectOperationProgress {
    state: OperationState.SUCCEEDED | OperationState.FAILED;
    project: ProjectResource;
  }

  export interface ProjectResource {
    fqpn: FQPN;
    metaData: {
      owner: string;
      name: string;
      labels: string[];
    };
    git: {
      uri: string;
      provider: 'UNKNOWN' | 'GITHUB' | 'AWS_CODECOMMIT';
      workingCopy?: {
        latestCommit: {
          hash: string;
          timestamp: string;
          message: string;
        };
      };
    };
  }
}
