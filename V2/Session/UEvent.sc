UEvent {

    classvar <>implementations; // (objectClass:UEventClass, ...)
    var <>object,<>startTime;
    var <>track;  //track number (horizontal segment) on the score editor
    var <>eventDuration = inf;
    var <>targets; //where the event will be played
    var <>muted = false;
    
    // event duration of non-inf will cause the event to release its chain
    // instead of the chain releasing itself

    /*
    * object -> UChain or UScore ?
    */
	*new { |object, startTime = 0, track = 0, eventDuration = inf|

	    var ueventClass = implementations[object.class];
	    if( ueventClass.notNil ) {
	        ^ueventClass.makeNew( object, startTime, track, eventDuration );
	    } {
	        Error("There is no UEvent class implemented for objects of type "++object.class).throw
	    }
	}

	duplicate {
		^UEvent( object.deepCopy, startTime, track, eventDuration ).muted_( muted );
	}

	waitTime { ^object.tryPerform( \waitTime ) ? 0 }
	prepareTime { ^startTime - this.waitTime } // time to start preparing
	
	<= { |that| ^this.prepareTime <= that.prepareTime } // sort support
	
	/*
     UEvent can impose a duration upon uchains using the variable eventDuration.
     The difference is that if a uchain has a non-inf duration, it ends itself but if the eventDuration
     is non-inf, and lower than the uchain duration, it frees the uchain via .release.
     The actual duration of the event is the lowest of these two durations (eventDuration, uchain.duration).
	*/
	duration { ^(object.tryPerform( \duration ) ? inf).min( eventDuration ) }
	dur { ^this.duration }


    endTime { ^startTime + this.duration; } // may be inf
    eventSustain { ^eventDuration - this.fadeOut; }
    eventEndTime { ^startTime + this.eventSustain }

	asUEvent { ^this }

	mute { this.muted_(true) }
	unMute { this.muted_(false) }
	toggleMute { this.muted_(muted.not) }

	printOn { arg stream;
		stream << "a "<< this.class.name << "( " <<* 
			([object, startTime, track] ++ 
			if( eventDuration != inf ) { [ eventDuration ] } { [] })
			<< " )";
	}
	
	prepare { |targets, loadDef = true, action|
		targets = targets ? Server.default;
		targets = targets.asCollection.collect(_.asTarget);
		object.tryPerform( \prepare, targets, loadDef, action );
		this.targets = targets; // remember the servers
	}
	
	start { |targets ...args|
		object.start( targets ? this.targets, *args );
		this.targets = nil; // forget targets
	}
	
	release { |time|
		object.release( time );
	}
	
	doesNotUnderstand { |selector ...args| // forward all calls to object
		var res;
		res = object.perform( selector, *args );
		if( res == object ) { ^this } { ^res };
	}

    /*
    *   server: Server or Array[Server]
    */
    play { |server| // plays a single event plus waittime
        ("preparing "++object).postln;
        object.tryPerform( \prepare, server);
        fork{
            object.waitTime.wait;
            ("playing "++object).postln;
            object.start(server);
            if( eventDuration != inf ) {
	           this.eventSustain.wait;
	           object.release;
            };
        }
    }

}

UScoreEvent : UEvent {

    *initClass{
        if(implementations.isNil) { implementations = IdentityDictionary.new };
        implementations[UScore] = UScoreEvent
    }

	*makeNew { |object, startTime = 0, track = 0, eventDuration = inf|
		^super.newCopyArgs( object, startTime, track, eventDuration );
	}

	makeView{ |i| ^UScoreEventView(this, i) }

    /*trimEnd { |newEnd, removeFade = false|

		var delta = newEnd - startTime;

		if( delta > 0) {

			this.dur = delta;
			if(removeFade){
				this.fadeOut = 0
			};
			//wfsSynth.clipFadeOut;
			if( wfsSynth.wfsPath.class == WFSPath ) {
				wfsSynth.adjustWFSPathToDuration;
			}
		}
	} */

	cutStart{ |newStart, belongsToFolder = false, removeFade = false|
        /*
          belongsToFolder: whether this event is part of a folder, i.e. a score inside a score.
          by splitting the code with the if statement on the belongsToFolder variable unnecessary calculations are avoided
           since in the first case the startTime is always changed, where in the seconde case not necessarily so

        */
        var delta1;
        if( belongsToFolder ) {
	        delta1 = newStart - startTime;
	        startTime = delta1  .neg.max(0);
            if( (this.startTime < newStart) && (this.endTime > newStart) ) {
                object.events = object.events.reject({ |ev|
                    ev.endTime < delta1
                });
                object.events.do{ |ev|
                    ev.cutStart(delta1, true, removeFade)
                };
            }
        } {

	        if( (this.startTime < newStart) && (this.endTime > newStart) ) {
                delta1 = newStart - startTime;
                startTime = newStart;
                object.events = object.events.reject({ |ev|
                    ev.endTime < delta1
                });
                object.events.do{ |ev|
                    //ev.startTime = (ev.startTime - delta1).max(0);
                };
            }

        }


	}

	/*trimStart{ |newStart,removeFade = false|
		var delta1,delta2;
		delta1 = newStart - startTime;
		if(newStart < this.endTime) {
            startTime = newStart;
			this.dur = this.dur - delta1;

			object.events.do{ |ev|
			    ev.
			}

			if(delta1 > 0) {
				//do something when making event shorter
			} {	//making event bigger
				//do something when making event bigger
			}

		}
	}*/
}

UChainEvent : UEvent {

    *initClass{
        if(implementations.isNil) { implementations = IdentityDictionary.new };
        implementations[UChain] = UChainEvent
    }

	*makeNew { |object, startTime = 0, track = 0, eventDuration = inf|
		^super.newCopyArgs( object, startTime, track, eventDuration );
	}

	makeView{ |i| ^UChainEventView(this, i) }

    /*
     When setting fadeout and fadein times each unit much me checked to see if the duration is bigger then
     the duration of the event, such that when calculating the fade times, the actual duration that is going
     to be played in the score is taken into account.
    */
	fadeIn { ^object.fadeIn ? 0 }
	fadeIn_ { |fadeIn|

	    fadeIn = fadeIn.max(0);

		object.units.do({ |unit|
		    var unitDur, unitFadeIn, unitFadeOut;
		    //each unit might have a different dur and fadeOut
			if( unit.def.canFreeSynth ) {
				unitDur = unit.get( \u_dur ).min( eventDuration );
				if( unitDur != inf ) {
			        unitFadeOut = unit.get( \u_fadeOut );
			        unitFadeIn = fadeIn.min( unitDur - unitFadeOut );
			        unit.set( \u_fadeIn, unitFadeIn );
		        } {
				    unit.set( \u_fadeIn, fadeIn );
				}
			};
		});
		object.changed( \fadeIn );
	}

	fadeOut { ^object.fadeOut ? 0 }
	fadeOut_ { |fadeOut|

	    fadeOut = fadeOut.max(0);

		object.units.do({ |unit|
		    var unitDur, unitFadeIn, unitFadeOut;
		    //each unit might have a different dur and fadeOut
			if( unit.def.canFreeSynth ) {
				unitDur = unit.get( \u_dur ).min( eventDuration );
				if( unitDur != inf ) {
			        unitFadeIn = unit.get( \u_fadeIn );
			        unitFadeOut = fadeOut.min( unitDur - unitFadeIn );
			        unit.set( \u_fadeOut, unitFadeOut );
		        } {
				    unit.set( \u_fadeOut, fadeOut );
				}
			};
		});
		object.changed( \fadeOut );
    }

	fadeTimes { ^[this.fadeIn, this.fadeOut] }

	//lets do it like this for the time being
	//then let's think of other options too.
	dur_ { |x|
	    eventDuration = x;
	    object.dur = x; //this will also correct the fade times;
	}

    //events can become bigger
	trimEnd { |newEnd, removeFade = false|
		var delta = newEnd - startTime;
		if( delta > 0) {
			this.dur = delta;
			if( removeFade ) {
				this.fadeOut_(0)
			};
		}
	}

	//events can only become smaller
	cutEnd{ |newEnd, removeFade = false|
        var delta;

        if((this.startTime < newEnd) && (( this.startTime + this.dur ) > newEnd) ) {
            this.dur = newEnd - startTime;
            if( removeFade ) {
                this.fadeOut_(0)
            };
        }
    }

    //events can become bigger
	trimStart{ |newStart,removeFade = false|
		var delta1,delta2;
		delta1 = newStart - startTime;
		if(newStart < this.endTime) {
            startTime = newStart;
			this.dur = this.dur - delta1;
			if(removeFade){
		        this.fadeIn = 0
			};
			if(delta1 > 0) {
				//do something when making event shorter
			} {	//making event bigger
				//do something when making event bigger
			}

		}
	}

	//events can only become smaller
	cutStart{ |newStart, belongsToFolder = false, removeFade = false|
        var delta1;
	    if( belongsToFolder ) {
	        delta1 = newStart - startTime;
	        startTime = delta1.neg.max(0);
	        if( (this.startTime < newStart) && (this.endTime > newStart) ) {
                this.dur = this.dur - delta1;
                if( removeFade ){
                    this.fadeIn = 0
                };
            }
        } {

	        if( (this.startTime < newStart) && (this.endTime > newStart) ) {
                delta1 = newStart - startTime;
	            startTime = newStart;
                this.dur = this.dur - delta1;
                if(removeFade){
                    this.fadeIn = 0
                };
            }

        }



	}



}
