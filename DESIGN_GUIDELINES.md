# Super Invoice - Design Guidelines

Este documento contém as diretrizes de design do aplicativo Super Invoice.

## Paleta de Cores

### Cores Principais
- **Verde Principal**: `#9DEA6E` - Usado em botões de ação, elementos selecionados, avatares e FABs
- **Branco**: `#FFFFFF` - Background principal de todas as telas e componentes
- **Preto**: `#000000` - Texto principal e ícones

### Cores Secundárias
- **Cinza Borda**: `#E0E0E0` - Bordas de cards, inputs e divisores
- **Cinza Texto**: `Color.Gray` - Textos secundários (emails, telefones, datas, valores)

## Tipografia

### Fonte
- **Família**: Inter (Google Fonts)
- Configurada em `ui/theme/Type.kt`
- Disponível via Google Fonts Provider

### Pesos de Fonte
- **SemiBold**: Usado em:
  - Títulos de telas ("Invoices", "New Invoice", "Clients", "Add Client")
  - Filtros ("Paid", "Unpaid")
  - Subtítulos ("Invoice Record")
  - Números de invoice ("Invoice #002")
  - Nomes em cards
  - Labels de botões ("Save", "Cancel")
  - Valores monetários (no lado direito)

- **Normal**: Usado em:
  - Textos descritivos
  - Placeholders de inputs
  - Textos secundários
  - Opções de menu

### Tamanhos de Fonte
- **28sp**: Títulos principais (ex: "Invoices")
- **20sp**: Títulos de modais/telas secundárias (ex: "New Invoice")
- **18sp**: Subtítulos (ex: "Invoice Record")
- **16sp**: Texto de corpo, labels de botões, nomes em cards
- **14sp**: Textos secundários, valores monetários
- **13sp**: Informações secundárias (email, telefone)
- **12sp**: Datas
- **10sp**: Badges (ex: "PAID")

## Componentes

### Bordas e Cantos
- **Largura da Borda**: `1dp`
- **Cor da Borda**: `#E0E0E0`
- **Raio de Canto**:
  - Cards e inputs: `8dp` ou `12dp`
  - Botões: `26dp` (totalmente arredondados)
  - Avatares: `CircleShape`

### Cards
- Fundo branco `#FFFFFF`
- Borda de `1dp` em `#E0E0E0`
- Cantos arredondados: `8dp` ou `12dp`
- Padding interno: `16dp`
- Sem elevação (shadow)

### Botões

#### Botão Primário (Save)
- Background: `#9DEA6E`
- Texto: Preto, SemiBold, 16sp
- Altura: `52dp`
- Cantos: `26dp`

#### Botão Secundário (Cancel)
- Background: Branco
- Borda: `1dp`, `#E0E0E0`
- Texto: Preto, SemiBold, 16sp
- Altura: `52dp`
- Cantos: `26dp`

#### FAB (Floating Action Button)
- Background: `#9DEA6E`
- Ícone: Preto
- Ícone: `Icons.Default.Add`

### Inputs

#### Campos de Texto
- Borda: `1dp`, `#E0E0E0`
- Cantos: `8dp`
- Padding horizontal: `16dp`
- Background: Branco
- Texto: Preto, 14sp ou 16sp
- Placeholder: Cinza

#### Campos com Ícone
- Ícone à esquerda, 20dp ou 24dp
- Ícone em preto
- Espaçamento de 12dp entre ícone e texto

#### Campo de Busca
- Ícone de lupa (Search) à esquerda
- Placeholder: "Search clients..."
- Mesmo estilo dos inputs padrão

### Filtros/Tabs
- Não selecionado: Background transparente, texto preto
- Selecionado: Background `#9DEA6E`, texto preto SemiBold
- Cantos arredondados: `20dp`
- Padding: `24dp` horizontal, `8dp` vertical

### Badges
- "PAID": Background `#9DEA6E`, texto preto Bold, 10sp
- Cantos: `4dp`
- Padding: `8dp` horizontal, `2dp` vertical

### Avatares
- Forma: Circular
- Tamanho: `40dp`
- Background: `#9DEA6E`
- Letra: Primeira letra do nome em maiúsculo, preto, SemiBold, 18sp

### Bottom Navigation Bar
- Background: Branco
- Borda superior: `1dp`, `#E0E0E0`
- Item selecionado: Ícone e texto em preto
- Item não selecionado: Ícone e texto em cinza
- Sem indicador de background

## Espaçamentos

### Padding de Tela
- Horizontal: `20dp` (padrão em todas as telas)
- Top (telas com header): `72dp` (para afastar da barra de status)
- Bottom (com botões): `80dp` (para afastar dos botões de navegação do sistema)

### Espaçamento entre Elementos
- Entre título e filtros: `32dp`
- Entre filtros e conteúdo: `40dp`
- Entre subtítulo e lista: `28dp`
- Entre cards em lista: `12dp` ou `16dp`
- Entre campos de input: `16dp`
- Entre botões horizontais: `12dp`

### Padding Interno
- Cards: `16dp`
- Botões: Interno automático com altura de `52dp`
- Inputs: `16dp` horizontal, `4dp` vertical

## Layout

### Estrutura de Telas

#### Tela Principal (Home)
- Título centralizado
- Filtros centralizados
- Lista com scroll vertical
- Bottom navigation fixa
- FAB no canto inferior direito

#### Telas Modais (Create/Add)
- Header com X à esquerda, título centralizado
- Conteúdo com scroll vertical
- Botões fixos na parte inferior
- Background branco cobrindo tela inteira

#### Tela de Listagem (Clients)
- Header com X à esquerda, título centralizado
- Campo de busca
- Lista com scroll vertical
- FAB no canto inferior direito

### Alinhamentos
- Títulos: Centralizados
- Conteúdo: Alinhado à esquerda
- Botões de ação: Ocupam largura total (em pares)

## Ícones

### Material Icons
- Person: Cliente/Usuário
- Email: Email
- Phone: Telefone
- Add: Adicionar
- Close: Fechar
- Search: Buscar
- Settings: Configurações
- MoreVert: Menu de opções (três pontos verticais)
- KeyboardArrowRight: Seta para direita

### Ícones Customizados (Drawable)
- ic_invoice: Ícone de documento/fatura
- ic_notes: Ícone de notas/anotações
- ic_tax: Ícone de taxa/imposto
- ic_discount: Ícone de desconto/tag
- ic_address: Ícone de localização/endereço
- ic_zipcode: Ícone de código postal/pacote

## Estados e Interações

### Estados de Navegação
- Bottom bar: Item selecionado em preto, demais em cinza
- Filtros: Selecionado com background verde, demais transparentes

### Comportamentos
- Campos de texto: Single line (exceto Notes que é multiline)
- Listas: Scroll vertical
- Busca: Filtragem em tempo real (case insensitive)
- Navegação: Sistema de telas com callbacks

## Acessibilidade

### Content Descriptions
- Todos os ícones possuem contentDescription
- Botões com labels descritivas
- Campos de input com placeholders claros

## Convenções de Nomenclatura

### Arquivos
- Screens: `NomeScreen.kt` (ex: `HomeScreen.kt`)
- Componentes: `NomeComponent.kt` (ex: `InvoiceCard.kt`)
- Ícones: `ic_nome.xml` (ex: `ic_invoice.xml`)

### Cores em Código
- Sempre usar formato hexadecimal: `Color(0xFFRRGGBB)`
- Verde: `Color(0xFF9DEA6E)`
- Cinza borda: `Color(0xFFE0E0E0)`
- Branco: `Color(0xFFFFFFFF)` ou `Color.White`
- Preto: `Color.Black`
- Cinza texto: `Color.Gray`

## Notas Importantes

1. **Sem elevação**: Cards não utilizam shadow/elevation, apenas bordas
2. **Fundo sempre branco**: Todas as telas e componentes principais têm background branco
3. **Verde consistente**: Sempre usar `#9DEA6E` para elementos de ação e destaque
4. **Inter SemiBold**: Preferir SemiBold para títulos e elementos importantes ao invés de Bold
5. **Espaçamento respirável**: Preferir espaçamentos generosos entre seções (32dp, 40dp)
6. **Ícones em preto**: Todos os ícones em preto, exceto em estados não selecionados (cinza)

---

*Última atualização: Dezembro 2024*
