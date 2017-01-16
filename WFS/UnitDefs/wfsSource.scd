
var staticPoint, dynamicPoint, staticPlane, dynamicPlane, multiDef;

staticPoint = HiddenFreeUdef( \staticPoint, [
		[ \point: 		0@0,  	WFSPointSpec( 200, 0.1 ) ],
		[ \type, 			\point, 	ListSpec( [ \point, \plane ] ), false, \init ],
		[ \latencyComp:  	0,		[0,1].asSpec ],
		[ \distanceFilter:	0,		[0,2,\lin,0,1].asSpec ],
		[ \dbRollOff:		-6,		[ -24, 0, \lin, 1, -6 ].asSpec ],
		[ \maxAmpRadius:	2,		[ 1, 5, \lin, 0, 2, "m" ].asSpec ],
	] )
	.category_( \wfs_panner )
	// first a few general things
	.envPut( \getArgsFor, { |evt, which, args, server, startPos = 0|
		if( which.notNil ) {
			U.formatArgs( 
				(args ? []).clump(2)
					.select({ |arr| which.includes( arr[0] ) }).flatten(1),
				server,
				startPos
			)
		} {
			[];
		}
	})
	.envPut( \getInitArgsFor, { |evt, defName, args, server, startPos = 0|
		var which;
		which = evt[ ("init_" ++ defName).asSymbol ] ++ evt[ ("set_" ++ defName).asSymbol ];
		evt.getArgsFor( which, args, server, startPos );
	})
	.envPut( \getSetArgsFor, { |evt, defName, args, server, startPos = 0|
		evt.getArgsFor( evt[ ("set_" ++ defName).asSymbol ], args, server, startPos );
	})
	.envPut( \init_wfsp, [ \latencyComp, \u_dur, \point, \u_doneAction ] ) // controls only set at init
	.envPut( \set_wfsp, [ // controls that can always be set
		\distanceFilter, \dbRollOff, \maxAmpRadius,
		\u_i_ar_0_bus, 
		\u_gain, \u_gate, \u_mute,
		\u_fadeIn, \u_fadeOut,
		\u_fadeInCurve, \u_fadeOutCurve,
		\u_globalGain,
		\u_globalEQ_setting, \u_globalEQ_bypass, 
	] )
	.envPut( \init_wfsa, [ \point ] )
	.envPut( \set_wfsa, [ \u_i_ar_0_bus ] )
	.addUEnv
	.addUGlobalEQ
	.setSpecMode( \latencyComp, \init, /* \point, \init */ )
	.addUIO( UIn, \ar, 0, 1 )
	.inputIsEndPoint_( true )
	.setSynthFunc_({ |unit ...keyValuePairs|
		unit.synths.do({ |sn|
			var server = sn.server;
			var key = switch( sn.class, Group, \wfsa, Synth, \wfsp );
			server.sendSyncedBundle( Server.default.latency, nil, 
				*server.makeBundle( false, {
					var args;
					args = unit.subDef.env.getSetArgsFor( key, keyValuePairs, server );
					if( args.size > 0 ) {
		    				sn.set( *unit.subDef.env.getSetArgsFor( key, keyValuePairs, server ) )
					};
	    			})
	    		);
		})
	});

if( WFSPreviewSynthDefs.pannerFuncs.keys.includes( WFSLib.previewMode ).not ) {
	staticPoint.createSynthFunc_({ |unit, target, startPos = 0|
		var server, n = 0;
		var arrayConfs;
		var arrayGroup;
		var prePanSynth;
		var normalLevels, focusLevels;
		var normalSynths = [], focusSynths = [];
		var defNames;
		var crossfader;
		var focusMul;
		var point;
		
		server = target.asTarget.server;
		
		point = unit.get( \point ).value.asPoint;
		
		arrayConfs = WFSSpeakerConf.default.getArraysFor( server );
		crossfader = WFSCrossfader( 
			point, arrayConfs, focusWidth: WFSSpeakerConf.default.focusWidth 
		);
		focusMul = WFSSpeakerConf.default.focusDetector.kr( point );
		
		arrayGroup = Group(target, addAction: \addToTail);
		
		defNames = [ "wfsa_nsn_", "wfsa_usn_" ]; // normal and uni static n
		
		normalLevels = crossfader.arraysShouldRun( false ) * crossfader.cornerfades;		focusLevels = crossfader.arraysShouldRun( true );
		
		// make the pre-panner synth (has to know the nodeID's of the panner synths)
		prePanSynth = Synth( "wfsp_n_0",
		 	unit.subDef.env.getInitArgsFor( \wfsp, unit.args, server, startPos ),
			arrayGroup, addAction: \addBefore
		);
		
		// make the panner synths
		n = 0;
		arrayConfs.do({ |ac, i|
			if( normalLevels[i] > 0 ) {
				normalSynths = normalSynths.add(							Synth( defNames[0] ++ ac.n, [ 
							\gain, WFSSpeakerConf.default.gain.dbamp,
							\arrayConf, ac, 
							\outOffset, n + WFSSpeakerConf.getOutputBusStartOffset( server ),
							\amp, normalLevels[i],
							\arrayLimit, WFSSpeakerConf.default.arrayLimit,
						] ++ unit.subDef.env.getInitArgsFor( \wfsa, unit.args, server, startPos ), 
						arrayGroup, addAction: \addToTail 
					) 
				);
			};
			n = n + arrayConfs[i].n;
		});
		
		if( focusMul > 0 ) {	
			n = 0;
			arrayConfs.do({ |ac, i|
				if( focusLevels[i] > 0 ) {
					focusSynths = focusSynths.add(							Synth( defNames[1] ++ ac.n, [ 
								\gain, WFSSpeakerConf.default.gain.dbamp,
								\arrayConf, ac, 
								\outOffset, n + WFSSpeakerConf.getOutputBusStartOffset( server ),
								\amp, focusLevels[i],
								\arrayLimit, WFSSpeakerConf.default.arrayLimit,
								\focusWidth, WFSSpeakerConf.default.focusWidth,
							] ++ unit.subDef.env.getInitArgsFor( \wfsa, unit.args, server, startPos ), 
							arrayGroup, addAction: \addToTail 
						) 
					);
				};
				n = n + arrayConfs[i].n;
			});
		};
		
		arrayGroup.freeAction_({ |group| unit.removeSynth( group ); });
		unit.synths = unit.synths.add( arrayGroup );
		
		prePanSynth;
	})
	.shouldPlayOnFunc_({ |unit, target|
		var arrays, crossfader;
		arrays = WFSSpeakerConf.default.getArraysFor( target.asTarget.server );
		(arrays.size > 0) && {
			crossfader = WFSCrossfader( unit.get( \point ).uPrepareValue.asPoint, arrays );
			[(crossfader.cornerfades * crossfader.arraysShouldRun( false )).maxItem,
			 crossfader.arraysShouldRun( true ).maxItem ].maxItem > 0
		};
	});
} {
	// preview panner; exact same parameters, different panner synthdefs
	staticPoint.createSynthFunc_({ |unit, target, startPos = 0|
		var server, n = 0;
		var prePanSynth;
		var previewSynth;
		var arrayGroup;
		
		server = target.asTarget.server;
		
		// make the pre-panner synth (has to know the nodeID's of the panner synths)
		prePanSynth = Synth( "wfsp_n_0",
		 	unit.subDef.env.getInitArgsFor( \wfsp, unit.args, server, startPos ),
			target, \addToTail
		);
		
		arrayGroup = Group(target, addAction: \addToTail);
		
		previewSynth = Synth( 
			WFSPreviewSynthDefs.getDefName( WFSLib.previewMode, \s ),
			unit.subDef.env.getInitArgsFor( \wfsa, unit.args, server, startPos ) ++
				[ \out, WFSSpeakerConf.getOutputBusStartOffset( target ) ],
			arrayGroup, \addToTail 
		);
				
		arrayGroup.freeAction_({ |synth| unit.removeSynth( synth ); });
		unit.synths = unit.synths.add( arrayGroup );
					
		prePanSynth;
	})
	.shouldPlayOnFunc_({ |unit, target|
		target.asTarget.server.isLocal; // safety measure: no output on wfs servers
	});		
};

dynamicPoint = HiddenFreeUdef( \dynamicPoint, [	
		[ \point: 		0@0,  	WFSPointSpec( 200, 0.1 ) ],
		[ \type, 			\point, 	ListSpec( [ \point, \plane ] ), false, \init ],
		[ \quality:		0,		ListSpec([ \good, \better ], 0 ) ], // \l or \c
		[ \distanceFilter:	0,		[0,2,\lin,0,1].asSpec ],
		[ \latencyComp:  	0,		[0,1].asSpec ],
		[ \dbRollOff:		-6,		[ -24, 0, \lin, 1, -6 ].asSpec ],
		[ \maxAmpRadius:	2,		[ 1, 5, \lin, 0, 2, "m" ].asSpec ],
		[ \crossfadeLag:	0.2,		[ 0, 1, \lin, 0, 0.2 ].asSpec, true ],
		[ \dontPause:		false,	BoolSpec( false ), true ]
	], true ).category_( \wfs_panner )
	// first a few general things
	.envPut( \getArgsFor, { |evt, which, args, server, startPos = 0|
		if( which.notNil ) {
			U.formatArgs( 
				(args ? []).clump(2)
					.select({ |arr| which.includes( arr[0] ) }).flatten(1),
				server,
				startPos
			)
		} {
			[];
		}
	})
	.envPut( \getInitArgsFor, { |evt, defName, args, server, startPos = 0|
		var which;
		which = evt[ ("init_" ++ defName).asSymbol ] ++ evt[ ("set_" ++ defName).asSymbol ];
		evt.getArgsFor( which, args, server, startPos );
	})
	.envPut( \getSetArgsFor, { |evt, defName, args, server, startPos = 0|
		evt.getArgsFor( evt[ ("set_" ++ defName).asSymbol ], args, server, startPos );
	})
	.envPut( \init_wfsp, [ \latencyComp, \u_dur ] )
	.envPut( \set_wfsp, [ 
		\point, \distanceFilter, \dbRollOff, 
		\maxAmpRadius, \crossfadeLag, \dontPause,
		\u_i_ar_0_bus,
		\u_doneAction, \u_gain, \u_gate, \u_mute,
		\u_fadeIn, \u_fadeOut,
		\u_fadeInCurve, \u_fadeOutCurve,
		\u_globalGain,
		\u_globalEQ_setting, \u_globalEQ_bypass,  
	] )
	.envPut( \set_wfsa, [ \point, \u_i_ar_0_bus ] )
	.addUGlobalEQ
	.addUIO( UIn, \ar, 0, 1 )
	.inputIsEndPoint_( true )
	.setSpecMode( \latencyComp, \init, \quality, \nonsynth )
	.setSynthFunc_({ |unit ...keyValuePairs|
		unit.synths.do({ |sn|
			var server = sn.server;
			var key = switch( sn.class, Group, \wfsa, Synth, \wfsp );
			server.sendSyncedBundle( Server.default.latency, nil, 
				*server.makeBundle( false, {
		    			sn.set( *unit.subDef.env.getSetArgsFor( key, keyValuePairs, server ) )
	    			})
	    		);
		})
	});

// ensure backwards compatibility
dynamicPoint.prepareArgsFunc = { |argPairs|
	var point;
	if( argPairs.notNil) {
		if( argPairs.pairsAt( \pointFromBus ) == true ) {
			if( argPairs.includes( \u_i_kr_0_bus ) or: { argPairs.includes( \u_i_kr_1_bus ) } ) {				argPairs = [ \point, [ \map_control_point, [ 
								\bus_x, argPairs.pairsAt( \u_i_kr_0_bus ) ? 0,
								\bus_y, argPairs.pairsAt( \u_i_kr_1_bus ) ? 1
						] 
					],
				  \pointFromBus, false
				] ++	argPairs;	
			} {
				argPairs = [ \point, \map_control_point, \pointFromBus, false ] ++ argPairs;
			};
		};
		if( argPairs.pairsAt( \pointLag ).notNil ) {
			if( argPairs.pairsAt( \point ).notNil ) {
				argPairs = [ \point, [ \lag_point, [ 
					\point, argPairs.pairsAt( \point ),
					\time, argPairs.pairsAt( \pointLag ),
				] ] ] ++ argPairs;
			} {
				argPairs = [ \point, [ \lag_point, [ \time, argPairs.pairsAt( \pointLag ) ] ] ] 
					++ argPairs;
			};
		};
		argPairs;
	};
};
	
if( WFSPreviewSynthDefs.pannerFuncs.keys.includes( WFSLib.previewMode ).not ) {
// dynamic point wfs panner
	dynamicPoint.createSynthFunc_({ |unit, target, startPos = 0|
		var server, n = 0;
		var arrayConfs;
		var arrayGroup;
		var prePanSynth;
		var normalSynths, focusSynths;
		var normalLevelBuses, focusLevelBuses;
		var defNames;
		
		server = target.asTarget.server;
		
		arrayConfs = WFSSpeakerConf.default.getArraysFor( server );
		
		normalLevelBuses = arrayConfs.collect({ |item, i| (i*2) + 2000 });
		focusLevelBuses = arrayConfs.collect({ |item, i| (i*2) + 2001 });
		
		arrayGroup = Group(target, addAction: \addToTail);
		
		defNames = [ 
			[ "wfsa_nd", "wfsa_fd" ], 
			[ switch( unit.get( \quality ), \good, \l_, \better, \c_, \l_ ) ] 
		].flop.collect(_.join);
		
		// make the panner synths, but don't send yet
		normalSynths = arrayConfs.collect({ |item| 
			Synth.basicNew( defNames[0] ++ item.n, server ); 
		}); 
		focusSynths = arrayConfs.collect({ |item| 
			Synth.basicNew( defNames[1] ++ item.n, server ); 
		});
		
		// make the pre-panner synth (has to know the nodeID's of the panner synths)
		prePanSynth = Synth( "wfsp_d_" ++ arrayConfs.size, arrayConfs.collect({ |item,i|
			[ "arrayConf" ++ i, item.asArray, "cornerPoints" ++ i, item.asCornersArray ]
			}).flatten( 1 ) ++ [ 
				\normalLevelBuses, normalLevelBuses, 
				\focusLevelBuses, focusLevelBuses,
				\normalIDs, normalSynths.collect(_.nodeID),
				\focusIDs, focusSynths.collect(_.nodeID),
				\sendPointRate, WFSPositionTracker.getRate,
				\focusWidth, WFSSpeakerConf.default.focusWidth
			] ++ unit.subDef.env.getInitArgsFor( \wfsp, unit.args, server, startPos ),
			arrayGroup, addAction: \addBefore
		);
		
		
		// send the panner synths
		n = 0;
		normalSynths.do({ |sn, i|
			server.sendMsg( *sn.newMsg( arrayGroup, [ 
					\gain, WFSSpeakerConf.default.gain.dbamp,
					\arrayConf, arrayConfs[i], 
					\amp, ("c" ++ normalLevelBuses[i]).asSymbol,
					\outOffset, n + WFSSpeakerConf.getOutputBusStartOffset( server ),
					\arrayLimit, WFSSpeakerConf.default.arrayLimit,
					\focusWidth, WFSSpeakerConf.default.focusWidth
				] ++ unit.subDef.env.getInitArgsFor( \wfsa, unit.args, server, startPos ), 
				\addToTail ) 
			);
			n = n + arrayConfs[i].n;
		});
		
		n = 0;
		focusSynths.do({ |sn, i|
			server.sendMsg( *sn.newMsg( arrayGroup, [ 
					\gain, WFSSpeakerConf.default.gain.dbamp,
					\arrayConf, arrayConfs[i], 
					\amp, ("c" ++ focusLevelBuses[i]).asSymbol,
					\outOffset, n + WFSSpeakerConf.getOutputBusStartOffset( server ),
					\arrayLimit, WFSSpeakerConf.default.arrayLimit,
					\focusWidth, WFSSpeakerConf.default.focusWidth
				] ++ unit.subDef.env.getInitArgsFor( \wfsa, unit.args, server, startPos ), 
				\addToTail ) 
			);
			n = n + arrayConfs[i].n;
		});
		
		arrayGroup.freeAction_({ |group| unit.removeSynth( group ); });
		unit.addSynth( arrayGroup );
		
		prePanSynth;
	})
	.shouldPlayOnFunc_({ |unit, target|
		WFSSpeakerConf.default.getArraysFor( target.asTarget.server ).size > 0;
	});

} {
	// preview panner; exact same parameters, different panner synthdefs
	dynamicPoint.createSynthFunc_({ |unit, target, startPos = 0|
		var server, n = 0;
		var prePanSynth;
		var previewSynth;
		var arrayGroup;
		
		server = target.asTarget.server;
		
		// make the pre-panner synth (has to know the nodeID's of the panner synths)
		prePanSynth = Synth( "wfsp_d_0",
			unit.subDef.env.getInitArgsFor( \wfsp, unit.args, server, startPos ) ++ 
				[ \sendPointRate, WFSPositionTracker.getRate ],
			target, \addToTail
		);
		
		arrayGroup = Group(target, addAction: \addToTail);
		
		previewSynth = Synth( 
			WFSPreviewSynthDefs.getDefName( WFSLib.previewMode, \d ),
			unit.subDef.env.getInitArgsFor( \wfsa, unit.args, server, startPos ) ++
				[ \out, WFSSpeakerConf.getOutputBusStartOffset( target ) ],
			arrayGroup, \addToTail 
		);
		
		arrayGroup.freeAction_({ |synth| unit.removeSynth( synth ); });
		unit.addSynth( arrayGroup );
		
		prePanSynth;
	})
	.shouldPlayOnFunc_({ |unit, target|
		target.asTarget.server.isLocal; // safety measure: no output on wfs servers
	});
};

staticPlane = HiddenFreeUdef( \staticPlane, [
		[ \point: 		0@0,  	WFSPlaneSpec( 200, 0.1 ) ],
		[ \type, 			\point, 	ListSpec( [ \point, \plane ] ), false, \init ],
		[ \latencyComp:  	0,		[0,1].asSpec ],
		[ \dbRollOff:		0,		[ -24, 0, \lin, 1, 0 ].asSpec ],
		[ \maxAmpRadius:	2,		[ 1, 5, \lin, 0, 2, "m" ].asSpec ],
	] )

	.category_( \wfs_panner )
	// first a few general things
	.envPut( \getArgsFor, { |evt, which, args, server, startPos = 0|
		if( which.notNil ) {
			U.formatArgs( 
				(args ? []).clump(2)
					.select({ |arr| which.includes( arr[0] ) }).flatten(1),
				server,
				startPos
			)
		} {
			[];
		}
	})
	.envPut( \getInitArgsFor, { |evt, defName, args, server, startPos = 0|
		var which;
		which = evt[ ("init_" ++ defName).asSymbol ] ++ evt[ ("set_" ++ defName).asSymbol ];
		evt.getArgsFor( which, args, server, startPos );
	})
	.envPut( \getSetArgsFor, { |evt, defName, args, server, startPos = 0|
		evt.getArgsFor( evt[ ("set_" ++ defName).asSymbol ], args, server, startPos );
	})
	.envPut( \init_wfsp, [ \latencyComp, \u_dur, \point ] ) // controls only set at init
	.envPut( \set_wfsp, [ // controls that can always be set
		\dbRollOff, \maxAmpRadius,
		\u_i_ar_0_bus, 
		\u_doneAction, \u_gain, \u_gate, \u_mute,
		\u_fadeIn, \u_fadeOut,
		\u_fadeInCurve, \u_fadeOutCurve,
		\u_globalGain,
		\u_globalEQ_setting, \u_globalEQ_bypass,  
	] )
	.envPut( \init_wfsa, [ \point ] )
	.envPut( \set_wfsa, [ \u_i_ar_0_bus ] )
	.addUEnv
	.addUGlobalEQ
	.setSpecMode( \latencyComp, \init, /* \point, \init */ )
	.addUIO( UIn, \ar, 0, 1 )
	.inputIsEndPoint_( true )
	.setSynthFunc_({ |unit ...keyValuePairs|
		unit.synths.do({ |sn|
			var server = sn.server;
			var key = switch( sn.class, Group, \wfsa, Synth, \wfsp );
			server.sendSyncedBundle( Server.default.latency, nil, 
				*server.makeBundle( false, {
		    			sn.set( *unit.subDef.env.getSetArgsFor( key, keyValuePairs, server ) )
	    			})
	    		);
		})
	});
	
if( WFSPreviewSynthDefs.pannerFuncs.keys.includes( WFSLib.previewMode ).not ) {
	staticPlane.createSynthFunc_({ |unit, target, startPos = 0|
		var server, n = 0;
		var arrayConfs;
		var arrayGroup;
		var prePanSynth;
		var planeLevels;
		var planeSynths = [];
		var defName;
		var crossfader;
		var gain;
		var point;
		
		server = target.asTarget.server;
		
		point = unit.get( \point ).value.asPoint;
		
		arrayConfs = WFSSpeakerConf.default.getArraysFor( server );
		crossfader = WFSCrossfaderPlane( point, arrayConfs );
		
		arrayGroup = Group(target, addAction: \addToTail);
		
		defName = "wfsa_psn_";
				
		planeLevels = crossfader.crossfades;
		
		// make the pre-panner synth (has to know the nodeID's of the panner synths)
		prePanSynth = Synth( "wfsp_n_0",
		 	unit.subDef.env.getInitArgsFor( \wfsp, unit.args, server, startPos ),
			arrayGroup, addAction: \addBefore
		);
		
		gain = WFSSpeakerConf.default.gain.dbamp * WFSSpeakerConf.default.planeWaveMakeUpGain;
		
		// make the panner synths
		n = 0;
		arrayConfs.do({ |ac, i|
			if( planeLevels[i] > 0 ) {
				planeSynths = planeSynths.add(							Synth( defName ++ ac.n, [ 
							\gain, gain,
							\arrayConf, ac, 
							\outOffset, n + WFSSpeakerConf.getOutputBusStartOffset( server ),
							\amp, planeLevels[i]
						] ++ unit.subDef.env.getInitArgsFor( \wfsa, unit.args, server, startPos ), 
						arrayGroup, addAction: \addToTail 
					) 
				);
			};
			n = n + arrayConfs[i].n;
		});
				
		arrayGroup.freeAction_({ |group| unit.removeSynth( group ); });
		unit.synths = unit.synths.add( arrayGroup );
		
		prePanSynth;
	})
	.shouldPlayOnFunc_({ |unit, target|
		var arrays, crossfader;
		arrays = WFSSpeakerConf.default.getArraysFor( target.asTarget.server );
		(arrays.size > 0) && {
			crossfader = WFSCrossfaderPlane( unit.get( \point ).uPrepareValue.asPoint, arrays );
			crossfader.crossfades.maxItem > 0;
		};
	});
} {
// preview panner; exact same parameters, different panner synthdefs
// the actual panning in this preview is no different to point sources
// since there is no difference in a stereo situation in this case
staticPlane.createSynthFunc_({ |unit, target, startPos = 0|
		var server, n = 0;
		var prePanSynth;
		var previewSynth;
		var arrayGroup;
		
		server = target.asTarget.server;
					
		// make the pre-panner synth (has to know the nodeID's of the panner synths)
		prePanSynth = Synth( "wfsp_n_0",
		 	unit.subDef.env.getInitArgsFor( \wfsp, unit.args, server, startPos ),
			target, \addToTail
		);
		
		arrayGroup = Group(target, addAction: \addToTail);
		
		previewSynth = Synth( 
			WFSPreviewSynthDefs.getDefName( WFSLib.previewMode, \s ),
			unit.subDef.env.getInitArgsFor( \wfsa, unit.args, server, startPos ) ++
				[ \out, WFSSpeakerConf.getOutputBusStartOffset( target ) ],
			arrayGroup, \addToTail 
		);
				
		arrayGroup.freeAction_({ |synth| unit.removeSynth( synth ); });
		unit.synths = unit.synths.add( arrayGroup );
		
		prePanSynth;
	})
	.shouldPlayOnFunc_({ |unit, target|
		target.asTarget.server.isLocal; // safety measure: no output on wfs servers
	});
};


dynamicPlane = HiddenFreeUdef( \dynamicPlane, [	
		[ \point: 		0@0,  	WFSPlaneSpec( 200, 0.1 ) ],
		[ \type, 			\point, 	ListSpec( [ \point, \plane ] ), false, \init ],
		[ \quality:		0,		ListSpec([ \normal, \better ], 0 ) ], // for \l or \c
		[ \latencyComp:  	0,		[0,1].asSpec ],
		[ \dbRollOff:		0,		[ -24, 0, \lin, 1, 0 ].asSpec ],
		[ \maxAmpRadius:	2,		[ 1, 5, \lin, 0, 2, "m" ].asSpec ],
		[ \dontPause:		false,	BoolSpec( false ), true ]
	] )
	.category_( \wfs_panner )
	// first a few general things
	.envPut( \getArgsFor, { |evt, which, args, server, startPos = 0|
		if( which.notNil ) {
			U.formatArgs( 
				(args ? []).clump(2)
					.select({ |arr| which.includes( arr[0] ) }).flatten(1),
				server,
				startPos
			)
		} {
			[];
		}
	})
	.envPut( \getInitArgsFor, { |evt, defName, args, server, startPos = 0|
		var which;
		which = evt[ ("init_" ++ defName).asSymbol ] ++ evt[ ("set_" ++ defName).asSymbol ];
		evt.getArgsFor( which, args, server, startPos );
	})
	.envPut( \getSetArgsFor, { |evt, defName, args, server, startPos = 0|
		evt.getArgsFor( evt[ ("set_" ++ defName).asSymbol ], args, server, startPos );
	})
	.envPut( \init_wfsp, [ \latencyComp, \u_dur ] )
	.envPut( \set_wfsp, [ 
		\point, \dbRollOff, 
		\maxAmpRadius, \dontPause,
		\u_i_ar_0_bus,
		\u_doneAction, \u_gain, \u_gate, \u_mute,
		\u_fadeIn, \u_fadeOut,
		\u_fadeInCurve, \u_fadeOutCurve,
		\u_globalGain,
		\u_globalEQ_setting, \u_globalEQ_bypass,  
	] )
	.envPut( \set_wfsa, [ \point, \u_i_ar_0_bus ] )
	.addUEnv
	.addUGlobalEQ
	.addUIO( UIn, \ar, 0, 1 )
	.inputIsEndPoint_( true )
	.setSpecMode( \latencyComp, \init, \quality, \init )
	.setSynthFunc_({ |unit ...keyValuePairs|
		unit.synths.do({ |sn|
			var server = sn.server;
			var key = switch( sn.class, Group, \wfsa, Synth, \wfsp );
			server.sendSyncedBundle( Server.default.latency, nil, 
				*server.makeBundle( false, {
		    			sn.set( *unit.subDef.env.getSetArgsFor( key, keyValuePairs, server ) )
	    			})
	    		);
		})
	});

// ensure backwards compatibility
dynamicPlane.prepareArgsFunc = { |argPairs|
	var point;
	if( argPairs.notNil) {
		if( argPairs.pairsAt( \pointFromBus ) == true ) {
			if( argPairs.includes( \u_i_kr_0_bus ) or: { argPairs.includes( \u_i_kr_1_bus ) } ) {				argPairs = [ \point, [ \map_control_point, [ 
								\bus_x, argPairs.pairsAt( \u_i_kr_0_bus ) ? 0,
								\bus_y, argPairs.pairsAt( \u_i_kr_1_bus ) ? 1
						] 
					],
				  \pointFromBus, false
				] ++	argPairs;	
			} {
				argPairs = [ \point, \map_control_point, \pointFromBus, false ] ++ argPairs;
			};
		};
		if( argPairs.pairsAt( \pointLag ).notNil ) {
			if( argPairs.pairsAt( \point ).notNil ) {
				argPairs = [ \point, [ \lag_point, [ 
					\point, argPairs.pairsAt( \point ),
					\time, argPairs.pairsAt( \pointLag ),
				] ] ] ++ argPairs;
			} {
				argPairs = [ \point, [ \lag_point, [ \time, argPairs.pairsAt( \pointLag ) ] ] ] 
					++ argPairs;
			};
		};
		argPairs;
	};
};

if( WFSPreviewSynthDefs.pannerFuncs.keys.includes( WFSLib.previewMode ).not ) {
	// dynamic plane wfs panner
	dynamicPlane.createSynthFunc_({ |unit, target, startPos = 0|
		var server, n = 0;
		var arrayConfs;
		var arrayGroup;
		var prePanSynth;
		var planeSynths;
		var planeLevelBuses;
		var defName;
		var gain;
		
		server = target.asTarget.server;
		
		arrayConfs = WFSSpeakerConf.default.getArraysFor( server );
		
		planeLevelBuses = arrayConfs.collect({ |item, i| i + 2000 });
		
		arrayGroup = Group(target, addAction: \addToTail);
		
		defName =  "wfsa_pd" ++ switch( unit.get( \quality ), \good, \l_, \better, \c_, \l_ );		
		// make the panner synths, but don't send yet
		planeSynths = arrayConfs.collect({ |item| 
			Synth.basicNew( defName ++ item.n, server ); 
		}); 
		
		// make the pre-panner synth (has to know the nodeID's of the panner synths)
		prePanSynth = Synth( "wfsp_p_" ++ arrayConfs.size, arrayConfs.collect({ |item,i|
			[ "arrayConf" ++ i, item.asArray ]
			}).flatten( 1 ) ++ [ 
				\planeLevelBuses, planeLevelBuses, 
				\planeIDs, planeSynths.collect(_.nodeID),
				\sendPointRate, WFSPositionTracker.getRate
			] ++ unit.subDef.env.getInitArgsFor( \wfsp, unit.args, server, startPos ),
			arrayGroup, addAction: \addBefore
		);
		
		gain = WFSSpeakerConf.default.gain.dbamp * WFSSpeakerConf.default.planeWaveMakeUpGain;
		
		// send the panner synths
		n = 0;
		planeSynths.do({ |sn, i|
			server.sendMsg( *sn.newMsg( arrayGroup, [ 
					\gain, gain, 
					\arrayConf, arrayConfs[i], 
					\amp, ("c" ++planeLevelBuses[i]).asSymbol,
					\outOffset, n +  WFSSpeakerConf.getOutputBusStartOffset( server )
				] ++ unit.subDef.env.getInitArgsFor( \wfsa, unit.args, server, startPos ), 
				\addToTail ) 
			);
			n = n + arrayConfs[i].n;
		});
		
		arrayGroup.freeAction_({ |group| unit.removeSynth( group ); });
		unit.addSynth( arrayGroup );
		
		prePanSynth;
	})
	.shouldPlayOnFunc_({ |unit, target|
		WFSSpeakerConf.default.getArraysFor( target.asTarget.server ).size > 0;
	});
} {
	// preview panner; exact same parameters, different panner synthdefs
	// the actual panning in this preview is no different to point sources
	// since there is no difference in a stereo situation in this case
	dynamicPlane.createSynthFunc_({ |unit, target, startPos = 0|
			var server, n = 0;
		var prePanSynth;
		var previewSynth;
		var arrayGroup;
		
		server = target.asTarget.server;		
		
		// make the pre-panner synth (has to know the nodeID's of the panner synths)
		prePanSynth = Synth( "wfsp_p_0",
			unit.subDef.env.getInitArgsFor( \wfsp, unit.args, server, startPos ) ++
				[ \sendPointRate, WFSPositionTracker.getRate ],
			target, \addToTail
		);
		
		arrayGroup = Group(target, addAction: \addToTail);
		
		previewSynth = Synth( 
			WFSPreviewSynthDefs.getDefName( WFSLib.previewMode, \d ),
			unit.subDef.env.getInitArgsFor( \wfsa, unit.args, server, startPos ) ++
				[ \out, WFSSpeakerConf.getOutputBusStartOffset( target ) ],
			arrayGroup, \addToTail 
		);
					
		arrayGroup.freeAction_({ |synth| unit.removeSynth( synth ); });
		unit.addSynth( arrayGroup );
		
		prePanSynth;
	})
	.shouldPlayOnFunc_({ |unit, target|
		target.asTarget.server.isLocal; // safety measure: no output on wfs servers
	});
};


multiDef = MultiUdef( \wfsSource, [staticPoint, dynamicPoint, staticPlane, dynamicPlane ], \wfs_experimental, \mode, true )
	.dontStoreArgNames_([ \mode ])
	.chooseFunc_( { |args|
		var index, plane;
		index = args.indexOf( \point );
		plane = args.indexOf( \type );
		if( plane.notNil ) { plane = args[ plane + 1 ] == \plane; } { plane = false };
		if( index.notNil && {
			args[index+1].isUMap && {
				args[index+1].def.allowedModes.includes( \init ).not;
			}
		}) { 
			if( plane, \dynamicPlane, \dynamicPoint );
		} {
			if( plane, \staticPlane, \staticPoint );
		};
	} );
