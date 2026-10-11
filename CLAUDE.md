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
- **Dinheiro é `Double` no banco, conta é em `BigDecimal`.** Todo total passa pelo
  `InvoiceCalculator` (`data/calc/`): HALF_UP em 2 casas, desconto antes do imposto. Não calcule
  total em outro lugar. O símbolo vem de `getCurrencySymbol` (`util/CurrencyUtils.kt`).
- **Formulário de fatura:** itens, desconto/imposto (% ou valor), frete e totais ficam no
  `InvoiceForm` (`ui/viewmodel/`), usado pela criação e pela edição; as duas telas desenham as
  mesmas partes de `ui/screens/InvoiceFormSections.kt`. Mudou o formulário, mude ali.
- **PDF:** todo rótulo vem de `strings.xml` via `text(R.string…)` no `InvoicePdfGenerator`; a
  fatura sai na língua do aparelho. Quantidade sai por `formatQuantity` mais a unidade do item.
- **Número digitado** é lido com `parseDecimalInput` e devolvido ao campo com `formatDecimalInput`
  (`util/DecimalInput.kt`): em pt-BR/es o teclado mostra vírgula, e `toDoubleOrNull()` dá `null`.
- **Datas de calendário** (vencimento) são a meia-noite UTC do dia, formatadas via
  `InvoiceDates.format` (`util/InvoiceDates.kt`). É o que o `DatePicker` devolve.

## Regras que já quebraram antes

- **Premium é assíncrono.** `PremiumStatus.Unknown` existe porque o RevenueCat demora a responder.
  Decisão irreversível (renderizar PDF, liberar criação de fatura) espera
  `BillingManager.awaitPremiumStatus()`; só UI pode ler `isPremium.value`.
- **Limite do plano grátis** (`BillingManager.FREE_INVOICE_LIMIT = 5`) conta
  `SettingsRepository.totalInvoicesCreated`, um contador que só sobe. Apagar fatura não devolve
  crédito. A checagem fica em `NavigationViewModel.canCreateInvoice()`. Orçamento conta como
  documento; a conversão em fatura (`InvoiceRepository.convertEstimateToInvoice`) não conta de novo.
- **PDF com várias páginas:** desenhe sempre via `InvoicePager` e releia `pager.canvas` a cada uso
  (o canvas troca quando a página vira). Nunca guarde o canvas numa `val`.
- **Marca d'água** é desenhada em todas as páginas para quem não é premium
  (`onBeforeFinishPage`). Não remova esse caminho.
- **Analytics nunca recebe dado de usuário** (nome, e-mail, valores de fatura, texto de erro).
  Use os enums de `AnalyticsEvent`; detalhes de erro vão para o Crashlytics.

## Banco de dados

- Versão atual: **5** (`Migration4To5`). A v4 foi a primeira em produção. Schemas em
  `app/schemas/.../AppDatabase/`.
- **Toda mudança de entidade exige `Migration` escrita à mão** em `AppDatabase` e registrada no
  builder. Nunca use `fallbackToDestructiveMigration`: apagaria as faturas dos usuários.
- Commite o JSON novo de `app/schemas/` junto com a migração.
- O backup automático do Android (`backup_rules.xml`, `data_extraction_rules.xml`) cobre o banco e o
  DataStore, mas não os arquivos de logo/assinatura/QR em `filesDir`.
- `Invoice.clientId` é `RESTRICT`: cliente com faturas ou orçamentos nunca é apagado, é
  arquivado (`clients.archived`, decisão D2). Listas e seleção de cliente usam
  `ClientRepository.getActiveClients()`; documentos de cliente arquivado continuam normais.
  `InvoiceItem.productServiceId` é `SET NULL`; o item guarda cópia de nome e preço, então telas
  remontam a linha a partir do `InvoiceItem` (`toLineItem`), nunca do cadastro de produto.
- **Orçamento é uma linha de `invoices` com `type = ESTIMATE`**, numerada EST- (sequência própria,
  `generateNextNumber`). Toda tela/consulta que lista faturas precisa filtrar por `type`. Status de
  orçamento: `DRAFT`/`SENT` (em aberto), `ACCEPTED`, `DECLINED`, `CONVERTED`; nunca recebe pagamento.
- `Invoice.amountPaid` é a soma de `payments`, mantida na mesma transação que grava o pagamento.
  Pagamento só entra ou sai por `InvoiceRepository.addPayment`/`deletePayment`, que recalculam
  `amountPaid`, status e `paymentDate` (`PaymentRules`); `updateInvoiceWithItems` refaz o mesmo
  cálculo, porque o total pode ter mudado. `PAID`/`PARTIALLY_PAID` nunca são escolhidos na tela.

Ainda pendente da spec da Onda 1 (`docs/spec-onda-1.md`, seção 3.5), fora dos 6 PRs da seção 8:
- `SENT` só é gravado quando o último pagamento é apagado; compartilhar rascunho ainda não marca
  como enviado, e `CANCELLED` não é gravado por nenhum código.
- "Vencida" (derivada de `dueDate`) ainda não aparece em nenhuma tela nem filtro.

## Build e verificação

```
./gradlew assembleDebug
./gradlew lint
./gradlew test
```

No WSL o `gradlew` não roda (CRLF e SDK no Windows): use `cmd.exe /c "gradlew.bat test"`.

Lógica nova de cálculo ou migração deve vir com teste: unitário em `app/src/test` para cálculo,
`MigrationTestHelper` em `androidTest/.../database/` para migração. No aparelho, rode o teste
instrumentado com `adb shell am instrument -w -e class …` depois de `installDebug
installDebugAndroidTest`; o `connectedAndroidTest` desinstala o app no fim e apaga os dados dele.
