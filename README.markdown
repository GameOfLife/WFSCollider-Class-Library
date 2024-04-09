GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
===============================================================================

GameOfLife WFSCollider is a library for SuperCollider, the audio synthesis engine and programming language, for Wave Field Synthesis spatialization.

It's currently being used in the 192 speakers system of the [Game Of Life Foundation](http://www.gameoflife.nl), based in The Hague, the Netherlands.

WFSCollider consists of an audio spatialization engine that places individual sound sources in space according to the principles of [Wave Field Synthesis](http://en.wikipedia.org/wiki/Wave_field_synthesis).

The system allows soundfiles, live input and synthesis processes to be placed in a score editor where start times, and durations can be set and trajectories or positions assigned to each event. It also allows realtime changement of parameters and on the fly starting and stopping of events via GUI or OSC control. Each event can be composed of varous objects ("units") in a processing chain.

Score files are saved as executable SuperCollider code. The system is setup in a modular way and can be scripted and expanded using the SuperCollider language.

WFSCollider is distributed as a 'Quark' for SuperCollider. For installation instructions see: [www.gameoflife.nl/software](http://www.gameoflife.nl/software).


## System Requirements ##

macOS 10.14 or later, Linux or Windows 64bits

Depends on:

* [SuperCollider 3.13](https://supercollider.github.io/)
* the Unit-Lib, NetLib, wslib and XML quarks.
* [sc3plugins](https://supercollider.github.io/sc3-plugins/) (optional)

## Installation ##

- Install SuperCollider
- Install sc3plugins (optional)
- in SuperCollider, run the following line:
`Quarks.install( "WFSCollider-Class-Library" );`
- Recompile Class Library (via menu option in SuperCollider)
- to use WFSCollider, run the following line in SuperCollider
`WFSLib.startup;`

## Acknowledgments ##
WFSCollider was conceived by the Game Of Life Foundation, and developed by W. Snoei, R. Ganchrow and J. Truetzler and M. Negrão.

## License ##
Both SuperCollider and the WFSCollider library are licensed under the GNU GENERAL PUBLIC LICENSE Version 3.  

