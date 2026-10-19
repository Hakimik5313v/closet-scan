# FUNDACIÓN UNIVERSITARIA COLOMBO INTERNACIONAL (UNICOLOMBO)

**FACULTAD DE INGENIERÍA**  
**PROGRAMA DE INGENIERÍA DE SISTEMAS / TECNOLOGÍA EN DESARROLLO DE SOFTWARE**  
**SEMESTRE 3**  
**CARTAGENA DE INDIAS D.T. y C.**  
**2026**

---

# Desarrollo de un Vestier Digital Interactivo con Inteligencia Artificial y Realidad Aumentada para la Gestión y Recomendación de Armario Personal Utilizando Android Studio y Java

---

### NOMBRES Y APELLIDOS COMPLETOS DE LOS AUTORES:
- **Ricardo Antonio López Pérez** — Código: 2520180018
- **Freys Torres**
- **Kenneth Ruiz**
- **Juan Camilo Moreno**

---

### DIRECTOR Y ASESOR DEL PROYECTO:
**Ing. Carlos Castro**  
Docente de Seminario de Investigación

---

# Contenido

- **Resumen**
- **Palabras clave**
- **1. Introducción**
- **2. Planteamiento del Problema**
  - 2.1. Descripción de la situación problema
  - 2.2. Delimitación del problema
  - 2.3. Formulación de la pregunta de investigación
- **3. Justificación**
- **4. Objetivos**
  - 4.1. Objetivo General
  - 4.2. Objetivos Específicos
- **5. Marco Referencial**
  - 5.1. Marco Teórico (Visión por Computador, MediaPipe, Persistencia Room y Teoría Cromática)
  - 5.2. Marco de Antecedentes (Virtual Try-On y referentes móviles)
- **6. Metodología y Avance de Desarrollo (Segundo Corte)**
  - 6.1. Arquitectura de Software implementada
  - 6.2. Capa de persistencia local (Room Database y DAOs)
  - 6.3. Motor de lógica cromática (`RecomendadorEngine`)
- **7. Cronograma de Sprints de Desarrollo**
- **8. Referencias Bibliográficas**

---

# Resumen

En el marco de la asignatura de Seminario de Investigación en la Fundación Universitaria Colombo Internacional (UNICOLOMBO), correspondiente al tercer semestre de Ingeniería de Sistemas / Tecnología en Desarrollo de Software, se presenta el avance de desarrollo del proyecto titulado **Closet/Scan**. El proyecto consiste en el diseño e implementación de un vestier digital interactivo y sistema de armario inteligente desarrollado de manera nativa para dispositivos móviles Android utilizando programación en Java y persistencia local con Android Jetpack Room.

La investigación aborda la problemática cotidiana de la desorganización de prendas, el tiempo excesivo invertido en la selección de vestimenta y la dificultad para visualizar combinaciones armónicas sin necesidad de manipular físicamente la ropa. Para solucionar esto, el software integra técnicas de visión por computador para el recorte automático de prendas (generación de imágenes PNG con transparencia), un motor algorítmico de teoría del color para evaluar la compatibilidad cromática de outfits (armonías monocromáticas, análogas y complementarias), y una interfaz interactiva de vestier 2.5D sobre silueta anatómica. Para este segundo corte académico, se presenta la consolidación de los requerimientos formales, el diseño arquitectónico de base de datos relacional y la construcción del repositorio de código con las entidades, DAOs y el motor de recomendación algorítmico.

### Palabras clave:
Vestier digital, Visión por computador, Android Studio, Java, Room SQLite, Teoría del color, Armario inteligente, Virtual Try-On.

---

# 1. Introducción

En la actualidad, la transformación digital y la computación móvil han permeado actividades esenciales de la vida cotidiana. No obstante, la gestión del vestuario personal continúa realizándose predominantemente de forma empírica y manual. Esta desconexión entre las pertenencias físicas del usuario y las herramientas digitales disponibles provoca ineficiencias en el uso del tiempo diario y decisiones de compra redundantes o poco informadas.

Este documento presenta el desarrollo formal de **Closet/Scan**, una aplicación móvil desarrollada en el entorno oficial Android Studio utilizando el lenguaje de programación Java. El sistema permite al usuario digitalizar su guardarropa mediante la cámara del dispositivo móvil, aislar las prendas eliminando el fondo para almacenarlas de forma estructurada en una base de datos local basada en SQLite (mediante la biblioteca Room ORM), y probar diversas configuraciones de conjuntos sobre una silueta interactiva antes de vestirse.

La estructura del presente informe sigue los lineamientos metodológicos institucionales de UNICOLOMBO: se inicia con el planteamiento y delimitación del problema, la formulación de la pregunta de investigación, la justificación del estudio, los objetivos general y específicos aprobados, el marco referencial compuesto por el marco teórico y de antecedentes, finalizando con el reporte del avance del software en código fuente y arquitectura para este segundo corte evaluativo.

---

# 2. Planteamiento del Problema

### 2.1. Descripción de la situación problema
En la vida moderna, las personas se enfrentan diariamente al dilema de seleccionar qué prendas vestir para sus diversas actividades académicas, laborales o sociales. Diversos estudios de comportamiento del consumidor indican que una persona invierte entre 15 y 20 minutos diarios en la elección de vestimenta, lo que representa cerca de 6 meses a lo largo de su vida adulta dedicados únicamente a esta tarea.

Este escenario se agrava debido a:
1. **Desorganización y falta de visibilidad:** Las personas suelen utilizar repetidamente menos del 30% de la ropa que poseen debido a que el resto permanece oculta en el fondo del clóset.
2. **Fatiga de decisión e inseguridad estética:** La falta de conocimientos formales sobre teoría del color y armonía estética genera dudas sobre la combinación de prendas superiores, inferiores y calzado.
3. **Pérdida de tiempo en cambios físicos:** El proceso tradicional de probarse físicamente diferentes conjuntos frente al espejo genera desgaste de las prendas, desorden físico y retrasos constantes en las rutinas matutinas.

### 2.2. Delimitación del problema
El presente proyecto se delimita al desarrollo de un prototipo funcional móvil para dispositivos con sistema operativo **Android 8.0 (API nivel 26) o superior**. El alcance se enfoca en prendas de vestuario cotidiano (categorías superior, inferior y calzado), excluyendo en esta etapa indumentarias complejas de alta costura o simulación física 3D avanzada de pliegues en tiempo real, priorizando un modelo eficiente de capas 2.5D que ejecute localmente sin requerir hardware de gama alta.

### 2.3. Formulación de la pregunta de investigación
A partir de la problemática expuesta, se formula la siguiente pregunta central:

> **¿Cómo desarrollar un vestier digital interactivo mediante una aplicación móvil nativa en Android Studio (Java) que integre visión por computador y principios de teoría del color, para optimizar la gestión del armario personal y la toma de decisiones al vestir?**

---

# 3. Justificación

La ejecución de este proyecto se justifica desde tres perspectivas complementarias:

1. **Relevancia Social y Práctica:** Brinda al usuario una herramienta que reduce drásticamente el tiempo diario dedicado a la selección de vestimenta, promoviendo el consumo sostenible al redescubrir prendas en desuso y disminuyendo la compra impulsiva de ropa innecesaria.
2. **Aporte Técnico y Académico:** Aplica conceptos rigurosos de ingeniería de software aprendidos en el programa: arquitectura en capas, patrones de diseño de software (Singleton, DAO, Repository), manejo de bases de datos relacionales en dispositivos embebidos mediante Android Room, e implementación de algoritmos cromáticos basados en el espacio de color HSV.
3. **Viabilidad de Innovación:** Demuestra que mediante el aprovechamiento de los sensores de cámara y las bibliotecas modernas de Google Jetpack, es factible crear probadores virtuales eficientes directamente en dispositivos de consumo masivo sin depender de costosos servidores en la nube.

---

# 4. Objetivos

### 4.1. Objetivo General
**Desarrollar un vestier digital interactivo como aplicación móvil en Android Studio (Java), utilizando técnicas de Inteligencia Artificial (Visión por Computador) y Realidad Aumentada, para la gestión y digitalización del armario personal, permitiendo generar recomendaciones personalizadas de outfits y comparar precios de prendas complementarias en tiendas locales.**

### 4.2. Objetivos Específicos
1. **Diagnosticar e Identificar:** Levantar y documentar los requerimientos funcionales y técnicos del sistema respecto a la clasificación de prendas, almacenamiento de metadatos y criterios de combinación estética.
2. **Diseñar Arquitectura y Datos:** Diseñar la arquitectura lógica de la aplicación bajo el patrón arquitectónico recomendado para Android, junto con el modelo entidad-relación para persistencia local en Room SQLite.
3. **Desarrollar Módulo de Visión Artificial:** Construir el módulo de captura con CameraX y segmentación de imágenes para extraer prendas sin fondo en formato PNG transparente y detectar su tonalidad cromática dominante.
4. **Construir Motor de Recomendación y Vestier:** Implementar el motor de compatibilidad cromática en Java y el lienzo interactivo del vestier en capas superpuestas sobre silueta anatómica.
5. **Validar y Evaluar:** Evaluar la precisión del recorte de prendas, la consistencia de las recomendaciones generadas y el nivel de usabilidad percibido por usuarios en pruebas piloto.

---

# 5. Marco Referencial

### 5.1. Marco Teórico
- **Lenguaje Java y Android SDK:** Java es un lenguaje orientado a objetos, robusto y multiplataforma. En el ecosistema Android, permite una manipulación precisa de memoria y control sobre los ciclos de vida de componentes (`Activity`, `Fragment`).
- **Android Jetpack Room:** Biblioteca de abstracción sobre SQLite nativo. Room proporciona validación de consultas SQL en tiempo de compilación, elimina el código boilerplate y permite mapear objetos Java a tablas relacionales mediante anotaciones formales (`@Entity`, `@Dao`, `@Database`).
- **Visión por Computador y MediaPipe Pose:** Biblioteca de Google basada en redes neuronales convolucionales optimizadas para dispositivos móviles, capaz de inferir 33 puntos de referencia anatómicos (hombros, cadera, rodillas) para alinear las prendas con la postura del usuario.
- **Teoría del Color y Armonía Cromática:** Estudio matemático de las relaciones de color en el círculo cromático. Se evalúan colores primarios y secundarios mediante el modelo HSV (Matiz, Saturación, Valor), estableciendo reglas para armonías complementarias (colores opuestos en 180°), análogas (colores vecinos en ±30°) y tonos neutros universales (blanco, negro, gris y denim).

### 5.2. Marco de Antecedentes
- **VTON (Virtual Try-On):** Investigaciones recientes como *"Mobile Fitting Room: On-device Virtual Try-on"* (arXiv:2402.01877) demuestran que la inferencia local de prueba de ropa en celulares es el estándar emergente frente a plataformas web centralizadas.
- **Sistemas de Armario Digital:** Proyectos académicos y comerciales previos (como Stylebook o Cladwell) evidencian que el 82% de los usuarios prefieren sistemas con categorización visual rápida y sugerencias automatizadas frente a hojas de cálculo o inventarios de texto plano.

---

# 6. Metodología y Avance de Desarrollo (Segundo Corte)

Para la ejecución del software se adoptó la metodología ágil **Scrum**, organizada en ciclos iterativos de desarrollo (Sprints). Para la entrega de este Segundo Corte, se consolidaron los siguientes componentes técnicos:

### 6.1. Arquitectura de Software
Se estructuró el proyecto en el paquete base `com.hakimik.closetscan`, dividiéndolo en paquetes funcionales limpios:
```
app/src/main/java/com/hakimik/closetscan/
├── data/
│   ├── AppDatabase.java            (Configuración Singleton de base de datos)
│   ├── dao/
│   │   └── PrendaDao.java          (Operaciones CRUD de base de datos)
│   └── entity/
│       ├── PrendaEntity.java       (Modelo relacional de prenda)
│       └── OutfitEntity.java       (Modelo relacional de outfit)
└── engine/
    └── RecomendadorEngine.java     (Motor de armonía de color)
```

### 6.2. Persistencia de Datos con Room
Se crearon las entidades Java anotadas para Room:
- **`PrendaEntity`:** Modela la tabla `prendas` con identificador autoincremental, nombre, categoría funcional (`SUPERIOR`, `INFERIOR`, `CALZADO`), código hexadecimal de color, ruta local de imagen PNG transparente y estado de favorito.
- **`PrendaDao`:** Interfaz que define las operaciones de inserción, actualización, eliminación y consultas filtradas por categoría y favoritas mediante sintaxis `@Query`.
- **`AppDatabase`:** Implementación del patrón Singleton para garantizar una única instancia abierta de conexión SQLite, optimizando el consumo de memoria RAM del teléfono.

### 6.3. Motor de Recomendación Cromática (`RecomendadorEngine`)
Se codificó la clase lógica que aplica la matriz de compatibilidad de color para emparejar prendas superiores con inferiores, validando combinaciones neutras y calculando puntajes de afinidad de 0 a 100 para asesorar al usuario.

### 6.4. Versionamiento en GitHub
Todo el avance de código se encuentra alojado y versionado en el repositorio oficial de GitHub del equipo:  
👉 **Repositorio:** [https://github.com/lopezperezricardo0316-a11y/closet-scan](https://github.com/lopezperezricardo0316-a11y/closet-scan)

---

# 7. Cronograma de Sprints de Desarrollo

| Sprint | Módulo de Trabajo | Estado al Segundo Corte |
| :---: | :--- | :---: |
| **Sprint 1** | Definición de Requisitos, Modelo Entidad-Relación y Arquitectura Java | **✅ 100% Completado** |
| **Sprint 2** | Capa de Persistencia Room (Entidades, DAOs, Database) y Motor Cromático | **✅ 100% Completado** |
| **Sprint 3** | Configuración de entorno Android Studio, Gradle y módulo de Cámara (CameraX) | **⏳ Programado Tercer Corte** |
| **Sprint 4** | Lienzo Interactivo 2.5D con capas de prendas superpuestas | **⏳ Programado Tercer Corte** |
| **Sprint 5** | Pruebas de integración, validación de usabilidad y sustanciación final | **⏳ Programado Tercer Corte** |

---

# 8. Referencias Bibliográficas

1. Google Developers. (2024). *Save data in a local database using Room*. Android Jetpack Documentation. https://developer.android.com/training/data-storage/room
2. Google AI Edge. (2024). *Pose landmark detection guide for Android*. MediaPipe Solutions. https://ai.google.dev/edge/mediapipe/solutions/vision/pose_landmarker/android
3. Itten, J. (1970). *The Elements of Color*. Van Nostrand Reinhold Company.
4. Pressman, R. S. (2010). *Ingeniería del Software: Un enfoque práctico* (7ma ed.). McGraw-Hill Interamericana.
5. Zhang, L., et al. (2024). *Mobile Fitting Room: On-device Virtual Try-on*. arXiv preprint arXiv:2402.01877.
