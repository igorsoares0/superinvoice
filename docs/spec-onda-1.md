# Spec — Onda 1: modelo de dados

Status: proposta · Versão alvo do app: 1.1.0 · Banco: versão 4 → 5

A Onda 1 entrega orçamentos, pagamentos parciais com recibo e um cálculo de totais completo
(imposto e desconto em % ou valor, frete, quantidade decimal, unidade). As três features mexem
nas mesmas tabelas, então saem numa única migração do Room. Aproveitamos a mesma migração para
corrigir quatro problemas do modelo atual que exigem recriar as tabelas de qualquer jeito.

Contexto de arquitetura e convenções: `CLAUDE.md`.

## 1. Escopo

Entra:

1. **Orçamentos** (estimates) com conversão em fatura num toque.
2. **Pagamentos** parciais ou totais por fatura, com histórico e **recibo em PDF**.
3. **Cálculo completo:** desconto e imposto em % ou valor fixo, frete, quantidade decimal,
   unidade por item.
4. **Correções do modelo** (já que as tabelas serão recriadas):
   - `dueDate` deixa de ser texto formatado e passa a ser epoch millis.
   - Apagar cliente deixa de apagar as faturas dele (`CASCADE` → `RESTRICT` + arquivar).
   - Apagar produto usado em fatura deixa de quebrar o app (`RESTRICT` → `SET NULL`).
   - Coluna `isPaid` (deprecated) é removida.

Fica de fora: imposto por item, mais de um imposto por fatura, backup no Google Drive,
lembretes, recorrência, IA. Imposto por item pode entrar depois com um `ADD COLUMN`, sem
recriar tabela.

## 2. Decisões em aberto

| # | Pergunta | Sugestão |
| --- | --- | --- |
| D1 | Orçamento conta no limite de 5 do plano grátis? | Sim, conta como documento. A conversão em fatura não conta de novo. |
| D2 | Cliente com faturas: o que acontece no "apagar"? | Oferecer **arquivar** (some das listas, faturas ficam). Apagar só cliente sem documentos. |
| D3 | Desconto antes ou depois do imposto? | Antes: imposto incide sobre `subtotal − desconto` (padrão de mercado). |
| D4 | Dinheiro em `Double` ou centavos `Long`? | Manter `Double` no banco nesta onda; todo cálculo passa por `BigDecimal` num lugar só (seção 4). |

O resto desta spec assume as sugestões. Se alguma mudar, ajustar as seções 3 e 5.

## 3. Modelo de dados (versão 5)

Orçamento e fatura ficam **na mesma tabela** `invoices`, com uma coluna `type`. Eles
compartilham cliente, itens, totais e PDF; a conversão vira uma cópia da linha com outro tipo.
Os nomes de classe (`Invoice`, `InvoiceItem`) continuam os mesmos para limitar a mudança.

### 3.1 `invoices`

| Coluna | Tipo Kotlin | Mudança | Observação |
| --- | --- | --- | --- |
| `id` | `Int` | — | |
| `type` | `DocumentType` | nova | `INVOICE` \| `ESTIMATE`, default `INVOICE` |
| `number` | `String` | — | Sequências separadas: `INV-001`, `EST-001` |
| `clientId` | `Int` | FK muda | `ON DELETE RESTRICT` (era `CASCADE`) |
| `createdDate` | `Long` | — | Data de emissão |
| `dueDate` | `Long` | **tipo muda** | Epoch millis (era `String` formatada). Em orçamento = "válido até" |
| `paymentDate` | `Long?` | — | Data do pagamento que quitou a fatura |
| `notes` | `String` | — | |
| `subtotal` | `Double` | — | Soma dos `lineTotal` |
| `discountType` | `AdjustmentType` | nova | `PERCENT` \| `FIXED`, default `FIXED` |
| `discountValue` | `Double` | nova | O que o usuário digitou (10 = 10% ou 10 na moeda) |
| `discountAmount` | `Double` | renomeia `discount` | Valor calculado em moeda |
| `taxType` | `AdjustmentType` | nova | default `FIXED` |
| `taxValue` | `Double` | nova | |
| `taxAmount` | `Double` | renomeia `tax` | Valor calculado em moeda |
| `shipping` | `Double` | nova | default 0 |
| `totalAmount` | `Double` | — | `subtotal − discountAmount + taxAmount + shipping` |
| `amountPaid` | `Double` | nova | Soma dos pagamentos, mantida pelo repositório (seção 5.2) |
| `status` | `InvoiceStatus` | valores novos | Ver 3.5 |
| `currency` | `String` | — | |
| `convertedFromId` | `Int?` | nova | Na fatura: id do orçamento de origem |
| `isPaid` | — | **removida** | |

`amountPaid` é desnormalizado de propósito: a Home filtra e mostra saldo em todas as linhas, e
somar pagamentos por fatura em cada lista custa mais do que manter o campo numa transação.

### 3.2 `invoice_items`

| Coluna | Tipo Kotlin | Mudança | Observação |
| --- | --- | --- | --- |
| `id`, `invoiceId` | `Int` | — | FK `invoiceId` continua `CASCADE` |
| `productServiceId` | `Int?` | **nullable**, FK muda | `ON DELETE SET NULL` (era `RESTRICT`) |
| `productServiceName`, `productServiceDescription` | `String` | — | Já são cópia; a fatura não depende do produto existir |
| `pricePerUnit` | `Double` | — | |
| `quantity` | `Double` | **tipo muda** | Era `Int` |
| `unit` | `String` | nova | Texto livre curto (`h`, `m²`, `un`); default `""` |
| `lineTotal` | `Double` | — | `pricePerUnit × quantity`, arredondado (seção 4) |

### 3.3 `payments` (nova)

| Coluna | Tipo Kotlin | Observação |
| --- | --- | --- |
| `id` | `Int` | |
| `invoiceId` | `Int` | FK → `invoices`, `ON DELETE CASCADE`, com índice |
| `amount` | `Double` | > 0 |
| `paidAt` | `Long` | Epoch millis |
| `method` | `PaymentMethod` | `CASH`, `BANK_TRANSFER`, `CARD`, `PIX`, `PAYPAL`, `CHECK`, `OTHER` |
| `note` | `String` | default `""` |
| `receiptNumber` | `String` | Sequência própria `REC-001` |
| `createdAt` | `Long` | |

### 3.4 Mudanças pequenas

- `products_services`: nova coluna `unit TEXT NOT NULL DEFAULT ''` (unidade padrão do produto,
  copiada para o item).
- `clients`: nova coluna `archived INTEGER NOT NULL DEFAULT 0` (decisão D2).

### 3.5 Status

```kotlin
enum class InvoiceStatus {
    DRAFT, SENT, PARTIALLY_PAID, PAID, CANCELLED,   // faturas
    ACCEPTED, DECLINED, CONVERTED,                  // orçamentos
    @Deprecated("Derivado da data, nunca gravado") OVERDUE
}
```

- **Vencida não é gravada.** É derivada: `type == INVOICE && status in (SENT, PARTIALLY_PAID, DRAFT)
  && dueDate < início de hoje`. Assim nenhum job precisa atualizar status à meia-noite.
  `OVERDUE` fica no enum só para o `Converters` continuar lendo valores antigos.
- Compartilhar um `DRAFT` muda para `SENT` (hoje nada grava `SENT`).
- `PAID` e `PARTIALLY_PAID` são consequência dos pagamentos (5.2), não um toggle na tela.

## 4. Cálculo — `InvoiceCalculator`

Um objeto puro em `data/calc/InvoiceCalculator.kt`, sem Android, usado por
`CreateInvoiceViewModel`, `EditInvoiceViewModel` e pela conversão de orçamento. Substitui os
`calculateSubtotal`/`calculateTotal` duplicados hoje.

```kotlin
data class Adjustment(val type: AdjustmentType, val value: Double)

data class Totals(
    val lineTotals: List<Double>,
    val subtotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val shipping: Double,
    val total: Double,
)

object InvoiceCalculator {
    fun calculate(
        lines: List<Pair<Double, Double>>,   // (pricePerUnit, quantity)
        discount: Adjustment,
        tax: Adjustment,
        shipping: Double,
    ): Totals
}
```

Regras:

1. Toda conta em `BigDecimal`, arredondando para 2 casas com `RoundingMode.HALF_UP` em cada
   `lineTotal`, no desconto, no imposto e no total. Volta para `Double` só na saída.
2. `discountAmount = PERCENT ? subtotal × value / 100 : value`, limitado a `[0, subtotal]`.
3. `taxBase = subtotal − discountAmount`; `taxAmount = PERCENT ? taxBase × value / 100 : value`.
4. `total = subtotal − discountAmount + taxAmount + shipping`.
5. Quantidade aceita até 3 casas decimais. A entrada aceita vírgula ou ponto conforme o
   `Locale` (pt-BR e es usam vírgula); use `KeyboardType.Decimal`.

Faturas existentes migram com `FIXED` e os mesmos valores, então o total delas não muda
(o cálculo atual é `subtotal + tax − discount`).

## 5. Comportamento

### 5.1 Orçamentos

- Home ganha um seletor **Faturas | Orçamentos**. O botão de criar respeita o tipo selecionado.
- Mesma tela de criação e edição, com `type` como parâmetro. Muda o título, o prefixo do número
  e o rótulo da data ("Válido até").
- Na pré-visualização do orçamento: **Marcar como aceito**, **Marcar como recusado**,
  **Converter em fatura**.
- **Conversão** (uma `@Transaction` no `InvoiceRepository`):
  1. Cria uma fatura `DRAFT` com cliente, itens, ajustes, frete, notas e moeda do orçamento;
     número novo `INV-`, `createdDate` = agora, `dueDate` = mesmo padrão de fatura nova,
     `convertedFromId` = id do orçamento.
  2. Muda o orçamento para `CONVERTED`.
  3. Abre a fatura nova em edição.
  4. Não incrementa `totalInvoicesCreated` (D1).
- Orçamento `CONVERTED` não pode ser convertido de novo; a tela mostra um link para a fatura.

### 5.2 Pagamentos e recibos

- Na pré-visualização da fatura: botão **Registrar pagamento** → bottom sheet com valor
  (preenchido com o saldo), data (hoje), método e observação.
- Lista de pagamentos na fatura, com opção de apagar.
- `InvoiceRepository.addPayment` / `deletePayment`, cada um numa `@Transaction`:
  1. Grava ou apaga a linha em `payments`.
  2. Recalcula `amountPaid = SUM(payments.amount)`.
  3. Atualiza status: `amountPaid ≥ totalAmount` → `PAID` e `paymentDate` = maior `paidAt`;
     `0 < amountPaid < totalAmount` → `PARTIALLY_PAID`; `0` → volta para `SENT`.
- Valor maior que o saldo: aceitar, mas avisar ("valor acima do saldo de X").
- O toggle atual pago/não pago do `EditInvoiceViewModel` some. "Marcar como paga" vira um atalho
  que registra um pagamento do saldo restante.
- **Recibo:** depois de salvar o pagamento, oferecer **Gerar recibo**. Novo método
  `InvoicePdfGenerator.generateReceiptPdf(invoice, payment, …)`: cabeçalho com logo e dados do
  negócio, número `REC-`, data, cliente, valor, método, fatura de referência e saldo restante.
  Um layout só nesta onda (usa a cor de destaque do usuário), com marca d'água para quem não é
  premium.
- Mudar itens de uma fatura com pagamentos recalcula o total e reaplica o passo 3.

### 5.3 Clientes e produtos

- Apagar cliente **sem** documentos: apaga. **Com** documentos: diálogo oferecendo arquivar.
  Clientes arquivados não aparecem na seleção nem na lista (filtro "Arquivados" na tela de clientes).
- Apagar produto sempre funciona; os itens que o usavam ficam com `productServiceId = null` e
  mantêm nome e preço copiados.

### 5.4 PDF

- Quantidade sem casas quando inteira (`2`), com até 3 quando não (`2.5`), mais a unidade.
- Desconto e imposto mostram o percentual quando `PERCENT` ("Imposto (8%)").
- Linha de frete quando > 0.
- Quando `amountPaid > 0`: linhas "Pago" e "Saldo devedor".
- Título e rótulos vêm de `strings.xml` (hoje "INVOICE", "INVOICE NO:" etc. estão escritos no
  código). Orçamento usa "ESTIMATE" / "ORÇAMENTO" / "PRESUPUESTO".
- `dueDate` é formatado com o `dateFormat` do usuário na hora de desenhar; os três
  `reformatDateIfNeeded` deixam de ser necessários.

### 5.5 Analytics

Eventos novos em `AnalyticsManager`, só com enums e contagens (sem valores nem nomes):
`estimate_created`, `estimate_converted`, `payment_recorded(method, is_partial)`,
`receipt_shared`. `logInvoiceSaved` ganha `discount_type`, `tax_type`, `has_shipping`,
`has_decimal_qty`.

### 5.6 Textos

Todas as strings novas nos três arquivos (`values`, `values-es`, `values-pt-rBR`).

## 6. Migração 4 → 5

`MIGRATION_4_5` escrita à mão em `AppDatabase` e registrada no builder. Nada de
`fallbackToDestructiveMigration`.

**Como escrever o SQL:** primeiro altere as entidades, rode o build e copie o `createSql` de cada
tabela do `app/schemas/.../5.json` gerado. O Room valida tipo, nulabilidade e `defaultValue` de
cada coluna ao abrir o banco; qualquer diferença entre o SQL da migração e o schema gerado
derruba o app na abertura. O SQL abaixo mostra a ordem e a cópia de dados, não substitui o do JSON.

Por que recriar tabelas: SQLite não muda tipo de coluna nem ação de FK com `ALTER TABLE`.
O Room roda a migração com as foreign keys desligadas (só liga em `onOpen`), então dá para
recriar pai e filho na mesma transação. Mesmo assim, apague filho antes do pai.

```sql
-- 1. invoices nova (createSql do 5.json, com nome invoices_new)
CREATE TABLE invoices_new (...);

INSERT INTO invoices_new (
  id, type, number, clientId, createdDate, dueDate, paymentDate, notes,
  subtotal, discountType, discountValue, discountAmount,
  taxType, taxValue, taxAmount, shipping, totalAmount, amountPaid,
  status, currency, convertedFromId)
SELECT
  id, 'INVOICE', number, clientId, createdDate, createdDate /* corrigido no passo 3 */,
  paymentDate, notes,
  subtotal, 'FIXED', discount, discount,
  'FIXED', tax, tax, 0, totalAmount,
  CASE WHEN status = 'PAID' THEN totalAmount ELSE 0 END,
  CASE WHEN status = 'OVERDUE' THEN 'SENT' ELSE status END,
  currency, NULL
FROM invoices;

-- 2. invoice_items nova
CREATE TABLE invoice_items_new (...);
INSERT INTO invoice_items_new (
  id, invoiceId, productServiceId, productServiceName, productServiceDescription,
  pricePerUnit, quantity, unit, lineTotal)
SELECT id, invoiceId, productServiceId, productServiceName, productServiceDescription,
  pricePerUnit, CAST(quantity AS REAL), '', lineTotal
FROM invoice_items;

-- 3. converter dueDate em Kotlin (ver abaixo), antes do DROP, lendo invoices.dueDate antigo

-- 4. troca
DROP TABLE invoice_items;
DROP TABLE invoices;
ALTER TABLE invoices_new RENAME TO invoices;
ALTER TABLE invoice_items_new RENAME TO invoice_items;
-- recriar os índices com o createSql do 5.json
-- (index_invoices_clientId, index_invoice_items_invoiceId, index_invoice_items_productServiceId)

-- 5. colunas novas
ALTER TABLE products_services ADD COLUMN unit TEXT NOT NULL DEFAULT '';
ALTER TABLE clients ADD COLUMN archived INTEGER NOT NULL DEFAULT 0;

-- 6. payments
CREATE TABLE payments (...);
CREATE INDEX index_payments_invoiceId ON payments (invoiceId);

-- 7. faturas já pagas ganham um pagamento, para o histórico bater com o status.
--    Feito em Kotlin: cursor sobre "SELECT id, totalAmount, paymentDate, createdDate
--    FROM invoices WHERE status = 'PAID' ORDER BY id" e um INSERT por linha, com
--    receiptNumber REC-001, REC-002… gerado no laço. (ROW_NUMBER() resolveria em SQL,
--    mas exige SQLite 3.25 / Android 11, e o minSdk é 24.)
--    paidAt = COALESCE(paymentDate, createdDate), method = 'OTHER'.

-- 8. checagem final: deve voltar vazio; se não, lançar exceção e abortar a migração
PRAGMA foreign_key_check;
```

**Passo 3 — converter `dueDate` (em Kotlin, dentro da migração):**

1. Ler `id, dueDate` da tabela `invoices` antiga com um cursor.
2. Tentar parsear com o formato de data atual do usuário primeiro, depois com a lista que hoje
   está em `reformatDateIfNeeded`, sempre com `Locale.getDefault()` (formatos com nome de mês
   dependem da língua). Mover essa lista para um `DateParsing` compartilhado.
3. Gravar o resultado em `invoices_new.dueDate`. Se nada parsear, manter `createdDate` e contar.
4. Ao final, se houve falhas, registrar só a contagem no Crashlytics (nunca o texto da data).

O formato atual do usuário está no DataStore. Passe para a migração pelo construtor
(`Migration4To5(dateFormatProvider)`), lido com `runBlocking` no `getDatabase`, que já roda fora
da main thread na primeira consulta.

## 7. Testes

- **Unitários** (`app/src/test`): `InvoiceCalculator` — % e fixo, desconto maior que o
  subtotal, arredondamento de meio centavo, quantidade decimal, frete, fatura antiga com valores
  fixos dando o mesmo total de antes.
- **Migração** (`app/src/androidTest`, dependência `androidx.room:room-testing`):
  `MigrationTestHelper` criando um banco v4 com:
  - fatura paga com `paymentDate` e outra sem;
  - `dueDate` em cada formato da lista, mais um inválido;
  - cliente com faturas e produto usado em item;
  - quantidade inteira.
  Verificar: totais iguais aos da v4, pagamento criado para cada fatura paga, `dueDate`
  convertido, apagar o produto não falha, apagar o cliente falha com `RESTRICT`.
- **Upgrade real:** instalar o APK 1.0.7 da Play, criar dados, instalar o 1.1.0 por cima.

## 8. Ordem de implementação

Cada item é um PR que compila e passa nos testes sozinho.

1. `InvoiceCalculator` + testes, ligado às duas telas de fatura (sem mexer no banco).
2. Entidades v5, `MIGRATION_4_5`, `5.json`, teste de migração, DAOs e repositórios com
   `@Transaction` (inclui `insertInvoiceWithItems`/`updateInvoiceWithItems`, que hoje não têm).
3. UI de cálculo: %/valor, frete, quantidade decimal, unidade; PDF atualizado e com textos
   traduzidos.
4. Pagamentos, saldo, recibo em PDF.
5. Orçamentos: seletor na Home, criação, aceitar/recusar, conversão.
6. Clientes arquivados e delete seguro de cliente e produto.

Lançamento: `versionName 1.1.0`, rollout gradual na Play (começar em 10%) acompanhando o
Crashlytics por falhas na abertura do banco antes de ampliar.
