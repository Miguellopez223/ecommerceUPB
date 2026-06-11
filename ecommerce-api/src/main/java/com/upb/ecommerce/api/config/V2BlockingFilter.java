package com.upb.ecommerce.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// --- PREGUNTA 6 ---
@Component
public class V2BlockingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // --- PREGUNTA 6 ---
        String ruta = request.getRequestURI();

        boolean tieneVersion2 = contieneVersionV2(ruta);

        // si tiene v2, RESPONDER 403 y NO DEJAR pasar la peticion
        if (tieneVersion2 == true) {
            response.setStatus(403);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"La version v2 no esta permitida (403)\"}");
            return;
        } else {
            filterChain.doFilter(request, response);
        }
    }

    // --- PREGUNTA 6 ---
    // metodo que revisa si la ruta tiene la palabra v2 como una de sus partes
    private boolean contieneVersionV2(String ruta) {

        if (ruta == null) {
            return false;
        }

        String rutaEnMinusculas = ruta.toLowerCase();

        // cortar la ruta cada vez que hay una barra / y guardar las partes
        String[] partes = rutaEnMinusculas.split("/");

        boolean encontroV2 = false;

        for (int i = 0; i < partes.length; i++) {
            String parteActual = partes[i];

            if (parteActual.equals("v2")) {
                encontroV2 = true;
            }
        }

        return encontroV2;
    }
}