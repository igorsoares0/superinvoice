# Spec — Links de pagamento (Pix, PayPal, link genérico)

Status: implementada (PR 1–4); falta o teste manual em apps de banco (seção 6) · Versão alvo do app: 1.2.0 · Banco: sem mudança (continua na versão 5)

Hoje a fatura diz *para onde* pagar (dados bancários, um QR enviado como imagem), mas nunca
*quanto* de um jeito que o pagador possa usar direto. Esta spec adiciona Pix com valor, PayPal.me
com valor e um link de pagamento genérico, no PDF (texto + QR gerado) e no texto de
compartilhamento. Tudo é gerado offline no aparelho; o app continua sem backend.

Contexto de arquitetura e convenções: `CLAUDE.md`.

## 1. Escopo

Entra:

1. **Pix:** chave, nome e cidade do recebedor nas configurações. Cada fatura em BRL ganha um
   BR Code estático (o "copia e cola") com o saldo devedor e o número da fatura.
2. **PayPal.me:** usuário nas configurações; a fatura ganha `paypal.me/<usuario>/<valor><moeda>`.
3. **Link genérico:** uma URL fixa colada pelo usuário (Stripe Payment Link, Mercado Pago, Wise,
   Venmo…), sem valor.
4. **QR gerado** no PDF a partir do Pix ou do link, desenhado em vetor.
5. **Texto de compartilhamento** com os links (é ali que eles ficam clicáveis, seção 5.4).
6. **Correção:** o bloco de pagamento do PDF só aparece hoje se `bankName` estiver preenchido
   (`InvoicePdfGenerator`, nos três templates). Quem só tem QR ou Pix não vê nada no PDF.

Fica de fora:

- Pagamento integrado (Stripe Connect, Mercado Pago com confirmação automática, webhook que marca
  a fatura como paga). Exige backend, contas e onboarding do vendedor; é outro projeto.
- Pix dinâmico (cobrança registrada via API do PSP, com vencimento e juros).
- Link ou chave diferente por fatura (exigiria coluna nova e migração). Se pedirem, entra depois
  com `ADD COLUMN`.
- Pix copia e cola impresso no PDF. Copiar texto de PDF no celular costuma inserir quebra de
  linha, e um caractere a mais invalida o código (CRC). Quem abre no celular usa o código da
  mensagem de compartilhamento. Se pedirem, testar uma linha única em fonte pequena antes.
- Marcar a fatura como paga sozinha. O usuário continua registrando o pagamento
  (`PaymentMethod.PIX` e `PAYPAL` já existem).

## 2. Decisões em aberto

| # | Pergunta | Sugestão |
| --- | --- | --- |
| L1 | O que é premium? | Chave, usuário e link **em texto** no PDF: grátis. **QR gerado com valor** (Pix ou link): premium. Grátis vê o QR na pré-visualização com cadeado e cai no paywall (`PaywallSource.PAYMENT_LINK`). |
| L2 | Só cabe um QR no bloco. Qual vence? | Pix (se a fatura é BRL e há chave) → imagem enviada pelo usuário → PayPal.me → link genérico. |
| L3 | Fatura já paga mostra links? | Não. Saldo zero: nada de Pix/PayPal/link nem QR gerado; dados bancários continuam. |
| L4 | Orçamento mostra links? | Não. Orçamento nunca recebe pagamento (`CLAUDE.md`). |
| L5 | Pix aparece para quem não usa BRL? | A seção Pix nas configurações só aparece se a moeda padrão é BRL ou se já há chave salva. |

O resto da spec assume as sugestões.

## 3. Dados

Nada no Room. Chaves novas no DataStore (`SettingsRepository`), já cobertas pelo backup automático:

| Chave | Tipo | Observação |
| --- | --- | --- |
| `pix_key_type` | `String` | `CPF`, `CNPJ`, `EMAIL`, `PHONE`, `EVP` (enum `PixKeyType`) |
| `pix_key` | `String` | Gravada já normalizada (5.1) |
| `pix_merchant_name` | `String` | Vazio = usar o nome do negócio |
| `pix_merchant_city` | `String` | Vazio = usar a cidade do negócio |
| `paypal_me_username` | `String` | Só o usuário, sem `paypal.me/` |
| `payment_link_url` | `String` | URL `https://` completa |

Leitura num lugar só, no padrão de `invoiceStyle()`:

```kotlin
suspend fun paymentLinkSettings(): PaymentLinkSettings
```

Hoje há quatro chamadas de `generateInvoicePdf` (download e share na pré-visualização, share e
download na Home) mais `generateInvoicePreviewBitmap`, cada uma relendo as preferências. As
novas entram via esse objeto, não como mais um `first()` em cada chamada.

## 4. Lógica — `data/payment/`

Kotlin puro, sem Android, testável em `app/src/test`.

```kotlin
object PixKey {
    /** Chave no formato do BR Code, ou null se inválida para o tipo. Usado ao salvar. */
    fun normalize(type: PixKeyType, raw: String): String?
}

object PixPayload {
    /** BR Code estático (EMV MPM). Devolve null se algum campo obrigatório for inválido. */
    fun build(key: String, name: String, city: String, amount: Double?, txid: String): String?
}

object PayPalMe {
    fun normalizeUsername(raw: String): String?
    fun url(username: String, amount: Double?, currency: String): String
}

object PaymentLinkUrl {
    fun normalize(raw: String): String?   // só https; sem esquema, acrescenta
}

enum class PaymentQrSource { NONE, UPLOADED, PIX, PAYPAL, CUSTOM_LINK }

data class PaymentLinks(
    val pixKey: String?,         // para a linha "Pix:" do PDF
    val pixPayload: String?,     // copia e cola
    val payPalUrl: String?,
    val customUrl: String?,
    val qrSource: PaymentQrSource,
    val qrContent: String?,      // o que vai no QR gerado, conforme L2 (null = imagem ou nada)
)

object PaymentLinksBuilder {
    fun forInvoice(invoice: Invoice, settings: PaymentLinkSettings, hasUploadedQr: Boolean): PaymentLinks
}
```

`PaymentLinksBuilder` aplica L2, L3 e L4 (fatura `CANCELLED` também fica sem link) e calcula o valor com
`InvoiceCalculator.balanceDue(invoice.totalAmount, invoice.amountPaid)`. Nenhuma conta de valor
fora do `InvoiceCalculator`.

## 5. Comportamento

### 5.1 Pix — formato do BR Code

Referência: Manual de Padrões para Iniciação do Pix (BACEN). Cada campo é `ID (2) + tamanho (2) +
valor`:

| ID | Campo | Valor |
| --- | --- | --- |
| `00` | Payload Format Indicator | `01` |
| `26` | Merchant Account Info | `00` = `br.gov.bcb.pix`, `01` = chave. Total ≤ 99 |
| `52` | Merchant Category Code | `0000` |
| `53` | Moeda | `986` |
| `54` | Valor | Omitido se nulo ou zero. Ver regra 1 |
| `58` | País | `BR` |
| `59` | Nome do recebedor | ≤ 25 caracteres |
| `60` | Cidade | ≤ 15 caracteres |
| `62` | Additional Data | `05` = txid |
| `63` | CRC16 | 4 hex maiúsculos |

Regras:

1. **Valor sempre com ponto:** `BigDecimal(...).setScale(2, HALF_UP).toPlainString()`. Nunca
   `String.format("%.2f")` nem `formatDecimalInput`: em pt-BR e es sai vírgula e o banco rejeita
   o código.
2. **CRC16-CCITT-FALSE** (polinômio `0x1021`, inicial `0xFFFF`), calculado sobre o payload
   inteiro **incluindo** o `6304`.
3. **Nome e cidade:** sem acento (`Normalizer` NFD + remover marcas), só ASCII imprimível,
   cortados em 25 e 15. Sem acento porque vários apps de banco rejeitam ou mostram lixo.
4. **txid:** número da fatura só com `[A-Za-z0-9]`, até 25 caracteres (`INV-012` → `INV012`).
   Vazio vira `***`.
5. **Chave normalizada ao salvar**, conforme o tipo escolhido pelo usuário (o tipo não é
   adivinhado: telefone e CPF têm os dois 11 dígitos):
   - CPF/CNPJ: só dígitos, com dígito verificador validado.
   - Telefone: `+55` + DDD + número, só dígitos depois do `+`.
   - E-mail: minúsculo, com `@`.
   - Aleatória (EVP): UUID minúsculo, com hífens.
6. `01` (Point of Initiation) é omitido: o código é estático e pode ser lido mais de uma vez.
7. A descrição (`26`/`02`) não entra: aumenta o QR e nem todo banco mostra.

### 5.2 PayPal.me

- `https://paypal.me/<usuario>/<valor><MOEDA>`, ex.: `paypal.me/joao/150.00BRL`. Valor com ponto
  (regra 1 da 5.1).
- Moeda sem casas decimais no PayPal (`JPY`) vai sem decimais: `15000JPY`.
- Moeda que o PayPal não aceita: link sem valor (`paypal.me/<usuario>`). Conferir na
  implementação a lista de moedas do PayPal contra as 12 do `getCurrencySymbol`; ARS, INR e CNY
  são as candidatas a ficar sem valor.
- Usuário digitado com `paypal.me/` ou `@` na frente: limpar ao salvar.

### 5.3 Configurações

Nova tela **Pix e links de pagamento** (`Screen.PAYMENT_LINKS`, item em Configurações logo abaixo
de "Instruções de pagamento"). A tela de dados bancários e a do QR por imagem ficam como estão.

- **Pix** (L5): tipo de chave (seletor), chave, nome e cidade (com placeholder mostrando o valor
  do negócio que será usado se vazio). Validação ao salvar, com erro no campo.
  - Botão **Testar no meu banco**: copia para a área de transferência um Pix de R$ 1,00 com a
    chave salva, para o usuário colar no app do banco e conferir nome e chave antes de mandar a
    primeira fatura.
- **PayPal:** usuário.
- **Outro link:** URL. Aceita só `https://`; sem esquema, prefixar `https://`.
- Mostrar uma prévia do QR gerado, com o cadeado de L1 para quem é grátis. Entra no PR 3,
  junto com o ZXing.

Formulários seguem os `Inv*` (`InvSettingsSubScreen`, `InvField`), como `PaymentInstructionsScreen`.

### 5.4 PDF

O `android.graphics.pdf.PdfDocument` não cria anotação de link, então a URL no PDF é texto. Chrome
e Adobe costumam detectar URL e torná-la clicável, mas não dá para garantir. Por isso o QR é o
caminho principal no PDF e o texto de compartilhamento é o caminho clicável (5.5).

- **Condição do bloco:** trocar `paymentInfo.bankName.isNotEmpty()` (três templates) por
  "tem dado bancário **ou** QR enviado **ou** algum link". Corrige o QR enviado que hoje some
  sem nome de banco.
- Linhas novas, depois das bancárias:
  - `Pix: <chave>` (a chave, não o copia e cola: o payload tem ~150 caracteres).
  - `PayPal: paypal.me/joao/150.00BRL` (sem `https://`).
  - `Pagar online: <url>`, quebrando em até 3 linhas por largura; acima disso, cortada com
    reticências pelo `InvoicePaints.truncate` (o QR continua completo).
- **QR gerado:** ZXing `core` (`com.google.zxing:core`, Java puro, sem conflito com o Kotlin
  preso pelo Firebase) gera a `BitMatrix`; cada módulo vira um `drawRect` no canvas. Vetorial,
  então não tem o problema de DPI que o `drawImageAt` resolve para imagens.
  - Correção de erro `M`, margem de 4 módulos, tamanho 90 pt (o Pix com valor dá ~45 módulos,
    ~2 pt por módulo).
  - Legenda abaixo: "Pague com Pix" / "Escaneie para pagar".
  - Desenhado via `InvoicePager`, relendo `pager.canvas`; se não couber na página, vira a página
    antes de desenhar (não partir o QR).
- Grátis (L1): linhas de texto aparecem; o QR gerado não. A imagem enviada continua grátis e
  aparece no lugar do QR gerado (`PaymentLinks.forPlan`).
- Recibo (`generateReceiptPdf`) não muda.

### 5.5 Compartilhamento

`shareTextsFor` (`util/ShareTexts.kt`) recebe `PaymentLinks` e acrescenta ao `text`, cada item
numa linha própria:

```
Fatura INV-012 em anexo.

Pagar com PayPal: https://paypal.me/joao/150.00BRL
Pagar online: https://buy.stripe.com/...
Pix copia e cola:
00020126...6304ABCD
```

- O copia e cola fica por último e sozinho na linha, para facilitar a seleção no WhatsApp (que
  usa o texto como legenda do PDF).
- Na pré-visualização da fatura, ação **Copiar código Pix** e **Copiar link de pagamento**
  (área de transferência), para o usuário mandar separado. Grátis (L1) também: é texto, não QR.
- Os links no texto valem para grátis e premium.

### 5.6 Analytics

Sem chave, usuário, URL ou valor (`CLAUDE.md`). Enums em `AnalyticsEvent`:

- `payment_links_saved(has_pix, has_paypal, has_custom_link)`.
- `logInvoicePdfShared` ganha `payment_qr`: `NONE`, `UPLOADED`, `PIX`, `PAYPAL`, `CUSTOM_LINK`.
- `payment_link_copied(kind)`: `PIX`, `PAYPAL`, `CUSTOM_LINK`.
- `PaywallSource.PAYMENT_LINK`.

Erro de geração do QR vai para o Crashlytics só com o tipo (`pix`/`link`), nunca o conteúdo.

### 5.7 Textos

Todas as strings novas nos três arquivos (`values`, `values-es`, `values-pt-rBR`): título da
tela, rótulos e erros de validação de cada tipo de chave, rótulos do PDF ("Pix", "Pagar online",
legendas do QR), linhas do texto de compartilhamento, ações de copiar e o toast de copiado.
"Pix" não se traduz; em inglês, "Pix (Brazil)" no título da seção.

## 6. Testes

- **Unitários** (`app/src/test`):
  - `PixPayload`: o exemplo estático do manual do BACEN reproduzido byte a byte (CRC incluso);
    com valor; valor com meio centavo; nome e cidade com acento e longos; txid com hífen e com
    mais de 25 caracteres; cada tipo de chave, com e sem máscara; CPF com dígito errado → null;
    **`Locale.setDefault(pt-BR)`** e conferir que o valor sai com ponto.
  - `PayPalMe`: BRL, JPY sem decimais, moeda não aceita sem valor, usuário com `paypal.me/` e `@`.
  - `PaymentLinksBuilder`: L2 (cada combinação), fatura paga, parcialmente paga (usa o saldo),
    orçamento, fatura em USD com Pix configurado (sem Pix).
- **Instrumentado** (`androidTest/.../pdf/`): no padrão do `InvoicePdfRenderTest`, PDF com Pix
  nos três templates sem exceção, e bloco de pagamento presente só com Pix (sem banco).
- **Manual, antes de lançar:** escanear o QR do PDF (tela e impresso) e colar o copia e cola em
  pelo menos quatro apps de banco (ex.: Nubank, Itaú, Banco do Brasil, Inter, Mercado Pago),
  conferindo nome, valor e que o pagamento de R$ 1,00 cai na conta. Abrir o link do PayPal com
  BRL e USD.

## 7. Ordem de implementação

Cada item é um PR que compila e passa nos testes sozinho.

1. `data/payment/` (`PixPayload`, `PayPalMe`, `PaymentLinksBuilder`) + testes unitários. Sem UI.
2. Preferências novas + `paymentLinkSettings()` + tela de configurações (5.3) + strings.
3. PDF: condição do bloco, linhas de texto, QR gerado com ZXing, gate premium (L1).
4. Compartilhamento e ações de copiar (5.5) + analytics (5.6).

Lançamento: `versionName 1.2.0`. Rollout gradual, acompanhando no analytics `payment_qr` e
`payment_link_copied` para ver se o recurso é usado, e no Crashlytics erros de geração de QR.
