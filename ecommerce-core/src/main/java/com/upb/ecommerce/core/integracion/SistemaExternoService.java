package com.upb.ecommerce.core.integracion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * Consume el backend de OTRO proyecto ecommerce (sistema par con el mismo contrato).
 * Mismo patron que {@code SistemaA} del proyecto eventop: un {@link RestClient} creado
 * con timeouts explícitos en {@link #create()}, llamadas que verifican el status y
 * lanzan {@code Exception} ante error.
 *
 * <p>Flujo: {@link #auth(SistemaExternoAuthRequest)} guarda el access_token; las demas
 * operaciones lo envian en el header {@code Authorization}.
 */
@Slf4j
@Service
public class SistemaExternoService {

    @Value("${sistema.externo.url-base}")
    private String urlBase;
    @Value("${sistema.externo.connect-timeout}")
    private int connectTimeout;
    @Value("${sistema.externo.read-timeout}")
    private int readTimeout;

    private String tokenGuardado;

    // --- auth ----------------------------------------------------------------

    public SistemaExternoAuthResponse auth(SistemaExternoAuthRequest request) throws Exception {
        RestClient restClient = create();
        tokenGuardado = null;

        ResponseEntity<SistemaExternoAuthResponse> response;
        try {
            response = restClient.post()
                    .uri(urlBase + "/api/auth")
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .body(request)
                    .retrieve()
                    .toEntity(SistemaExternoAuthResponse.class);
        } catch (Exception e) {
            log.error("Exception al autenticar contra sistema externo. ", e);
            throw e;
        }

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Se genero error: {}", response.getStatusCode().value());
            throw new Exception("Se genero error");
        }

        tokenGuardado = response.getBody() != null ? response.getBody().getAccessToken() : null;
        log.info("Autenticacion en sistema externo OK");
        return response.getBody();
    }

    // --- usuarios ------------------------------------------------------------

    public List<UsuarioExternoResponse> listarUsuarios(Long tiendaId) throws Exception {
        RestClient restClient = create();

        ResponseEntity<List<UsuarioExternoResponse>> response;
        try {
            response = restClient.get()
                    .uri(urlBase + "/api/usuarios/tienda/" + tiendaId)
                    .header("Authorization", "Bearer " + obtenerToken())
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<List<UsuarioExternoResponse>>() {});
        } catch (Exception e) {
            log.error("Exception al listar usuarios del sistema externo. ", e);
            throw e;
        }

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Se genero error: {}", response.getStatusCode().value());
            throw new Exception("Se genero error");
        }
        return response.getBody();
    }

    public UsuarioExternoResponse crearUsuario(UsuarioExternoRequest request) throws Exception {
        RestClient restClient = create();

        ResponseEntity<UsuarioExternoResponse> response;
        try {
            response = restClient.post()
                    .uri(urlBase + "/api/usuarios/registrar")
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .header("Authorization", "Bearer " + obtenerToken())
                    .body(request)
                    .retrieve()
                    .toEntity(UsuarioExternoResponse.class);
        } catch (Exception e) {
            log.error("Exception al crear usuario en el sistema externo. ", e);
            throw e;
        }

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Se genero error: {}", response.getStatusCode().value());
            throw new Exception("Se genero error");
        }
        return response.getBody();
    }

    // --- tiendas -------------------------------------------------------------

    public List<TiendaExternoResponse> listarTiendas() throws Exception {
        RestClient restClient = create();

        ResponseEntity<List<TiendaExternoResponse>> response;
        try {
            response = restClient.get()
                    .uri(urlBase + "/api/tiendas")
                    .header("Authorization", "Bearer " + obtenerToken())
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<List<TiendaExternoResponse>>() {});
        } catch (Exception e) {
            log.error("Exception al listar tiendas del sistema externo. ", e);
            throw e;
        }

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Se genero error: {}", response.getStatusCode().value());
            throw new Exception("Se genero error");
        }
        return response.getBody();
    }

    public TiendaExternoResponse crearTienda(TiendaExternoRequest request) throws Exception {
        RestClient restClient = create();

        ResponseEntity<TiendaExternoResponse> response;
        try {
            response = restClient.post()
                    .uri(urlBase + "/api/tiendas")
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .header("Authorization", "Bearer " + obtenerToken())
                    .body(request)
                    .retrieve()
                    .toEntity(TiendaExternoResponse.class);
        } catch (Exception e) {
            log.error("Exception al crear tienda en el sistema externo. ", e);
            throw e;
        }

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Se genero error: {}", response.getStatusCode().value());
            throw new Exception("Se genero error");
        }
        return response.getBody();
    }

    // --- helpers -------------------------------------------------------------

    private String obtenerToken() {
        if (tokenGuardado == null) {
            throw new IllegalStateException("No hay token del sistema externo. Llama primero a auth(...)");
        }
        return tokenGuardado;
    }

    private RestClient create() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        clientHttpRequestFactory.setReadTimeout(Duration.ofMillis(readTimeout));

        return RestClient.builder().requestFactory(clientHttpRequestFactory).build();
    }
}
