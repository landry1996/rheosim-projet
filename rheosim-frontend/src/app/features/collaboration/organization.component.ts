import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface Organization {
  id: string;
  name: string;
  slug: string;
  ownerUserId: string;
  createdAt: string;
}

interface Member {
  id: string;
  userId: string;
  role: string;
  joinedAt: string;
}

@Component({
  selector: 'app-organization',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="org-container">
      <div class="org-header">
        <h2>Organizations</h2>
        <button class="btn-primary" (click)="showCreateForm.set(!showCreateForm())">
          + New Organization
        </button>
      </div>

      @if (showCreateForm()) {
        <div class="create-form">
          <input [(ngModel)]="newOrgName" placeholder="Organization name" />
          <input [(ngModel)]="newOrgSlug" placeholder="slug (url-friendly)" />
          <button class="btn-primary" (click)="createOrganization()">Create</button>
          <button class="btn-secondary" (click)="showCreateForm.set(false)">Cancel</button>
        </div>
      }

      <div class="org-list">
        @for (org of organizations(); track org.id) {
          <div class="org-card" [class.selected]="selectedOrg()?.id === org.id" (click)="selectOrg(org)">
            <h3>{{ org.name }}</h3>
            <span class="slug">/{{ org.slug }}</span>
          </div>
        }
      </div>

      @if (selectedOrg()) {
        <div class="members-section">
          <h3>Members of {{ selectedOrg()!.name }}</h3>
          <div class="invite-form">
            <input [(ngModel)]="inviteEmail" placeholder="Email address" />
            <select [(ngModel)]="inviteRole">
              <option value="MEMBER">Member</option>
              <option value="ADMIN">Admin</option>
            </select>
            <button class="btn-primary" (click)="inviteMember()">Invite</button>
          </div>
          <div class="members-list">
            @for (member of members(); track member.id) {
              <div class="member-row">
                <span>{{ member.userId }}</span>
                <span class="role-badge">{{ member.role }}</span>
                <span class="date">{{ member.joinedAt | date:'short' }}</span>
              </div>
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .org-container { padding: 24px; }
    .org-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    .create-form { display: flex; gap: 8px; margin-bottom: 16px; padding: 16px; background: #f5f5f5; border-radius: 8px; }
    .create-form input { padding: 8px; border: 1px solid #ddd; border-radius: 4px; }
    .org-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 12px; margin-bottom: 24px; }
    .org-card { padding: 16px; border: 1px solid #e0e0e0; border-radius: 8px; cursor: pointer; transition: all 0.2s; }
    .org-card:hover { border-color: #1976d2; }
    .org-card.selected { border-color: #1976d2; background: #e3f2fd; }
    .org-card h3 { margin: 0 0 4px; }
    .slug { color: #666; font-size: 12px; }
    .members-section { margin-top: 24px; padding: 20px; background: #fafafa; border-radius: 8px; }
    .invite-form { display: flex; gap: 8px; margin: 12px 0; }
    .invite-form input, .invite-form select { padding: 8px; border: 1px solid #ddd; border-radius: 4px; }
    .members-list { margin-top: 12px; }
    .member-row { display: flex; gap: 12px; align-items: center; padding: 8px 0; border-bottom: 1px solid #eee; }
    .role-badge { background: #e3f2fd; color: #1976d2; padding: 2px 8px; border-radius: 12px; font-size: 11px; }
    .date { color: #999; font-size: 12px; }
    .btn-primary { padding: 8px 16px; background: #1976d2; color: white; border: none; border-radius: 4px; cursor: pointer; }
    .btn-secondary { padding: 8px 16px; background: #f5f5f5; border: 1px solid #ddd; border-radius: 4px; cursor: pointer; }
  `]
})
export class OrganizationComponent implements OnInit {
  organizations = signal<Organization[]>([]);
  selectedOrg = signal<Organization | null>(null);
  members = signal<Member[]>([]);
  showCreateForm = signal(false);

  newOrgName = '';
  newOrgSlug = '';
  inviteEmail = '';
  inviteRole = 'MEMBER';

  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadOrganizations();
  }

  loadOrganizations(): void {
    this.http.get<Organization[]>(`${this.apiUrl}/organizations`).subscribe(orgs => {
      this.organizations.set(orgs);
    });
  }

  createOrganization(): void {
    this.http.post<Organization>(`${this.apiUrl}/organizations`, {
      name: this.newOrgName,
      slug: this.newOrgSlug
    }).subscribe(org => {
      this.organizations.update(list => [...list, org]);
      this.showCreateForm.set(false);
      this.newOrgName = '';
      this.newOrgSlug = '';
    });
  }

  selectOrg(org: Organization): void {
    this.selectedOrg.set(org);
    this.loadMembers(org.id);
  }

  loadMembers(orgId: string): void {
    this.http.get<Member[]>(`${this.apiUrl}/organizations/${orgId}/members`).subscribe(members => {
      this.members.set(members);
    });
  }

  inviteMember(): void {
    const org = this.selectedOrg();
    if (!org) return;
    this.http.post(`${this.apiUrl}/organizations/${org.id}/invitations`, {
      email: this.inviteEmail,
      role: this.inviteRole
    }).subscribe(() => {
      this.inviteEmail = '';
      this.loadMembers(org.id);
    });
  }
}
