UScore {

	/*
	*   events: Array[UEvent]
	*/
	var <>events, <>name = "untitled";
	var <prepareTask, <startTask, <releaseTask, <startedAt, <stoppedAt = 0;

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
	
	waitTime {
		(events.select({ |item| item.prepareTime <= 0 })
			.sort({ |a,b| a.prepareTime <= b.prepareTime })[0] ? 0).neg;
	}

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
	
	prepare { |targets, startPos = 0, action|
		action = MultiActionFunc( action );
		targets = targets.asCollection.collect(_.asTarget);
		events.sort({ |a,b| a.startTime <= b.startTime })
			.select({ |evt| (evt.startTime >= startPos) && { evt.prepareTime < startPos } })
			.do({ |item|
				item.prepare( targets, action: action.getAction );
			});
	}
	
	start { |targets, startPos = 0, assumePrepared = false, callStopFirst = true|
		var prepareEvents, startEvents, releaseEvents;
		if( callStopFirst ) { this.stop; }; // in case it was still running
		
		prepareEvents = events.select({ |evt| evt.startTime >= startPos }).sort;
		startEvents = prepareEvents.sort({ |a,b| a.startTime <= b.startTime });
		releaseEvents = events
			.select({ |item| (item.eventDuration < inf) && { item.eventEndTime >= startPos } })
			.sort({ |a,b| a.eventEndTime <= b.eventEndTime });
		if( assumePrepared ) {
			prepareEvents = prepareEvents.select({ |item| item.prepareTime >= startPos });
			this.prStart( targets, startPos, prepareEvents, startEvents, releaseEvents );
		} {
			this.prStart( targets, prepareEvents[0].prepareTime, prepareEvents, 
				startEvents, releaseEvents );
		};
		
	}
	
	prStart { |targets, startPos = 0, prepareEvents, startEvents, releaseEvents|
		startedAt = [ startPos, SystemClock.seconds ];
		stoppedAt = nil;
		
		prepareTask = Task({
			var pos = startPos;
			prepareEvents.do({ |item|
				(item.prepareTime - pos).wait;
				pos = item.prepareTime;
				//"prepare % at %, %".format( events.indexOf(item), 
				//	pos, thisThread.seconds ).postln;
				item.prepare( targets );
			});
		}).start;
		
		startTask = Task({
			var pos = startPos;
			startEvents.do({ |item|
				(item.startTime - pos).wait;
				pos = item.startTime;
				//"start % at %, %".format(  events.indexOf(item), 
				//	pos, thisThread.seconds ).postln;
				item.start;
			});
		}).start;
		
		if( releaseEvents.size > 0 ) {
			releaseTask = Task({
				var pos = startPos;
				releaseEvents.do({ |item|
					(item.eventEndTime - pos).wait;
					pos = item.eventEndTime;
					//"release % at %, %".format( events.indexOf(item), 
					//	pos, thisThread.seconds ).postln;
					item.release;
				});
			}).start;
		};
		
		this.changed( \start, startPos );
	}
	
	pos {
		if( startedAt.notNil ) {
			^((SystemClock.seconds - startedAt[1]) + startedAt[0]).min( stoppedAt ? inf );
		} {
			stoppedAt ? 0;
		};
	}
	
	stop { |releaseTime = 0|
		[ prepareTask, startTask, releaseTask ].do(_.stop);
		stoppedAt = this.pos;
		if( releaseTime.notNil ) {
			events.do(_.release(releaseTime));
		};
		this.changed( \stop );
	}
	
	release { |time = 0.1|
		this.stop( time );
	}
	
	pause { 
		this.stop(nil);
	}
	
	resume { |targets|
		this.start( targets, this.pos, true, false );
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