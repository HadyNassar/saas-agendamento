package com.hady.saas.infrastructure.webconfig;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebConfig — configurações MVC globais.
 *
 * REMOVIDO: addResourceHandlers para /uploads/**
 *
 * Antes, essa classe servia a pasta de uploads como recurso estático público,
 * permitindo acesso direto às fotos sem autenticação via /uploads/...
 * Isso expunha fotos privadas de clientes (antes/depois) para qualquer pessoa
 * com a URL, anulando o @PreAuthorize do FotoPainelController.
 *
 * As fotos de clientes agora só são servidas pelo endpoint autenticado:
 *   GET /painel/fotos/clientes/{fotoId}/serve  (exige JWT)
 *
 * Quando a migração para storage em nuvem (Backblaze B2 / Cloudflare R2)
 * for concluída, este arquivo pode ser deletado completamente.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // Nenhum resource handler público — fotos servidas apenas via endpoints autenticados.
}
