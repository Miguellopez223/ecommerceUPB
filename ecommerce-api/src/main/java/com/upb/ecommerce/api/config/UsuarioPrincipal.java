package com.upb.ecommerce.api.config;

/**
 * @deprecated Ya no se usa. La entidad {@link com.upb.ecommerce.domain.entities.Usuario}
 * implementa directamente {@link org.springframework.security.core.userdetails.UserDetails},
 * por lo que este adaptador es innecesario. Se mantiene solo para referencia histórica.
 */
@Deprecated
public class UsuarioPrincipal {
    // Ver: Usuario.java — implementa UserDetails directamente.
}
