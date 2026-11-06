package com.mecanicadm.mecanicadm_api.core.shared.usecase;

import com.mecanicadm.mecanicadm_api.core.shared.domain.InboxEntry;
import com.mecanicadm.mecanicadm_api.core.shared.domain.port.InboxGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessInboundMessageUseCaseTest {

    @Mock
    private InboxGateway inboxGateway;

    @InjectMocks
    private ProcessInboundMessageUseCase useCase;

    @Test
    @DisplayName("Deve executar o handler e marcar a mensagem como processada")
    void shouldProcessMessage() {
        InboxEntry entry = entry();
        AtomicBoolean executed = new AtomicBoolean(false);
        when(inboxGateway.tryStart(entry)).thenReturn(true);

        boolean processed = useCase.execute(entry, () -> executed.set(true));

        assertTrue(processed);
        assertTrue(executed.get());
        verify(inboxGateway).markProcessed(entry);
    }

    @Test
    @DisplayName("Deve ignorar o handler quando a mensagem já foi processada")
    void shouldSkipDuplicatedMessage() {
        InboxEntry entry = entry();
        Runnable handler = mock(Runnable.class);
        when(inboxGateway.tryStart(entry)).thenReturn(false);

        boolean processed = useCase.execute(entry, handler);

        assertFalse(processed);
        verifyNoInteractions(handler);
        verify(inboxGateway, never()).markProcessed(any());
    }

    @Test
    @DisplayName("Não deve marcar como processada quando o handler falhar")
    void shouldNotMarkProcessedWhenHandlerFails() {
        InboxEntry entry = entry();
        when(inboxGateway.tryStart(entry)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> useCase.execute(entry, () -> {
            throw new IllegalStateException("falha na regra de negócio");
        }));

        verify(inboxGateway, never()).markProcessed(any());
    }

    private InboxEntry entry() {
        return new InboxEntry(UUID.randomUUID(), "budget", "BudgetGeneratedEvent",
                UUID.randomUUID(), Instant.now());
    }
}
