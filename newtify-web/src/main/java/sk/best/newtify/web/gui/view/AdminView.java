package sk.best.newtify.web.gui.view;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.http.ResponseEntity;
import sk.best.newtify.api.ArticlesApi;
import sk.best.newtify.api.CommentsApi;
import sk.best.newtify.api.dto.ArticleDTO;
import sk.best.newtify.api.dto.CommentsDTO;
import sk.best.newtify.web.bootstrap.NewtifyWebApplication;
import sk.best.newtify.web.gui.component.article.ArticleEditor;
import sk.best.newtify.web.gui.component.comments.CommentsEditor;
import sk.best.newtify.web.util.ArticleMapper;
import sk.best.newtify.web.util.CommentsMapper;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author Marek Urban
 * Copyright © 2022 BEST Technická univerzita Košice.
 * All rights reserved.
 */
@PageTitle("Administration")
@Route("admin")
public class AdminView extends SplitLayout {

    private static final long              serialVersionUID = -5284513347869397044L;
    private static final DateTimeFormatter DATE_FORMATTER   = DateTimeFormatter.ofPattern("dd. MM. uuuu");

    private final ArticlesApi                  articlesApi;
    private final ObjectFactory<ArticleEditor> articleEditorObjectFactory;
    private final CommentsApi commentsApi;
    private final ObjectFactory<CommentsEditor> commentsEditorObjectFactory;

    private final HorizontalLayout topLayout    = new HorizontalLayout();

    private final HorizontalLayout bottomLayout    = new HorizontalLayout();
    private final VerticalLayout   topRightPane = new VerticalLayout();
    private final VerticalLayout   editArticle = new VerticalLayout();
    private final VerticalLayout   commentsList = new VerticalLayout();
    public ListBox<CommentsDTO> commentsSelector;

    public AdminView(ArticlesApi articlesApi, ObjectFactory<ArticleEditor> articleEditorObjectFactory, CommentsApi commentsApi, ObjectFactory<CommentsEditor> commentsEditorObjectFactory) {
        this.articlesApi                = articlesApi;
        this.articleEditorObjectFactory = articleEditorObjectFactory;
        this.commentsApi                = commentsApi;
        this.commentsEditorObjectFactory = commentsEditorObjectFactory;
    }

    @PostConstruct
    protected void init() {
        this.setOrientation(Orientation.VERTICAL);

        topLayout.removeAll();
        topRightPane.removeAll();
        bottomLayout.removeAll();
        editArticle.removeAll();
        commentsList.removeAll();

        ArticleEditor articleEditor = articleEditorObjectFactory.getObject();
        CommentsEditor commentsEditor = commentsEditorObjectFactory.getObject();
        ListBox<ArticleDTO> articleSelector = createArticleSelector(articleEditor);
        commentsSelector = createCommentsSelector(commentsEditor);
        Button fetchArticlesButton = createArticlesFetchButton(articleSelector);
        Button createArticlesButton = createArticlesCreateButton(articleSelector);
        Button updateArticlesButton = createArticleUpdateButton(articleEditor, articleSelector);
        Button deleteArticlesButton = createArticleDeleteButton(articleEditor, articleSelector);
        Button editCommentsButton = createCommentsUpdateButton(commentsEditor, commentsSelector);

        topRightPane.add(fetchArticlesButton, createArticlesButton, updateArticlesButton, deleteArticlesButton);
        topRightPane.setSizeFull();
        topRightPane.getStyle()
                .set("border", "var(--lumo-contrast-5pct) 5px solid");

        topLayout.add(articleSelector, topRightPane);
        topLayout.setSizeFull();

        editArticle.add(articleEditor.getFormLayout(), articleEditor.getContentTextArea());
        editArticle.getStyle().set("overflow", "hidden");
        editArticle.getStyle().set("width", "75%");

        commentsList.getStyle().set("overflow", "hidden");
        commentsList.getStyle().set("width", "25%");
        commentsList.add(commentsSelector);

        bottomLayout.add(editArticle);
        bottomLayout.add(commentsList);
        bottomLayout.getStyle().set("overflow", "hidden");
        bottomLayout.setSizeFull();

        this.addToPrimary(topLayout);
        this.addToSecondary(bottomLayout);
        this.setSplitterPosition(25);
        this.setSizeFull();
    }

    private ListBox<ArticleDTO> createArticleSelector(ArticleEditor articleEditor) {
        ListBox<ArticleDTO> selector = new ListBox<>();
        selector.setSizeFull();
        selector.setRenderer(createSelectorRenderer());
        fetchArticles(selector);

        selector.addValueChangeListener(event -> {
            if (event.getValue() == null) {
                return;
            }
            articleEditor.getArticleBinder().setBean(event.getValue());
            articleEditor.getContentTextArea().setValue(event.getValue().getText());
            articleEditor.getArticleBinder().getBean().setUuid(event.getValue().getUuid());
            NewtifyWebApplication.newtifyStateService.setCurrentArticleId(event.getValue().getUuid());
            System.out.println("[DEBUG] Current article ID has changed to:  " + NewtifyWebApplication.newtifyStateService.getCurrentArticleId());
            fetchComments(commentsSelector);
        });

        return selector;
    }

    private ListBox<CommentsDTO> createCommentsSelector(CommentsEditor commentsEditor) {
        ListBox<CommentsDTO> selector = new ListBox<>();
        selector.setSizeFull();
        selector.setRenderer(createCommentsSelectorRenderer());
        fetchComments(selector);

        selector.addValueChangeListener(event -> {
            if (event.getValue() == null) {
                return;
            }

            String commentsCid = event.getValue().getCid();
            NewtifyWebApplication.newtifyStateService.setCommentAuthor(event.getValue().getName());
            NewtifyWebApplication.newtifyStateService.setCommentAuthorEmail(event.getValue().getEmail());
            NewtifyWebApplication.newtifyStateService.setCommentContent(event.getValue().getComment());
            NewtifyWebApplication.newtifyStateService.setCurrentArticleId(event.getValue().getUuid());
            NewtifyWebApplication.newtifyStateService.setCommentCommentId(event.getValue().getCid());
            NewtifyWebApplication.newtifyStateService.setCommentCreatedAt(event.getValue().getCreatedAt());
            System.out.println("[DEBUG] AID::CID: " + NewtifyWebApplication.newtifyStateService.getCurrentArticleId() + "::" + commentsCid);
            ConfirmDialog commentEditorDialog = commentsEditorObjectFactory.getObject().getConfirmDialog();
            commentEditorDialog.addConfirmListener(confirmEvent -> fetchComments(commentsSelector));
            commentEditorDialog.addRejectListener(rejectEvent -> fetchComments(commentsSelector));
            commentEditorDialog.open();
        });

        return selector;
    }

    private Button createArticleDeleteButton(ArticleEditor articleEditor,
                                             ListBox<ArticleDTO> articleSelector) {
        Button button = new Button("Delete article", VaadinIcon.DEL.create());
        button.addThemeVariants(ButtonVariant.LUMO_ERROR);

        button.addClickListener(event -> {
            if (!articleEditor.getArticleBinder().validate().isOk()) {
                return;
            }
            ResponseEntity<Void> response = articlesApi.deleteArticle(
                    articleEditor.getArticleBinder().getBean().getUuid()
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Notification successNotification = new Notification("Deleted");
                successNotification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                successNotification.setPosition(Notification.Position.TOP_CENTER);
                successNotification.setDuration(5000);
                successNotification.open();
                articleEditor.clear();
                fetchArticles(articleSelector);
                return;
            }

            Notification errorNotification = new Notification("Error");
            errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            errorNotification.setPosition(Notification.Position.TOP_CENTER);
            errorNotification.setDuration(5000);
            errorNotification.open();
        });

        return button;
    }

    private Button createArticleUpdateButton(ArticleEditor articleEditor,
                                             ListBox<ArticleDTO> articleSelector) {
        Button button = new Button("Update article", VaadinIcon.UPLOAD.create());
        button.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        button.addClickListener(event -> {
            if (!articleEditor.getArticleBinder().validate().isOk()) {
                return;
            }
            ResponseEntity<Void> response = articlesApi.updateArticle(
                    articleEditor.getArticleBinder().getBean().getUuid(),
                    ArticleMapper.toCreateArticle(articleEditor.getArticleBinder().getBean())
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Notification successNotification = new Notification("Saved");
                successNotification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                successNotification.setPosition(Notification.Position.TOP_CENTER);
                successNotification.setDuration(5000);
                successNotification.open();
                articleEditor.clear();
                fetchArticles(articleSelector);
                return;
            }

            Notification errorNotification = new Notification("Error");
            errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            errorNotification.setPosition(Notification.Position.TOP_CENTER);
            errorNotification.setDuration(5000);
            errorNotification.open();
        });

        return button;
    }

    private Button createArticlesCreateButton(
            ListBox<ArticleDTO> articleSelector) {
        Button button = new Button("Create article", VaadinIcon.PLUS_SQUARE_O.create());
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClickListener(event -> {
            ConfirmDialog articleEditorDialog = articleEditorObjectFactory.getObject().getConfirmDialog();
            articleEditorDialog.addConfirmListener(confirmEvent -> fetchArticles(articleSelector));
            articleEditorDialog.open();
        });
        return button;
    }

    private Button createArticlesFetchButton(ListBox<ArticleDTO> articleSelector) {
        Button button = new Button("Reload", VaadinIcon.DOWNLOAD.create());
        button.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        button.addClickListener(event -> fetchArticles(articleSelector));
        return button;
    }

    private Button createCommentsUpdateButton(CommentsEditor commentsEditor,
                                             ListBox<CommentsDTO> commentsSelector) {
        Button button = new Button("Edit comment", VaadinIcon.EDIT.create());
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClickListener(event -> {
            ConfirmDialog commentsEditorDialog = commentsEditorObjectFactory.getObject().getConfirmDialog();
            commentsEditorDialog.addConfirmListener(confirmEvent -> fetchComments(commentsSelector));
            commentsEditorDialog.open();
        });
        return button;
    }

    private void fetchArticles(ListBox<ArticleDTO> articleSelector) {
        articleSelector.setItems(
                articlesApi
                        .retrieveArticles(null)
                        .getBody()
        );
    }

    private void fetchComments(ListBox<CommentsDTO> commentsSelector) {
        commentsSelector.setItems(
                commentsApi
                        .getComments(NewtifyWebApplication.newtifyStateService.getCurrentArticleId())
                        .getBody()
        );
    }

    private static ComponentRenderer<FlexLayout, ArticleDTO> createSelectorRenderer() {
        return new ComponentRenderer<>(article -> {
            FlexLayout wrapper = new FlexLayout();

            Span topicBadge = new Span(article.getTopicType().getValue());
            topicBadge.getElement().getThemeList().add("badge secondary");
            topicBadge.getStyle().set("margin-right", "1em");
            topicBadge.setWidth("8em");

            Div titleAndContent = new Div();
            titleAndContent.setText(article.getTitle());

            Div content = new Div();
            content.add(new Span(article.getAuthor()));
            content.add(new Span(
                    DATE_FORMATTER.format(
                            Instant.ofEpochSecond(article.getCreatedAt())
                                    .atZone(ZoneId.systemDefault())
                    )
            ));

            content.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("display", "flex")
                    .set("flex-direction", "column");
            titleAndContent.add(content);

            wrapper.add(topicBadge, titleAndContent);
            return wrapper;
        });
    }

    private static ComponentRenderer<FlexLayout, CommentsDTO> createCommentsSelectorRenderer() {
        return new ComponentRenderer<>(article -> {
            FlexLayout wrapper = new FlexLayout();

            Span topicBadge = new Span(article.getUuid());
            topicBadge.getElement().getThemeList().add("badge secondary");
            topicBadge.getStyle().set("margin-right", "1em");
            topicBadge.setWidth("8em");

            Div titleAndContent = new Div();
            titleAndContent.setText(article.getName());

            Div content = new Div();
            content.add(new Span(article.getComment()));
            content.add(new Span(
                    DATE_FORMATTER.format(
                            Instant.ofEpochSecond(article.getCreatedAt())
                                    .atZone(ZoneId.systemDefault())
                    )
            ));

            content.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("display", "flex")
                    .set("flex-direction", "column");
            titleAndContent.add(content);

            wrapper.add(topicBadge, titleAndContent);
            return wrapper;
        });
    }

}
