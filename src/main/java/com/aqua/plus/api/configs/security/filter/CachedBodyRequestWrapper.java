package com.aqua.plus.api.configs.security.filter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * Envuelve el HttpServletRequest original y reemplaza su body con el
 * payload ya descifrado.
 *
 * Necesario porque un InputStream de Servlet solo se puede leer una vez.
 * El filtro lee el sobre cifrado, valida, descifra y reengloba la petición
 * con el JSON plano — los controladores reciben @RequestBody normal.
 */
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;

    public CachedBodyRequestWrapper(HttpServletRequest request, String decryptedBody) {
        super(request);
        this.body = decryptedBody.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override public boolean isFinished()                         { return bais.available() == 0; }
            @Override public boolean isReady()                           { return true; }
            @Override public void setReadListener(ReadListener listener) { /* no-op */ }
            @Override public int read() throws IOException               { return bais.read(); }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(
            new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public int getContentLength() {
        return body.length;
    }

    @Override
    public long getContentLengthLong() {
        return body.length;
    }

    @Override
    public String getContentType() {
        return "application/json";
    }
}
