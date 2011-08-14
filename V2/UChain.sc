
// a UChain is a serial chain of U's (also called "units").
// they are played together in a Group. There should be only one chain playing
// per Server at a time, although it is not impossible to play multiple instances
// of at once.

UChain {
	var <>units, <>groups;
	var <prepareTasks;
	
	*new { |...units|
		^super.newCopyArgs( units.collect(_.asUnit) ).init;
	}
	
	init {
		prepareTasks = [];
	}
	
	prGetCanFreeSynths {
		^units.select({ |unit| unit.def.canFreeSynth });
	}
	
	prSetCanFreeSynths { |...args|
		units.do({ |unit|
			if( unit.def.canFreeSynth ) {
				unit.set( *args );
			};
		});
	}
	
	setDur { |dur = inf| // sets same duration for all units
		this.prSetCanFreeSynths( \u_doneAction, 14, \u_dur, dur );
		this.changed( \dur );
	}
	
	setFadeIn { |fadeIn = 0|
		this.prSetCanFreeSynths( \u_fadeIn, fadeIn );
		this.changed( \fadeIn );	
	}
	
	setFadeOut { |fadeOut = 0|
		this.prSetCanFreeSynths( \u_fadeOut, fadeOut );
		this.changed( \fadeOut );	
	}
	
	getFadeOut { 
		^this.prGetCanFreeSynths.collect({ |item| item.get( \u_fadeOut ) }).maxItem ? 0;
	}
	
	getFadeIn { 
		^this.prGetCanFreeSynths.collect({ |item| item.get( \u_fadeIn ) }).maxItem ? 0;
	}
	
	useSndFileDur { // look for SndFiles in all units, use the longest duration found
		var durs;
		units.do({ |unit|
			unit.allValues.do({ |val|
				if( val.isKindOf( AbstractSndFile ) ) {
					if( val.loop.not ) {
						durs = durs.add( val.eventDuration );
					};
				}
			});
		});
		if( durs.size > 0 ) { // only act if a sndFile is found
			this.setDur( durs.maxItem );
		};
	}
	
	getMaxDurUnit { // get unit with longest non-inf duration
		var dur, out;
		units.do({ |unit|
			var u_dur;
			if( unit.def.canFreeSynth ) {
				u_dur = unit.get( \u_dur );
				if( (u_dur > (dur ? 0)) && { u_dur != inf } ) {
					dur = u_dur;
					out = unit;
				};
			};
		});
		^out;	
	}
	
	getDur { // get longest duration
		var unit;
		unit = this.getMaxDurUnit;
		if( unit.isNil ) { 
			^inf 
		} {
			^unit.get( \u_dur );
		};
	}
	
	dur { ^this.getDur }
	duration { ^this.getDur }
	
	setDoneAction { // set doneAction 14 for unit with longest non-inf duration
		var maxDurUnit;
		maxDurUnit = this.getMaxDurUnit;
        	this.prGetCanFreeSynths.do({ |item|
	        	if( item == maxDurUnit ) {
		        	item.set( \u_doneAction, 14 );
	        	} {
		        	item.set( \u_doneAction, 0 );
	        	};
        	});
	}

	makeGroupAndSynth { |target|
		var maxDurUnit;
	    var group = Group( target )
                .startAction_({ |synth|
                    // only add if started (in case this is a bundle)
                    this.changed( \go, group );
                })
                .freeAction_({ |synth|
                    groups.remove( group );
                    this.changed( \end, group );
                });
        groups = groups.add( group );
        this.changed( \start, group );
        this.setDoneAction;
        units.do( _.makeSynth(group) );
	}

	makeBundle { |targets|
	    ^targets.asCollection.collect{ |target|
	        target.asTarget.server.makeBundle( false, {
                this.makeGroupAndSynth(target)
            })
		}
	}
	
	start { |target, latency|
		var targets, bundles;
		target = target ? Server.default;
		targets = target.asCollection;
		bundles = this.makeBundle( targets );
		targets.do({ |target, i|
			target.asTarget.server.sendBundle( latency, *bundles[i] );
		});
		if( target.size == 0 ) {
			^groups[0]
		} {
			^groups;
		};
	}
	
	stopPrepareTasks {
		if( prepareTasks.size > 0 ) { 
			prepareTasks.do(_.stop);
			prepareTasks = [];
		};
	}
	
	free { groups.do(_.free) }
	stop { this.stopPrepareTasks; this.free; }
	
	release { |time|
		var releaseUnits;
		releaseUnits = units.select({ |unit| unit.def.canFreeSynth });
		if( releaseUnits.size > 0 ) {
			if( time.isNil ) {
				releaseUnits = releaseUnits.sort({ |a,b| // reversed sort
					a.get( \u_fadeOut ) >= b.get( \u_fadeOut )
				});
			};
			releaseUnits[0].release( time, 14 ); // longest fadeOut releases group
			releaseUnits[1..].do( _.release( time,0 ) );
		} {
			this.stop; // stop if no releaseable synths
		};
	}
	
	at { |index| ^units[ index ] }
		
	last { ^units.last }
	first { ^units.first }

	prepare { |target, loadDef = true, action|
		action = MultiActionFunc( action );
	     units.do( _.prepare(target, loadDef, action.getAction ) )
	}

	prepareAndStart{ |target, loadDef = true|
		var task;
		task = fork { 
			this.prepare( target, loadDef );
			target.asCollection.do{ |t|
				t.asTarget.server.sync;
			};
	       	this.start(target);
	       	prepareTasks.remove(task);
		};
	    prepareTasks = prepareTasks.add( task );
	}
	
	waitTime { ^this.units.collect(_.waitTime).sum }
	
	prepareWaitAndStart { |target, loadDef = true|
		var task;
		task = fork { 
			this.prepare( target, loadDef );
			this.waitTime.wait; // doesn't care if prepare is done
	       	this.start(target);
	       	prepareTasks.remove(task);
		};
	    prepareTasks = prepareTasks.add( task );
	}

	dispose { units.do( _.dispose ) }
	
	resetGroups { groups = []; } // after unexpected server quit
	
	/*
	*   uchain: UChain
	*/
	<< { |uchain|
	    ^UChain(*(units++uchain.units))
	}

	/*
	*   units: U or Array[U]
	*/
	<| { |unit|
	    ^UChain(*(units++unit.asCollection))
	}

    asUEvent{ |startTime=0, dur=10, fadeIn=2, fadeOut=2, track =0|
	    ^UEvent(this, track, startTime, dur, fadeIn, fadeOut)
	}

	printOn { arg stream;
		stream << "a " << this.class.name << "(" <<* units.collect(_.defName)  <<")"
	}

}