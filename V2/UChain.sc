
// a UChain is a serial chain of U's (also called "units").
// they are played together in a Group. There should be only one chain playing
// per Server at a time, although it is not impossible to play multiple instances
// of at once.
//
// UChain implements the UEvent interface, therefore it has a startTime, track, duration, muted, releaseSelf variables.

UChain : UEvent {
	
	classvar <>defaultServers;
	classvar <>verbose = false;
	classvar <>groupDict;
	
	var <units; //, <>groups;
	var <prepareTasks;
	var <>preparedServers;
	
	*initClass { groupDict = IdentityDictionary( ) }
	
	*new { |...args|
		^super.new.init( args );
	}

	/*
	* Syntaxes for UChain creation:
	* UChain(\defName)
	* UChain(startTime,\defName1,\defName2,...)
	* UChain(startTime,track,\defName1,\defName2,...)
	* UChain(startTime,track,duration,\defName1,\defName2,...)
	* Uchain(startTime,track,duration,releaseSelf,\defName1,\defName2,...)
	*/

	init { |args|
	    var bools = 3.collect{ |i| args[i].notNil.if({args[i].isNumber},{false}) }++[args[3].notNil.if({args[3].class.superclass == Boolean},{false})];
	    if(bools[3]) {
	        startTime = args[0];
	        track = args[1];
	        duration = args[2];
	        units = args[4..].collect(_.asUnit);
	        this.releaseSelf_(args[3]);
        }{
            if(bools[2]) {
	            startTime = args[0];
	            track = args[1];
	            duration = args[2];
	            units = args[3..].collect(_.asUnit);
            } {
                if(bools[1]) {
	            startTime = args[0];
	            track = args[1];
	            units = args[2..].collect(_.asUnit);
                } {
                    if(bools[0]) {
	                    startTime = args[0];
	                    units = args[1..].collect(_.asUnit);
                    } {
                        units = args.collect(_.asUnit);
                    }
                }
            }
        };
		prepareTasks = [];
		//groups = [];
	}

    //will this work ? Yes
	duplicate{
	    ^this.deepCopy;
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
	
	units_ { |newUnits|
		units = newUnits;
		this.changed( \units );
	}

    eventSustain { ^duration - this.fadeOut; }

	fadeIn_ { |fadeIn = 0|

		fadeIn = fadeIn.max(0);

		units.do({ |unit|
		    var unitDur, unitFadeIn, unitFadeOut;
		    //each unit might have a different dur and fadeOut
			if( unit.def.canFreeSynth ) {
				unitDur = unit.get( \u_dur );
				if( unitDur != inf ) {
			        unitFadeOut = unit.get( \u_fadeOut );
			        unitFadeIn = fadeIn.min( unitDur - unitFadeOut );
			        unit.set( \u_fadeIn, unitFadeIn );
		        } {
				    unit.set( \u_fadeIn, fadeIn );
				}
			};
		});
		this.changed( \fadeIn );	
	}
	
	fadeOut_ { |fadeOut = 0|

		fadeOut = fadeOut.max(0);

		units.do({ |unit|
		    var unitDur, unitFadeOut, unitFadeIn;
		    //each unit might have a different dur and fadeIn
			if( unit.def.canFreeSynth ) {
				unitDur = unit.get( \u_dur );
				if( unitDur != inf ) {
			        unitFadeIn = unit.get( \u_fadeIn );
			        unitFadeOut = fadeOut.min( unitDur - unitFadeIn );
			        unit.set( \u_fadeOut, unitFadeOut );
		        } {
				    unit.set( \u_fadeOut, fadeOut );
				}
			};
		});
		this.changed( \fadeOut );
	}
	
	fadeOut {
		^this.prGetCanFreeSynths.collect({ |item| item.get( \u_fadeOut ) }).maxItem ? 0;
	}
	
	fadeIn {
		^this.prGetCanFreeSynths.collect({ |item| item.get( \u_fadeIn ) }).maxItem ? 0;
	}

	fadeTimes { ^[this.fadeIn, this.fadeOut] }
	
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
			this.dur_( durs.maxItem );
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
	
	prGetChainsDur { // get longest duration
		var unit;
		unit = this.getMaxDurUnit;
		if( unit.isNil ) { 
			^inf 
		} {
			^unit.get( \u_dur );
		};
	}

    /*
	* sets same duration for all units
	* clipFadeIn = true clips fadeIn
	* clipFadeIn = false clips fadeOut
	*/
	prSetChainsDur { |dur = inf, clipFadeIn = true| //
		this.prSetCanFreeSynths( \u_doneAction, 14, \u_dur, dur );
		if( clipFadeIn ) {
		    this.fadeIn = this.fadeIn.min(dur);
		    this.fadeOut = this.fadeOut.min(dur - this.fadeIn);
		} {
		    this.fadeOut = this.fadeOut.min(dur);
		    this.fadeIn = this.fadeIn.min(dur - this.fadeOut);
		}
	}

	duration_{ |dur|
        duration = dur;
        this.changed( \dur );
        if(releaseSelf){
            this.prSetChainsDur(dur);
        }
    }

    releaseSelf_ { |bool|

        if(releaseSelf != bool) {
	        releaseSelf = bool;
	        this.changed( \releaseSelf );
            if(bool){
                this.prSetChainsDur(duration);
            } {
                this.prSetChainsDur(inf);
            }
        }
    }
	
	dur { ^this.duration }
	dur_ { |x| this.duration_(x)}
	
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

	 makeView{ |i=0,maxWidth| ^UChainEventView(this, i, maxWidth) }

	
	/// creation
	
	groups { ^groupDict[ this ] ? [] }
	
	groups_ { |groups| groupDict.put( this, groups ); }
	
	addGroup { |group|
		 groupDict.put( this, groupDict.at( this ).add( group ) ); 
	}
	
	removeGroup { |group|
		var groups;
		groups = this.groups;
		groups.remove( group );
		if( groups.size == 0 ) {
			groupDict.put( this, nil ); 
		} {
			groupDict.put( this, groups );  // not needed?
		};
	}

	makeGroupAndSynth { |target, startPos = 0|
		var maxDurUnit;
	    var group;
	    if( this.shouldPlayOn( target ) != false ) {
	    		group = Group( target )
	                .startAction2_({ |synth|
	                    // only add if started (in case this is a bundle)
	                    this.changed( \go, group );
	                })
	                .freeAction2_({ |synth|
	                    this.removeGroup( group );
	                    this.changed( \end, group );
	                });
	        this.addGroup( group );
	        this.changed( \start, group );
	        units.do( _.makeSynth(group, startPos) );
	    };
	}

	makeBundle { |targets, startPos = 0|
		this.setDoneAction;
	    ^targets.asCollection.collect{ |target|
	        target.asTarget.server.makeBundle( false, {
                this.makeGroupAndSynth(target, startPos)
            })
		}
	}
	
	start { |target, startPos = 0, latency|
		var targets, bundles;
		if( target.isNil ) {
			target = preparedServers ? this.class.defaultServers ? Server.default;
		};
		preparedServers = nil;
		targets = target.asCollection;
		 if( verbose ) { "% starting on %".format( this, targets ).postln; };
		bundles = this.makeBundle( targets, startPos );
		latency = latency ?? { Server.default.latency; };
		targets.do({ |target, i|
			if( bundles[i].size > 0 ) {
				target.asTarget.server.sendSyncedBundle( latency, nil, *bundles[i] );
			};
		});
		if( target.size == 0 ) {
			^this.groups[0]
		} {
			^this.groups;
		};
	}
	
	stopPrepareTasks {
		if( prepareTasks.size > 0 ) { 
			prepareTasks.do(_.stop);
			prepareTasks = [];
		};
	}
	
	free { this.groups.do(_.free) }
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

	apxCPU {
		^units.collect(_.apxCPU).sum;
	}

	prepare { |target, startPos = 0, action|
		var cpu;
		action = MultiActionFunc( action );
		if( target.isNil or: { target.size == 0 } ) {
			target = this.class.defaultServers ? Server.default;
		};
		
		target = target.asCollection.select({ |tg|
			this.shouldPlayOn( tg ) != false;
		});
		cpu = this.apxCPU;
		target = target.collect({ |tg|
			tg.asTarget(cpu);
		});
		preparedServers = target;
	     units.do( _.prepare(target, startPos, action.getAction ) );
	     action.getAction.value; // fire action at least once
	     
	     if( verbose ) { "% preparing for %".format( this, preparedServers ).postln; };
	     
	     ^target; // return array of actually prepared servers
	}

	prepareAndStart{ |target, startPos = 0|
		var task, cond;
		if( target.isNil ) {
			target = this.class.defaultServers ? Server.default;
		};
		cond = Condition(false);
		task = fork { 
			target = this.prepare( target, startPos, { cond.test = true; cond.signal } );
			cond.wait;
	       	this.start(target, startPos);
	       	prepareTasks.remove(task);
		};
	    prepareTasks = prepareTasks.add( task );
	}
	
	waitTime { ^this.units.collect(_.waitTime).sum }
	
	prepareWaitAndStart { |target, startPos = 0|
		var task;
		task = fork { 
			target = this.prepare( target, startPos );
			this.waitTime.wait; // doesn't care if prepare is done
	       	this.start(target, startPos);
	       	prepareTasks.remove(task);
		};
	    prepareTasks = prepareTasks.add( task );
	}

	dispose { 
		units.do( _.dispose );
		preparedServers.do({ |srv|
			srv.asTarget.server.loadBalancerAddLoad( this.apxCPU.neg );
		});
		preparedServers = [];
	}
	
	resetGroups { this.groups = nil; } // after unexpected server quit
	
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
	*   unit: U or Array[U]
	*/
	<| { |unit|
	    ^UChain(*(units++unit.asCollection))
	}

	isFolder { ^false }
    getAllUChains{ ^this }

	printOn { arg stream;
		stream << "a " << this.class.name << "(" <<* units.collect(_.defName)  <<")"
	}
	
	storeArgs { ^units }

}