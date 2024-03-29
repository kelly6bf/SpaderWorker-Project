package site.devtown.spadeworker.domain.project.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import site.devtown.spadeworker.domain.file.service.AmazonS3ImageService;
import site.devtown.spadeworker.domain.project.dto.CreateProjectRequest;
import site.devtown.spadeworker.domain.project.dto.UpdateProjectRequest;
import site.devtown.spadeworker.domain.project.entity.Project;
import site.devtown.spadeworker.domain.project.entity.ProjectLike;
import site.devtown.spadeworker.domain.project.entity.ProjectSubscribe;
import site.devtown.spadeworker.domain.project.repository.ProjectLikeRepository;
import site.devtown.spadeworker.domain.project.repository.ProjectRepository;
import site.devtown.spadeworker.domain.project.repository.ProjectSubscribeRepository;
import site.devtown.spadeworker.domain.user.model.entity.User;
import site.devtown.spadeworker.domain.user.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static site.devtown.spadeworker.domain.file.constant.ImageFileType.PROJECT_THUMBNAIL_IMAGE;
import static site.devtown.spadeworker.global.util.fixture.ProjectFixture.*;
import static site.devtown.spadeworker.global.util.fixture.UserFixture.getUserEntity;

@DisplayName("[비즈니스 로직] - Project(게시판)")
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @InjectMocks
    private ProjectService sut;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectLikeRepository projectLikeRepository;
    @Mock
    private ProjectSubscribeRepository projectSubscribeRepository;
    @Mock
    private AmazonS3ImageService imageService;
    @Mock
    private UserService userService;

    @DisplayName("[성공 테스트] - 프로젝트 전체 조회")
    @Test
    void success_get_all_projects() throws Exception {
        // Given
        List<Project> projects = getProjects(10);

        given(projectRepository.findAll())
                .willReturn(projects);
        given(projectLikeRepository.countAllByProject(any(Project.class)))
                .willReturn(0);
        given(projectSubscribeRepository.countAllByProject(any(Project.class)))
                .willReturn(0);

        // When
        sut.getAllProjects();

        // Then
        then(projectRepository).should().findAll();
    }

    @DisplayName("[성공 테스트] - 프로젝트 단건 조회")
    @Test
    void success_get_project() throws Exception {
        // Given
        Long projectId = 1L;
        Project savedProject = getProject();

        given(projectRepository.findById(projectId))
                .willReturn(Optional.of(savedProject));

        // When
        sut.getProject(projectId);

        // Then
        then(projectRepository).should().findById(projectId);
    }

    @DisplayName("[성공 테스트] - 프로젝트 생성")
    @Test
    void success_create_project() throws Exception {
        // Given
        CreateProjectRequest request = getCreateProjectRequest();
        User user = getUserEntity();
        Project project = getProject(request, user);

        given(imageService.saveThumbnailImage(
                eq(PROJECT_THUMBNAIL_IMAGE),
                any(MultipartFile.class))
        )
                .willReturn("stored-image-uri");
        given(userService.getCurrentAuthorizedUser())
                .willReturn(user);
        given(projectRepository.save(any(Project.class)))
                .willReturn(project);

        // When
        sut.createProject(request);

        // Then
        then(projectRepository).should().save(any(Project.class));
    }

    @DisplayName("[성공 테스트] - 프로젝트 수정")
    @Test
    void success_update_project() throws Exception {
        // Given
        Long projectId = 1L;
        UpdateProjectRequest request = getUpdateProjectRequest();
        Project savedProject = getProject();

        given(projectRepository.findById(projectId))
                .willReturn(Optional.of(savedProject));
        given(userService.getCurrentAuthorizedUser())
                .willReturn(savedProject.getUser());
        given(imageService.updateThumbnailImage(
                eq(PROJECT_THUMBNAIL_IMAGE),
                eq(request.thumbnailImage()),
                eq(savedProject.getThumbnailImageUri())
        ))
                .willReturn(request.thumbnailImage().getOriginalFilename());

        // When
        sut.updateProject(projectId, request);

        // Then
        assertThat(savedProject)
                .hasFieldOrPropertyWithValue("title", request.title())
                .hasFieldOrPropertyWithValue("description", request.description())
                .hasFieldOrPropertyWithValue("thumbnailImageUri", request.thumbnailImage().getOriginalFilename());
    }

    @DisplayName("[성공 테스트] - 프로젝트 좋아요 등록")
    @Test
    void success_register_project_like() throws Exception {
        // Given
        Long projectId = 1L;
        Project savedProject = getProject();
        User user = getUserEntity();
        ProjectLike projectLike = ProjectLike.of(savedProject, user);

        given(projectRepository.findById(projectId))
                .willReturn(Optional.of(savedProject));
        given(userService.getCurrentAuthorizedUser())
                .willReturn(user);
        given(projectLikeRepository.existsByProjectAndUser(
                savedProject,
                user
        ))
                .willReturn(false);
        given(projectLikeRepository.saveAndFlush(any(ProjectLike.class)))
                .willReturn(projectLike);

        // When
        sut.registerProjectLike(projectId);

        // Then
        then(projectLikeRepository).should().saveAndFlush(any(ProjectLike.class));
    }

    @DisplayName("[성공 테스트] - 프로젝트 좋아요 취소")
    @Test
    void success_cancel_project_like() throws Exception {
        // Given
        Long projectId = 1L;
        Project savedProject = getProject();
        User user = getUserEntity();
        ProjectLike savedProjectLike = ProjectLike.of(savedProject, user);

        given(projectRepository.findById(projectId))
                .willReturn(Optional.of(savedProject));
        given(userService.getCurrentAuthorizedUser())
                .willReturn(user);
        given(projectLikeRepository.findByProjectAndUser(savedProject, user))
                .willReturn(Optional.of(savedProjectLike));
        willDoNothing().given(projectLikeRepository).delete(savedProjectLike);

        // When
        sut.cancelProjectLike(projectId);

        // Then
        then(projectLikeRepository).should().delete(savedProjectLike);
    }

    @DisplayName("[성공 테스트] - 프로젝트 구독 등록")
    @Test
    void success_register_project_subscribe() throws Exception {
        // Given
        Long projectId = 1L;
        Project savedProject = getProject();
        User user = getUserEntity();
        ProjectSubscribe projectSubscribe = ProjectSubscribe.of(savedProject, user);

        given(projectRepository.findById(projectId))
                .willReturn(Optional.of(savedProject));
        given(userService.getCurrentAuthorizedUser())
                .willReturn(user);
        given(projectSubscribeRepository.existsByProjectAndSubscriber(
                savedProject,
                user
        ))
                .willReturn(false);
        given(projectSubscribeRepository.saveAndFlush(any(ProjectSubscribe.class)))
                .willReturn(projectSubscribe);

        // When
        sut.registerProjectSubscribe(projectId);

        // Then
        then(projectSubscribeRepository).should().saveAndFlush(any(ProjectSubscribe.class));
    }

    @DisplayName("[성공 테스트] - 프로젝트 구독 취소")
    @Test
    void success_cancel_project_subscribe() throws Exception {
        // Given
        Long projectId = 1L;
        Project savedProject = getProject();
        User user = getUserEntity();
        ProjectSubscribe projectSubscribe = ProjectSubscribe.of(savedProject, user);

        given(projectRepository.findById(projectId))
                .willReturn(Optional.of(savedProject));
        given(userService.getCurrentAuthorizedUser())
                .willReturn(user);
        given(projectSubscribeRepository.findByProjectAndSubscriber(
                savedProject,
                user
        ))
                .willReturn(Optional.of(projectSubscribe));
        willDoNothing().given(projectSubscribeRepository).delete(projectSubscribe);

        // When
        sut.cancelProjectSubscribe(projectId);

        // Then
        then(projectSubscribeRepository).should().delete(projectSubscribe);
    }
}