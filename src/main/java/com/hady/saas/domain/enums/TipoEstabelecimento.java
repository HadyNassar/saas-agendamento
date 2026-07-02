package com.hady.saas.domain.enums;

public enum TipoEstabelecimento {
    // Beleza
    SALAO("Salão de Beleza"),
    BARBEARIA("Barbearia"),
    CLINICA_ESTETICA("Clínica de Estética"),
    STUDIO_MANICURE("Studio de Manicure"),
    SPA("Spa"),
    STUDIO_SOBRANCELHA("Studio de Sobrancelha"),
    // Saúde & Bem-estar
    MASSOTERAPIA("Massoterapia"),
    PODOLOGIA("Podologia"),
    NUTRICIONISTA("Nutricionista"),
    // Esporte & Fitness
    PERSONAL_TRAINER("Personal Trainer"),
    STUDIO_PILATES("Studio de Pilates"),
    ACADEMIA_LUTAS("Academia de Lutas"),
    YOGA_MEDITACAO("Yoga & Meditação"),
    // Genérico
    OUTRO("Estabelecimento");

    private final String label;

    TipoEstabelecimento(String label) {
        this.label = label;
    }

    // Label exibido publicamente (ex: badge na página de agendamento).
    // Trocar o tipo no painel muda esse texto automaticamente em todo lugar
    // que o consome, sem precisar editar nada manualmente.
    public String getLabel() {
        return label;
    }
}