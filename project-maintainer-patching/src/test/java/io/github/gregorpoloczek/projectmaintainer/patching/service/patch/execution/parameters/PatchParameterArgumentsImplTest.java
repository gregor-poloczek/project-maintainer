package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.parameters;

import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameter;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterArgument;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.exceptions.PatchParameterNotFoundException;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterType;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.exceptions.PatchParameterTypeMismatchException;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.exceptions.PatchParameterValueMissingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class PatchParameterArgumentsImplTest {

    static final PatchParameter STRING = PatchParameter.builder().id("string").type(PatchParameterType.STRING).build();
    static final PatchParameter INTEGER = PatchParameter.builder().id("integer").type(PatchParameterType.INTEGER).build();
    static final PatchParameter BOOLEAN = PatchParameter.builder().id("boolean").type(PatchParameterType.BOOLEAN).build();
    static final PatchParameter FILES = PatchParameter.builder().id("files").type(PatchParameterType.FILES).build();

    @Test
    void typedGetters_returnValuesOfMatchingType() {
        PatchParameterArgumentsImpl arguments = new PatchParameterArgumentsImpl(
                List.of(STRING, INTEGER, BOOLEAN, FILES),
                List.of(new PatchParameterArgumentImpl<>(STRING, "text"),
                        new PatchParameterArgumentImpl<>(INTEGER, 42),
                        new PatchParameterArgumentImpl<>(BOOLEAN, true),
                        new PatchParameterArgumentImpl<>(FILES, List.of())));

        assertThat(arguments.getString("string").getValue()).contains("text");
        assertThat(arguments.getString("string").requireValue()).isEqualTo("text");
        assertThat(arguments.getInteger("integer").getValue()).contains(42);
        assertThat(arguments.getBoolean("boolean").getValue()).contains(true);
        assertThat(arguments.getFiles("files").getValue()).contains(List.of());

        // default methods of PatchParameterArguments, delegating via the id of the passed parameter
        assertThat(arguments.getString(STRING).getValue()).contains("text");
        assertThat(arguments.getInteger(INTEGER).getValue()).contains(42);
        assertThat(arguments.getBoolean(BOOLEAN).getValue()).contains(true);
        assertThat(arguments.getFiles(FILES).getValue()).contains(List.of());
    }

    @Test
    void typedGetters_returnEmptyArgumentIfNoValueIsPresent() {
        PatchParameterArgumentsImpl arguments = new PatchParameterArgumentsImpl(List.of(STRING), List.of());

        PatchParameterArgument<String> argument = arguments.getString("string");

        assertThat(argument.getParameter()).isEqualTo(STRING);
        assertThat(argument.getValue()).isEmpty();
        assertThatThrownBy(argument::requireValue)
                .isInstanceOfSatisfying(PatchParameterValueMissingException.class,
                        e -> assertThat(e.getParameterId()).isEqualTo("string"));
    }

    @Test
    void typedGetters_rejectMismatchingTypeIfValueIsPresent() {
        PatchParameterArgumentsImpl arguments = new PatchParameterArgumentsImpl(
                List.of(INTEGER), List.of(new PatchParameterArgumentImpl<>(INTEGER, 42)));

        assertThatThrownBy(() -> arguments.getString("integer"))
                .isInstanceOfSatisfying(PatchParameterTypeMismatchException.class, e -> {
                    assertThat(e.getParameterId()).isEqualTo("integer");
                    assertThat(e.getDeclaredType()).isEqualTo(PatchParameterType.INTEGER);
                    assertThat(e.getRequestedType()).isEqualTo(PatchParameterType.STRING);
                });
    }

    @Test
    void typedGetters_rejectMismatchingTypeEvenIfNoValueIsPresent() {
        // without a value, a cast would never fail, so the mismatch must be detected via the declared type
        PatchParameterArgumentsImpl arguments = new PatchParameterArgumentsImpl(List.of(INTEGER, STRING), List.of());

        assertThatThrownBy(() -> arguments.getString("integer"))
                .isInstanceOf(PatchParameterTypeMismatchException.class);
        assertThatThrownBy(() -> arguments.getInteger("string"))
                .isInstanceOf(PatchParameterTypeMismatchException.class);
        assertThatThrownBy(() -> arguments.getBoolean("string"))
                .isInstanceOf(PatchParameterTypeMismatchException.class);
        assertThatThrownBy(() -> arguments.getFiles("string"))
                .isInstanceOf(PatchParameterTypeMismatchException.class);
    }

    @Test
    void typedGetters_rejectUnknownParameter() {
        PatchParameterArgumentsImpl arguments = new PatchParameterArgumentsImpl(List.of(STRING), List.of());

        assertThatThrownBy(() -> arguments.getString("unknown"))
                .isInstanceOfSatisfying(PatchParameterNotFoundException.class,
                        e -> assertThat(e.getParameterId()).isEqualTo("unknown"));
    }

    @Test
    void getAll_returnsAllArguments() {
        PatchParameterArgumentsImpl arguments = new PatchParameterArgumentsImpl(
                List.of(STRING, INTEGER),
                List.of(new PatchParameterArgumentImpl<>(STRING, "text"),
                        new PatchParameterArgumentImpl<>(INTEGER, 42)));

        assertThat(arguments.getAll())
                .extracting(PatchParameterArgument::getParameter, a -> a.getValue().orElseThrow())
                .containsExactly(
                        tuple(STRING, "text"),
                        tuple(INTEGER, 42));
    }
}
