package com.aqua.plus.api.configs.security.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.aqua.plus.api.utils.EncriptarDesencriptar;
import com.aqua.plus.commons.entities.ParametrosSistemaEntity;
import com.aqua.plus.commons.exceptions.ProcessGenericException;
import com.aqua.plus.commons.repositories.ParametrosSistemaRepository;
import com.aqua.plus.commons.utils.Constantes;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author dchavarro.ext
 * @version 1.0
 * Clase encargada de los utilitarios requeridos jwt
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {
	
	private final ParametrosSistemaRepository parametrosSistemaRepository;
	
	private final EncriptarDesencriptar encriptarDesencriptar;
	
	 /**
     * Metodo encargado de obtener parametro por llave
	 * @author dchavarro.ext
	 * @since 21-08-2024
	 * @version 1.0 
     * @param key
     * @return Devuelve el parametro
     */
	public ParametrosSistemaEntity getParameter(final String key) {
		return this.parametrosSistemaRepository.findByLlave(key).orElseThrow(() -> new ProcessGenericException(""));
	}
	
	private final Set<String> blacklistedTokens = new HashSet<>();

    public void invalidateToken(String token) {
        blacklistedTokens.add(token);
    }

    public boolean isTokenInvalidated(String token) {
        return blacklistedTokens.contains(token);
    }
	
	/**
	 * 
	 * @param token
	 * @return
	 */
	public String getUsernameFromToken(String token) {
		return getClaimFromToken(token, Claims::getSubject);
	}

	/**
	 * Metodo encargado de obtener la fecha de vencimiento del token
	 * @author dchavarro.ext
	 * @since 21-08-2024
	 * @version 1.0 
	 * @param token
	 * @return Devuelve la fecha de vencimiento del token
	 */
	public Date getExpirationDateFromToken(String token) {
		return getClaimFromToken(token, Claims::getExpiration);
	}

	/**
	 * Metodo encargado de obtener la datos ya resuelto del token
	 * @author dchavarro.ext
	 * @since 21-08-2024
	 * @version 1.0
	 * @param <T>
	 * @param token
	 * @param claimsResolver
	 * @return Devuelve los datos  del token
	 */
	public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = getAllClaimsFromToken(token);
		return claimsResolver.apply(claims);
	}
	
	/**
	 * Metodo encargado de obtener la datos del token
	 * @author dchavarro.ext
	 * @since 21-08-2024
	 * @version 1.0  
	 * @param token
	 * @return Devuelve los datos  del token en Reclamos
	 */
	private Claims getAllClaimsFromToken(String token) {
		return Jwts.parser().setSigningKey(encriptarDesencriptar.desencriptar(this.getParameter(Constantes.KEY_TOKEN).getValorParametro())).parseClaimsJws(token).getBody();
	}

	/**
	 * Metodo encargado de validar si el token ya expiro
	 * @author dchavarro.ext
	 * @since 21-08-2024
	 * @version 1.0   
	 * @param token
	 * @return Devuelve true si expiro y false si no
	 */
	private Boolean isTokenExpired(String token) {
		final Date expiration = getExpirationDateFromToken(token);
		return expiration.before(new Date());
	}

	public String generateToken(String clientId) {
		Map<String, Object> claims = new HashMap<>();
		return doGenerateToken(claims, clientId);
	}

	/**
	 * Metodo encargado de generar el token
	 * @author dchavarro.ext
	 * @since 21-08-2024
	 * @version 1.0 
	 * @param claims
	 * @param clientId
	 * @return Devuelve el token generado
	 */
	private String doGenerateToken(Map<String, Object> claims, String clientId) {

		return Jwts.builder().setClaims(claims).setSubject(clientId).setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + Long.valueOf(this.getParameter(Constantes.TIEMPO_VIGENCIA_TOKEN).getValorParametro())))
				.signWith(SignatureAlgorithm.HS512, encriptarDesencriptar.desencriptar(this.getParameter(Constantes.KEY_TOKEN).getValorParametro())).compact();
	}
	
	public Date getIssuedAtDateFromToken(String token) {
	    return getClaimFromToken(token, Claims::getIssuedAt);
	}
	
	/**
	 * Metodo encargado de validar si el token es valido
	 * @author dchavarro.ext
	 * @since 21-08-2024
	 * @version 1.0 
	 * @param token
	 * @param clientId
	 * @return Devuelve true si es valido de lo contrario false
	 */
	public boolean validateToken(String token, UserDetails userDetails) {
	    final String usuario = getUsernameFromToken(token);
	    final Date issuedAt = getIssuedAtDateFromToken(token);

	    return usuario.equals(userDetails.getUsername())
	            && !isTokenExpired(token);
	}
}