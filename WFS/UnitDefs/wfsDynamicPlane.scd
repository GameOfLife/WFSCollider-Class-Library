var def;

def = FreeUdef( \wfsDynamicPlane, [	
		[ \pointFromBus: 	false,	BoolSpec( false ) ],
		[ \point: 		0@0,  	WFSPlaneSpec( 200, 0.1 ) ],
		[ \pointLag:		0,		[ 0, 5, \lin, 0, 0 ].asSpec ], // lagtime for point
		[ \quality:		0,		ListSpec([ \normal, \better ], 0 ) ], // for \l or \c
		[ \latencyComp:  	0,		[0,1].asSpec ],
		[ \dbRollOff:		0,		[ -24, 0, \lin, 1, 0 ].asSpec ],
		[ \maxAmpRadius:	2,		[ 1, 5, \lin, 0, 2, "m" ].asSpec ],
		[ \dontPause:		false,	BoolSpec( false ) ]
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
		\pointFromBus, \point, \pointLag, \dbRollOff, 
		\maxAmpRadius, \dontPause,
		\u_i_ar_0_bus, \u_i_kr_0_bus, \u_i_kr_1_bus, 
		\u_doneAction, \u_gain, \u_gate, \u_mute,
		\u_fadeIn, \u_fadeOut,
		\u_globalGain,
		\u_globalEQ_setting, \u_globalEQ_bypass,  
	] )
	.envPut( \set_wfsa, [ \arrayDbRollOff, \u_i_ar_0_bus, 
		\u_i_kr_0_bus, \u_i_kr_1_bus,  
	] )
	.addUEnv
	.addUGlobalEQ
	.addUIO( UIn, \ar, 0, 1 )
	.addUIO( UIn, \kr, 0, 2 )
	.setControlInName( [0,1], ['x', 'y'] );

if( WFS.previewMode.isNil ) {		
	// dynamic plane wfs panner
	def.createSynthFunc_({ |unit, target, startPos = 0|
		var server, n = 0;
		var arrayConfs;
		var arrayGroup;
		var prePanSynth;
		var planeSynths;
		var planeLevelBuses;
		var defName;
		var planeWaveMakeUpGain;
		
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
			] ++ unit.def.env.getInitArgsFor( \wfsp, unit.args, server, startPos ),
			arrayGroup, addAction: \addBefore
		);
		
		planeWaveMakeUpGain = WFSSpeakerConf.default.planeWaveMakeUpGain;
		
		// send the panner synths
		n = 0;
		planeSynths.do({ |sn, i|
			server.sendMsg( *sn.newMsg( arrayGroup, [ 
					\arrayConf, arrayConfs[i], 
					\levelBus, planeLevelBuses[i],
					\outOffset, n,
					\gain, planeWaveMakeUpGain
				] ++ unit.def.env.getInitArgsFor( \wfsa, unit.args, server, startPos ), 
				\addToTail ) 
			);
			n = n + arrayConfs[i].n;
		});
		
		arrayGroup.freeAction_({ |group| unit.removeSynth( group ); });
		unit.addSynth( arrayGroup );
		
		prePanSynth;
	})
	.setSynthFunc_({ |unit ...keyValuePairs|
		unit.synths.do({ |sn|
			var server = sn.server;
			var key = switch( sn.class, Group, \wfsa, Synth, \wfsp );
			server.sendSyncedBundle( Server.default.latency, nil, 
				*server.makeBundle( false, {
		    			sn.set( *unit.def.env.getSetArgsFor( key, keyValuePairs, server ) )
	    			})
	    		);
		})
	})
	.shouldPlayOnFunc_({ |unit, target|
		WFSSpeakerConf.default.getArraysFor( target.asTarget.server ).size > 0;
	});
} {
	// preview panner; exact same parameters, different panner synthdefs
	// the actual panning in this preview is no different to point sources
	// since there is no difference in a stereo situation in this case
	def.createSynthFunc_({ |unit, target, startPos = 0|
			var server, n = 0;
		var prePanSynth;
		var previewSynth;
		
		server = target.asTarget.server;		
		
		// make the pre-panner synth (has to know the nodeID's of the panner synths)
		prePanSynth = Synth( "wfsp_p_0",
			unit.def.env.getInitArgsFor( \wfsp, unit.args, server, startPos ) ++
				[ \sendPointRate, WFSPositionTracker.getRate ],
			target, \addToTail
		);
		
		previewSynth = Synth( 
			WFSPreviewSynthDefs.getDefName( WFS.previewMode, \d ),
			unit.def.env.getInitArgsFor( \wfsa, unit.args, server, startPos ),
			target, \addToTail 
		);
					
		previewSynth.freeAction_({ |synth| unit.removeSynth( synth ); });
		unit.addSynth( previewSynth );
		
		prePanSynth;
	})
	.setSynthFunc_({ |unit ...keyValuePairs|
		unit.synths.do({ |sn|
			var server = sn.server;
			var key = switch( sn.defName.asString[..4].asSymbol, 
				\wfsx_, \wfsa, \wfsp_, \wfsp );
			server.sendSyncedBundle( Server.default.latency, nil, 
				*server.makeBundle( false, {
		    			sn.set( *unit.def.env.getSetArgsFor( key, keyValuePairs, server ) )
	    			})
	    		);
		})
	})
	.shouldPlayOnFunc_({ |unit, target|
		target.asTarget.server.isLocal; // safety measure: no output on wfs servers
	});
};

def; // return def