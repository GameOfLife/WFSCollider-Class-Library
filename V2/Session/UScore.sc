UScore {

	/*
	*   events: Array[UEvent]
	*/

	//public
	var <>events, <>name = "untitled";
	var pos = 0, <isPlaying = false, <isPaused = false;
	//private
	var <prepareTask, <startTask, <releaseTask, <updatePosTask, <startedAt;

	*new { |... events| 
		^super.newCopyArgs( events.select({ |item| UEvent.implementations.asArray.includes(item.class) }) );
	}

    duplicate { ^UScore( *events.collect( _.duplicate ) ).name_( name ); }

	//ARRAY SUPPORT
	at { |index|  ^events[ index ];  }
	collect { |func|  ^events.collect( func );  }
	do { |func| events.do( func ); }
	first { ^events.first }
	last { ^events.last }

    /*
    * newEvents -> UEvent or Array[UEvent]
    */
	add { |newEvents| events = events ++ newEvents.asCollection.collect( _.asUEvent ) }
	<| { |newEvents| this.add(newEvents) }

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

	cleanOverlaps {

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
	
	start { |targets, startPos = 0, assumePrepared = false, callStopFirst = true, updatePosition = true|
		var prepareEvents, startEvents, releaseEvents;
		if( callStopFirst ) { this.stop(nil,false); }; // in case it was still running

		prepareEvents = events.select({ |evt| evt.startTime >= startPos }).sort;
		startEvents = prepareEvents.sort({ |a,b| a.startTime <= b.startTime });
		releaseEvents = events
			.select({ |item| (item.eventDuration < inf) && { item.eventEndTime >= startPos } })
			.sort({ |a,b| a.eventEndTime <= b.eventEndTime });

		if(prepareEvents.size > 0) {
            if( assumePrepared ) {
                prepareEvents = prepareEvents.select({ |item| item.prepareTime >= startPos });
                this.prStart( targets, startPos, prepareEvents, startEvents, releaseEvents );

            } {
                this.prStart( targets, prepareEvents[0].prepareTime, prepareEvents,
                    startEvents, releaseEvents );
            };
            ^true
		} {
		    ^false
		}
	}
	
	prStart { |targets, startPos = 0, prepareEvents, startEvents, releaseEvents, prepareDoneAction, updatePosition = true|
		startedAt = [ startPos, SystemClock.seconds ];

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
			startEvents.do({ |item,i|
				(item.startTime - pos).wait;
				//when first event starts playing this means that the score has started playing
				if(i == 0) {
				    isPlaying = true;
				    this.changed(\playing);
				};
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
				releaseEvents.last.fadeOut.wait;
				// the score has stopped playing i.e. all events are finished
				startedAt = nil;
				this.pos = this.duration;
				isPlaying = false;
				this.changed( \stop );
			}).start;

		};

        if( updatePosition ) {
            updatePosTask = Task({
                var t = 0;
                var waitTime = 0.1;
                (startEvents[0].startTime - startPos).wait;
                while({t < (this.duration-(startEvents[0].startTime))}, {
                    waitTime.wait;
                    t = t + waitTime;
                    this.changed(\pos, this.pos);
                });

            }).start;
		};

		this.changed( \start, startPos );
	}

	/*
	In case the score is not self-updating the pos variable, then the current pos (which might go on forever as the score plays)
	is given by the ammount of time elapsed since the score started playing;
	*/
	pos {
		^if( startedAt.notNil && isPlaying ) {
			((SystemClock.seconds - startedAt[1]) + startedAt[0]);
		} {
			pos ? 0;
		};
	}

	pos_ { |x|
	    pos = x;
	    this.changed(\pos);
	}

	//stop just the spawning and releasing of events
	stopScore {
		[ prepareTask, startTask, releaseTask, updatePosTask ].do(_.stop);
	}

    //stop synths
	stopChains { |releaseTime = 0.1|
		if( releaseTime.notNil ) {
			events.do(_.release(releaseTime));
		};
	}

	//stop everything
	stop{ |releaseTime, changed = true|
	    //no nil allowed
	    releaseTime = releaseTime ? 0.1;
	    pos = this.pos;
	    startedAt = nil;
	    this.stopScore;
	    this.stopChains(releaseTime);
	    isPlaying = false;
	    isPaused = false;
	    if( changed) {
	        this.changed(\stop)
	    }
	}
	
	pause {
	    if(isPlaying && isPaused.not){
		    this.stopScore;
		    isPaused = true;
		    pos = this.pos;
		    startedAt = nil;
		    this.changed(\paused)
		}
	}
	
	resume { |targets|
	    if(isPlaying && isPaused){
		    this.start( targets, this.pos, true, false );
		    isPaused = false;
		    this.changed(\resumed)
		}
	}

}