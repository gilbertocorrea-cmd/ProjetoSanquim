package com.lab.projetosanquim.exception;

import com.lab.projetosanquim.dto.ErroResponse;
import com.lab.projetosanquim.dto.ErroResponse.CampoInvalido;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse camposInvalidos(MethodArgumentNotValidException exception) {
        var campos = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new CampoInvalido(error.getField(), error.getDefaultMessage()))
                .toList();
        return new ErroResponse(400, "Dados de entrada inválidos", campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse corpoInvalido() {
        return new ErroResponse(400, "Corpo da requisição inválido");
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErroResponse credenciaisInvalidas() {
        return new ErroResponse(401, "E-mail ou senha inválidos");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErroResponse acessoNegado() {
        return new ErroResponse(403, "Acesso negado para este perfil");
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse rotaInexistente() {
        return new ErroResponse(404, "Recurso não encontrado");
    }
}
