package io.github.gregorpoloczek.projectmaintainer.core.common.events;

public class DomainObjectDeletedEvent<I> extends DomainObjectEvent<I> {

    public DomainObjectDeletedEvent(I id) {
        super(id);
    }
}
