package com.aqua.plus.api.utils;

import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class EncriptarDesencriptar {
    
	private static String secretKey = "";
	
	@Value("${seguridad.llave}")
	private String llave;
	
	/**
	 * Metodo de encriptacion de la contraseña acesso.
	 * 
	 * @param texto
	 * @return Contraseña encriptada.
	 */
	public String encriptar(String texto) {
		log.info("--------------------------ENTRADA TEXTO------------------------  " + texto);
		secretKey ="keyacuaplus";//this.llave; // llave para encriptar datos
		String base64EncryptedString = "";
		log.info("--------------------------ENTRADA llave------------------------  " + llave);

		try {

			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digestOfPassword = md.digest(secretKey.getBytes("utf-8"));
			byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);

			SecretKey key = new SecretKeySpec(keyBytes, "DESede");
			Cipher cipher = Cipher.getInstance("DESede");
			cipher.init(Cipher.ENCRYPT_MODE, key);

			byte[] plainTextBytes = texto.getBytes("utf-8");
			byte[] buf = cipher.doFinal(plainTextBytes);
			byte[] base64Bytes = Base64.encodeBase64(buf);
			base64EncryptedString = new String(base64Bytes);

		} catch (Exception e) {
			log.error("Error encriptar: " + e.getMessage());
			e.printStackTrace();
		}
		return base64EncryptedString;
	}

	/**
	 * Metodo de desencriptacion de contraseña.
	 * 
	 * @param textoEncriptado
	 * @return contraseña desencriptada.
	 * @throws Exception
	 */
	public String desencriptar(String textoEncriptado) {
		log.info("Ingreso desencriptar:  {}", textoEncriptado );
		secretKey ="keyacuaplus";//this.llave; llave para desenciptar datos
		String base64EncryptedString = "";

		try {
			byte[] message = Base64.decodeBase64(textoEncriptado.getBytes("utf-8"));
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digestOfPassword = md.digest(secretKey.getBytes("utf-8"));
			byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);
			SecretKey key = new SecretKeySpec(keyBytes, "DESede");

			Cipher decipher = Cipher.getInstance("DESede");
			decipher.init(Cipher.DECRYPT_MODE, key);

			byte[] plainText = decipher.doFinal(message);

			base64EncryptedString = new String(plainText, "UTF-8");

		} catch (Exception e) {
			log.error("Error desencriptar: " + e.getMessage());
			e.printStackTrace();
		}
		log.info("Salida desencriptar: {} ", base64EncryptedString );
		return base64EncryptedString;
	}
	
	public static void main(String[] args) throws Exception {
		EncriptarDesencriptar ed = new EncriptarDesencriptar();
		//System.out.println("clave " + ed.desencriptar("c1RRHpoyHT/GeO3x+5tQBgBCKaeYKb/EuqBg6gE8GBKVQN0S7/pQOmLLUjHnAfe2XigogilUij6vJyX/eB4Pqj85kfa3iTaI9xTFm0AsqUiu9ZFUPZe+rr763Z+WoqgyXSsv/yDIkBGkz9X7GHl5rTscX4F7At2+sCcCyFrVefrhWqD9t/82Ffc6PnpotBM8ElFq9WLyRTf1+LfwlnaqZNd6fPQ3UDb9EkU="));
		System.out.println("clave " + ed.encriptar("S2V5LXRva2VuLWV4dGVybmFsLXRoaXMtaXMtYS1zZWNyZXQta2V5LXRoYXQtaXMtYXQtbGVhc3QtNjQtYnl0ZXMtbG9uZy1jb2RlLW1ha2Vycy1hY3VhLXBsdXM="));
	}
}