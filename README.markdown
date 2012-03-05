GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
===============================================================================

GameOfLife WFSCollider is an adapted version of SuperCollider, the audio synthesis engine and programming language, for Wave Field Synthesis spatialization.

It's currently being used in the 192 speakers system of the [Game Of Life Foundation](http://gameoflife.nl/en), based in Leiden, the Netherlands.

WFSCollider consists of an audio spatialization engine that places individual sound sources in space according to the principles of [Wave Field Synthesis](http://en.wikipedia.org/wiki/Wave_field_synthesis).

The system is currently allows to import mono sound files and place them in a score editor where start times, and durations can be set and trajectories or positions assigned to each event.

Each score can be saved as an xml file.

## System Requirements ##

Mac OS X 10.5 or greater
Depends on:

* the wslib, NetLib and XML quarks.
* Unit Lib.
* sc3plugins.

## Download ##

A prepackaged version is available from the download section at [github](https://github.com/GameOfLife/WFSCollider).

## Installation ##

To install, just drag the application to your applications folder.

## Building ##

Get the source:

	git clone --recursive git://github.com/GameOfLife/WFSCollider.git

switch to the wfscurrent branch

	git checkout wfscurrent

Then build according to the general SuperCollider instructions (see readme): In XCode, first build the Synth project, then the plugins project and finally in the language project build the target "WFSCollider". You should then have the application ready in the build folder.

## Acknowledgments ##
WFSCollider was conceived by the Game Of Life Foundation, and developed by W. Snoei, R. Ganchrow and J. Truetzler and M. Negrão.

## License ##
Both SuperCollider and the WFSCollider library are licensed under the GNU GENERAL PUBLIC LICENSE Version 3.  

