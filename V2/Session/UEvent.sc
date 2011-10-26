/*
    Unit Library
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2011 Miguel Negrao, Wouter Snoei.

    GameOfLife Unit Library: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GameOfLife Unit Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GameOfLife Unit Library.  If not, see <http://www.gnu.org/licenses/>.
*/

UEvent : UArchivable {

    var <startTime=0;
    var <>track=0;  //track number (horizontal segment) on the score editor
    var <duration = inf;
    var <>disabled = false;
    var <releaseSelf = true;

    /*
    If 'releaseSelf' is set to false, then uchains will not free themselves automatically when the events stop playing.
    If 'releaseSelf' is set to true, then uchains will free themselves even if the score is paused;
	*/
    
    // event duration of non-inf will cause the event to release its chain
    // instead of the chain releasing itself

	waitTime { this.subclassResponsibility(thisMethod) }
	prepareTime { ^startTime - this.waitTime } // time to start preparing
	
	<= { |that| ^this.prepareTime <= that.prepareTime } // sort support

    duration_{ this.subclassResponsibility(thisMethod) }
    isPausable_{ this.subclassResponsibility(thisMethod) }
    
    startTime_ { |newTime|
	   startTime = newTime; 
	   this.changed( \startTime )
    }

    endTime { ^startTime + this.duration; } // may be inf
    eventEndTime { ^startTime + this.eventSustain }

	disable { this.disabled_(true) }
	enable { this.disabled_(false) }
	toggleDisable { this.disabled_(disabled.not) }

    /*
    *   server: Server or Array[Server]
    */
    play { |server| // plays a single event plus waittime
        ("preparing "++this).postln;
        this.prepare(server);
        fork{
            this.waitTime.wait;
            ("playing "++this).postln;
            this.start(server);
            if( duration != inf ) {
	           this.eventSustain.wait;
	           this.release;
            };
        }
    }

}