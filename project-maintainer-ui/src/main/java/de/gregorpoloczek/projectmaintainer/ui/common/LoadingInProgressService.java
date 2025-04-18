package de.gregorpoloczek.projectmaintainer.ui.common;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import de.gregorpoloczek.projectmaintainer.bootstrap.BootstrapService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoadingInProgressService implements VaadinServiceInitListener {

    BootstrapService bootstrapService;

    @Override
    public void serviceInit(ServiceInitEvent serviceInitEvent) {
        serviceInitEvent.getSource().addUIInitListener(
                _ -> {
                    if (Boolean.TRUE.equals(bootstrapService.isInitialized().blockFirst())) {
                        return;
                    }
                    blockUIUntilInitialized();
                });
    }

    private void blockUIUntilInitialized() {
        log.info("Projects are still being initialized, blocking page.");
        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        Dialog dialog = new Dialog(new Text("Projects are being intialized, please wait ..."), progressBar);
        dialog.setModal(true);
        dialog.setCloseOnOutsideClick(false);
        dialog.setCloseOnEsc(false);
        dialog.open();

        // reload ui
        UI ui = UI.getCurrent();
        Disposable disposable = bootstrapService.isInitialized()
                .takeUntil(initialized -> initialized)
                .filter(initialized -> initialized)
                .subscribe(_ -> {
                    log.info("Projects are initialized, reloading page.");
                    ui.access(() -> ui.getPage().reload());
                });
        ui.addDetachListener(_ -> disposable.dispose());
    }
}
