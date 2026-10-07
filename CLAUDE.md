# SuperInvoice — guia do código

App Android de faturas (invoice maker). Kotlin + Jetpack Compose, um único módulo `app`.
Pacote `online.isdevapps.superinvoice` (applicationId); o código vive em `com.example.superinvoice`.

Leia também: `DESIGN_GUIDELINES.md` (cores, tipografia, componentes) e, para trabalho em
andamento, as specs em `docs/`.

## Stack

| Peça | Versão / onde |
| --- | --- |
| Kotlin / KSP | 2.0.21 / 2.0.21-1.0.28 |
| AGP | 8.13.2, `minSdk 24`, `targetSdk 36`, JVM 11 |
| UI | Compose BOM 2024.09.00, Material 3 |
| Banco | Room 2.6.1, schema exportado em `app/schemas/` |
| Preferências | DataStore Preferences (`SettingsRepository`) |
| DI | Hilt 2.51 |
| Assinatura | RevenueCat (`BillingManager`) |
| Analytics / crash | Firebase Analytics + Crashlytics (`data/analytics/`) |
| Imagens | Coil |
| PDF | Gerador próprio sobre `android.graphics.pdf.PdfDocument` (`data/pdf/`) |

O Firebase BOM está preso em 34.10.0 de propósito (ver comentário em `gradle/libs.versions.toml`):
subir o Firebase exige subir Kotlin, kotlin-compose e KSP juntos.

## Estrutura

```
data/
  Client, Invoice, InvoiceItem, ProductService   entidades Room
  database/   AppDatabase, Converters, dao/, entities/InvoiceStatus
  repository/ ClientRepository, InvoiceRepository, ProductServiceRepository, SettingsRepository
  pdf/        InvoicePdfGenerator (3 templates), InvoicePager, InvoiceStyle/Accent/Font/Paints
  billing/    BillingManager, PremiumStatus
  analytics/  AnalyticsManager, AnalyticsEvent (enums de dimensão), CrashReporter, ConsentRegion
di/           DatabaseModule (DAOs)
ui/
  navigation/ AppNavigation (navegação própria, ver abaixo)
  screens/    uma tela por arquivo
  viewmodel/  um @HiltViewModel por tela
  components/ componentes reutilizáveis com prefixo Inv* (InvButton, InvField, InvScaffold…)
  theme/      Color, Type, Shape, Dimens
util/         CurrencyUtils
```

Fluxo de dados: tela → ViewModel (`MutableStateFlow` privado + `StateFlow` público) →
Repository → DAO/DataStore. Telas não falam com repositórios diretamente.

## Convenções

- **Navegação não usa Navigation Compose.** `AppNavigation` guarda `currentScreen` (enum
  `Screen`) e uma pilha `navigationStack` em `rememberSaveable`; argumentos são estados como
  `selectedInvoiceId`. Tela nova = valor novo no enum `Screen` + ramo no `when` + função
  `navigateTo…` quando precisar de argumento. Respeite a lógica do `BackHandler` documentada ali.
- **Strings sempre em recurso**, nas três línguas: `values/` (EN), `values-es/`, `values-pt-rBR/`.
  Os três arquivos têm o mesmo conjunto de chaves; adicione nos três no mesmo commit.
- **Componentes:** reutilize os `Inv*` de `ui/components` antes de criar outro. Cores, tamanhos e
  cantos vêm de `ui/theme`, não de literais.
- **Comentários e KDoc em português**, explicando o porquê (veja `PremiumStatus`, `InvoicePager`).
- **Dinheiro hoje é `Double`.** O símbolo vem de `getCurrencySymbol` (`util/CurrencyUtils.kt`) e o
  valor de `String.format("%.2f")`; não introduza outra forma de arredondar sem centralizar
  (ver `docs/spec-onda-1.md`).

## Regras que já quebraram antes

- **Premium é assíncrono.** `PremiumStatus.Unknown` existe porque o RevenueCat demora a responder.
  Decisão irreversível (renderizar PDF, liberar criação de fatura) espera
  `BillingManager.awaitPremiumStatus()`; só UI pode ler `isPremium.value`.
- **Limite do plano grátis** (`BillingManager.FREE_INVOICE_LIMIT = 5`) conta
  `SettingsRepository.totalInvoicesCreated`, um contador que só sobe. Apagar fatura não devolve
  crédito. A checagem fica em `NavigationViewModel.canCreateInvoice()`.
- **PDF com várias páginas:** desenhe sempre via `InvoicePager` e releia `pager.canvas` a cada uso
  (o canvas troca quando a página vira). Nunca guarde o canvas numa `val`.
- **Marca d'água** é desenhada em todas as páginas para quem não é premium
  (`onBeforeFinishPage`). Não remova esse caminho.
- **Analytics nunca recebe dado de usuário** (nome, e-mail, valores de fatura, texto de erro).
  Use os enums de `AnalyticsEvent`; detalhes de erro vão para o Crashlytics.

## Banco de dados

- Versão atual: **4** (primeira versão em produção). Schema em
  `app/schemas/.../AppDatabase/4.json`.
- **Toda mudança de entidade exige `Migration` escrita à mão** em `AppDatabase` e registrada no
  builder. Nunca use `fallbackToDestructiveMigration`: apagaria as faturas dos usuários.
- Commite o JSON novo de `app/schemas/` junto com a migração.
- O backup automático do Android (`backup_rules.xml`, `data_extraction_rules.xml`) cobre o banco e o
  DataStore, mas não os arquivos de logo/assinatura/QR em `filesDir`.

Problemas conhecidos no modelo atual (tratados em `docs/spec-onda-1.md`):
- `Invoice.clientId` tem `ON DELETE CASCADE`: apagar um cliente apaga as faturas dele em silêncio.
- `InvoiceItem.productServiceId` tem `ON DELETE RESTRICT`: apagar um produto já usado lança
  `SQLiteConstraintException` dentro de `viewModelScope.launch`, sem tratamento.
- `dueDate` é `String` no formato de data escolhido pelo usuário; a leitura tenta uma lista de
  formatos (`reformatDateIfNeeded`, duplicado em três classes).
- Os status `SENT`, `OVERDUE` e `CANCELLED` existem no enum mas nenhum código os grava.
- `insertInvoiceWithItems` / `updateInvoiceWithItems` não rodam dentro de `@Transaction`.
- `CreateInvoiceViewModel` e `EditInvoiceViewModel` duplicam o cálculo de totais.
- Parte dos rótulos do PDF está escrita direto em inglês no `InvoicePdfGenerator` ("INVOICE",
  "INVOICE NO:", "DUE DATE:"…), então o PDF não acompanha a língua do app.

## Build e verificação

```
./gradlew assembleDebug
./gradlew lint
./gradlew test
```

Não há testes automatizados ainda. Lógica nova de cálculo ou migração deve vir com teste
(unitário para cálculo; `MigrationTestHelper` em `androidTest` para migração).
