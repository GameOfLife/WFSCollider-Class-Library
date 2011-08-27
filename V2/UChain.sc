
// a UChain is a serial chain of U's (also called "units").
// they are played together in a Group. There should be only one chain playing
// per Server at a time, although it is not impossible to play multiple instances
// of at once.

UChain {
	
	classvar <>defaultServers;
	
	var <>units, <>groups;
	var <prepareTasks;
	
	*new { |...units|
		^super.newCopyArgs( units.collect(_.asUnit) ).init;
	}
	
	init {
		prepareTasks = [];
		groups = [];
	}
	
	
	// global setters (acces specific units inside the chain)
	
	prGetCanFreeSynths { // returns the units that can free synths (they will free the whole group)
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
		var dur, fadeOut;
		dur = this.getDur;
		if( dur != inf ) {
			fadeOut = this.getFadeOut;
			fadeIn = fadeIn.min( dur - fadeOut );
		};
		this.prSetCanFreeSynths( \u_fadeIn, fadeIn );
		this.changed( \fadeIn );	
	}
	
	setFadeOut { |fadeOut = 0|
		var dur, fadeIn;
		dur = this.getDur;
		if( dur != inf ) {
			fadeIn = this.getFadeIn;
			fadeOut = fadeOut.min( dur - fadeIn );
		};
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
					durs = durs.add( val.duration );
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
	
	setGain { |gain = 0| // set the average gain of all units that have a u_gain arg
		var mean, add;
		mean = this.getGain;
		add = gain - mean;
		this.prGetCanFreeSynths.do({ |unit|
			 unit.set( \u_gain, unit.get( \u_gain ) + add );
		});
		this.changed( \gain );		
	}
	
	getGain {
		var gains;
		gains = this.prGetCanFreeSynths.collect({ |item| item.get( \u_gain ) });
		if( gains.size > 0 ) { ^gains.mean } { ^0 };
	}
	
	setDoneAction { // set doneAction 14 for unit with longest non-inf duration
		var maxDurUnit;
		maxDurUnit = this.getMaxDurUnit;
		if( maxDurUnit.isNil ) { // only inf synths
			this.prGetCanFreeSynths.do({ |item, i|
		        	item.set( \u_doneAction, 14 );        			});
		} {	 
			this.prGetCanFreeSynths.do({ |item, i|
		        	if( item == maxDurUnit or: { item.get( \u_dur ) == inf } ) {
			        	item.set( \u_doneAction, 14 );
		        	} {
			        	item.set( \u_doneAction, 0 );
		        	};
	        	});
		};
	}
	
	/// creation

	makeGroupAndSynth { |target|
		var maxDurUnit;
	    var group;
	    if( this.shouldPlayOn( target ) != false ) {
	    		group = Group( target )
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
	        units.do( _.makeSynth(group) );
	    };
	}

	makeBundle { |targets|
		this.setDoneAction;
	    ^targets.asCollection.collect{ |target|
	        target.asTarget.server.makeBundle( false, {
                this.makeGroupAndSynth(target)
            })
		}
	}
	
	start { |target, latency|
		var targets, bundles;
		if( target.isNil ) {
			target = this.class.defaultServers ? Server.default;
		};
		targets = target.asCollection;
		bundles = this.makeBundle( targets );
		latency = latency ? 0.2;
		targets.do({ |target, i|
			if( bundles[i].size > 0 ) {
				target.asTarget.server.sendSyncedBundle( latency, nil, *bundles[i] );
			};
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
			releaseUnits[1..].do( _.release( time, 0 ) );
		} {
			this.stop; // stop if no releaseable synths
		};
	}
	
	shouldPlayOn { |target|
		var res;
		res = units.collect({ |unit|
			unit.shouldPlayOn( target );
		}).select(_.notNil);
		case { res.size == 0 } {
			^nil;
		} { res.any(_ == true) } { // if any of the units specifically shouldPlayOn, all play
			^true;
		} {
			^false;
		};
	}

	prepare { |target, loadDef = true, action|
		action = MultiActionFunc( action );
		if( target.isNil ) {
			target = this.class.defaultServers ? Server.default;
		};
		target = target.asCollection.select({ |tg|
			this.shouldPlayOn( tg ) != false;
		});
	     units.do( _.prepare(target, loadDef, action.getAction ) );
	     action.getAction.value; // fire action at least once
	     ^target; // return array of actually prepared servers
	}

	prepareAndStart{ |target, loadDef = true|
		var task;
		if( target.isNil ) {
			target = this.class.defaultServers ? Server.default;
		};
		task = fork { 
			target = this.prepare( target, loadDef );
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
	
	// indexing / access
		
	at { |index| ^units[ index ] }
		
	last { ^units.last }
	first { ^units.first }
	
	add { |unit|
		units = units.add( unit.asUnit );
		this.changed( \units );
	}
	
	addAll { |inUnits| // a UChain or Array with units
		if( inUnits.class == this.class ) { inUnits = inUnits.units; };
		units = units.addAll( inUnits.collect(_.asUnit) );
		this.changed( \units );
	}
	
	put { |index, unit|
		units.put( index, unit.asUnit );
		this.changed( \units );
	}
	
	insert { |index, unit|
		units = units.insert( index, unit.asUnit );
		this.changed( \units );

	}
	
	removeAt { |index|
		var out;
		out = units.removeAt( index );
		this.changed( \units );
		^out;
	}
	
	
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

    asUEvent{ |startTime=0, track =0, dur|
	    ^UEvent(this, startTime, track, dur ? inf)
	}

	printOn { arg stream;
		stream << "a " << this.class.name << "(" <<* units.collect(_.defName)  <<")"
	}

}