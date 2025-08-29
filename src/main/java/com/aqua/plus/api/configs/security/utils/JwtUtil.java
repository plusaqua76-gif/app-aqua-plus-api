package com.aqua.plus.api.configs.security.utils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.aqua.plus.api.utils.EncriptarDesencriptar;
import com.aqua.plus.commons.entities.ParametrosSistemaEntity;
import com.aqua.plus.commons.exceptions.ProcessGenericException;
import com.aqua.plus.commons.repositories.ParametrosSistemaRepository;
import com.aqua.plus.commons.utils.Constantes;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
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
		return this.parametrosSistemaRepository.findByLlave(key).orElseThrow(() -> new ProcessGenericException(Constantes.PARAM_NOT_FOUND));
	}
	
	/**
	 * 
	 * @param token
	 * @return
	 */
	public String getUsernameFromToken(String token, String key) {
		return getClaimFromToken(token, Claims::getSubject, key);
	}

	/**
	 * Metodo encargado de obtener la fecha de vencimiento del token
	 * @author dchavarro.ext
	 * @since 21-08-2024
	 * @version 1.0 
	 * @param token
	 * @return Devuelve la fecha de vencimiento del token
	 */
	public Date getExpirationDateFromToken(String token, String key) {
		return getClaimFromToken(token, Claims::getExpiration, key);
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
	public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver, String key) {
		final Claims claims = getAllClaimsFromToken(token, key);
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
	private Claims getAllClaimsFromToken(String token, String keyToken) {
		String secret = encriptarDesencriptar.desencriptar(this.getParameter(keyToken).getValorParametro());
		SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
	}

	/**
	 * Metodo encargado de validar si el token ya expiro
	 * @author dchavarro.ext
	 * @since 21-08-2024
	 * @version 1.0   
	 * @param token
	 * @return Devuelve true si expiro y false si no
	 */
	public Boolean isTokenExpired(String token, String key) {
		final Date expiration = getExpirationDateFromToken(token, key);
		return expiration.before(new Date());
	}

	public String generateToken(String clientId, String key, String keyVigencia) {
		Map<String, Object> claims = new HashMap<>();
		return doGenerateToken(claims, clientId, key, keyVigencia);
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
	private String doGenerateToken(Map<String, Object> claims, String clientId, String keyToken, String keyVigencia) {
		
	    String secret = encriptarDesencriptar.desencriptar(this.getParameter(keyToken).getValorParametro());
	    SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	    
	    long expirationTime = Long.parseLong(this.getParameter(keyVigencia).getValorParametro());
	    
	    return Jwts.builder().setClaims(claims).setSubject(clientId).setIssuedAt(new Date(System.currentTimeMillis()))
	    		.setExpiration(new Date(System.currentTimeMillis() + expirationTime))
	    		.signWith(key, SignatureAlgorithm.HS512).compact();
	}

	
	public Date getIssuedAtDateFromToken(String token, String key) {
	    return getClaimFromToken(token, Claims::getIssuedAt, key);
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
	public boolean validateToken(String token, UserDetails userDetails, String key) {
	    final String usuario = getUsernameFromToken(token, key);

	    return usuario.equals(userDetails.getUsername())
	            && !isTokenExpired(token, key);
	}
	
	
	/**
	 * Valida la FIRMA del token con la llave de BD (KEY_TOKEN).
	 * Devuelve true si la firma es válida, incluso si el token está EXPIRADO.
	 */
	public boolean isSignatureValid(String token, String keyParam) {
	    try {
	        parseClaimsAllowExpired(token, keyParam); // si firma no coincide, lanzará JwtException
	        return true;
	    } catch (JwtException | IllegalArgumentException e) {
	        // firma inválida, token corrupto, etc.
	        return false;
	    }
	}

	/**
	 * Obtiene el username (subject) permitiendo token EXPIRADO (pero con firma válida).
	 */
	public String getUsernameFromTokenAllowExpired(String token, String keyParam) {
	    try {
	        Claims claims = parseClaimsAllowExpired(token, keyParam);
	        return claims.getSubject();
	    } catch (Exception e) {
	        return null;
	    }
	}

	/**
	 * Parser que valida firma y retorna Claims.
	 * - Si el token está expirado, captura ExpiredJwtException y retorna sus Claims.
	 * - Si la firma es inválida, lanza JwtException.
	 */
	private Claims parseClaimsAllowExpired(String token, String keyParam) {
	    String secret = encriptarDesencriptar.desencriptar(this.getParameter(keyParam).getValorParametro());
	    SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	    try {
	        return Jwts.parserBuilder()
	            .setSigningKey(key)
	            .build()
	            .parseClaimsJws(token)
	            .getBody();
	    } catch (ExpiredJwtException eje) {
	        return eje.getClaims();
	    }
	}
	
	// En JwtUtil

	public String generateRefreshToken(String clientId, String keyParam, String keyVigenciaRefreshParam) {
	    Map<String, Object> claims = new HashMap<>();
	    claims.put("typ", "refresh");
	    return doGenerateToken(claims, clientId, keyParam, keyVigenciaRefreshParam);
	}


}