# 👗 Closet/Scan — Vestier Digital Interactivo con IA

![Estado](https://img.shields.io/badge/Estado-En%20Desarrollo-6E56CF?style=flat-square)
![Plataforma](https://img.shields.io/badge/Plataforma-Android-green?style=flat-square&logo=android)
![Lenguaje](https://img.shields.io/badge/Lenguaje-Java-orange?style=flat-square&logo=java)
![Materia](https://img.shields.io/badge/Materia-Seminario%20de%20Investigaci%C3%B3n-blue?style=flat-square)

**Aplicación móvil nativa para Android que permite digitalizar prendas con la cámara del celular, organizar un armario virtual inteligente y probar combinaciones de outfits sobre una silueta anatómica interactiva (Vestier Virtual 2.5D).**

> Proyecto desarrollado para la asignatura de **Seminario de Investigación** — UNICOLOMBO  
> Estudiante: Ricardo Antonio Lopez Perez | Docente: Carlos Castro

---

## 📱 ¿Qué hace la aplicación?

| Módulo | Descripción |
| :--- | :--- |
| 📷 **Escáner de Prendas** | Toma foto de ropa, recorta el fondo automáticamente y guarda PNG transparente |
| 👗 **Armario Digital** | Inventario local de todas las prendas organizadas por categoría y estilo |
| 🎭 **Vestier Interactivo 2.5D** | Combina prendas en capas sobre silueta anatómica o foto personal |
| 🛍️ **Recomendaciones** | Sugerencias de outfits y ofertas de tiendas que combinan con tu ropa |

---

## 🛠️ Tecnologías

- **Android Studio** (Java) — Plataforma de desarrollo
- **Room ORM / SQLite** — Persistencia local del armario
- **CameraX (Jetpack)** — Módulo de escaneo de prendas
- **Google MediaPipe Pose** — Detección de 33 puntos corporales para el vestier
- **Material Design 3** — Sistema de diseño visual (Dark Mode Noir)

---

## 🏗️ Estructura del Proyecto

```
app/src/main/java/com/hakimik/closetscan/
├── data/
│   ├── entity/          # Entidades Room (@Entity)
│   │   ├── PrendaEntity.java
│   │   ├── OutfitEntity.java
│   │   └── TiendaEntity.java
│   ├── dao/             # Data Access Objects (@Dao)
│   │   ├── PrendaDao.java
│   │   └── OutfitDao.java
│   └── AppDatabase.java # Base de datos Room (@Database)
├── ui/
│   ├── dashboard/       # Pantalla 1: Inicio y estadísticas
│   ├── escaner/         # Pantalla 2: CameraX + Segmentación
│   ├── ficha/           # Pantalla 3: Ficha de prenda detectada
│   ├── vestier/         # Pantalla 4: Vestier Interactivo 2.5D
│   └── recomendaciones/ # Pantalla 5: Outfits y ofertas
└── engine/
    ├── VestierEngine.java       # Motor de capas del vestier
    └── RecomendadorEngine.java  # Algoritmo de matching
```

---

## 📊 Estado de Avance

- [x] Requisitos funcionales y no funcionales
- [x] Diseño UX/UI de 5 pantallas en Figma (alta fidelidad)
- [x] Modelo de base de datos relacional (Room/SQLite)
- [x] Diagrama de clases POO en Java (MVVM)
- [x] Prototipo web funcional v2
- [x] Repositorio Git inicializado
- [ ] Navegación Android con BottomNavigationView
- [ ] Base de datos Room con @Entity y @Dao
- [ ] Módulo CameraX para escaneo
- [ ] Vestier Interactivo 2.5D con Canvas
- [ ] Motor de recomendación y matching

---

## 🎨 Prototipo en Figma

Diseño completo de alta fidelidad con Dark Mode Noir:  
🔗 https://www.figma.com/design/0e6RiUd1fq5Me6f0RqLhcI/Closet-Scan-Prototipo-Movil-v1.0

---

## 📚 Referencias Académicas

1. *Mobile Fitting Room: On-device Virtual Try-on* — arXiv:2402.01877
2. *An Android App Using Virtual Trial Room* — Journal of Computer Science, 2023
3. Google MediaPipe Pose — Documentación oficial
4. Android Jetpack Room Persistence Library — Documentación oficial Google
