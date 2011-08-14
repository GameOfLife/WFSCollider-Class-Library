UScore {

	/*
	*   events: Array[UEvent]
	*/
	var <>events, <>name = "untitled";

	*new { |... events| 
		^super.newCopyArgs( events.select({ |item| item.class == UEvent }) );
	}

	//ARRAY SUPPORT
	at { |index|  ^events[ index ];  }
	collect { |func|  ^events.collect( func );  }
	do { |func| events.do( func ); }
	first { ^events.first }
	last { ^events.last }

	add { |event| events = events.add( event.asUEvent ); }
	<| { |event| this.add(event) }

	<< { |score|
	    ^UScore(*(events++score.events))
	}

	size { ^events.size }
	
	startTimes { ^events.collect( _.startTime ); }
	durations { ^events.collect( _.dur ); }
	
	duration { ^(this.startTimes + this.durations).maxItem ? 0; }
	dur { ^this.duration } 

	track { |track = 0| ^this.class.new( events.select({ |event| event.track == track }) ); }
	
	sort { events.sort; }

    //TRACK RELATED
	findEmptyTrack { |startTime = 0, endTime = inf|
		var evts, tracks;

		evts = events.select({ |item|
			(item.startTime <= endTime) and: (item.endTime >= startTime )
		});

		tracks = evts.collect(_.track);

		(tracks.maxItem+2).do({ |i|
			if( tracks.includes( i ).not ) { ^i };
		});
	}

	checkIfInEmptyTrack { |evt|
		var evts, tracks;

		evts = events.detect({ |item|
			(item.startTime <= evt.endTime) and:
			(item.endTime >= evt.startTime ) and:
			(item.track == evt.track)
		});

		^evts.isNil;
	}

	addEventToEmptyTrack { |evt|
		if( this.checkIfInEmptyTrack( evt ).not ) {
			evt.track = this.findEmptyTrack( evt.startTime, evt.endTime );
		};
		events = events.add( evt );

	}

	findCompletelyEmptyTrack {
		^( (events.collect(_.track).maxItem ? -1) + 1);
	}

	addEventToCompletelyEmptyTrack { |evt|
		evt.track = this.findCompletelyEmptyTrack;
		events = events.add( evt );

	}
    /*
    *   server: Server or Array[Server]
    */
	play{ |server, startTimeOffset = 0|

		var playEvents = events
		    .sort
			.select( _.startTime >= startTimeOffset );
        ("playing "++events).postln;
		^Task({
			var currentTime;
			currentTime = startTimeOffset;
			playEvents.do({ |event|
				var delta;
				delta = (event.startTime - currentTime);
				delta.wait;
				event.play( server );
				currentTime = currentTime + delta;
				});
		}, WFSSynth.clock).start;

	}

}