UScore {

	/*
	*   events: Array[UEvent]
	*/

	//public
	var <>events, <>name = "untitled";
	var pos = 0, <isPlaying = false, <isPaused = false;
	//private
	var <prepareTask, <startTask, <releaseTask, <updatePosTask, <startedAt, <pausedAt;

	*new { |... events| 
		^super.newCopyArgs( events );
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
	finiteDuration { ^(this.startTimes + this.durations).select( _ < inf ).maxItem ? 1 }

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

	//SCORE PLAYING

	eventsToPrepareNow{ |startPos|
	    ^events.sort({ |a,b| a.startTime <= b.startTime })
			.select({ |evt| (evt.startTime >= startPos) && { evt.prepareTime < startPos } })
	}

	prepare { |targets, startPos = 0, action|
		action = MultiActionFunc( action );
		targets = targets.asCollection.collect(_.asTarget);
		this.eventsToPrepareNow(startPos).do({ |item|
		    item.prepare( targets, action: action.getAction );
		});
	}

    eventsForPlay{ |startPos, assumePrepared = false|
        var evs, prepareEvents, startEvents, releaseEvents;

        evs = events.select({ |evt| evt.startTime >= startPos }).sort;
		prepareEvents = if(assumePrepared){evs.select({ |item| item.prepareTime >= startPos })}{evs};
		startEvents = evs.sort({ |a,b| a.startTime <= b.startTime });
		releaseEvents = events
			.select({ |item| (item.duration < inf) && { item.eventEndTime >= startPos } })
			.sort({ |a,b| a.eventEndTime <= b.eventEndTime });

		^[prepareEvents,startEvents,releaseEvents]
	}

    //prepares events as fast as possible and starts the playing the score.
	start{ |targets, startPos = 0, updatePosition = true|
	    var prepEvents, servers, prepStartRelEvents, playStatus;
	    targets = targets.asCollection.collect(_.asTarget);
	    servers = targets.collect(_.server);

	    this.stop(nil,false);

	    prepEvents = this.eventsToPrepareNow(startPos);
        prepStartRelEvents = this.eventsForPlay(startPos, true);
	    playStatus = prepStartRelEvents.flat.size > 0;
	    if( playStatus ){
            if(prepEvents.size > 0) {
                fork{
                    prepEvents.do({ |item|
                        item.prepare( targets );
                    });
                    servers.do( _.sync );
                    this.prStartTasks( targets, startPos, prepStartRelEvents, updatePosition );

                };
            } {
                this.prStartTasks( targets, startPos, prepStartRelEvents, updatePosition );

            };
        }
        ^playStatus

	}

    //starts the score some time before startPos in order to give enough time for events to prepare.
    preRollStart{ |targets, startPos = 0, updatePosition = true|
        this.prStart(targets, startPos, false, true, updatePosition)
    }

	prStart { |targets, startPos = 0, assumePrepared = false, callStopFirst = true, updatePosition = true|
		var prepStartRelEvents, playStatus;
		if( callStopFirst ) { this.stop(nil,false); }; // in case it was still running

		prepStartRelEvents = this.eventsForPlay(startPos, assumePrepared).postln;
	    playStatus = prepStartRelEvents.flat.size > 0;

        if( playStatus ){
            this.prStartTasks( targets, startPos, prepStartRelEvents, updatePosition );
        };
        ^playStatus
	}
	
	prStartTasks { |targets, startPos = 0, prepStartRelEvents, updatePosition = true|
        var prepareEvents, startEvents, releaseEvents, preparePos, lastActionIsAStartEvent;
        var dur;
        #prepareEvents, startEvents, releaseEvents = prepStartRelEvents;
        preparePos = if(prepareEvents.size == 0){ startPos }{ prepareEvents[0].prepareTime.min(startPos) };
		lastActionIsAStartEvent = if(startEvents.size == 0){false}{
		    if(releaseEvents.size >0){startEvents.last.startTime >= releaseEvents.last.endTime}{nil}
		};
		startedAt = [ startPos, SystemClock.seconds ];
		("prepareEvents :"++prepareEvents).postln;
		("startEvents :"++startEvents).postln;
		("releaseEvents :"++releaseEvents).postln;



        if( prepareEvents.size > 0 ) {
            prepareTask = Task({
                var pos = preparePos;
                prepareEvents.do({ |item|
                    (item.prepareTime - pos).wait;
                    pos = item.prepareTime;
                    //"prepare % at %, %".format( events.indexOf(item),
                    //	pos, thisThread.seconds ).postln;
                    item.prepare( targets );
                });
            }).start;
        };

        if( startEvents.size > 0 ) {
            startTask = Task({
                var pos = startPos;
                (startPos - preparePos).wait;
                isPlaying = true;
                this.changed(\playing);
                startEvents.do({ |item,i|
                    (item.startTime - pos).wait;
                    pos = item.startTime;
                    //"start % at %, %".format(  events.indexOf(item),
                    //	pos, thisThread.seconds ).postln;
                    item.start;
                });
                if( lastActionIsAStartEvent ) {
                    // the score has stopped playing i.e. all events are finished
                    1.postln;
                    startedAt = nil;
                    this.pos = startEvents.last.startTime;
                    isPlaying = false;
                    this.changed( \stop );
                }
            }).start;
        };

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
				if( lastActionIsAStartEvent.not ) {
                    // the score has stopped playing i.e. all events are finished
                    startedAt = nil;
                    this.pos = releaseEvents.last.endTime;
                    isPlaying = false;
                    this.changed( \stop );
                }
			}).start;

		};

        if( updatePosition ) {
            dur = this.duration;
            updatePosTask = Task({
                var t = startPos;
                var waitTime = 0.1;
                (startPos - preparePos).wait;
                while({t < dur}, {
                    waitTime.wait;
                    t = t + waitTime;
                    this.changed(\pos, t);
                });

            }).start;
		};

		this.changed( \start, startPos );
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
	    //events.do{ _.disposeIfNotPlaying };
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
		    pausedAt = pos;
		    startedAt = nil;
		    this.changed(\paused)
		}
	}
	
	resume { |targets|
	    if(isPlaying.postln && isPaused.postln){
		    this.prStart( targets, pausedAt, true, false );
		    isPaused = false;
		    pausedAt = nil;
		    this.changed(\resumed);
		}
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

}