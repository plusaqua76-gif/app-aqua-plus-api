package com.aqua.plus.api.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

public class Utils {

	public static String objectToBase64(Object obj) {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream out = new ObjectOutputStream(bos)) {
			out.writeObject(obj);
			byte[] bytes = bos.toByteArray();
			return Base64.getEncoder().encodeToString(bytes);

		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error convirtiendo objeto a Base64", e);
		}
	}

	public static Object base64ToObject(String base64) {
		try (ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
				ObjectInputStream in = new ObjectInputStream(bis)) {
			return in.readObject();

		} catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException("Error convirtiendo Base64 a objeto", e);
		}
	}

	public static Date restarMes(Date fecha, Integer meses) {
		LocalDate localDate = fecha.toInstant().atZone(ZoneId.of("America/Bogota")).toLocalDate().minusMonths(meses);

		return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}
	
	public static String formatDate(Date fecha, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		
		return sdf.format(fecha);
	}
}
