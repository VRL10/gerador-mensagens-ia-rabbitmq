# Consumer 2 – Traffic Sign Classifier

Serviço Java que **consome mensagens do RabbitMQ** com routing key `sign` e as classifica usando um modelo **Smile RandomForest**.

---

## Estrutura do Projeto

```
consumer2/
├── src/main/java/com/traffic/consumer2/
│   ├── Main.java                          # Ponto de entrada e loop principal
│   ├── config/
│   │   └── AppConfig.java                 # Todas as variáveis de ambiente
│   ├── messaging/
│   │   ├── MessageSource.java             # Interface (ponto de troca mock↔real)
│   │   ├── MockMessageSource.java         # Gerador interno (sem infra)
│   │   ├── RabbitMQMessageSource.java     # Consumidor RabbitMQ real
│   │   └── MessageSourceFactory.java     # Fábrica que decide qual usar
│   ├── model/
│   │   ├── TrafficSignMessage.java        # Mensagem recebida
│   │   └── SignClassification.java       # Resultado da classificação
│   ├── ai/
│   │   └── SignClassifier.java           # Modelo Smile RandomForest
│   └── util/
│       └── FeatureExtractor.java         # Extrai features da mensagem/imagem
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

---

## Como Rodar

### Modo mock (sem nenhuma infra)

```bash
docker compose up --build
```

O serviço gera suas próprias mensagens internamente a cada `MOCK_RATE_MS` ms.

### Modo real (com RabbitMQ)

```bash
docker compose --profile real up --build
```

Sobe o RabbitMQ junto e o consumer conecta nele automaticamente.

### Integração no sistema completo

Quando o Gerador e o Consumer 1 estiverem prontos, basta:

1. Garantir que todos os containers usam a network `traffic-net`
2. Setar `USE_MOCK=false` no Consumer 2
3. O Gerador deve publicar com routing key `sign` no exchange `traffic.exchange`

---

## Variáveis de Ambiente

| Variável          | Padrão              | Descrição                              |
|-------------------|---------------------|----------------------------------------|
| `USE_MOCK`        | `true`              | `false` → conecta ao RabbitMQ real     |
| `MOCK_RATE_MS`    | `1500`              | Intervalo entre msgs mock (ms)         |
| `RABBITMQ_HOST`   | `rabbitmq`          | Host do broker                         |
| `RABBITMQ_PORT`   | `5672`              | Porta AMQP                             |
| `RABBITMQ_USER`   | `guest`             |                                        |
| `RABBITMQ_PASS`   | `guest`             |                                        |
| `RABBITMQ_VHOST`  | `/`                 |                                        |
| `EXCHANGE_NAME`   | `traffic.exchange`  | Exchange do tipo topic                 |
| `QUEUE_NAME`      | `sign.queue`        | Fila vinculada ao routing key `sign`   |
| `ROUTING_KEY`     | `sign`              | Routing key esperada                   |

---

## Topologia RabbitMQ Esperada

```
Producer → [traffic.exchange] --sign.#--> [sign.queue] → Consumer 2
                                --plate.#-> [plate.queue] → Consumer 1
```

O Consumer 2 **cria automaticamente** o exchange, a fila e o binding se não existirem, então não é necessário configurar o RabbitMQ manualmente.

---

## Modelo de IA

**Algoritmo:** Random Forest (Smile 3.0.2)

**Features de entrada:**

| Feature      | Tipo   | Valores                                      |
|--------------|--------|----------------------------------------------|
| `shape`      | double | 0=circular, 1=triangular, 2=octagonal, 3=retangular |
| `color`      | double | 0=vermelho, 1=azul, 2=amarelo, 3=branco      |
| `hasNumber`  | double | 0=não, 1=sim                                 |
| `hasArrow`   | double | 0=não, 1=sim                                 |
| `brightness` | double | 0.0 – 1.0                                    |

**Classes de saída:**

| Label           | Descrição                        |
|-----------------|----------------------------------|
| `STOP`          | Pare                             |
| `SPEED_LIMIT`   | Velocidade Máxima                |
| `NO_TURN_LEFT`  | Proibido Virar à Esquerda        |
| `NO_TURN_RIGHT` | Proibido Virar à Direita         |
| `NO_ENTRY`      | Proibido Entrar                  |
| `YIELD`         | Ceda a Passagem                  |
| `WARNING_AHEAD` | Advertência Geral                |
| `PEDESTRIAN`    | Passagem de Pedestres            |
| `UNKNOWN`       | Desconhecido                     |

O modelo é treinado em tempo de inicialização com **dados sintéticos** que codificam as regras do CTB. Para usar um dataset real (ex.: GTSRB), substitua os arrays `TRAINING_FEATURES` e `TRAINING_LABELS` em `SignClassifier.java`.

---

## Fluxo de Processamento

```
MessageSource.nextMessage()
       │
       ▼
FeatureExtractor.extract()
  ├─ imageDataBase64 presente → extractFromImage() [stub, implementar]
  └─ metadata JSON presente   → extractFromMetadata() [ativo no mock]
       │
       ▼
SignClassifier.classify()
  └─ RandomForest.predict() → Label + confidence
       │
       ▼
MessageSource.acknowledge()  ou  reject()
```

---

## Formato das Mensagens

JSON esperado no campo `metadata` (ou corpo da mensagem):

```json
{
  "shape": 2,
  "color": 0,
  "hasNumber": 0,
  "hasArrow": 0,
  "brightness": 0.75
}
```

Campos opcionais na raiz da mensagem (quando vem do RabbitMQ):

```json
{
  "messageId": "uuid",
  "timestamp": "2024-01-01T12:00:00Z",
  "routingKey": "sign",
  "imageDataBase64": "base64...",
  "metadata": "{...}"
}
```

---

## Dependências Principais

| Dependência     | Versão | Uso                     |
|-----------------|--------|-------------------------|
| smile-core      | 3.0.2  | RandomForest classifier |
| amqp-client     | 5.20.0 | Conexão RabbitMQ        |
| jackson-databind| 2.16.1 | Deserialização JSON     |
| logback-classic | 1.4.14 | Logging                 |
