package sk.best.newtify.web.gui.component.comments;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.validator.EmailValidator;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import sk.best.newtify.api.ArticlesApi;
import sk.best.newtify.api.CommentsApi;
import sk.best.newtify.api.dto.*;
import sk.best.newtify.web.bootstrap.NewtifyWebApplication;
import sk.best.newtify.web.util.ArticleMapper;
import sk.best.newtify.web.util.CommentsMapper;

import javax.annotation.PostConstruct;

@Getter
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CommentsEditor extends VerticalLayout {
    private static final long serialVersionUID = 8512638507866679718L;

    private final CommentsApi         commentsApi;

    private final FormLayout          formLayout          = new FormLayout();
    private final ConfirmDialog       confirmDialog       = new ConfirmDialog();

    private final TextField           nameField           = new TextField();
    private final TextField           emailField          = new TextField();
    private final TextArea            contentTextArea     = new TextArea();
    private final Binder<CommentsDTO> commentsDTOBinder   = new Binder<>();

    public CommentsEditor(CommentsApi commentsApi) {
        this.commentsApi = commentsApi;

        nameField.setSizeFull();
        emailField.setSizeFull();
        contentTextArea.setSizeFull();

        confirmDialog.setHeader("Edit comment");
        confirmDialog.setHeight("600px");
    }

    @PostConstruct
    protected void init() {
        formLayout.addFormItem(nameField, "Your name");
        formLayout.addFormItem(emailField, "Your email");

        contentTextArea.setLabel("Comment");
        formLayout.add(contentTextArea);

        createBinder();

        confirmDialog.setCancelable(true);
        confirmDialog.setRejectable(true);
        confirmDialog.setRejectText("Delete");
        confirmDialog.setConfirmText("Save");
        confirmDialog.addConfirmListener(this::onConfirmAction);
        confirmDialog.addCancelListener(this::onCancelAction);
        confirmDialog.addRejectListener(this::onRejectAction);
        confirmDialog.addAttachListener(event -> {
            formLayout.setSizeFull();
            contentTextArea.setHeight("200px");
            contentTextArea.setMaxHeight("200px");
            confirmDialog.add(formLayout);
        });

        this.setSizeFull();
        this.add(formLayout);

        System.out.println("[UI MANAGER] +++++++++++++ DRAWING EDIT COMMENTS INFO +++++++++++++");
        System.out.println("[UI MANAGER] @param name: " + NewtifyWebApplication.newtifyStateService.getCommentAuthor());
        System.out.println("[UI MANAGER] @param email: " + NewtifyWebApplication.newtifyStateService.getCommentAuthorEmail());
        System.out.println("[UI MANAGER] @param content: " + NewtifyWebApplication.newtifyStateService.getCommentContent());

        nameField.setValue(NewtifyWebApplication.newtifyStateService.getCommentAuthor());
        emailField.setValue(NewtifyWebApplication.newtifyStateService.getCommentAuthorEmail());
        contentTextArea.setValue(NewtifyWebApplication.newtifyStateService.getCommentContent());

    }

    private void onRejectAction(ConfirmDialog.RejectEvent rejectEvent) {
        commentsApi.deleteComment(NewtifyWebApplication.newtifyStateService.getCommentCommentId(), NewtifyWebApplication.newtifyStateService.getCurrentArticleId());
        Notification successNotification = new Notification("Deleted");
        successNotification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        successNotification.setPosition(Notification.Position.TOP_CENTER);
        successNotification.setDuration(5000);
        successNotification.open();
        clear();
        confirmDialog.close();
    }

    private void onCancelAction(ConfirmDialog.CancelEvent cancelEvent) {
        clear();
        confirmDialog.close();
    }

    private void onConfirmAction(ConfirmDialog.ConfirmEvent confirmEvent) {
        if (!commentsDTOBinder.validate().isOk()) {
            confirmDialog.open();
            return;
        }
        CreateCommentsDTO createCommentsDTO = CommentsMapper.toCreateComment(commentsDTOBinder.getBean());
        commentsApi.updateComment(NewtifyWebApplication.newtifyStateService.getCommentCommentId(), NewtifyWebApplication.newtifyStateService.getCurrentArticleId(), createCommentsDTO);
        Notification successNotification = new Notification("Saved");
        successNotification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        successNotification.setPosition(Notification.Position.TOP_CENTER);
        successNotification.setDuration(5000);
        successNotification.open();
        clear();
        confirmDialog.close();
    }

    private void createBinder() {
        commentsDTOBinder.setBean(new CommentsDTO());

        commentsDTOBinder
                .forField(nameField)
                .withValidator((value, context) -> {
                    if (StringUtils.isBlank(value)) {
                        return ValidationResult.error("Please enter your name!");
                    }
                    return ValidationResult.ok();
                })
                .bind(CommentsDTO::getName, CommentsDTO::setName);

        commentsDTOBinder
                .forField(emailField)
                .withValidator(new EmailValidator("Invalid e-mail address {0}"))
                .bind(CommentsDTO::getEmail, CommentsDTO::setEmail);



        commentsDTOBinder
                .forField(contentTextArea)
                .withValidator((value, context) -> {
                    if (StringUtils.isBlank(value)) {
                        return ValidationResult.error("Content can't be empty!");
                    }
                    return ValidationResult.ok();
                })
                .bind(CommentsDTO::getComment, CommentsDTO::setComment);

        commentsDTOBinder.addValueChangeListener(event -> commentsDTOBinder.validate());
    }

    public void clear() {
        commentsDTOBinder.removeBean();

        nameField.clear();
        emailField.clear();
        contentTextArea.clear();
    }
}
