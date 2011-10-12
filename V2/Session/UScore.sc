UScore : UEvent {
	
	classvar <>activeScores;

	/*
	*   events: Array[UEvent]
	*/

	//public
	var <>events, <>name = "untitled";
	var pos = 0;
	var <playState = \stopped;
	var <>filePath;

	/* playState is a finite state machine. The transitions graph:
                                       stop
        |---------------------------------------paused ----|
        |                                          ^       |
        |-----------|stop                  pause   |       | resume
        v           |                              |       |
     stopped --> preparing --> prepared -----> playing<----|
        ^   prepare       prepare |                |
        |   prepareAndStart       |                |
        |   prepareWaitAndStart   |                |
        |                         |                |
        |-------------------------|stop            |
        |------------------------------------------|stop

    */

	//private
	var <playTask, <updatePosTask, <startedAt, <pausedAt;
	
	*initClass {
		activeScores = Set();
	}

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

	isPlaying{ ^playState == \playing }
	isPaused{ ^playState == \paused }
	isPreparing{ ^playState == \preparing }
	isPrepared{ ^playState == \prepared }
	isStopped{ ^playState == \stopped }
	playState_{ |newState, changed = true|

	    if(changed){
	        this.changed(\playState,newState,playState);  //send newState oldState
	    };
	    playState = newState;
	    
	    if( playState === \stopped ) {
		    activeScores.remove( this );
	    } {
		    activeScores.add( this );
	    };
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
	add { |newEvents| events = events ++ newEvents.asCollection }
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

	findEmptyRegion { |startTime, endTime, startTrack, endTrack|
		^events.select({ |item|
			( (item.startTime <= endTime) and: (item.startTime >= startTime ) ) or:
			( (item.endTime <= endTime) and: (item.endTime >= startTime ) )
		}).collect(_.track).maxItem !? (_+1) ?? {events.collect(_.track).maxItem};

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

    moveEventToEmptyTrack { |evt|
        if( this.checkIfInEmptyTrack( evt ).not ) {
			evt.track = this.findEmptyTrack( evt.startTime, evt.endTime );
		}
    }

	addEventToEmptyTrack { |evt|
		this.moveEventToEmptyTrack(evt);
		events = events.add( evt );
	}

	addEventsToEmptyRegion { |events|
	    var startTime = events.collect(_.startTime).minItem;
	    var endTime = events.collect(_.endTime).maxItem;
	    var startTrack = events.collect(_.track).minItem;
	    var endTrack = events.collect(_.track).maxItem;
	    var startRegion =  this.findEmptyRegion(startTime, endTime, startTrack, endTrack);
	    this <| events.collect{ |x| x.track = x.track + startRegion - startTrack }
	}

	findCompletelyEmptyTrack {
		^( (events.collect(_.track).maxItem ? -1) + 1);
	}

	addEventToCompletelyEmptyTrack { |evt|
		evt.track = this.findCompletelyEmptyTrack;
		events = events.add( evt );

	}

    //need to add a
	cleanOverlaps {
		events.do{ |x| this.moveEventToEmptyTrack(x) }
    }

	//SCORE PLAYING

	eventsToPrepareNow{ |startPos|
	    ^events.sort({ |a,b| a.startTime <= b.startTime })
			.select({ |evt| (evt.startTime >= startPos) && { evt.prepareTime < startPos } && evt.muted.not })
	}

    eventsForPlay{ |startPos, assumePrepared = false|
        var evs, prepareEvents, startEvents, releaseEvents;

        evs = events.select({ |evt| (evt.startTime >= startPos) && evt.muted.not }).sort;
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
			multiAction = MultiActionFunc( {
			    this.playState_(\prepared);
			    action.value;
			} );
			// targets = targets.asCollection.collect(_.asTarget); // leave this to UChain:prepare
			this.playState_(\preparing);
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
	        if(this.isPrepared) {
                this.playState_(\playing);
                this.prStartTasks( targets, startPos, prepStartRelEvents, updatePosition )
            } {
                this.prepare(targets, startPos, {
                    this.playState_(\playing);
                    this.prStartTasks( targets, startPos, prepStartRelEvents, updatePosition )
                });
            }
        }
        ^playStatus

	}

    //prepare during waitTime and start after that, no matter if the prepare succeeded
    prepareWaitAndStart{ |targets, startPos = 0, updatePosition = true|
        this.playState_(\preparing);
        ^this.prStart(targets, startPos, false, true, updatePosition)
    }

	prStart { |targets, startPos = 0, assumePrepared = false, callStopFirst = true, updatePosition = true|
		var prepStartRelEvents, playStatus;
		if( callStopFirst ) { this.stop(nil,false); }; // in case it was still running

		prepStartRelEvents = this.eventsForPlay(startPos, assumePrepared);
	    playStatus = prepStartRelEvents.flat.size > 0;

        if( playStatus ){
            this.playState_(\playing);
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
        preparePos = if(prepareEvents.size == 0){ startPos }{ prepareEvents[0].prepareTime.min(startPos)�};
        deltaToStart = startPos - preparePos;
        if(deltaToStart !=0){
            fork{
                deltaToStart.wait;
                this.playState_(\playing)
            }
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
                    this.pos = 0;
                    this.playState_(\stopped);
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
	    if([\playing,\paused].includes(playState) ) {
            //no nil allowed
            releaseTime = releaseTime ? 0.1;
            pos = this.pos;
            startedAt = nil;
            this.stopScore;
            this.stopChains(releaseTime);
            events.select(_.isFolder).do(_.stop);
            //events.do{ _.disposeIfNotPlaying };
            this.playState_(\stopped,changed);
	    }
	}
	
	pause {
	    if(playState == \playing){
		    this.stopScore;
		    events.select(_.isFolder).do(_.pause);
		    pos = this.pos;
		    pausedAt = pos;
		    startedAt = nil;
		    this.playState_(\paused);
		    //events.select(_.isFolder).do(_.pause);
		}
	}
	
	resume { |targets|
	    if(playState == \paused){
		    this.prStart( targets, pausedAt, true, false );
		    events.select(_.isFolder).do(_.resume);
		    pausedAt = nil;
		}
	}

	/*
	In case the score is not self-updating the pos variable, then the current pos (which might go on forever as the score plays)
	is given by the ammount of time elapsed since the score started playing;
	*/
	pos {
		^if( startedAt.notNil && this.isPlaying ) {
			((SystemClock.seconds - startedAt[1]) + startedAt[0]);
		} {
			pos ? 0;
		};
	}

	pos_ { |x|
	    pos = x;
	    this.changed(\pos, x);
	}

	edit{ ^UScoreEditorGUI(this) }

	printOn { arg stream;
		stream << "a " << this.class.name << "( " << events.size <<" events )"
	}

	save {
	    filePath !? { |x| this.write(x,true, { |x| filePath = x}) } ?? {
	        this.saveAs
	    }
	}

	saveAs { |path|
	    this.write(path, true, { |x| filePath = x})
	}

	readTextArchive { |pathname|
	    super.readTextArchive(pathname);
	    filePath = pathname;
    }

    *readTextArchive { |pathname|
        var res = super.readTextArchive(pathname);
        ^if(res.notNil) {
            res.filePath_(pathname)
        } {
            res
        }
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