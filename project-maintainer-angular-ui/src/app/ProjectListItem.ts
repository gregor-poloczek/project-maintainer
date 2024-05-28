import { API } from './API';

export class ProjectListItem {
  name: string;
  fpqn: string;

  constructor(public readonly project: API.ProjectResource) {
    this.name = project.metaData.name;
    this.fpqn = project.fqpn;
  }
}
