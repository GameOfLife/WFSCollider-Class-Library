
var staticPoint, dynamicPoint, staticPlane, dynamicPlane, multiDef;

staticPoint = HiddenFreeUdef( \staticPoint, [
		[ \point: 		0@0,  	WFSPointSpec( 200, 0.1 ) ],
		[ \type, 			\point, 	ListSpec( [ \point, \plane ] ), false, \init ],
		[ \latencyComp:  	0,		[0,1].asSpec, true, \init ],
		[ \distanceFilter:	0,		[0,2,\lin,0,1].asSpec ],
		[ \dbRollOff:		-6,		[ -24, 0, \lin, 1, -6 ].asSpec ],
		[ \maxAmpRadius:	5,		[ 1, 5, \lin, 0, 5, "m" ].asSpec ],
	] )
	.addUEnv
	.addUGlobalEQ
	.addUIO( UIn, \ar, 0, 1 )
	.inputIsEndPoint_( true )
	.setSynthFunc_({ |unit ...keyValuePairs|
		keyValuePairs = (keyValuePairs ? []).clump(2) // exclude point for staticPoint
			.select({ |arr| arr[0] !== \point }).flatten(1);
		unit.synths.do({ |sn|
			var args = keyValuePairs;
			if( sn.class == Group ) { // is an array group or preview synth
				args = (args ? []).clump(2)
					.select({ |arr| arr[0] === \u_i_ar_0_bus }).flatten(1);
			};
			if( args.size > 0 ) { 
				unit.subDef.prSetSynth( sn, *args );
			};
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
		 	unit.getArgsFor( server, startPos ),
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
						] ++ U.formatArgs( [ \point, unit.point, \u_i_ar_0_bus, unit.u_i_ar_0_bus ], server, startPos ),
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
							] ++ U.formatArgs( [ \point, unit.point, \u_i_ar_0_bus, unit.u_i_ar_0_bus ], server, startPos ), 
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
		 	unit.getArgsFor( server, startPos ),
			target, \addToTail
		);
		
		arrayGroup = Group(target, addAction: \addToTail);
		
		previewSynth = Synth( 
			WFSPreviewSynthDefs.getDefName( WFSLib.previewMode, \s ),
			U.formatArgs( [ \point, unit.point, \u_i_ar_0_bus, unit.u_i_ar_0_bus ], server, startPos ) ++
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
		[ \quality:		0,		ListSpec([ \good, \better ], 0 ), false, \nonsynth ], // \l or \c
		[ \distanceFilter:	0,		[0,2,\lin,0,1].asSpec ],
		[ \latencyComp:  	0,		[0,1].asSpec, false, \init ],
		[ \dbRollOff:		-6,		[ -24, 0, \lin, 1, -6 ].asSpec ],
		[ \maxAmpRadius:	5,		[ 1, 5, \lin, 0, 5, "m" ].asSpec ],
		[ \crossfadeLag:	0.2,		[ 0, 1, \lin, 0, 0.2 ].asSpec, true ],
		[ \dontPause:		false,	BoolSpec( false ), true ]
	], true )
	.addUGlobalEQ
	.addUIO( UIn, \ar, 0, 1 )
	.inputIsEndPoint_( true )
	.setSynthFunc_({ |unit ...keyValuePairs|
		unit.synths.do({ |sn|
			var args = keyValuePairs;
			if( sn.class == Group ) { // is an array group or preview synth
				args = (args ? []).clump(2)
					.select({ |arr| [ \point, \u_i_ar_0_bus ].includes( arr[0] ) }).flatten(1);
			};
			if( args.size > 0 ) { 
				unit.subDef.prSetSynth( sn, *args );
			};
		})
	});
	
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
			] ++ unit.getArgsFor( server, startPos ),
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
				] ++ U.formatArgs( [ \point, unit.point, \u_i_ar_0_bus, unit.u_i_ar_0_bus ], server, startPos ),
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
				] ++ U.formatArgs( [ \point, unit.point, \u_i_ar_0_bus, unit.u_i_ar_0_bus ], server, startPos ),
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
			unit.getArgsFor( server, startPos ) ++
				[ \sendPointRate, WFSPositionTracker.getRate ],
			target, \addToTail
		);
		
		arrayGroup = Group(target, addAction: \addToTail);
		
		previewSynth = Synth( 
			WFSPreviewSynthDefs.getDefName( WFSLib.previewMode, \d ),
			U.formatArgs( [ \point, unit.point, \u_i_ar_0_bus, unit.u_i_ar_0_bus ], server, startPos ) ++
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
		[ \latencyComp:  	0,		[0,1].asSpec, true, \init ],
		[ \dbRollOff:		0,		[ -24, 0, \lin, 1, 0 ].asSpec ],
		[ \maxAmpRadius:	5,		[ 1, 5, \lin, 0, 5, "m" ].asSpec ],
	] )
	.addUEnv
	.addUGlobalEQ
	.addUIO( UIn, \ar, 0, 1 )
	.inputIsEndPoint_( true )
	.setSynthFunc_({ |unit ...keyValuePairs|
		keyValuePairs = (keyValuePairs ? []).clump(2) // exclude point for staticPlane
			.select({ |arr| arr[0] !== \point }).flatten(1);
		unit.synths.do({ |sn|
			var args = keyValuePairs;
			if( sn.class == Group ) { // is an array group or preview synth
				args = (args ? []).clump(2)
					.select({ |arr| arr[0] === \u_i_ar_0_bus }).flatten(1);
			};
			if( args.size > 0 ) { 
				unit.subDef.prSetSynth( sn, *args );
			};
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
		 	unit.getArgsFor( server, startPos ),
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
						] ++ U.formatArgs( [ \point, unit.point, \u_i_ar_0_bus, unit.u_i_ar_0_bus ], server, startPos ),
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
		 	unit.getArgsFor( server, startPos ),
		 	target, \addToTail
		);
		
		arrayGroup = Group(target, addAction: \addToTail);
		
		previewSynth = Synth( 
			WFSPreviewSynthDefs.getDefName( WFSLib.previewMode, \s ),
			U.formatArgs( [ \point, unit.point, \u_i_ar_0_bus, unit.u_i_ar_0_bus ], server, startPos ) ++
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
		[ \quality:		0,		ListSpec([ \normal, \better ], 0 ), true, \nonsynth ], // for \l or \c
		[ \latencyComp:  	0,		[0,1].asSpec, true, \init ],
		[ \dbRollOff:		0,		[ -24, 0, \lin, 1, 0 ].asSpec ],
		[ \maxAmpRadius:	5,		[ 1, 5, \lin, 0, 5, "m" ].asSpec ],
		[ \dontPause:		false,	BoolSpec( false ), true ]
	] )
	.addUEnv
	.addUGlobalEQ
	.addUIO( UIn, \ar, 0, 1 )
	.inputIsEndPoint_( true )
	.setSynthFunc_({ |unit ...keyValuePairs|
		unit.synths.do({ |sn|
			var args = keyValuePairs;
			if( sn.class == Group ) { // is an array group or preview synth
				args = (args ? []).clump(2)
					.select({ |arr| [ \point, \u_i_ar_0_bus ].includes( arr[0] ) }).flatten(1);
			};
			if( args.size > 0 ) { 
				unit.subDef.prSetSynth( sn, *args );
			};
		})
	
	});

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
			] ++ unit.getArgsFor( server, startPos ),
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
				] ++ U.formatArgs( [ \point, unit.point, \u_i_ar_0_bus, unit.u_i_ar_0_bus ], server, startPos ),
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
			unit.getArgsFor( server, startPos ) ++
				[ \sendPointRate, WFSPositionTracker.getRate ],
			target, \addToTail
		);
		
		arrayGroup = Group(target, addAction: \addToTail);
		
		previewSynth = Synth( 
			WFSPreviewSynthDefs.getDefName( WFSLib.previewMode, \d ),
			U.formatArgs( [ \point, unit.point, \u_i_ar_0_bus, unit.u_i_ar_0_bus ], server, startPos ) ++
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


multiDef = MultiUdef( \wfsSource, [staticPoint, dynamicPoint, staticPlane, dynamicPlane ], \wfs_panner, \mode, true )
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
