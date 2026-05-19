CREATE TABLE estabelecimento (
                                 id SERIAL PRIMARY KEY,
                                 nome VARCHAR(255) NOT NULL,
                                 email VARCHAR(255),
                                 telefone VARCHAR(20),
                                 endereco VARCHAR(255),
                                 slug VARCHAR(100) UNIQUE
);