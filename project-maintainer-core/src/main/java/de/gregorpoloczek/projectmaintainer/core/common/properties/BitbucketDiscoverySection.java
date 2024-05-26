package de.gregorpoloczek.projectmaintainer.core.common.properties;

import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class BitbucketDiscoverySection {

    List<String> users;

}
