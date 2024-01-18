import { API } from './API';
import moment from 'moment/moment';
import OperationState = API.OperationState;

export class ProjectListItem {
  name: string;
  commit: string | null;
  fpqn: string;
  operationProgress: API.ProjectOperationProgress | null = null;
  selected: boolean = false;

  get cloned(): boolean {
    return !!this.project.git.workingCopy;
  }

  get operationLabel(): string | null {
    if (!this.operationProgress || !this.operationInProgress) {
      return null;
    }
    const { message, progress, operation } = this.operationProgress;
    let result = `${operation}`;
    if (message) {
      result += ` (${message})`;
    }
    if (progress != -1) {
      result += ` ${Math.ceil(progress * 100)}%`;
    }
    return result;
  }

  get operationInProgress(): boolean {
    let b =
      this.operationProgress != null &&
      ![OperationState.SUCCEEDED, OperationState.FAILED].includes(
        this.operationProgress.state,
      );
    return b;
  }

  get statusLine(): string {
    if (this.operationInProgress) {
      return this.operationLabel as string;
    }
    if (!this.cloned) {
      return 'Not cloned';
    }
    let latestCommit = this.project.git.workingCopy?.latestCommit;
    if (latestCommit) {
      return `${moment(latestCommit.timestamp).fromNow()} - ${latestCommit.message}`;
    }
    return '??undefined state??';
  }

  constructor(public readonly project: API.ProjectResource) {
    this.name = project.metaData.name;
    this.fpqn = project.fqpn;
    let latestCommit = project.git.workingCopy?.latestCommit;
    if (latestCommit) {
      this.commit = `${moment(latestCommit.timestamp).fromNow()} - ${latestCommit.message}`;
    } else {
      this.commit = null;
    }
  }
}
