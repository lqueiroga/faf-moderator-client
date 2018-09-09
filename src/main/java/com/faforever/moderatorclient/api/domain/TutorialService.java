package com.faforever.moderatorclient.api.domain;

import com.faforever.commons.api.dto.Tutorial;
import com.faforever.commons.api.dto.TutorialCategory;
import com.faforever.moderatorclient.api.ElideRouteBuilder;
import com.faforever.moderatorclient.api.FafApiCommunicationService;
import com.faforever.moderatorclient.mapstruct.TutorialCategoryMapper;
import com.faforever.moderatorclient.mapstruct.TutorialMapper;
import com.faforever.moderatorclient.ui.domain.TutorialCategoryFX;
import com.faforever.moderatorclient.ui.domain.TutorialFx;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Service
@Slf4j
public class TutorialService {
    private final FafApiCommunicationService fafApi;
    private final TutorialMapper tutorialMapper;
    private final TutorialCategoryMapper tutorialCategoryMapper;

    @Inject
    public TutorialService(FafApiCommunicationService fafApi, TutorialMapper tutorialMapper, TutorialCategoryMapper tutorialCategoryMapper) {
        this.fafApi = fafApi;
        this.tutorialMapper = tutorialMapper;
        this.tutorialCategoryMapper = tutorialCategoryMapper;
    }

    public List<Tutorial> getAllTutorialsFromApi() {
        log.debug("Retrieving all tutorials");
        List<Tutorial> result = fafApi.getAll(ElideRouteBuilder.of(Tutorial.class)
                .addInclude("mapVersion")
                .addInclude("category"));
        log.trace("found {} tutorials", result.size());
        return result;
    }

    public List<TutorialCategory> getAllCategoriesFromApi() {
        log.debug("Retrieving all tutorial categories");
        List<TutorialCategory> result = fafApi.getAll(ElideRouteBuilder.of(TutorialCategory.class)
                .addInclude("tutorials"));
        log.trace("found {} tutorial categories", result.size());
        return result;
    }

    public CompletableFuture<List<TutorialFx>> getAllTutorials() {
        return CompletableFuture.supplyAsync(() -> tutorialMapper.map(getAllTutorialsFromApi()));
    }

    public void updateTutorial(TutorialFx rowValue) {
        update(tutorialMapper.map(rowValue));
    }

    public void deleteTutorial(TutorialFx tutorialFx) {
        deleteTutorial(tutorialMapper.map(tutorialFx));
    }

    private Tutorial update(Tutorial tutorial) {
        log.debug("Patching Tutorial of id: ", tutorial.getId());
        return fafApi.patch(ElideRouteBuilder.of(Tutorial.class).id(tutorial.getId()), tutorial);
    }

    private void deleteTutorial(Tutorial tutorial) {
        log.debug("Deleting tutorial category: {}", tutorial);
        fafApi.delete(ElideRouteBuilder.of(Tutorial.class).id(tutorial.getId()));
    }

    public Tutorial create(TutorialFx tutorialFx) {
        return create(tutorialMapper.map(tutorialFx));
    }

    private Tutorial create(Tutorial tutorial) {
        log.debug("Adding tutorial: {}", tutorial);
        return fafApi.post(ElideRouteBuilder.of(Tutorial.class), tutorial);
    }

    public CompletionStage<List<TutorialCategoryFX>> getAllCategories() {
        return CompletableFuture.supplyAsync(() -> tutorialCategoryMapper.map(getAllCategoriesFromApi()));
    }

    public void deleteCategory(TutorialCategoryFX selectedItem) {
        log.debug("Deleting tutorial category: {}", selectedItem);
        fafApi.delete(ElideRouteBuilder.of(TutorialCategory.class).id(String.valueOf(selectedItem.getId())));
    }

    public void updateCategory(TutorialCategoryFX category) {
        log.debug("Updating tutorial category: {}", category);
        fafApi.patch(ElideRouteBuilder.of(TutorialCategory.class).id(String.valueOf(category.getId())), tutorialCategoryMapper.map(category));
    }

    public TutorialCategory createCategory(TutorialCategory tutorialCategory) {
        log.debug("Adding tutorial category: {}", tutorialCategory);
        return fafApi.post(ElideRouteBuilder.of(TutorialCategory.class), tutorialCategory);
    }
}