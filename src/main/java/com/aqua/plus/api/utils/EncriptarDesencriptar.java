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
		secretKey =this.llave; // llave para encriptar datos
		String base64EncryptedString = "";

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
		secretKey =this.llave; 
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
		return base64EncryptedString;
	}
	
	/*public static void main(String[] args) throws Exception {
		EncriptarDesencriptar ed = new EncriptarDesencriptar();
	    System.out.println("clave " + ed.desencriptar("ZTIQeAQExg+wWhOssC20rA=="));
		System.out.println("clave " + ed.encriptar("xnot axdq dgqd afsz"));
	}*/
	/*public static void main(String[] args) throws Exception {
	    String llave = "keyacuaplus";
	    String textoAEncriptar = "irrfnklvjcsqsyjt";

	    MessageDigest md = MessageDigest.getInstance("MD5");
	    byte[] digestOfPassword = md.digest(llave.getBytes("utf-8"));
	    byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);

	    SecretKey key = new SecretKeySpec(keyBytes, "DESede");
	    Cipher cipher = Cipher.getInstance("DESede");
	    cipher.init(Cipher.ENCRYPT_MODE, key);

	    byte[] plainTextBytes = textoAEncriptar.getBytes("utf-8");
	    byte[] buf = cipher.doFinal(plainTextBytes);
	    byte[] base64Bytes = Base64.encodeBase64(buf);

	    System.out.println("Texto encriptado: " + new String(base64Bytes));
	}*/
	/*public static void main(String[] args) throws Exception {

	    String llave = "keyacuaplus";

	    // Texto encriptado en Base64
	    String textoEncriptado = "ji62inmB2Ps5L4zcJE84lw==";

	    // 1. Generar la misma llave MD5
	    MessageDigest md = MessageDigest.getInstance("MD5");

	    byte[] digestOfPassword = md.digest(
	        llave.getBytes("utf-8")
	    );

	    // 2. Obtener los 24 bytes para DESede
	    byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);

	    SecretKey key = new SecretKeySpec(keyBytes, "DESede");

	    // 3. Crear Cipher
	    Cipher cipher = Cipher.getInstance("DESede");

	    // 4. Modo DESENCRIPTAR
	    cipher.init(Cipher.DECRYPT_MODE, key);

	    // 5. Decodificar Base64
	    byte[] encryptedTextBytes = Base64.decodeBase64(textoEncriptado);

	    // 6. Desencriptar
	    byte[] decryptedBytes = cipher.doFinal(encryptedTextBytes);

	    // 7. Convertir a texto
	    String textoDesencriptado = new String(
	        decryptedBytes,
	        "utf-8"
	    );

	    System.out.println("Texto desencriptado: " + textoDesencriptado);
	}*/
}