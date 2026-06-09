package com.hady.saas.application.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ─── Estrutura padrão de erro ─────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> erro(HttpStatus status, String codigo, String mensagem, HttpServletRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("codigo", codigo);
        body.put("mensagem", mensagem);
        body.put("path", req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    // ─── Regras de negócio (RuntimeException com mensagem amigável) ───────────

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleNegocio(RuntimeException ex, HttpServletRequest req) {
        String msg = ex.getMessage();

        // Erros de plano/acesso → 403
        // FIX: "não pertence a este estabelecimento" é mais específico que "não pertence"
        // para evitar falso positivo em mensagens futuras que contenham a substring genérica.
        if (msg != null && (
                msg.contains("não pertence a este estabelecimento") ||
                        msg.contains("Acesso negado") ||
                        msg.contains("No plano Platinum") ||
                        msg.contains("Relatório financeiro disponível"))) {
            log.warn("[NEGOCIO-403] {} | path={}", msg, req.getRequestURI());
            return erro(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", msg, req);
        }

        // Conflito de recurso → 409
        if (msg != null && (
                msg.contains("já cadastrado") ||
                        msg.contains("já está em uso") ||
                        msg.contains("já preenchida") ||
                        msg.contains("já possui") ||
                        msg.contains("já utilizado") ||
                        msg.contains("já foi avaliado") ||
                        msg.contains("conflita"))) {
            log.warn("[NEGOCIO-409] {} | path={}", msg, req.getRequestURI());
            return erro(HttpStatus.CONFLICT, "CONFLITO", msg, req);
        }

        // Recurso não encontrado → 404
        if (msg != null && (
                msg.contains("não encontrado") ||
                        msg.contains("não encontrada"))) {
            log.warn("[NEGOCIO-404] {} | path={}", msg, req.getRequestURI());
            return erro(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", msg, req);
        }

        // Pagamento / plano → 402
        if (msg != null && (
                msg.contains("plano ativo") ||
                        msg.contains("Trial já utilizado") ||
                        msg.contains("aceita pagamento") ||
                        msg.contains("não está aceitando agendamentos"))) {
            log.warn("[NEGOCIO-402] {} | path={}", msg, req.getRequestURI());
            return erro(HttpStatus.PAYMENT_REQUIRED, "PAGAMENTO_NECESSARIO", msg, req);
        }

        // FIX: limite de usuários/ADMINs → 422 com código próprio.
        // Este bucket DEVE vir antes do de arquivo ("Limite de fotos atingido") para que
        // "Limite de usuários" e "Limite de ADMINs" não caiam em ERRO_ARQUIVO.
        // Ordem importa: mais específico antes do mais genérico.
        if (msg != null && (msg.contains("Limite de usuários") || msg.contains("Limite de ADMINs"))) {
            log.warn("[PLANO-422] {} | path={}", msg, req.getRequestURI());
            return erro(HttpStatus.UNPROCESSABLE_ENTITY, "LIMITE_PLANO", msg, req);
        }

        // Erros de integração externa → 502
        if (msg != null && (
                msg.contains("Mercado Pago") ||
                        msg.contains("Evolution API") ||
                        msg.contains("QR Code"))) {
            log.error("[INTEGRACAO-502] {} | path={}", msg, req.getRequestURI());
            return erro(HttpStatus.BAD_GATEWAY, "ERRO_INTEGRACAO",
                    "Erro ao comunicar com serviço externo. Tente novamente em instantes.", req);
        }

        // Erros de arquivo/upload → 400
        // FIX: removido "Limite de" genérico deste bucket — seria ambíguo com limite de plano.
        // Mantido apenas "fotos atingido" que é suficientemente específico.
        if (msg != null && (
                msg.contains("Arquivo") ||
                        msg.contains("Formato inválido") ||
                        msg.contains("fotos atingido"))) {
            log.warn("[UPLOAD-400] {} | path={}", msg, req.getRequestURI());
            return erro(HttpStatus.BAD_REQUEST, "ERRO_ARQUIVO", msg, req);
        }

        // Erros de agendamento → 422
        // FIX: adicionado "data passada" e "Duração do serviço" que antes caíam no fallback 400
        // sem categoria. "Horario" e "almoco" sem acento cobrem as mensagens lançadas
        // pelo AgendamentoService (que usa strings sem acento por consistência histórica).
        if (msg != null && (
                msg.contains("Apenas agendamentos") ||
                        msg.contains("Cliente bloqueado") ||
                        msg.contains("Estabelecimento fechado") ||
                        msg.contains("Horario") ||
                        msg.contains("fora do funcionamento") ||
                        msg.contains("almoco") ||
                        msg.contains("data passada") ||
                        msg.contains("limite de antecipação") ||
                        msg.contains("Duração do serviço inválida"))) {
            log.warn("[AGENDAMENTO-422] {} | path={}", msg, req.getRequestURI());
            return erro(HttpStatus.UNPROCESSABLE_ENTITY, "ERRO_AGENDAMENTO", msg, req);
        }

        // Erros de senha/segurança → 400
        if (msg != null && (
                msg.contains("Senha atual incorreta") ||
                        msg.contains("senhas não coincidem") ||
                        msg.contains("criptografar") ||
                        msg.contains("descriptografar"))) {
            log.warn("[SEGURANCA-400] {} | path={}", msg, req.getRequestURI());
            return erro(HttpStatus.BAD_REQUEST, "ERRO_SEGURANCA", msg, req);
        }

        // Fallback genérico → 400 (sem vazar detalhe interno)
        log.error("[RUNTIME] Erro não mapeado | path={} | mensagem={}", req.getRequestURI(), msg, ex);
        return erro(HttpStatus.BAD_REQUEST, "ERRO_REQUISICAO",
                msg != null ? msg : "Requisição inválida.", req);
    }

    // ─── Validação de @Valid / @Validated ─────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String campos = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("[VALIDACAO-400] {} | path={}", campos, req.getRequestURI());
        return erro(HttpStatus.BAD_REQUEST, "VALIDACAO", campos, req);
    }

    // ─── Parâmetro obrigatório ausente ────────────────────────────────────────

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleParametroFaltando(MissingServletRequestParameterException ex, HttpServletRequest req) {
        String msg = "Parâmetro obrigatório ausente: " + ex.getParameterName();
        log.warn("[PARAM-400] {} | path={}", msg, req.getRequestURI());
        return erro(HttpStatus.BAD_REQUEST, "PARAMETRO_FALTANDO", msg, req);
    }

    // ─── Tipo de parâmetro incorreto (ex: string onde espera Long) ───────────

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTipoErrado(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String msg = "Valor inválido para o parâmetro '" + ex.getName() + "'";
        log.warn("[TIPO-400] {} | path={}", msg, req.getRequestURI());
        return erro(HttpStatus.BAD_REQUEST, "TIPO_INVALIDO", msg, req);
    }

    // ─── Upload acima do limite configurado no Spring ─────────────────────────

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUploadGrande(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        log.warn("[UPLOAD-413] Arquivo muito grande | path={}", req.getRequestURI());
        return erro(HttpStatus.PAYLOAD_TOO_LARGE, "ARQUIVO_MUITO_GRANDE",
                "O arquivo enviado ultrapassa o tamanho máximo permitido.", req);
    }

    // ─── Acesso negado pelo Spring Security (@PreAuthorize) ───────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAcessoNegado(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("[SECURITY-403] Acesso negado | path={}", req.getRequestURI());
        return erro(HttpStatus.FORBIDDEN, "ACESSO_NEGADO",
                "Você não tem permissão para realizar esta ação.", req);
    }

    // ─── Qualquer outra exceção não prevista → 500 sem expor detalhe ─────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeral(Exception ex, HttpServletRequest req) {
        log.error("[INTERNO-500] Erro inesperado | path={}", req.getRequestURI(), ex);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                "Ocorreu um erro interno. Nossa equipe foi notificada.", req);
    }
}
