package io.github.gregorpoloczek.projectmaintainer.core.common.events;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DomainObjectCreatedEvent<I, T> extends DomainObjectEvent<I> {
    T value;

    public DomainObjectCreatedEvent(I id, T value) {
        super(id);
        this.value = value;
    }
}
