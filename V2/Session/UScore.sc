UScore : UEvent {

	/*
	*   events: Array[UEvent]
	*/

	//public
	var <>events, <>name = "untitled";
	var pos = 0, <isPlaying = false, <isPaused = false, <isPreparing = false;
	//private
	var <playTask, <updatePosTask, <startedAt, <pausedAt;

	*new { |... events| 
		^super.new.init( events );
	}
	
	/*
	* Syntaxes for UScore creation:
	* UScore( <UEvent 1>, <UEvent 2>,...)
	* UChain(startTime,<UEvent 1>, <UEvent 2>,...)
	* UChain(startTime,track,<UEvent 1>, <UEvent 2>,...)
	*/

	init{ |args|
		if( args[0].isNumber ) { 
			startTime = args[0]; 
			args = args[1..] 
		};
		if( args[0].isNumber ) { 
			track = args[0]; 
			args = args[1..] 
		};
	    events = args;
	    
	    this.changed( \init );
	}

    duplicate { ^UScore( *events.collect( _.duplicate ) ).name_( name ); }

    makeView{ |i,maxWidth| ^UScoreEventView(this,i,maxWidth) }
    isFolder{ ^true }
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
	getAllUChains{
	    ^events.collect(_.getAllUChains).flat
	}
	startTimes { ^events.collect( _.startTime ); }
	durations { ^events.collect( _.dur ); }
	
	duration { ^(this.startTimes + this.durations).maxItem ? 0; }
	dur { ^this.duration }
	finiteDuration { ^(this.startTimes + this.durations).select( _ < inf ).maxItem ? ((this.startTimes.maxItem ? 0) + 10) }
    isFinite{ ^this.duration < inf}

    //mimic a UChain
    eventSustain{ ^inf }
    release{ ^this.stop }

    cutStart{}

    cutEnd{}


	waitTime {
		^(events.collect(_.prepareTime).minItem ? 0).min(0).neg
	}

	fromTrack { |track = 0| ^this.class.new( events.select({ |event| event.track == track }) ); }
	
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

    eventsForPlay{ |startPos, assumePrepared = false|
        var evs, prepareEvents, startEvents, releaseEvents;

        evs = events.select({ |evt| evt.startTime >= startPos }).sort;
		prepareEvents = if(assumePrepared){evs.select({ |item| item.prepareTime >= startPos })}{evs};
		startEvents = evs.sort({ |a,b| a.startTime <= b.startTime });
		releaseEvents = events
			.select({ |item| (item.duration < inf) && { item.eventEndTime >= startPos } && item.isFolder.not })
			.sort({ |a,b| a.eventEndTime <= b.eventEndTime });

		^[prepareEvents,startEvents,releaseEvents]
	}

    //prepare resources needed to play score, i.e. load buffers, send synthdefs.
	prepare { |targets, startPos = 0, action|
	    var eventsToPrepareNow, multiAction;
	    eventsToPrepareNow = this.eventsToPrepareNow(startPos);
	    if( eventsToPrepareNow.size > 0 ) {
			multiAction = MultiActionFunc( { isPreparing = false; action.value; } );
			// targets = targets.asCollection.collect(_.asTarget); // leave this to UChain:prepare
			isPreparing = true;
			this.eventsToPrepareNow(startPos).do({ |item|
			    item.prepare( targets, action: multiAction.getAction );
			});
	    } {
		    action.value; // fire immediately if nothing to prepare
	    };
	}

    //start immediately, assume prepared by default
    start{ |targets, startPos = 0, updatePosition = true|
        ^this.prStart(targets, startPos, true, true, updatePosition)
    }

    //prepares events as fast as possible and starts the playing the score.
	prepareAndStart{ |targets, startPos = 0, updatePosition = true|
	    var prepEvents, servers, prepStartRelEvents, playStatus;

	    this.stop(nil,false);

        prepStartRelEvents = this.eventsForPlay(startPos, true);
	    playStatus = prepStartRelEvents.flat.size > 0;

	    if( playStatus ){
            this.prepare(targets, startPos, {
                this.prStartTasks( targets, startPos, prepStartRelEvents, updatePosition )
            });
        }
        ^playStatus

	}

    //prepare during waitTime and start after that, no matter if the prepare succeeded
    prepareWaitAndStart{ |targets, startPos = 0, updatePosition = true|
        ^this.prStart(targets, startPos, false, true, updatePosition)
    }

	prStart { |targets, startPos = 0, assumePrepared = false, callStopFirst = true, updatePosition = true|
		var prepStartRelEvents, playStatus;
		if( callStopFirst ) { this.stop(nil,false); }; // in case it was still running

		prepStartRelEvents = this.eventsForPlay(startPos, assumePrepared);
	    playStatus = prepStartRelEvents.flat.size > 0;

        if( playStatus ){
            this.prStartTasks( targets, startPos, prepStartRelEvents, updatePosition );
        };
        ^playStatus
	}
	
	prStartTasks { |targets, startPos = 0, prepStartRelEvents, updatePosition = true|
        var prepareEvents, startEvents, releaseEvents, preparePos, allEvents, deltaToStart;
        var dur;
        var actions;

        actions = [
            { |event| event.prepare( targets ) },
            { |event| event.start },
            { |event| event.release }
        ];

        #prepareEvents, startEvents, releaseEvents = prepStartRelEvents;
        //("prepareEvents :"++prepareEvents).postln;
		//("startEvents :"++startEvents).postln;
		//("releaseEvents :"++releaseEvents).postln;

		allEvents = prepareEvents.collect{ |x| [x.prepareTime, 0, x]}
         ++ startEvents.collect{ |x| [x.startTime, 1, x]}
         ++ releaseEvents.collect{ |x| [x.eventEndTime, 2, x]};

        //if the time for the event to happen is different order them as usual
        //if they happen at the same time then the order is prepare < start < release
        allEvents = allEvents.sort{ |a,b|
            if(a[0] != b[0]) {
                a[0] <= b[0]
            } {
                a[1] <= b[1]
            }
        };


        //this allows to be able to get the current pos when the update task is not running
		startedAt = [ startPos, SystemClock.seconds ];

        //this is for prepareWaitAndStart
        preparePos = if(prepareEvents.size == 0){ startPos }{ prepareEvents[0].prepareTime.min(startPos) };
        deltaToStart = startPos - preparePos;
        if(deltaToStart !=0){
            fork{
                this.changed(\preparing);
                isPreparing = true;
                deltaToStart.wait;
                this.changed(\playing);
                isPlaying = true;
                isPreparing = false;
            }
        }{
            this.changed(\playing);
            isPlaying = true;
        };

        if( allEvents.size > 0) {
            playTask = Task({
                var pos = preparePos;
                allEvents.do({ |item|
                    (item[0] - pos).wait;
                    pos = item[0];
                    //"prepare % at %, %".format( events.indexOf(item),
                    //	pos, thisThread.seconds ).postln;
                    actions[item[1]].value(item[2]);
                });
                if( this.isFinite ) {
                    (this.duration - pos).wait;
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
                while({t <= dur}, {
                    waitTime.wait;
                    t = t + waitTime;
                    this.pos_(t);
                });

            }).start;
		};

		this.changed( \start, startPos );
	}

	//stop just the spawning and releasing of events
	stopScore {
		[playTask, updatePosTask ].do(_.stop);
	}

    //stop synths
	stopChains { |releaseTime|
		events.do(_.release(releaseTime ? 0.1));
	}

	//stop everything
	stop{ |releaseTime, changed = true|
	    //no nil allowed
	    releaseTime = releaseTime ? 0.1;
	    pos = this.pos;
	    startedAt = nil;
	    this.stopScore;
	    this.stopChains(releaseTime);
	    events.select(_.isFolder).do(_.stop);
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
		    events.select(_.isFolder).do(_.pause);
		    isPaused = true;
		    pos = this.pos;
		    pausedAt = pos;
		    startedAt = nil;
		    this.changed(\paused);
		    //events.select(_.isFolder).do(_.pause);
		}
	}
	
	resume { |targets|
	    if(isPlaying && isPaused){
		    this.prStart( targets, pausedAt, true, false );
		    events.select(_.isFolder).do(_.resume);
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
	    pos = if(x>=this.duration){0}{x};
	    this.changed(\pos, pos);
	}
	
	printOn { arg stream;
		stream << "a " << this.class.name << "( " << events.size <<" events )"
	}
	
	getInitArgs {
		var numPreArgs = -1;
		
		if( track != 0 ) {
			numPreArgs = 1
		} {
			if( startTime != 0 ) {
				numPreArgs = 0
			}
		};
		
		^([ startTime, track ][..numPreArgs]) ++ events;
	}
	
	storeArgs { ^this.getInitArgs }
	
}