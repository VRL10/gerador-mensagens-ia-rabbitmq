# Trabalho 06 - Sistema de Carga com RabbitMQ e IA embarcada

## O que este projeto entrega

Este repositório entrega os 4 containers exigidos pelo enunciado:

- `rabbitmq` com interface de administração habilitada
- `generator` publicando uma carga constante acima de 5 mensagens por segundo
- `consumer-plate` processando mensagens de placas com OCR e classificação de veículo
- `consumer-sign` processando mensagens de sinais com classificação embutida

O broker usa `topic exchange` e publica em duas rotas: `plate` e `sign`.

## Como executar

1. Suba tudo com Docker Compose:

```bash
docker compose up --build
```

2. Acesse a interface do RabbitMQ em:

```text
http://localhost:15672
```

Credenciais padrão:

- usuário: `guest`
- senha: `guest`

## Como funciona

- O gerador alterna entre placas e sinais, publicando em média 10 mensagens por segundo.
- As mensagens de placa incluem imagem PNG renderizada, texto da placa e tipo do veículo.
- As mensagens de sinal incluem imagem PNG renderizada e o tipo do sinal.
- Os consumidores usam `prefetch=1` e processam cada mensagem com atraso proposital para a fila crescer de forma visível.
- O consumidor de placas reconhece o texto por OCR por template e classifica o tipo do veículo com KNN.
- O consumidor de sinais classifica o tipo do sinal com KNN.

## Estrutura

- `shared`: contratos, utilitários de imagem, serialização e topologia RabbitMQ
- `generator`: serviço publicador
- `consumer-plate`: serviço consumidor das placas
- `consumer-sign`: serviço consumidor dos sinais

## Observação

O projeto foi escrito para ser executável de ponta a ponta em container, sem dependência de protótipo manual ou etapa parcial para demonstrar o fluxo principal.