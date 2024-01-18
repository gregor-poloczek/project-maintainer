import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectOverviewListComponent } from './project-overview-list.component';

describe('ProjectOverviewListComponent', () => {
  let component: ProjectOverviewListComponent;
  let fixture: ComponentFixture<ProjectOverviewListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectOverviewListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ProjectOverviewListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
