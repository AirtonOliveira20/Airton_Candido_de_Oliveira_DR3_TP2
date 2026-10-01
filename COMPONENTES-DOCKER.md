# Componentes Docker utilizados – Sistema de Biblioteca

Visão geral do projeto:

```
           Windows (host)                        Docker Engine
  ┌───────────────────────────┐   ┌──────────────────────────────────────────────┐
  │ navegador / PowerShell    │   │  rede "biblioteca-net" (bridge)              │
  │                           │   │                                              │
  │ localhost:18081 ──────────┼───┼─► container livros-service      :8081       │
  │                           │   │          ▲                                   │
  │ localhost:18082 ──────────┼───┼─► container emprestimos-service :8082       │
  └───────────────────────────┘   │          └── http://livros-service:8081 ─────┘
                                  └──────────────────────────────────────────────┘
```

---

## 1. Dockerfile

**Função:** é a receita, em texto, de como montar uma imagem: imagem base, arquivos a copiar, comandos de build, porta e comando de inicialização. Cada instrução gera uma camada, e o Docker reaproveita do cache as camadas que não mudaram.

**No projeto:** há um Dockerfile por microsserviço (`livros-service/Dockerfile` e `emprestimos-service/Dockerfile`), ambos com *multi-stage build*:

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build   # estágio 1: tem Maven + JDK
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B             # camada de dependências (fica em cache)
COPY src ./src
RUN mvn package -DskipTests -B               # gera target/app.jar

FROM eclipse-temurin:17-jre                  # estágio 2: só o JRE
WORKDIR /app
COPY --from=build /app/target/app.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

O primeiro estágio compila o código e o segundo leva só o `app.jar` e o JRE. A imagem final fica menor e não carrega Maven nem código-fonte. O Dockerfile do `emprestimos-service` também define `ENV LIVROS_SERVICE_URL=http://livros-service:8081`.

---

## 2. Image (imagem)

**Função:** é o pacote **imutável** gerado a partir do Dockerfile. Contém o sistema de arquivos (camadas), a aplicação, as dependências e os metadados de execução (porta, variáveis de ambiente, `ENTRYPOINT`). Serve de molde para criar containers e pode ser versionada com *tags* e enviada para um registry (Docker Hub, por exemplo).

**No projeto:** foram geradas 2 imagens:

```powershell
docker build -t biblioteca/livros-service:1.0      .\livros-service
docker build -t biblioteca/emprestimos-service:1.0 .\emprestimos-service
docker images "biblioteca/*"
```

`biblioteca/livros-service` é o nome e `1.0` é a tag (versão).

---

## 3. Container

**Função:** é uma **instância em execução** de uma imagem. É um processo isolado com sistema de arquivos, rede e variáveis próprias. Sobre as camadas somente-leitura da imagem, o Docker cria uma camada gravável que pertence só àquele container. Ele pode ser iniciado, parado, removido e recriado.

**No projeto:** foram criados 2 containers, um para cada imagem:

```powershell
docker run -d --name livros-service --network biblioteca-net -p 18081:8081 biblioteca/livros-service:1.0
docker run -d --name emprestimos-service --network biblioteca-net -p 18082:8082 `
    -e LIVROS_SERVICE_URL=http://livros-service:8081 biblioteca/emprestimos-service:1.0
docker ps --filter "network=biblioteca-net"
```

Os dados dos livros e dos empréstimos ficam na memória do processo Java de cada container. Por isso, um `docker rm -f livros-service` seguido de um novo `docker run` volta aos 3 livros iniciais.

---

## 4. Docker Engine

**Função:** é o motor que faz tudo funcionar. Tem três partes:

- o **daemon** (`dockerd`), que constrói imagens, cria e executa containers, gerencia redes e volumes;
- a **API REST** do daemon;
- o **CLI** (`docker`), que envia os comandos para essa API.

O isolamento usa recursos do kernel Linux: *namespaces* separam processos, rede e arquivos, e *cgroups* limitam CPU e memória.

**No projeto:** no Windows, o Docker Desktop roda o Engine dentro de uma VM Linux (WSL 2), por isso aparece `docker:desktop-linux` no build. Cada `docker build`, `docker run` ou `docker network create` do script é o CLI pedindo ao daemon que execute a ação. O DNS interno que resolve `livros-service` também é um serviço do Engine.

---

## 5. Port Mapping (mapeamento de portas)

**Função:** a rede de um container é isolada, então por padrão nada de fora consegue acessá-lo. A opção `-p PORTA_HOST:PORTA_CONTAINER` cria um redirecionamento: o que chega na porta do host é encaminhado para a porta do container. `EXPOSE` no Dockerfile apenas documenta a porta; quem abre de verdade é o `-p`.

**No projeto:**

| Container | Porta interna | `-p` | Acesso pelo Windows |
|---|---|---|---|
| livros-service | 8081 | `-p 18081:8081` | `http://localhost:18081/livros` |
| emprestimos-service | 8082 | `-p 18082:8082` | `http://localhost:18082/emprestimos` |

Usamos 18081/18082 no host porque a porta 8081 do Windows já estava ocupada por outro projeto (o primeiro `docker run` falhou com *"ports are not available"*). Dentro do container nada mudou.

O mapeamento serve **só para quem está fora do Docker**. A comunicação entre os containers usa direto a porta interna (`livros-service:8081`) e funcionaria até sem `-p`.

---

## 6. Network (rede)

**Função:** liga containers entre si. Numa rede *bridge* criada pelo usuário, cada container recebe um IP interno e o Engine oferece um **DNS embutido** (`127.0.0.11`) que resolve **o nome do container** para esse IP. A rede *bridge* padrão (usada quando não se passa `--network`) não tem essa resolução por nome.

**No projeto:**

```powershell
docker network create biblioteca-net                         # cria a rede
docker run ... --network biblioteca-net --name livros-service ...
docker exec emprestimos-service getent hosts livros-service   # mostra o IP resolvido, ex.: 172.18.0.2
```

O `emprestimos-service` chama `http://livros-service:8081/livros/{id}` (classe `LivrosClient`). O nome `livros-service` vem do `--name` e é resolvido pelo DNS da rede. Por isso o código nunca usa um IP fixo, que mudaria a cada recriação do container, e nunca usa `localhost`, que dentro de um container aponta para ele mesmo.

---

## Qual é a diferença entre uma imagem e um container?

A **imagem** é o modelo estático e somente-leitura; o **container** é esse modelo em execução. A analogia em Java: a imagem é a **classe** e o container é o **objeto** (`new`).

| | Imagem | Container |
|---|---|---|
| O que é | Pacote com app + dependências + configuração | Processo isolado rodando a partir da imagem |
| Estado | Imutável (camadas somente-leitura) | Tem estado: camada gravável, memória, IP, logs |
| Criado com | `docker build` | `docker run` (ou `docker create` + `start`) |
| Ciclo de vida | Existe até ser removida (`docker rmi`) | Criado, rodando, parado, removido (`docker rm`) |
| Quantidade | Uma imagem... | ...pode gerar vários containers |
| No projeto | `biblioteca/livros-service:1.0` | container `livros-service` (porta 18081) |

Exemplo prático: com a mesma imagem `biblioteca/livros-service:1.0` dá para subir um segundo container independente, com outra memória e outra porta:

```powershell
docker run -d --name livros-service-2 --network biblioteca-net -p 18091:8081 biblioteca/livros-service:1.0
```

Um livro cadastrado em `localhost:18081` não aparece em `localhost:18091`. A imagem é a mesma, mas cada container tem seu próprio estado. Para limpar depois: `docker rm -f livros-service-2`.
