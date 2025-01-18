package com.example.diplom.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException, ServletException {

        // Базовое сообщение - не удаляем!
        // (Эта часть всегда будет присутствовать в ответе.)
        StatusResponse status = new StatusResponse(
                "UNAUTHORIZED",
                "Требуется аутентификация для доступа к этому ресурсу."
        );

        // Получаем детальное сообщение об ошибке от Spring Security
        String detailMessage = authException.getMessage();

        // Анализируем detailMessage, чтобы добавить конкретное пояснение
        if (detailMessage != null) {
            // Пример ключевых слов (можете расширить по желанию)
            if (detailMessage.contains("expired")) {
                // Случай, когда срок действия токена истёк
                // Добавляем конкретное сообщение, не удаляя старое.
                status.setMessage(status.getMessage() + " Срок действия токена истек!");
            } else if (detailMessage.contains("invalid")
                    || detailMessage.contains("Invalid")
                    || detailMessage.contains("signature")) {
                // Случай, когда подпись токена неверна или сам токен некорректен
                status.setMessage(status.getMessage() + " Неверный JWT-токен!");
            } else {
                // Любые другие детализированные сообщения
                status.setMessage(status.getMessage() + " Детали: " + detailMessage);
            }
        }

        // Устанавливаем код ответа и Content-Type
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // Пишем JSON в выходной поток
        objectMapper.writeValue(response.getOutputStream(), status);
    }
}
