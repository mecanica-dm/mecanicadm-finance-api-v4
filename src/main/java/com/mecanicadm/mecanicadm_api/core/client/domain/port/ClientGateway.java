package com.mecanicadm.mecanicadm_api.core.client.domain.port;

import com.mecanicadm.mecanicadm_api.core.client.domain.Client;

import java.util.Optional;
import java.util.UUID;

public interface ClientGateway {
    Client create(Client client);

    Client update(Client client);

    Optional<Client> findById(UUID id);

    boolean existsByDocument(String document);

    boolean existsByEmail(String email);

    boolean existsByDocumentAndIdNot(String document, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);

    ClientPageResult findAll(ClientPageQuery pageQuery);

    boolean existsById(UUID id);
}
