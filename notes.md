## Verantwortlichkeiten

- Robert: BackEnd-Entwicklung, Dokumentation
- Leon: FrontEnd-Entwicklung, (GUI)
- Thomas: Dokumentation



## Zeitplan

gesamte Bearbeitungszeit: 03.05.2021 - 30.07.2021 (12 Wochen)

| Woche(n) | Aufgabe bzw. _Meilenstein_                                   |
| -------- | ------------------------------------------------------------ |
| 1 - 4    | Ausgangssituation, bestehende Software, Anforderungen analysieren |
| 5 -  6   | Konzept entwickeln, _fertigstellen_                          |
| 6 - 10   | Beginn Prototyp-Entwicklung, währenddessen Dokumentation des Prozesses |
| 11       | _Entwicklung des Prototypen abschließen_                     |
| 12       | _Dokumentation abschließen_                                  |



## Framework

- OpenSource verwenden - Mycroft (Holmes)



### STT

| Framework                                                    | pro                                                        | contra                                                       |
| ------------------------------------------------------------ | ---------------------------------------------------------- | ------------------------------------------------------------ |
| PocketSphinx                                                 | - OpenSource<br />- offline-fähig<br />- Custom Wake Words | - nicht präzise                                              |
| Google Cloud STT                                             | - sehr ausgereift                                          | - GOOGLE !11!!111!<br />- nicht offline-fähig (vielleicht)<br />- nicht OpenSource |
| Mozilla DeepSpeech                                           | - OpenSource<br />- offline-fähig                          | - nicht präzise                                              |
| [Kaldi](http://publications.idiap.ch/downloads/papers/2012/Povey_ASRU2011_2011.pdf) | - OpenSource                                               |                                                              |
| IBM Watson STT                                               |                                                            |                                                              |
| wit.ai                                                       |                                                            |                                                              |

### Verarbeitung

| Framework  | pro                                                          | contra                                         |
| ---------- | ------------------------------------------------------------ | ---------------------------------------------- |
| MyCroft AI | - OpenSource<br />- individuell anpassbar<br />- offline nutzbar | - nicht besonders ausgereift                   |
| DialogFlow | - sehr ausgereift<br />- anpassbar                           | - nur online nutzbar<br />- GOOGLE !!111!1!!!! |
| Almond     | - OpenSource                                                 |                                                |
| Leon       |                                                              |                                                |

### TTS

| Framework  | pro                               | contra |
| ---------- | --------------------------------- | ------ |
| Coqui AI   | - OpenSource<br />- offline-fähig |        |
| Google TTS |                                   |        |
| Mimic 1    |                                   |        |

