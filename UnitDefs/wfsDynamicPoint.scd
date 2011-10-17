// dynamic point wfs panner
(
FreeUdef( \wfsDynamicPoint, [	
		[ \pointFromBus: 	false,	BoolSpec( false ) ],
		[ \point: 		0@0,  	WFSPointSpec( 200, 0.1 ) ],
		[ \pointLag:		0,		[ 0, 5, \lin, 0, 0 ].asSpec ], // lagtime for point
		[ \quality:		0,		ListSpec([ \good, \better ], 0 ) ], // \l or \c
		[ \latencyComp:  	0,		[0,1].asSpec ],
		[ \dbRollOff:		-6,		[ -24, 0, \lin, 1, -6 ].asSpec ],
		[ \maxAmpRadius:	2,		[ 1, 5, \lin, 0, 2, "m" ].asSpec ],
		[ \arrayLimit:	1,		[ 0.164, 1, \lin, 0, 1 ].asSpec ],  // better name?
		[ \crossfadeLag:	0.2,		[ 0, 1, \lin, 0, 0.2 ].asSpec ],
		[ \dontPause:		false,	BoolSpec( false ) ]
] )
	.category_( \wfs_panner )
	// first a few general things
	.envPut( \getArgsFor, { |evt, which, args, server|
		if( which.notNil ) {
			(args ? []).clump(2)
				.select({ |arr| which.includes( arr[0] ) })
				.collect({ |arr| [arr[0],arr[1].asControlInputFor(server)] })
				.flatten;
		} {
			[];
		}
	})
	.envPut( \getInitArgsFor, { |evt, defName, args, server|
		var which;
		which = evt[ ("init_" ++ defName).asSymbol ] ++ evt[ ("set_" ++ defName).asSymbol ];
		evt.getArgsFor( which, args, server );
	})
	.envPut( \getSetArgsFor, { |evt, defName, args, server|
		evt.getArgsFor( evt[ ("set_" ++ defName).asSymbol ], args, server );
	})
	.envPut( \init_wfsp, [ \latencyComp, \u_dur ] )
	.envPut( \set_wfsp, [ 
		\pointFromBus, \point, \pointLag, \dbRollOff, 
		\maxAmpRadius, \crossfadeLag, \dontPause,
		\u_i_ar_0_bus, \u_i_kr_0_bus, \u_i_kr_1_bus, 
		\u_doneAction, \u_gain, \u_gate, \u_mute,
		\u_fadeIn, \u_fadeOut,
		\u_globalGain,
		\u_globalEQ_setting, \u_globalEQ_bypass,  
	] )
	.envPut( \set_wfsa, [ \arrayLimit, \arrayDbRollOff, \u_i_ar_0_bus, 
		\u_i_kr_0_bus, \u_i_kr_1_bus,  
	] )
	.createSynthFunc_({ |unit, target|
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
			] ++ unit.def.env.getInitArgsFor( \wfsp, unit.args, server ),
			arrayGroup, addAction: \addBefore
		);
		
		
		// send the panner synths
		n = 0;
		normalSynths.do({ |sn, i|
			server.sendMsg( *sn.newMsg( arrayGroup, [ 
					\arrayConf, arrayConfs[i], 
					\levelBus, normalLevelBuses[i],
					\outOffset, n
				] ++ unit.def.env.getInitArgsFor( \wfsa, unit.args, server ), 
				\addToTail ) 
			);
			n = n + arrayConfs[i].n;
		});
		
		n = 0;
		focusSynths.do({ |sn, i|
			server.sendMsg( *sn.newMsg( arrayGroup, [ 
					\arrayConf, arrayConfs[i], 
					\levelBus, focusLevelBuses[i],
					\outOffset, n
				] ++ unit.def.env.getInitArgsFor( \wfsa, unit.args, server ), 
				\addToTail ) 
			);
			n = n + arrayConfs[i].n;
		});
		
		arrayGroup.freeAction_({ |group| unit.synths.remove( group ); });
		unit.addSynth( arrayGroup );
		
		prePanSynth;
	})
	.setSynthFunc_({ |unit ...keyValuePairs|
		unit.synths.do({ |sn|
			switch( sn.class,
				Group, {
					sn.set( *unit.def.env.getSetArgsFor( \wfsa, keyValuePairs, sn.server ) )
				},
				Synth, {
					sn.set( *unit.def.env.getSetArgsFor( \wfsp, keyValuePairs, sn.server ) )
				}
			)
		})
	})
	.shouldPlayOnFunc_({ |unit, target|
		WFSSpeakerConf.default.getArraysFor( target.asTarget.server ).size > 0;
	})
	.addUEnv
	.addUGlobalEQ
	.addUIO( UIn, \ar, 0, 1 )
	.addUIO( UIn, \kr, 0, 2 );
);