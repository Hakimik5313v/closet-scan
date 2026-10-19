# Closet/Scan — Vestier Digital Interactivo con IA

## Documentación de Avance de Proyecto
### Segundo Corte — Seminario de Investigación

---

**Proyecto:** Closet/Scan — Vestier Digital Interactivo con IA y Realidad Aumentada  
**Estudiante:** Ricardo Antonio Lopez Perez  
**Docente:** Carlos Castro  
**Asignatura:** Seminario de Investigación  
**Fecha de avance:** Octubre 2026  
**Institución:** UNICOLOMBO  

---

## 1. ¿Qué es Closet/Scan?

Closet/Scan es una aplicación móvil nativa para Android que permite a los usuarios digitalizar su ropa mediante la cámara del celular, organizar un armario virtual inteligente y probar combinaciones de prendas sobre una silueta anatómica en tiempo real (Vestier Virtual Interactivo 2.5D).

El sistema utiliza **Inteligencia Artificial** (Visión por Computador con Google MediaPipe) para detectar, recortar y clasificar las prendas automáticamente, y **Realidad Aumentada ligera** para superponer las prendas digitalizadas sobre una silueta o foto del usuario.

---

## 2. Objetivo General

Desarrollar un vestier digital interactivo como aplicación móvil en Android Studio (Java), utilizando técnicas de Inteligencia Artificial (Visión por Computador) y Realidad Aumentada, para la gestión y digitalización del armario personal, permitiendo generar recomendaciones personalizadas de outfits y comparar precios de prendas complementarias en tiendas locales.

---

## 3. Objetivos Específicos

1. **Diagnóstico y Requisitos:** Identificar los requerimientos funcionales del sistema y las necesidades del usuario respecto a la clasificación de prendas y criterios de combinación.
2. **Diseño de Arquitectura y Base de Datos:** Diseñar el modelo de datos relacional con Room/SQLite y la arquitectura de la aplicación móvil en Android Studio.
3. **Módulo de Visión Artificial (IA):** Implementar CameraX + segmentación de fondo para digitalizar prendas como PNG transparentes y extraer el color dominante.
4. **Módulo Interactivo y Recomendación:** Construir el motor de recomendación de outfits y la interfaz interactiva del vestier digital con capas de prendas superpuestas.
5. **Validación y Pruebas:** Evaluar la precisión de clasificación, la usabilidad de la interfaz y la satisfacción del usuario.

---

## 4. Tecnologías Utilizadas

| Tecnología | Uso en el Proyecto |
| :--- | :--- |
| **Android Studio (Java)** | Entorno de desarrollo de la app móvil |
| **SQLite / Room ORM** | Base de datos local para el armario digital |
| **CameraX (Jetpack)** | Captura de fotos de prendas con la cámara trasera |
| **MediaPipe Pose (Google)** | Detección de 33 puntos corporales para el vestier |
| **Figma** | Prototipo de alta fidelidad de las 5 pantallas |
| **Git / GitHub** | Control de versiones del código fuente |

---

## 5. Avance Actual del Proyecto

### Fase 1 — Completada: Diseño y Planificación

| Entregable | Estado |
| :--- | :---: |
| Requisitos funcionales y no funcionales documentados | ✅ Completado |
| Historias de usuario y alcance MVP definido | ✅ Completado |
| Marco Lógico y Objetivos de Investigación | ✅ Completado |
| Diseño UX/UI de 5 pantallas en Figma (alta fidelidad) | ✅ Completado |
| Modelo de base de datos relacional (Prendas, Outfits, Tiendas) | ✅ Completado |
| Diagrama de clases POO en Java (Arquitectura MVVM) | ✅ Completado |
| Prototipo web funcional v2 (demo de matching y armario) | ✅ Completado |
| Repositorio Git inicializado con estructura del proyecto | ✅ Completado |

### Fase 2 — En Progreso: Desarrollo Android

| Módulo | Estado |
| :--- | :---: |
| Estructura del proyecto Android creada (Gradle, dependencias) | ✅ Completado |
| Navegación con BottomNavigationView (5 pantallas) | 🔄 En progreso |
| Base de datos Room con entidades @Entity y @Dao | 🔄 En progreso |
| Módulo CameraX para escaneo de prendas | ⏳ Pendiente (Sprint 3) |
| Vestier Interactivo 2.5D con capas de prendas | ⏳ Pendiente (Sprint 4) |
| Motor de recomendación y matching de ofertas | ⏳ Pendiente (Sprint 5) |

---

## 6. Diseño de la Base de Datos (Modelo Relacional)

El armario digital utiliza **SQLite** gestionado con **Room ORM** de Android Jetpack, con las siguientes tablas:

- **`prendas`:** Almacena cada prenda digitalizada (`id`, `nombre`, `categoria`, `color_hex`, `estilo`, `ruta_foto_png`).
- **`outfits`:** Combinaciones guardadas por el usuario (`id_prenda_superior`, `id_prenda_inferior`, `id_calzado`, `porcentaje_afinidad`).
- **`tiendas_catalogo`:** Catálogo de prendas de tiendas locales con precios y descuentos reales.

Las imágenes PNG de las prendas **NO** se guardan en la base de datos; se almacenan en el almacenamiento privado del celular y en SQLite solo se guarda la ruta del archivo, manteniendo la base de datos ligera (< 1 MB).

---

## 7. Prototipo de Alta Fidelidad (Figma)

El prototipo visual de las 5 pantallas principales fue diseñado en Figma con un sistema de Design Tokens estandarizado (Dark Mode Noir):

- **Pantalla 1 — Dashboard:** Distribución de estilos en gráfico de dona, conteo total de prendas y carrusel de escaneos recientes.
- **Pantalla 2 — Escáner:** Visor CameraX con guías de encuadre y botón de captura.
- **Pantalla 3 — Ficha de Prenda:** Preview PNG recortado, selectores de categoría, material y ocasión, botón "Guardar en mi Armario".
- **Pantalla 4 — Vestier Virtual:** Lienzo interactivo 2.5D con carrusel de prendas Superior/Inferior, botón "Guardar Outfit".
- **Pantalla 5 — Recomendaciones:** Outfit sugerido con métrica de afinidad (95%), comparativa de ofertas de tiendas (Zara, Pull&Bear) con etiquetas de descuento.

**Enlace al prototipo en Figma:**  
https://www.figma.com/design/0e6RiUd1fq5Me6f0RqLhcI/Closet-Scan-Prototipo-Movil-v1.0

---

## 8. Repositorio GitHub

**Enlace al repositorio:**  
https://github.com/lopezperezricardo0316-a11y/closet-scan

**Estructura del proyecto:**
```
closet-scan/
├── app/
│   ├── src/main/java/com/hakimik/closetscan/
│   │   ├── data/           # Entidades Room y DAOs
│   │   ├── ui/             # Activities y Fragments de cada pantalla
│   │   └── engine/         # VestierEngine y RecomendadorEngine
│   └── res/                # Layouts XML, recursos gráficos
├── .gitignore              # Exclusiones estándar de Android
└── README.md               # Descripción general del proyecto
```

---

## 9. Plan de Trabajo — Cronograma de Sprints

| Sprint | Módulo | Semanas | Estado |
| :---: | :--- | :---: | :---: |
| **Sprint 1** | Arquitectura base, Gradle, Navegación y UI base | 1-2 | 🔄 En Progreso |
| **Sprint 2** | Base de datos Room: Entidades, DAOs, CRUD | 3-4 | ⏳ Próximo |
| **Sprint 3** | CameraX + Segmentación de fondo (PNG transparente) | 5-6 | ⏳ Pendiente |
| **Sprint 4** | Vestier Interactivo 2.5D con Canvas y capas | 7-8 | ⏳ Pendiente |
| **Sprint 5** | Motor de recomendación y matching con tiendas | 9 | ⏳ Pendiente |
| **Sprint 6** | Pruebas QA, APK firmado y sustentación | 10 | ⏳ Pendiente |

---

## 10. Referencias Académicas

1. **"Mobile Fitting Room: On-device Virtual Try-on"** — arXiv:2402.01877. Viabilidad de probadores de ropa ejecutados directamente en dispositivos móviles.
2. **"An Android App Using Virtual Trial Room"** — Journal of Computer Science, 2023. Uso de superposición de prendas en Android para reducir tiempo de decisión.
3. **Google MediaPipe Pose Documentation** — Detección de 33 landmarks corporales en tiempo real para dispositivos móviles Android.
4. **Android Jetpack Room Persistence Library** — Documentación oficial Google. Gestión de base de datos SQLite con ORM en Java.
