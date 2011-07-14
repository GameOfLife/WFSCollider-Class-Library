UEvent {

    classvar <waitTime = 1; //5 seconds delay to load buffers.

	var <>uchain, <>track;
    var <>startTime, <>dur, <>fadeIn, <>fadeOut;

	*new { |uchain, track = 0, startTime = 0, dur = 10, fadeIn = 1, fadeOut =1 |
		^super.newCopyArgs( uchain, track = 0, startTime, dur, fadeIn, fadeOut );
	}

	<= { |that| ^startTime <= that.startTime } // sort support

    endTime { ^startTime + this.dur; }

	asUEvent { ^this }

	printOn { arg stream;
		stream << "a "<< this.class.name << "( " <<* [uchain, track, startTime, dur, fadeIn, fadeOut] << " )";
	}

	prEventUnit {
	    ^U(\eventEnv, [\eventEnv,EventEnv(dur,fadeIn,fadeOut)] )
	}

    prPannerUnit {
        ^U(\output)
    }

	prChainForPlay {
        ^uchain <| [this.prEventUnit, this.prPannerUnit]
    }

    /*
    *   server: Server or Array[Server]
    */
    play { |server|
        ("preparing "++uchain).postln;
        uchain.prepare(server);
        fork{
            waitTime.wait;
            ("playing "++uchain).postln;
            this.prChainForPlay.start(server);
        }
    }

}