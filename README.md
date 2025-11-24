# 🎙️ Cloud Audio Recorder (Java 25)

Um gravador de áudio "invisível" (System Tray) de alta performance, desenvolvido com **Java 25**. Projetado para rodar continuamente em background, capturando áudio em chunks e sincronizando automaticamente com a nuvem (S3) através de URLs assinadas.

![Java](https://img.shields.io/badge/Java-25-ed8b00?style=for-the-badge&logo=openjdk)
![Build](https://img.shields.io/badge/Build-Maven-C71A36?style=for-the-badge&logo=apachemaven)
![Architecture](https://img.shields.io/badge/Architecture-Clean%20Code-blue?style=for-the-badge)

## 🚀 Funcionalidades

* **Modo "Ghost":** Roda minimizado na bandeja do sistema (System Tray), sem janelas intrusivas.
* **Zero-Blocking I/O:** Utiliza **Virtual Threads** (Project Loom) para garantir que o salvamento em disco e o upload de rede nunca bloqueiem a captura de áudio.
* **Cloud Native:** Envia arquivos `.wav` automaticamente para um bucket S3 usando *Pre-signed URLs*.
* **Resiliência (Retry Pattern):**
    * Se a internet cair, os arquivos acumulam no disco local.
    * Um agendador (Cron Job) roda a cada 30 minutos (e na inicialização) para reprocessar falhas antigas.
* **Segurança de Dados:**
    * **Shutdown Hook:** Salva o arquivo corretamente mesmo se o PC for desligado abruptamente.
    * **Atomicidade:** O arquivo local só é deletado após a confirmação de sucesso (HTTP 200) da nuvem.

## 🛠️ Stack Tecnológica

O projeto explora o estado da arte do ecossistema Java:

* **Java 25 Preview Features:** Uso intensivo de *Virtual Threads* para concorrência estruturada.
* **JavaFX + AWT:** Abordagem híbrida para gerenciar o ciclo de vida da aplicação sem interface gráfica visível.
* **Java HTTP Client (Nativo):** Comunicação de rede assíncrona (HTTP/2) sem dependências externas pesadas (como Apache HttpClient).
* **JPackage + WiX Toolset:** Empacotamento para instalador nativo `.msi` (Windows) com JRE embutido.

## 🏗️ Arquitetura (Clean Code)

O projeto foi refatorado seguindo princípios **SOLID**, separando responsabilidades em camadas de serviço:

```text
src/main/java/dev/jlucasbs/gravador/
├── config/
│   └── AppConfig.java       # (Record) Configurações centralizadas (URLs, Timeouts)
├── service/
│   ├── AudioService.java    # Captura de microfone e gerenciamento de buffer
│   ├── NetworkService.java  # Comunicação com Backend e S3
│   └── StorageService.java  # I/O de disco, nomenclatura e limpeza
├── ui/
│   └── TrayManager.java     # Gerenciamento do ícone e menu da bandeja
└── MainApp.java             # Ponto de entrada e Injeção de Dependência
```

## ⚙️ Configuração
Todas as configurações sensíveis estão centralizadas em ```src/main/java/dev/jlucasbs/gravador/config/AppConfig.java.```

Você pode alterar:
- Backend URL: Endpoint que gera a URL assinada do S3.
- Chunk Duration: Tempo de cada arquivo de áudio (Padrão: 600s / 10 min).
- Retry Interval: Frequência de verificação de arquivos pendentes.

```java
// Exemplo em AppConfig.java
return new AppConfig(
    "[https://api.seubackend.com/generate-signed-url](https://api.seubackend.com/generate-signed-url)",
    600, // Tempo em segundos
    ...
);
```

## 📦 Como Rodar e Compilar
Pré-requisitos
1. JDK 25 (Recomendado: Azul Zulu ou OpenJDK EA) instalado e configurado.
2. Maven 3.9+.

### Rodando em Desenvolvimento
Como usamos Preview Features, o Maven já está configurado para passar as flags ```--enable-preview.```

```bash
mvn clean javafx:run
```

## 📝 Backend Esperado
Para o upload funcionar, seu backend deve responder ao seguinte contrato:
- Request: GET ```https://api.seubackend.com/generate-signed-url?filename=Usuario_Data_p001.wav```
- Response (200 OK): Deve retornar apenas a String da URL assinada (PUT) do S3.