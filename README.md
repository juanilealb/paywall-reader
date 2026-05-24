# PaywallReader

Navegador Android nativo, minimalista y curado, para leer noticias sin paywall usando [removepaywalls.com](https://removepaywalls.com) como backend.

## Concepto

Juani quiere evitar el flujo actual:
1. Navegar a un diario
2. Copiar el link
3. Ir a removepaywalls.com
4. Pegar el link

La app resuelve esto: **lista de sources → tocar → leer sin paywall**, todo dentro de la app.

No es un navegador full. Es un **reader curado** con las páginas que a Juani le interesan.

## Stack

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Kotlin |
| UI | Jetpack Compose |
| Design | Material 3 (con motion y colores expresivos) |
| Arquitectura | MVVM + StateFlow |
| Persistencia | Room |
| Navegación | Navigation Compose |
| Contenido | WebView + removepaywalls.com |

## Funcionalidad MVP

1. **HomeScreen**: grid/lista de sources guardados. Cada source muestra favicon + nombre + URL base.
2. **Agregar source**: bottom sheet o dialog para ingresar nombre + URL.
3. **ReaderScreen**: WebView que carga `https://removepaywalls.com/?url=<URL>` con controls mínimos (back, reload, progress).
4. **Navegación**: dentro del WebView se puede navegar links internos (artículos relacionados, etc).
5. **Persistencia**: sources guardados en Room.
6. **Defaults**: al instalar, viene pre-cargado con fuentes argentinas comunes (Clarín, La Nación, Infobae, Ámbito, etc.).

## Material 3 Expressive

- Colores: esquema dinámico (seed color vívido, ej. `#006C4C` o un verde expresivo) + tonalidades altas en cards
- Motion: animated transitions entre screens (slide + fade), spring-based enter/exit
- Typography: Material 3 type scale con pesos expresivos (bold display, medium body)
- Cards: elevated con rounded corners grandes (16-24dp) y padding generoso
- Bottom sheet para agregar sources con drag-to-dismiss

## Estructura

```
com.juani.paywallreader/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── SourceDao.kt
│   │   └── SourceEntity.kt
│   └── repository/
│       └── SourceRepository.kt
├── domain/
│   └── model/
│       └── Source.kt
├── ui/
│   ├── theme/
│   │   ├── Color.kt         // seed + expressive palette
│   │   ├── Theme.kt         // Material 3 theme
│   │   └── Type.kt          // typography scale
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── reader/
│   │   ├── ReaderScreen.kt
│   │   └── ReaderViewModel.kt
│   ├── components/
│   │   ├── SourceCard.kt    // card de source con icono
│   │   ├── AddSourceSheet.kt
│   │   └── AnimatedEntrance.kt
│   └── navigation/
│       └── AppNavigation.kt // NavHost con transitions
├── MainActivity.kt
└── PaywallReaderApp.kt      // Application class
```

## API removepaywalls

```
GET https://removepaywalls.com/?url=<ENCODED_URL>
```

El WebView carga esa URL. removepaywalls devuelve la página procesada (HTML limpio). La app no parsea nada, solo delega.

## Notas

- WebView debe tener JavaScript habilitado (removepaywalls lo usa).
- Dominios de sources se validan (URL bien formada, http/https).
- Room migrations: version 1, schema export.
- Proguard: reglas básicas para Room y Compose.
