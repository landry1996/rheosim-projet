export interface Project {
  id: string;
  name: string;
  description: string;
  status: ProjectStatus;
  ownerId: string;
  materialIds: string[];
  createdAt: string;
  updatedAt: string;
}

export type ProjectStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';

export interface CreateProjectRequest {
  name: string;
  description: string;
}
