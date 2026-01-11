package de.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.bitbucketcloud;

import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.BitbucketCloudProjectConnection;
import de.gregorpoloczek.projectmaintainer.ui.common.ImageResolverService;
import de.gregorpoloczek.projectmaintainer.ui.views.workspace.connections.common.AbstractGenericProjectConnectionFormComponent;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

public class BitbucketCloudProjectConnectionFormComponent extends AbstractGenericProjectConnectionFormComponent<BitbucketCloudProjectConnection> {

    public static final String EMAIL = "email";
    public static final String BITBUCKET_USERNAME = "bitbucketUsername";
    public static final String PASSWORD = "password";

    protected @NonNull String getTitle() {
        return "Bitbucket Cloud";
    }

    public BitbucketCloudProjectConnectionFormComponent(ImageResolverService imageResolverService) {
        super(imageResolverService, BitbucketCloudProjectConnection.TYPE);
    }

    @Override
    protected List<FieldDefinition> getFieldDefinition() {
        return List.of(FieldDefinition.builder()
                        .type(FieldDefinition.Type.STRING)
                        .id(EMAIL)
                        .defaultValue("")
                        .label("Atlassian E-Mail").build(),
                FieldDefinition.builder()
                        .type(FieldDefinition.Type.SECRET_STRING)
                        .id(BITBUCKET_USERNAME)
                        .defaultValue("")
                        .label("Bitbucket username").build(),
                FieldDefinition.builder()
                        .type(FieldDefinition.Type.SECRET_STRING)
                        .id(PASSWORD)
                        .defaultValue("")
                        .label("Password").build());
    }


    @Override
    protected Map<String, Object> extractData(BitbucketCloudProjectConnection connection) {
        return Map.of(
                EMAIL, connection.getEmail(),
                PASSWORD, connection.getPassword(),
                BITBUCKET_USERNAME, connection.getBitbucketUsername());
    }

    @Override
    protected @NonNull BitbucketCloudProjectConnection createConnection(String id, Map<String, Object> data) {
        return new BitbucketCloudProjectConnection(id,
                (String) data.get(EMAIL),
                (String) data.get(BITBUCKET_USERNAME),
                (String) data.get(PASSWORD)
        );
    }

    protected VerticalLayout createDescriptionComponent() {
        Pre pre = new Pre("""
                read:repository:bitbucket
                read:project:bitbucket
                read:workspace:bitbucket
                read:user:bitbucket
                read:pullrequest:bitbucket
                
                write:repository:bitbucket
                write:pullrequest:bitbucket
                """);
        pre.getClassNames().addAll(List.of(LumoUtility.Padding.SMALL, LumoUtility.BoxSizing.BORDER));
        pre.getStyle().setMarginTop("3px");
        Span label = new Span("Necessary Permissions");
        label.getStyle().setMarginTop("11px");
        label.getStyle()
                .setColor("var(--lumo-secondary-text-color)")
                .setFontSize("var(--vaadin-input-field-label-font-size, var(--lumo-font-size-s))");
        VerticalLayout right = new VerticalLayout(label, pre);
        right.setSpacing(false);
        right.setPadding(false);
        return right;
    }


}
