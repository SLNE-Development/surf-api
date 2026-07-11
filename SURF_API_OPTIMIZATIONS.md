# Surf API – kompatible Optimierungen und API-Review

## Ziel und Kompatibilitätsregeln

Dieses Dokument protokolliert die schrittweise Prüfung und Überarbeitung des gesamten
`surf-api`-Repositories. Öffentliche API wird nicht entfernt. Bestehende Signaturen bleiben
binär kompatibel; neue API und Deprecations sind zulässig. Implementierungsmodule dürfen
intern stärker refaktoriert werden.

## Ausgangslage

- Branch: `version/26.2`
- Startzustand: sauberer Git-Worktree
- Umfang: ungefähr 840 Kotlin-/Java-Quelldateien in Core, Paper, Velocity, Shared,
  Standalone, Gradle-Plugin und Generator
- Automatisierte Tests: im Ausgangszustand keine regulären `src/test`-Tests gefunden
- ABI-Basis: `./gradlew checkKotlinAbi` erfolgreich
- Baseline-Compilerwarnungen: redundantes `-Xcontext-parameters`, ein unsicherer Map-Cast,
  redundante Varianz und ein redundanter exhaustiver `when`-Zweig sowie zwei
  Context-Parameter-Namenswarnungen in Paper

## Umgesetzte Änderungen

### Core API und Core-Implementierung

1. **Unendliche nullable Zufallssequenz korrigiert**
   - `RandomSelector.sequenceOrNull()` verwendet nun einen `sequence`-Builder mit expliziter
     Endlosschleife.
   - Grund: `generateSequence { ... }` interpretiert `null` als Ende der Sequenz. Dadurch
     endete die alte API beim ersten erfolglosen Pick – bei `successRate = 0.0` sogar sofort,
     obwohl eine unendliche `Sequence<E?>` dokumentiert ist.
   - Kompatibilität: keine Signaturänderung; reine Verhaltenskorrektur.

2. **Convex-Hull-Sonderfall mit genau einem Punkt korrigiert**
   - `ConvexHull2D.compute()` gibt den einzelnen Punkt zurück.
   - Grund: der abschließende Duplikat-Abbau entfernte vorher bei einer Eingabe der Größe eins
     den einzigen Punkt und lieferte fälschlich eine leere Hülle.
   - Nebenbei wurde die lokale Variable von `points` zu `sortedPoints` umbenannt, um Shadowing
     zu vermeiden.
   - Kompatibilität: keine Signaturänderung; reine Verhaltenskorrektur.

3. **Standalone-PacketEvents-Fallback wirklich als No-op implementiert**
   - Nullable Zugriffe auf Plugin, Channel und Netty-Operatoren liefern nun `null`.
   - `isPlayerSet()` liefert `false`.
   - Grund: ein No-op-Adapter darf bei normalen Capability-Abfragen nicht durch verbliebene
     `TODO()`-/`UnsupportedOperationException`-Platzhalter abstürzen.
   - Kompatibilität: nur internes Servermodul; robusteres Verhalten.

4. **Compiler-Warnungen ohne API-Änderung reduziert**
   - Redundante Varianz in `SurfComponentBuilderImpl` entfernt.
   - Unmöglichen `else`-Zweig im exhaustiven `NbtOps`-`when` entfernt.

5. **Fastutil-Extensions automatisch und verlustfrei generiert**
   - Die bisherige, knapp 2.800 Zeilen lange `fast-util-util.kt` wird nicht mehr manuell
     gepflegt. `GenerateFastutilExtensions` erzeugt sie unter `build/generated/sources` und ist
     als Quelle sowie als Abhängigkeit von KSP, Kotlin-Kompilierung und Sources-JAR verdrahtet.
   - Die Schablonen liegen getrennt unter
     `surf-api-core/surf-api-core/src/codegen/fastutil/`: Header, Object-/Primitive-Set,
     Object-/Primitive-List sowie die verschiedenen Map-Familien sind eigenständige
     `.kt.template`-Dateien. Die Generator-Klasse enthält keine Kotlin-Quellcode-Stringblöcke.
   - Die Typmatrix enthält alle Fastutil-Primitive. Für Map-Keys wird `Boolean` ausgelassen, weil
     Fastutil keine `Boolean2*`-Familie bereitstellt.
   - Additiv generiert werden direkte `BooleanArray`/`ByteArray`/`CharArray`/`ShortArray`/
     `IntArray`/`LongArray`/`FloatArray`/`DoubleArray`-Konvertierungen zu spezialisierten Sets
     sowie die zuvor fehlenden `DoubleArray`-Konvertierungen zu `DoubleList`.
   - ABI-Nachweis: Gegen den vor der Umstellung gesicherten Core-Dump wurden **0 entfernte** und
     **18 hinzugefügte** Methoden ermittelt. `checkKotlinAbi` ist mit den separaten Templates
     erfolgreich.

6. **Transformierende Set-Views konsistent gemacht**
   - Nicht transformierbare Werte werden nun auch beim Iterieren tatsächlich übersprungen.
   - `size` und `isEmpty` spiegeln die sichtbare View statt des ungefilterten Backing-Sets.
   - Fehlgeschlagene Rücktransformationen fügen kein `null` mehr ein und werden bei
     `containsAll` nicht mehr stillschweigend ignoriert.
   - Bulk-Add/Remove vermeidet temporäre Listen bzw. Sets, soweit die Operation dies erlaubt.
   - Kompatibilität: Signaturen bleiben unverändert; das Verhalten entspricht jetzt der
     bestehenden Filter-Dokumentation und dem `Set`-Vertrag.

7. **Typisierte Fast-NBT-Listen korrigiert**
   - `FastCompoundBinaryTag.getList(key, expectedType, defaultValue)` prüft jetzt
     `ListBinaryTag.elementType()` statt `ListBinaryTag.type()`.
   - Grund: der alte Vergleich prüfte immer den Container-Typ `LIST` gegen den erwarteten
     Elementtyp und lieferte deshalb für gültige typisierte Listen den Defaultwert.

8. **Optionale asynchrone Playerprofile korrigiert**
   - `awaitAsyncPlayerProfileOptional()` verwendet `firstOrNull()`.
   - Grund: eine erfolgreich aufgelöste, aber leere Trefferliste ist ein reguläres
     „nicht gefunden“ und darf in der optionalen API keine `NoSuchElementException` auslösen.

9. **Additive RandomSelector-Mehrfachauswahl**
   - `pickMany(count)` und `pickManyOrNull(count, successRate)` ergänzen die bestehende API,
     ohne Interface-Implementierer um neue abstrakte Methoden zu erweitern.
   - Beide Helfer validieren negative Counts. Die nullable Variante behält `null`-Ergebnisse
     als Listenelemente bei.
   - Der interne Flow-Reservoir-Selector verwendet zusätzlich einen separaten
     Initialisierungsstatus statt `selectedElement!!`; dadurch sind auch nullable Elementtypen
     korrekt abbildbar.

10. **Event-Registrierung ergonomischer abmeldbar**
   - Der additive Extension-Helfer `SurfEventBus.unregister(token)` benennt die Abmeldung von
     Tokens aus `on`/`onAsync` eindeutig und delegiert kompatibel an `unregisterListeners`.

11. **Message-Bundle-Ladepfad repariert und effizienter gemacht**
   - Fehlende Bundles werden nun tatsächlich aus den gebündelten Ressourcen kopiert; vorher
     wurde die Data-Folder-Liste versehentlich mit sich selbst verglichen.
   - Punktnotation wie `messages.example` wird korrekt in
     `messages/example[_locale].properties` übersetzt.
   - Root-Locale-Dateien erhalten keinen überflüssigen Unterstrich; lokalisierte
     Ressourcennamen enthalten wieder den Basisnamen.
   - Nach dem Ergänzen fehlender Keys werden Bundles neu geladen, sodass der Translator nicht
     mit veralteten `ResourceBundle`-Instanzen arbeitet.
   - Ein `HashSet` ersetzt die lineare Namenssuche beim Kopieren. Ein Temp-Verzeichnis-Test deckt
     Root- und `de`-Bundle ab.
   - Wiederholtes `load()` ersetzt die zuvor registrierte Translation-Source, statt weitere
     globale Sources anzusammeln. Die JVM-Locale-Liste wird einmal gecacht und ohne mehrere
     Sequence-Zwischenstufen durchsucht.

12. **Pagination-Überlauf verhindert**
   - Die Seitenanzahl wird als `((count - 1) / pageSize) + 1` berechnet. Die vorherige Formel
     `count + pageSize - 1` konnte bei großen Collections einen `Int`-Überlauf erzeugen.
   - Ungültige negative Counts und nichtpositive Seitengrößen werden explizit abgewiesen.

### Shared

13. **Annotation-Cache threadsicher gemacht**
   - Zugriffe auf die inneren, nicht threadsicheren `HashMap`-Instanzen erfolgen nun innerhalb
     desselben Locks wie Zugriffe auf die äußere synchronisierte `WeakHashMap`.
   - Grund: `Collections.synchronizedMap` synchronisiert nur Operationen der äußeren Map. Das
     bisherige `getOrPut` plus anschließende Mutation der inneren Map konnte bei paralleler
     Komponenten-Erkennung Datenrennen erzeugen.
   - Kompatibilität: keine API- oder Verhaltensänderung im Single-Thread-Fall.

### Paper

14. **Context-Parameter stabil benannt**
   - Die geschützten View-Hooks verwenden jetzt konsistent `modificationContext`.
   - Grund: überschreibende DSL-Klassen warnten vor abweichenden Parameternamen, was bei
     benannten Argumenten problematisch werden kann. Die Namen sind jetzt über die
     Vererbungshierarchie konsistent.
   - Kompatibilität: JVM-Signaturen bleiben unverändert; Kotlin-Quellnutzung wird stabiler.

15. **Zeitüberspringen terminiert zuverlässig**
   - Default-Dauern werden aus dem Betrag der Zeitspanne berechnet und auf mindestens einen Tick
     begrenzt; kleine Spannen führen nicht mehr zur Division durch null.
   - Die Schrittweite wird aus verbleibender Zeit und verbleibenden Ticks neu berechnet. Auch
     `duration > abs(timeToAdd)` endet daher garantiert statt mit Schrittweite null zu hängen.
   - `duration <= 0` wird mit einer dokumentierten `IllegalArgumentException` abgewiesen.
   - Der `skippingWorlds`-Marker wird in `finally` entfernt, auch bei Cancellation oder Fehlern.
   - Multiworld-Varianten erzeugen nicht mehr erst eine Map von `Deferred` und anschließend eine
     zweite Ergebnis-Map, sondern sammeln parallele Paare direkt in die Ergebnis-Map.

16. **Additiver Erfolgsstatus für TimeSkipResult**
   - `TimeSkipResult.isSuccess` ergänzt den vorhandenen `toBoolean()`-Aufruf als lesbare Kotlin-
     und Java-API. `toBoolean()` bleibt erhalten und delegiert an die neue Property.

17. **Scoreboard- und Coroutine-Lifecycle gehärtet**
   - Der Auto-Updater wird vor dem Schließen des Scoreboards abgebrochen und nullable behandelt;
     `disable()` kann dadurch keinen `updater!!`-Fehler mehr erzeugen.
   - Veraltete Long-Tick-Delays wurden auf `ticksDuration` umgestellt.
   - Suspend-Pagination-Sources verzichten auf eine identische `thenApply { it }`-Future-Stufe.

### Build

18. **Redundantes Kotlin-Compiler-Flag entfernt**
   - `-Xcontext-parameters` ist mit der eingesetzten Kotlin-Sprachversion 2.4 bereits Standard
     und wurde bei jedem Modul als redundant gemeldet.
   - Grund: weniger Build-Noise und keine Abhängigkeit von einem inzwischen überflüssigen Flag.

19. **Generator- und Gradle-Plugin-Allokationen reduziert**
   - Der NMS-Modulgenerator verarbeitet den `FileTreeWalk` lazy statt zunächst alle Quelldateien
     in eine Liste zu materialisieren und prüft das Löschen des Zielverzeichnisses.
   - Die Dependency-Auswertung des Surf-Gradle-Plugins befüllt Artifact- und Project-Sets in
     einem Durchlauf statt Dependency-Liste plus mehreren `map`-/`toSet`-Zwischenstufen.
   - Fehlende Generator-Ressourcen liefern eine verständliche Fehlermeldung statt eines nackten
     `NullPointerException` durch `!!`.

20. **I/O und Diagnose in Server-Helfern verbessert**
   - `.env` wird zeilenweise mit `useLines` gelesen statt komplett in den Speicher geladen.
   - Flogger-Fehlerpfade für `.env` und Component-Properties verwenden `withCause`, sodass die
     Exception nicht als überzähliges Formatargument verloren geht.
   - Der Mojang-Skin-Fetcher prüft HTTP-Statuscodes und parst JSON direkt aus dem Response-Reader,
     ohne vorher einen vollständigen Response-String und zwei Map-Sequenzen anzulegen.

### Zweiter Performance- und Visualizer-Durchgang

21. **Visualizer-Chunk-Hotpath von linear auf indexiert umgestellt**
   - Der Multi-Location-Visualizer pflegt zusätzlich einen `chunkKey -> entityIds`-Index.
     Chunk-Load und Chunk-Unload kopieren und durchsuchen dadurch nicht mehr für jedes Event den
     vollständigen Punktbestand, sondern nur noch die Punkte des betroffenen Chunks.
   - Ein `location -> entityIds`-Index ersetzt die lineare Suche über veränderliche
     `VisualPoint`-Hash-Keys. Entfernen ist damit im Regelfall O(1); nachträgliche Mutation von
     Display-Settings kann den Index nicht mehr beschädigen.
   - Position, Chunkkoordinaten und Chunk-Key eines `VisualPoint` werden einmal berechnet statt
     bei Sichtbarkeitsprüfung und Paketerzeugung wiederholt konvertiert zu werden.
   - Bulk-Adds tragen Punkte unter einem einzigen Write-Lock ein. Der bestehende API-Overload für
     gemeinsame Settings wird intern überschrieben, sodass keine `Pair`-Liste mehr entsteht.

22. **Visualizer-Viewer-, Entity- und Coroutine-Lifecycle korrigiert**
   - Single-Location-Visualizer verfolgen explizit, welchem Viewer das Entity tatsächlich
     gesendet wurde. Ein Wechsel von einem unsichtbaren in einen sichtbaren Chunk erzeugt nun ein
     Spawn- statt eines wirkungslosen Teleport-Pakets; doppelte Spawn-/Despawn-Pakete entfallen.
   - Verzögerte Single-Visualizer-Updates prüfen eine State-Version und laufen auf dem
     Entity-Dispatcher. Veraltete Updates können nach Stop/Restart nicht mehr erneut spawnen.
   - Player-Quit verarbeitet nur noch die dem Spieler zugeordneten Visualizer statt global alle
     Instanzen zu durchsuchen. Leere Reverse-Index-Sets werden entfernt; Chunk-Dispatch erzeugt
     keine temporären Active-Visualizer-Listen mehr.
   - Area-Recompute fängt Fehler pro Berechnung ab, statt den einzigen Worker dauerhaft sterben zu
     lassen. Versionsprüfungen verhindern, dass ein älteres asynchrones Height-Ergebnis nach einer
     neueren Konfiguration wieder Punkte einträgt.
   - Height-Auflösung materialisiert keine `Pair<Vector3d, Settings>`-Listen mehr und verliert
     Pending-Punkte bei einem fehlgeschlagenen Snapshot-Load nicht stillschweigend.

23. **Voxel-Tracing terminiert für beliebige Koordinaten und besitzt einen Eager-Hotpath**
   - Der Bresenham-Pfad arbeitet mit den ganzzahligen Voxelkoordinaten der Endpunkte. Bei
     unterschiedlichen Nachkommastellen konnte die bisherige Double-Schleife das Ziel
     überschreiten und unendlich laufen.
   - Additiv steht `VoxelLineTracer.traceTo(p0, p1, destination)` bereit. Der Area-Visualizer nutzt
     diesen direkten Collection-Pfad und vermeidet damit pro Polygonkante den coroutine-basierten
     `Sequence`-State und Iterator.
   - Die bestehende lazy `trace`-API bleibt erhalten und liefert für ganzzahlige Eingaben weiterhin
     dieselbe Punktfolge.

24. **Weighted-Random-Sampling ohne Distribution pro Pick**
   - `RandomSelectorImpl` hält normalisierte kumulative Gewichte in primitiven `DoubleArray`s und
     sucht per Binärsuche. Der Deprecated-Kompatibilitäts-Overload mit JDK-`RandomGenerator`
     konstruiert nicht mehr bei jedem Pick eine neue Apache-Distribution samt PMF-Kopie.
   - Die Skalierung am Maximalgewicht verhindert einen Überlauf der Summe endlicher Gewichte.
   - Nullable Flow-/Sequence-Schleifen validieren die Erfolgsrate einmal und nicht bei jedem
     emittierten Element erneut.

25. **Component-, Event- und Reflection-Caches beschleunigt und korrigiert**
   - Component-Abhängigkeiten werden über ein Set geladener Klassennamen statt per wiederholtem
     linearem Scan geprüft. `@ConditionalOnMissingComponent` lädt die Zielklasse einmal pro
     Bedingung statt einmal pro bereits geladenem Component.
   - Post-Processor erzeugen nur noch den finalen `ComponentEntry`; Reverse-Lifecycle-Schleifen
     laufen über Indizes statt kopierte `reversed()`-Listen. Der Topological Sort benötigt keinen
     zusätzlichen Guava-Graphen mehr.
   - Der EventBus cached die reflektierten Handler-Methoden per `ClassValue`, liest den Coroutine-
     Context einmal pro Dispatch und schützt seinen Dispatch-Cache mit einer Handler-Version gegen
     Concurrent-Register/Dispatch-Races.
   - `SurfTypeParameterMatcher` berücksichtigt im Cache nun auch den parametrisierten Zieltyp.
     Zuvor kollidierten etwa `First<T>` und `Second<T>` derselben Implementierung bei gleichem
     Parameternamen. Der Cache ist jetzt zusätzlich class-unloading-freundlich.
   - `AnnotationUtils` verwendet einen lockfreien `ClassValue`-/`ConcurrentHashMap`-Cache statt
     eines global synchronisierten `WeakHashMap`-Hotpaths.

26. **Core-Collection-, Config- und NBT-Mikroallokationen entfernt**
   - `NoDuplicates` erkennt Duplikate in einem Durchlauf und bricht beim ersten Treffer ab, statt
     Iterable/Array erst als Liste und danach nochmals als Set zu kopieren.
   - Enum-/ID-Map-Helfer füllen Fastutil-Maps direkt; die vorherige boxed Kotlin-Map als
     Zwischenstufe entfällt.
   - Der fehlertolerante Map-Serializer erzeugt Key-Cleanup-Sets nur noch, wenn `WriteKeyBack`
     beziehungsweise `clearInvalids` aktiv ist.
   - Pagination berechnet die identische Indent-Komponente einmal pro Row-Collection.
   - `NbtOps.createByteList` respektiert Position und Limit des `ByteBuffer`; `createList`
     konsumiert den Stream direkt in den Builder statt über eine Zwischenliste.

27. **PacketOperation für alle NMS-Versionen abgeflacht**
   - Kombinierte Packet-Operationen bilden keine rekursive `andThen`-Lambda-Kette mehr, sondern
     hängen ihre Operationen an eine flache `ArrayList` an.
   - Die Ausführung verwendet eine kompakte `ArrayList` für Pakete statt einer `LinkedList` mit
     einem Node-Objekt pro Paket. Besonders Multi-Point-Visualizer, Lore und Glow vermeiden damit
     tausende Lambda-/Listenknoten und tiefe Aufrufketten.
   - Die öffentliche `PacketOperation`-API bleibt unverändert; nur die drei versionierten
     Implementierungen wurden refaktoriert.

28. **Lore- und Glowing-Packetpfade auf Copy-on-Write reduziert**
   - Container-Lore-Pakete kopieren die Itemliste erst ab dem ersten tatsächlich geänderten Slot.
     Bei ausschließlich globalen Handlern entfällt zusätzlich die kombinierte Handlerliste samt
     erneuter Sortierung für jedes Item.
   - Glowing-Listener geben Bundles ohne Player-Glow-State sofort unverändert zurück und kopieren
     Entity-Data nur, wenn das Flag wirklich ergänzt werden muss.
   - Farbteams werden Folia-sicher in einer Concurrent Map erzeugt. Das Entfernen eines Entities
     sendet immer dessen Team-Removal; der bisherige boolesche Viewer-Marker ließ bei mehreren
     Entities derselben Farbe spätere Team-Einträge clientseitig zurück.
   - Leere per-Player Entity-Maps werden unmittelbar aus dem globalen Glow-State entfernt.

29. **Paper-Chunk- und Block-PDC-Arbeit gebündelt**
   - `computeHighestYBlock` startet höchstens 16 Worker-Coroutines, die Chunk-Keys über einen
     atomaren Index abarbeiten. Zuvor wurde pro Chunk eine Coroutine erzeugt, von denen die meisten
     an einem Semaphore warteten; auch die Plugin-Auflösung erfolgt nur noch einmal.
   - Dirty-Block-Cleanup wird einmal pro Tick gebündelt statt für jeden markierten Block einen
     separaten Scheduler-Job zu starten. Ein kompakter `DirtyBlockKey` vermeidet `Location`,
     `BlockPosition` und `Pair` pro Lookup.
   - Structure-/Fertilize-Events laufen direkt über `BlockState`; Piston-Moves benötigen nur noch
     eine Reverse-Snapshot-Liste statt `toList().reversed()`.

30. **Weitere Laufzeit-Mikrooptimierungen**
   - `GlyphShift` berechnet Enum-Einträge direkt aus dem gesetzten Bit statt über eine Fastutil-Map,
     reserviert die exakte Stringkapazität und behandelt nun auch `Int.MIN_VALUE` korrekt.
   - `ViewBlockCellComponent` indexiert sechs geordnete Enumwerte direkt; Navigation-History
     entfernt leere Deques unmittelbar.
   - Scoreboard-Gradienten und Component-Disable durchlaufen bestehende Listen rückwärts, ohne
     `reversed()`-Kopien zu erzeugen.
   - Player-Lookup normalisiert Namen für den Cache, parst Fehlerantworten nicht als JSON und lässt
     Coroutine-Cancellation wieder unverändert passieren statt sie als einminütigen Fehler zu
     cachen.

31. **Generator- und KSP-Durchgang vertieft**
   - Registry-JSON wird streaming-basiert und mit sicher geschlossenem InputStream dekodiert.
     Veralteter Outputpfad und falsches Generated-Package wurden auf das aktuelle Core-Modul
     korrigiert.
   - Häufig verwendete Advancement-RegExes und NMS-Rename-RegExes werden einmal kompiliert statt
     pro Key beziehungsweise pro Quelldatei.
   - Der Component-KSP-Prozessor sammelt Module, Components und Post-Processor ohne mehrere
     `map`-/`toList`-/`toSet`-Zwischenstufen; die kürzeste Meta-Annotationsebene wird einpassig
     bestimmt.
   - Standalone-Shutdown verwendet ein erfolgreiches CAS statt bei jedem wiederholten Aufruf einen
     atomaren Write auszuführen.

32. **Visualizer-Races bei Clear, Stop, Restart und parallelen Punktänderungen geschlossen**
   - `clearVisualLocations()` führt bereits vorbereitete Despawns auch dann noch aus, wenn
     gleichzeitig gestoppt wird. Zuvor war der Sent-State schon geleert, während die
     Versionsprüfung das einzige Despawn verwerfen konnte; das Entity blieb dann clientseitig
     dauerhaft sichtbar.
   - Der Multi-Visualizer validiert und markiert Entity-IDs nun unter demselben Write-Lock, unter
     dem Spawn-Operationen zusammengestellt werden. Ein paralleles `remove`/`clear` kann dadurch
     nicht mehr zwischen einem alten Snapshot und dem Send ein Geister-Entity erzeugen.
   - Cleaner entfernen Viewer- und Reverse-Index-Zustand auch bei einem bereits leeren
     Punktbestand. Ein World-Wechsel des Single-Location-Visualizers verwendet einen vollständigen
     Respawn statt eines dimensionsübergreifend wirkungslosen Teleport-Pakets.
   - Area-Visualizer erzwingen nach jedem Neustart eine neue Berechnung. So kann ein beim Stop
     konsumiertes und anschließend abgebrochenes Channel-Signal keinen partiellen Zustand
     hinterlassen. Die Sichtbarkeitssuche läuft chunkweise, dekodiert Koordinaten einmal und
     beendet die Viewer-Suche beim ersten Treffer.

33. **Weitere Core-Algorithmen und Grenzfälle optimiert**
   - `RandomSelectorImpl` baut seine primitiven Gewichtsarrays direkt aus dem Input auf; die
     vorherige `Object2DoubleMap.Entry`-Allokation pro Element entfällt. Auch der unendliche
     Reservoir-Flow skaliert am jeweils größten Gewicht und friert bei einer überlaufenden
     Gewichtssumme nicht mehr ein.
   - Convex-Hull sortiert eine Array-Kopie direkt statt `List` plus Fastutil-Kopie und verdichtet
     identische 2D-Koordinaten in-place. Eine Eingabe aus ausschließlich identischen Punkten
     liefert jetzt korrekt genau einen Hull-Punkt.
   - Pagination berechnet nicht nur die Seitenzahl, sondern auch Start und Ende einer Seite mit
     `Long`-Zwischenwerten. Die letzte Seite einer Collection nahe `Int.MAX_VALUE` verschwindet
     dadurch nicht mehr wegen eines negativen, übergelaufenen Endindex.
   - Config-Size-Constraints berücksichtigen jetzt auch alle primitiven JVM-Arrays. Der
     caller-sensitive StackWalker verwendet den Stream direkt statt eines Kotlin-Sequence-Wrappers.

34. **Packet-Listener- und Chat-Chain-Lifecycle weiter gehärtet**
   - Beide Packet-Listener-Systeme registrieren und entfernen Listener pro Packet-Klasse atomar.
     Leere `CopyOnWriteArraySet`s und ihre Class-Keys bleiben nach Plugin-Unloads nicht mehr
     dauerhaft in den drei NMS-Providern liegen; eine parallele Neuregistrierung kann dabei nicht
     in einem bereits aus der Map entfernten Set landen.
   - Die NMS-Chat-Message-Chain vervollständigt ihr `CompletableFuture` nun auch dann, wenn der
     übergebene Coroutine-Scope schon vor Ausführung des Blocks abgebrochen wurde. Vorher konnte
     die gesamte nachfolgende Chat-Chain dauerhaft warten.
   - Creative-Tab-Indices bewahren für identische Item-/Component-Varianten den ersten statt des
     letzten Index. Das entspricht der dokumentierten Reihenfolge und dem bereits verwendeten
     Item-Type-Fallback.
   - Auch der Core-EventBus entfernt nach `unregisterListeners` leere Priority- und Event-Type-
     Einträge atomar; Listener-Neuregistrierung und Cleanup können kein verwaistes Set erzeugen.

35. **Paper-PDC-, Placeholder- und Command-Races korrigiert**
   - `CustomBlockData.readFromBytes()` persistiert die eingelesenen Daten jetzt wie alle anderen
     mutierenden PDC-Operationen zurück in den Chunk. Die Block-Key-Erzeugung verwendet direkt das
     Surf-Plugin und löst nicht mehr bei jedem Block einen StackWalk zur Plugin-Erkennung aus.
   - PAPI behandelt einen leeren Parameterstring tatsächlich über `parseWithNoParams()`. Der alte
     `split` lieferte hierfür eine Liste mit einem leeren String, wodurch der dokumentierte Pfad
     unerreichbar war. Normale Requests vermeiden zugleich `split` plus `drop`-Doppelkopie.
   - Suspend-Requirements prüfen die Verbindung vor und nach dem Eintrag in den Ready-State.
     Ein Disconnect während einer laufenden Prüfung kann dadurch keine UUID in Ready-/Blocker-Sets
     zurücklassen und kein `updateCommands()` auf einer getrennten Verbindung auslösen.

36. **Dialog-, Inventory- und Scoreboard-Laufzeit stabilisiert**
   - Dialog-State-Transformationen sind mit einem Mutex serialisiert; eine Versionsnummer verwirft
     ältere, langsamer fertig gewordene Render-Ergebnisse. `remember` kann nullable Werte über
     einen Sentinel cachen, statt bei `ConcurrentHashMap` mit einer Null-Value zu scheitern oder
     den Block bei jedem Render erneut auszuführen.
   - `CursorState` validiert wie `PageState` eine positive Seitengröße. Paginated Views erzeugen
     ihr Layout direkt als fertiges Array statt Nullable-Array plus nachträglicher Prüfung.
   - Auto-Player-Scoreboards rufen `addPlayer` nicht mehr alle fünf Ticks für jeden bereits
     bekannten Online-Spieler erneut auf. Getrennte UUIDs werden aus dem lokalen Tracking entfernt,
     damit Reconnects weiterhin sauber hinzugefügt werden.

37. **Weitere kleine Allokations- und Korrekturverbesserungen**
   - Chunk-Snapshots werden während paralleler Height-Berechnung in eindeutig indexierte Arrays
     geschrieben und erst danach in eine primitive `Long2ObjectOpenHashMap` überführt. Damit
     entfallen boxed Long-Keys und Synchronisationskosten der vorherigen `ConcurrentHashMap`.
   - `World.doInChunkAsync` vervollständigt sein Deferred auch bei einem `Error`; zuvor konnte der
     Aufrufer bei einem Nicht-`Exception`-Throwable unbegrenzt warten.
   - Resource-Pack-Varargs verwenden `copyOfRange` statt `drop`-Liste plus Array. ASM-Descriptoren
     des Inventory-Remappers sind Konstanten und werden nicht pro besuchter Instruktion neu gebaut.

## Verifikation

- `./gradlew updateKotlinAbi`: erfolgreich.
- Finaler ABI-Diff gegenüber `HEAD`: **0 entfernte Zeilen/Signaturen**, ausschließlich additive
  Einträge.
  - Core: 18 Fastutil-Konvertierungen, 2 RandomSelector-Helfer, 1 EventBus-Helfer und
    `VoxelLineTracer.traceTo`.
  - Paper: `TimeSkipResult.isSuccess`.
- Aktueller Testbestand: 20 `@Test`-Fälle für RandomSelector, ConvexHull, Fastutil,
  transformierende Sets, Message-Bundles, Fast-NBT, NBT-Ops, Type-Matching, Voxel-Tracing und
  Pagination; alle erfolgreich.
- `./gradlew check`: erfolgreich, 115 Tasks (42 ausgeführt, 73 up-to-date). Dabei wurden Core,
  Core-Server, Shared, Paper-API/-Server, alle drei NMS-Versionen, Velocity, Standalone,
  Generator, KSP-Prozessor und Gradle-Plugin geprüft beziehungsweise kompiliert.
- Fastutil-Generator plus `sourcesJar` mit `--configuration-cache`: erfolgreich; Cache-Eintrag
  wurde gespeichert und die generierte `fast-util-util.kt` ist Bestandteil des Sources-JARs.

## Bewusst verbleibende Empfehlungen

- Das Gradle-Plugin kombiniert Gradles eingebettetes Kotlin `2.3.20` mit dem Projekt-Kotlin
  `2.4.0`. Gradle warnt bei der Konfiguration vor möglichem unterschiedlichem Verhalten. Eine
  Behebung verlangt eine bewusste Toolchain-Entscheidung (Plugin-Kotlin angleichen oder
  Projekt-Kotlin zurücksetzen) und wurde nicht als Mikrorefactoring angenommen.
- Velocities Annotation Processor meldet beim KAPT-Lauf fehlende Originating Sources für
  `velocity-plugin.json`. Der Build ist erfolgreich; die Warnung liegt im externen Processor.
- Plattformgebundene Pfade wie echte Paper/Folia-Scheduler-Wechsel, Packet-Injection und
  Visualizer-Pakete benötigen Integrationstests auf laufenden Servern. Die vorhandene
  `surf-api-paper-plugin-test`-Quelle wird kompiliert, enthält aber keine automatisierte
  Server-Test-Harness.
- Die bStats-`Metrics.java`-Dateien sind vendored Upstream-Code und wurden trotz einzelner
  Deprecation-Hinweise nicht lokal umgeschrieben; Updates sollten als vollständiges
  Upstream-Upgrade erfolgen.
