import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectOverviewListItemComponent } from './project-overview-list-item.component';

describe('ProjectOverviewListItemComponent', () => {
  let component: ProjectOverviewListItemComponent;
  let fixture: ComponentFixture<ProjectOverviewListItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectOverviewListItemComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ProjectOverviewListItemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
