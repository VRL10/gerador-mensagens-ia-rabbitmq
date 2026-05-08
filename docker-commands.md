# Docker Commands for this Project

## 1) Criar a rede externa
```bash
docker network create --driver bridge gerador-mensagens-ia-rabbitmq_trabalho06-net
```

## 2) Rodar os containers do projeto principal
No diretório raiz do projeto:
```bash
docker compose up -d
```

## 3) Rodar o consumer2
No diretório `consumer2`:
```bash
docker compose --profile real up -d
```

## 4) Parar os containers
No diretório raiz:
```bash
docker compose down
```

### Remover volumes também
```bash
docker compose down -v
```

## 5) Verificar se o consumer2 está rodando
```bash
docker ps --filter name=consumer2-sign-classifier-real
```

## 6) Verificar a rede e os containers conectados
```bash
docker network inspect gerador-mensagens-ia-rabbitmq_trabalho06-net
```

## 7) Ver logs do consumer2
```bash
docker logs -f consumer2-sign-classifier-real
```

### Filtrar apenas classificações
```bash
docker logs -f consumer2-sign-classifier-real | grep -i "Classificação"
```

ou
```bash
docker logs -f consumer2-sign-classifier-real | grep -i "Sinal"
```

## 8) Ver logs do compose principal
No diretório raiz:
```bash
docker compose logs -f
```

## 9) Verificar a fila no RabbitMQ
Abrir no navegador:
```text
http://localhost:15672
```
Usuário/senha:
```text
guest / guest
```

## Configuração importante do consumer2
O `consumer2` deve usar estas variáveis:
- `EXCHANGE_NAME=trabalho06.topic`
- `QUEUE_NAME=trabalho06.signs`
- `ROUTING_KEY=sign`
