INSERT INTO setor (nome, sigla) VALUES
    ('Coordenação de Tecnologia da Informação', 'COTIC'),
    ('Coordenação de Manutenção', 'MANUT');

INSERT INTO categoria (nome, descricao, setor_id) VALUES
    ('Rede e Wi-Fi', 'Falhas de conexão cabeada ou sem fio', (SELECT id FROM setor WHERE sigla = 'COTIC')),
    ('Computadores e Software', 'Defeitos em computadores, instalação de programas', (SELECT id FROM setor WHERE sigla = 'COTIC')),
    ('Projetores e Multimídia', 'Projetores, caixas de som e equipamentos de sala', (SELECT id FROM setor WHERE sigla = 'COTIC')),
    ('Elétrica', 'Tomadas, lâmpadas e quadros de energia', (SELECT id FROM setor WHERE sigla = 'MANUT')),
    ('Climatização', 'Ar-condicionado e ventiladores', (SELECT id FROM setor WHERE sigla = 'MANUT'));

INSERT INTO usuario (nome, email, matricula, tipo, setor_id) VALUES
    ('Ana Souza', 'ana.souza@escolar.ifrn.edu.br', '20261001', 'SOLICITANTE', NULL),
    ('Bruno Lima', 'bruno.lima@ifrn.edu.br', '1900123', 'SOLICITANTE', NULL),
    ('Carla Dias', 'carla.dias@ifrn.edu.br', '1800456', 'TECNICO', (SELECT id FROM setor WHERE sigla = 'COTIC')),
    ('Diego Alves', 'diego.alves@ifrn.edu.br', '1700789', 'TECNICO', (SELECT id FROM setor WHERE sigla = 'MANUT'));
