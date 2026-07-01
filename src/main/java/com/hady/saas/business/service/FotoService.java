package com.hady.saas.business.service;

import com.hady.saas.application.dto.FichaClienteDTO;
import com.hady.saas.application.dto.FotoClienteDTO;
import com.hady.saas.application.dto.FotoProfissionalDTO;
import com.hady.saas.domain.fotocliente.FotoCliente;
import com.hady.saas.domain.fotoprofissional.FotoProfissional;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.profissional.Profissional;
import com.hady.saas.infrastructure.repository.*;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FotoService {

    private final FotoClienteRepository fotoClienteRepository;
    private final FotoProfissionalRepository fotoProfissionalRepository;
    private final ClienteRepository clienteRepository;
    private final ProfissionalRepository profissionalRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final RespostaAnamneseRepository respostaAnamneseRepository;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${r2.bucket}")
    private String bucket;

    // URL da API (backend) — usada para montar links de /serve das fotos.
    // Diferente de app.base-url, que aponta para o FRONTEND (usado em e-mails e WhatsApp).
    // Se app.api-url não estiver definida, cai para app.base-url (compatibilidade).
    @Value("${app.api-url:${app.base-url:http://localhost:8080}}")
    private String baseUrl;

    // URL pública do bucket R2 (domínio público habilitado no Cloudflare,
    // ex: https://pub-xxxx.r2.dev ou domínio customizado). Usada para montar
    // a URL direta da foto de perfil do profissional, que é sempre pública
    // (diferente das fotos de cliente/portfólio, que são privadas/presigned).
    @Value("${r2.public-url}")
    private String r2PublicUrl;

    private static final int MAX_FOTOS = 10;
    private static final long MAX_TAMANHO_BYTES = 2L * 1024 * 1024; // 2 MB
    private static final Set<String> TIPOS_ACEITOS = Set.of("image/jpeg", "image/png", "image/webp");

    public FotoService(FotoClienteRepository fotoClienteRepository,
                       FotoProfissionalRepository fotoProfissionalRepository,
                       ClienteRepository clienteRepository,
                       ProfissionalRepository profissionalRepository,
                       AgendamentoRepository agendamentoRepository,
                       RespostaAnamneseRepository respostaAnamneseRepository,
                       S3Client s3Client,
                       S3Presigner s3Presigner) {
        this.fotoClienteRepository = fotoClienteRepository;
        this.fotoProfissionalRepository = fotoProfissionalRepository;
        this.clienteRepository = clienteRepository;
        this.profissionalRepository = profissionalRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.respostaAnamneseRepository = respostaAnamneseRepository;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    // ─── FICHA COMPLETA DO CLIENTE (painel autenticado) ───────────────────────

    @Transactional(readOnly = true)
    public FichaClienteDTO montarFichaCliente(Long clienteId, Long estabelecimentoId) {
        Cliente cliente = clienteRepository.findByIdAndEstabelecimentoId(clienteId, estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        FichaClienteDTO ficha = new FichaClienteDTO();
        ficha.setId(cliente.getId());
        ficha.setNome(cliente.getNome());
        ficha.setTelefone(cliente.getTelefone());
        ficha.setEmail(cliente.getEmail());
        ficha.setAlergias(cliente.getAlergias());
        ficha.setPreferencias(cliente.getPreferencias());
        ficha.setRestricoes(cliente.getRestricoes());
        ficha.setObservacoes(cliente.getObservacoes());
        ficha.setBloqueado(cliente.isBloqueado());
        ficha.setTotalFaltas(cliente.getTotalFaltas());
        ficha.setCriadoEm(cliente.getCriadoEm());

        // Histórico — últimos 20 agendamentos
        ficha.setHistorico(
                agendamentoRepository
                        .findTop20ByClienteIdOrderByDataHoraInicioDesc(clienteId, estabelecimentoId)
                        .stream()
                        .map(a -> {
                            FichaClienteDTO.AgendamentoResumoDTO r = new FichaClienteDTO.AgendamentoResumoDTO();
                            r.setId(a.getId());
                            r.setDataHoraInicio(a.getDataHoraInicio());
                            r.setStatus(a.getStatus().name());
                            r.setProfissionalNome(a.getProfissional().getNome());
                            r.setServicoNome(a.getServico().getNome());
                            r.setValor(a.getServico().getPreco() != null
                                    ? a.getServico().getPreco().doubleValue() : null);
                            return r;
                        })
                        .collect(Collectors.toList())
        );

        // Respostas de anamnese
        ficha.setAnamnese(
                respostaAnamneseRepository
                        .findByClienteIdAndEstabelecimentoId(clienteId, estabelecimentoId)
                        .stream()
                        .map(r -> {
                            FichaClienteDTO.RespostaAnamneseDTO dto = new FichaClienteDTO.RespostaAnamneseDTO();
                            dto.setPerguntaId(r.getPergunta().getId());
                            dto.setPergunta(r.getPergunta().getTexto());
                            dto.setResposta(r.getResposta());
                            dto.setDetalhe(r.getDetalhe());
                            return dto;
                        })
                        .collect(Collectors.toList())
        );

        // Fotos antes/depois — URL aponta para endpoint autenticado (stream via JWT)
        ficha.setFotos(
                fotoClienteRepository
                        .findByClienteIdAndClienteEstabelecimentoIdOrderByCriadoEmDesc(clienteId, estabelecimentoId)
                        .stream()
                        .map(f -> FotoClienteDTO.from(f, baseUrl))
                        .collect(Collectors.toList())
        );

        return ficha;
    }

    // ─── FOTOS DO CLIENTE ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FotoClienteDTO> listarFotosCliente(Long clienteId, Long estabelecimentoId) {
        clienteRepository.findByIdAndEstabelecimentoId(clienteId, estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        return fotoClienteRepository
                .findByClienteIdOrderByCriadoEmDesc(clienteId)
                .stream()
                .map(f -> FotoClienteDTO.from(f, baseUrl))
                .collect(Collectors.toList());
    }

    public FotoClienteDTO adicionarFotoCliente(Long clienteId, Long estabelecimentoId,
                                               MultipartFile arquivo, String tipo) throws IOException {
        validarArquivo(arquivo);

        Cliente cliente = clienteRepository.findByIdAndEstabelecimentoId(clienteId, estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        long total = fotoClienteRepository.countByClienteId(clienteId);
        if (total >= MAX_FOTOS) {
            throw new RuntimeException("Limite de " + MAX_FOTOS + " fotos atingido para este cliente");
        }

        FotoCliente.TipoFoto tipoFoto;
        try {
            tipoFoto = FotoCliente.TipoFoto.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Tipo inválido. Use ANTES ou DEPOIS");
        }

        // Chave no R2: clientes/{clienteId}/{uuid}.jpg
        String key = "clientes/" + clienteId + "/" + UUID.randomUUID() + ".jpg";
        uploadComprimido(arquivo, key);

        FotoCliente foto = new FotoCliente();
        foto.setCliente(cliente);
        foto.setTipo(tipoFoto);
        // Armazenamos a chave R2 no campo "caminho" para delete posterior
        foto.setCaminho(key);
        foto.setNomeOriginal(arquivo.getOriginalFilename());
        foto.setCriadoEm(LocalDateTime.now());

        fotoClienteRepository.save(foto);
        return FotoClienteDTO.from(foto, baseUrl);
    }

    // ─── SERVE ARQUIVO AUTENTICADO (fotos de clientes) ────────────────────────
    // Endpoint: GET /painel/fotos/clientes/{fotoId}/serve
    // Valida que a foto pertence ao estabelecimento antes de retornar o stream.

    @Transactional(readOnly = true)
    public Resource serveArquivoCliente(Long fotoId, Long estabelecimentoId) throws IOException {
        FotoCliente foto = fotoClienteRepository.findById(fotoId)
                .orElseThrow(() -> new RuntimeException("Foto não encontrada"));

        if (!foto.getCliente().getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Acesso negado");
        }

        byte[] bytes = downloadR2(foto.getCaminho());
        return new ByteArrayResource(bytes);
    }

    @Transactional
    public void removerFotoCliente(Long fotoId, Long estabelecimentoId) {
        FotoCliente foto = fotoClienteRepository.findById(fotoId)
                .orElseThrow(() -> new RuntimeException("Foto não encontrada"));

        if (!foto.getCliente().getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Acesso negado");
        }

        deletarR2(foto.getCaminho());
        fotoClienteRepository.delete(foto);
    }

    // ─── FOTOS DO PROFISSIONAL ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FotoProfissionalDTO> listarFotosProfissional(Long profissionalId, Long estabelecimentoId) {
        profissionalRepository.findByIdAndEstabelecimentoId(profissionalId, estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        return fotoProfissionalRepository
                .findByProfissionalIdOrderByCriadoEmDesc(profissionalId)
                .stream()
                .map(f -> FotoProfissionalDTO.from(f, baseUrl))
                .collect(Collectors.toList());
    }

    public FotoProfissionalDTO adicionarFotoProfissional(Long profissionalId, Long estabelecimentoId,
                                                         MultipartFile arquivo) throws IOException {
        validarArquivo(arquivo);

        Profissional profissional = profissionalRepository
                .findByIdAndEstabelecimentoId(profissionalId, estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        long total = fotoProfissionalRepository.countByProfissionalId(profissionalId);
        if (total >= MAX_FOTOS) {
            throw new RuntimeException("Limite de " + MAX_FOTOS + " fotos atingido para este profissional");
        }

        // Chave no R2: profissionais/{profissionalId}/{uuid}.jpg
        String key = "profissionais/" + profissionalId + "/" + UUID.randomUUID() + ".jpg";
        uploadComprimido(arquivo, key);

        FotoProfissional foto = new FotoProfissional();
        foto.setProfissional(profissional);
        foto.setCaminho(key);
        foto.setNomeOriginal(arquivo.getOriginalFilename());
        foto.setCriadoEm(LocalDateTime.now());

        fotoProfissionalRepository.save(foto);
        return FotoProfissionalDTO.from(foto, baseUrl);
    }

    // ─── FOTO DE PERFIL DO PROFISSIONAL (pública, chave fixa) ─────────────────
    // Diferente do portfólio (várias fotos, UUID, privadas): aqui é UMA foto,
    // chave fixa "profissionais/{id}/perfil.jpg" que se sobrescreve a cada
    // upload, e a URL é pública direta do R2 (sem presign), pois a foto de
    // perfil sempre aparece na página pública sem precisar de autenticação.

    @Transactional
    public String atualizarFotoPerfil(Long profissionalId, Long estabelecimentoId,
                                      MultipartFile arquivo) throws IOException {
        validarArquivo(arquivo);

        Profissional profissional = profissionalRepository
                .findByIdAndEstabelecimentoId(profissionalId, estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        String key = "profissionais/" + profissionalId + "/perfil.jpg";
        uploadComprimido(arquivo, key);

        String urlPublica = r2PublicUrl + "/" + key;
        profissional.setFotoPerfil(urlPublica);
        profissionalRepository.save(profissional);

        return urlPublica;
    }

    @Transactional
    public void removerFotoPerfil(Long profissionalId, Long estabelecimentoId) {
        Profissional profissional = profissionalRepository
                .findByIdAndEstabelecimentoId(profissionalId, estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        String key = "profissionais/" + profissionalId + "/perfil.jpg";
        deletarR2(key);
        profissional.setFotoPerfil(null);
        profissionalRepository.save(profissional);
    }

    // ─── SERVE ARQUIVO AUTENTICADO (fotos de profissionais — painel) ──────────
    // Endpoint: GET /painel/fotos/profissionais/{fotoId}/serve

    @Transactional(readOnly = true)
    public Resource serveArquivoProfissional(Long fotoId, Long estabelecimentoId) throws IOException {
        FotoProfissional foto = fotoProfissionalRepository.findById(fotoId)
                .orElseThrow(() -> new RuntimeException("Foto não encontrada"));

        if (!foto.getProfissional().getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Acesso negado");
        }

        byte[] bytes = downloadR2(foto.getCaminho());
        return new ByteArrayResource(bytes);
    }

    /**
     * Gera URL pré-assinada (1 hora) para servir fotos públicas de portfólio.
     * Chamado pelo FotoPublicoController (portfólio público do profissional).
     */
    @Transactional(readOnly = true)
    public String gerarUrlPublicaProfissional(Long fotoId, Long estabelecimentoId) {
        FotoProfissional foto = fotoProfissionalRepository.findById(fotoId)
                .orElseThrow(() -> new RuntimeException("Foto não encontrada"));

        if (!foto.getProfissional().getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Acesso negado");
        }

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(r -> r.bucket(bucket).key(foto.getCaminho()))
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    @Transactional
    public void removerFotoProfissional(Long fotoId, Long estabelecimentoId) {
        FotoProfissional foto = fotoProfissionalRepository.findById(fotoId)
                .orElseThrow(() -> new RuntimeException("Foto não encontrada"));

        if (!foto.getProfissional().getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Acesso negado");
        }

        deletarR2(foto.getCaminho());
        fotoProfissionalRepository.delete(foto);
    }

    // ─── HELPERS R2 ───────────────────────────────────────────────────────────

    /**
     * Comprime com Thumbnailator (máx 1200px, 80% qualidade, JPG) e faz putObject no R2.
     */
    private void uploadComprimido(MultipartFile arquivo, String key) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Thumbnails.of(arquivo.getInputStream())
                .width(1200)
                .keepAspectRatio(true)
                .outputQuality(0.80)
                .outputFormat("jpg")
                .toOutputStream(baos);

        byte[] bytes = baos.toByteArray();

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("image/jpeg")
                .contentLength((long) bytes.length)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(bytes));
    }

    /**
     * Faz getObject do R2 e retorna os bytes do arquivo.
     */
    private byte[] downloadR2(String key) throws IOException {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getRequest)) {
            return response.readAllBytes();
        }
    }

    /**
     * Remove objeto do R2. Não lança exceção se o objeto não existir.
     */
    private void deletarR2(String key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteRequest);
        } catch (NoSuchKeyException ignored) {
            // Objeto já não existe no R2 — ok
        }
    }

    // ─── VALIDAÇÃO ────────────────────────────────────────────────────────────

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RuntimeException("Arquivo vazio");
        }
        String contentType = arquivo.getContentType();
        if (contentType == null || !TIPOS_ACEITOS.contains(contentType)) {
            throw new RuntimeException("Formato inválido. Use JPG, PNG ou WEBP");
        }
        if (arquivo.getSize() > MAX_TAMANHO_BYTES) {
            throw new RuntimeException("Arquivo muito grande. Máximo: 2MB");
        }
    }
}