UEvent {

    var <>object, <>startTime, <>track;
    var <>eventDuration = inf;
    var <>targets;
    
    // event duration of non-inf will cause the event to release its chain
    // instead of the chain releasing itself

	*new { |object, startTime = 0, track = 0, eventDuration = inf|
		^super.newCopyArgs( object, startTime, track, eventDuration );
	}

	waitTime { ^object.tryPerform( \waitTime ) ? 0 }
	prepareTime { ^startTime - this.waitTime } // time to start preparing
	
	<= { |that| ^this.prepareTime <= that.prepareTime } // sort support
	
	durarion { ^(object.tryPerform( \duration ) ? inf).min( eventDuration ) }
	dur { ^this.duration }
	
	fadeIn { ^object.tryPerform( \getFadeIn ) ? 0 }
	fadeOut { ^object.tryPerform( \getFadeOut ) ? 0 }

     endTime { ^startTime + this.duration; } // may be inf
     eventSustain { ^eventDuration - this.fadeOut; }
     eventEndTime { ^startTime + this.eventSustain }
     

	asUEvent { ^this }

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