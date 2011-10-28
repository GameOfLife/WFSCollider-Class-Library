// static plane wfs panner
/*
This panner performs the crossfades on the language side, and only starts panner synths for 
those arrays that need them. The shouldPlayOn func also checks if there are any arrays
with non-zero level, and if not blocks playback for the given server.

The plane is entered as a point location, can only be changed at init, and cannot be from a bus (which is why this is a "static" point). The latencyComp parameter can also only be set at init. The dbRollOff (default: 0!!) and other global amplitude parameters however can be changed dynamically

*/
if( WFS.previewMode.isNil ) {	
	(
	FreeUdef( \wfsStaticPlane, [
			[ \point: 		0@0,  	WFSPointSpec( 200, 0.1 ) ],
			[ \latencyComp:  	0,		[0,1].asSpec ],
			[ \dbRollOff:		0,		[ -24, 0, \lin, 1, -6 ].asSpec ],
			[ \maxAmpRadius:	2,		[ 1, 5, \lin, 0, 2, "m" ].asSpec ],
			[ \arrayLimit:	1,		[ 0.164, 1, \lin, 0, 1 ].asSpec ],  // better name?
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
			\u_globalGain,
			\u_globalEQ_setting, \u_globalEQ_bypass,  
		] )
		.envPut( \init_wfsa, [ \arrayLimit, \arrayDbRollOff, \u_i_ar_0_bus, \point ] )
		.createSynthFunc_({ |unit, target, startPos = 0|
			var server, n = 0;
			var arrayConfs;
			var arrayGroup;
			var prePanSynth;
			var planeLevels;
			var planeSynths = [];
			var defName;
			var crossfader;
			
			server = target.asTarget.server;
			
			arrayConfs = WFSSpeakerConf.default.getArraysFor( server );
			crossfader = WFSCrossfaderPlane( unit.get( \point ), arrayConfs );
			
			arrayGroup = Group(target, addAction: \addToTail);
			
			defName = "wfsa_psn_";
					
			planeLevels = crossfader.crossfades;
			
			// make the pre-panner synth (has to know the nodeID's of the panner synths)
			prePanSynth = Synth( "wfsp_n_0",
			 	unit.def.env.getInitArgsFor( \wfsp, unit.args, server, startPos ),
				arrayGroup, addAction: \addBefore
			);
			
			// make the panner synths
			n = 0;
			arrayConfs.do({ |ac, i|
				if( planeLevels[i] > 0 ) {
					planeSynths = planeSynths.add(							Synth( defName ++ ac.n, [ 
								\arrayConf, ac, 
								\outOffset, n,
								\amp, planeLevels[i]
							] ++ unit.def.env.getInitArgsFor( \wfsa, unit.args, server, startPos ), 
							arrayGroup, addAction: \addToTail 
						) 
					);
				};
				n = n + arrayConfs[i].n;
			});
					
			arrayGroup.freeAction_({ |group| unit.synths.remove( group ); });
			unit.synths = unit.synths.add( arrayGroup );
			
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
			var arrays, crossfader;
			arrays = WFSSpeakerConf.default.getArraysFor( target.asTarget.server );
			(arrays.size > 0) && {
				crossfader = WFSCrossfaderPlane( unit.get( \point ), arrays );
				crossfader.crossfades.maxItem > 0;
			};
		})
		.addUEnv
		.addUGlobalEQ
		.addUIO( UIn, \ar, 0, 1 );
	);
} {
	// preview panner; exact same parameters, different panner synthdefs
	// the actual panning in this preview is no different to point sources
	// since there is no difference in a stereo situation in this case
	(
	FreeUdef( \wfsStaticPlane, [
			[ \point: 		0@0,  	WFSPointSpec( 200, 0.1 ) ],
			[ \latencyComp:  	0,		[0,1].asSpec ],
			[ \dbRollOff:		0,		[ -24, 0, \lin, 1, -6 ].asSpec ],
			[ \maxAmpRadius:	2,		[ 1, 5, \lin, 0, 2, "m" ].asSpec ],
			[ \arrayLimit:	1,		[ 0.164, 1, \lin, 0, 1 ].asSpec ],  // better name?
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
			\u_globalGain,
			\u_globalEQ_setting, \u_globalEQ_bypass,  
		] )
		.envPut( \init_wfsa, [ \arrayLimit, \arrayDbRollOff, \u_i_ar_0_bus, \point ] )
		.createSynthFunc_({ |unit, target, startPos = 0|
			var server, n = 0;
			var prePanSynth;
			var previewSynth;
			
			server = target.asTarget.server;
						
			// make the pre-panner synth (has to know the nodeID's of the panner synths)
			prePanSynth = Synth( "wfsp_n_0",
			 	unit.def.env.getInitArgsFor( \wfsp, unit.args, server, startPos ),
				target, \addToTail
			);
			
			previewSynth = Synth( 
				WFSPreviewSynthDefs.getDefName( WFS.previewMode, \s ),
				unit.def.env.getInitArgsFor( \wfsa, unit.args, server, startPos ),
				target, \addToTail 
			);
					
			previewSynth.freeAction_({ |synth| unit.removeSynth( synth ); });
			unit.synths = unit.synths.add( previewSynth );
			
			prePanSynth;
		})
		.setSynthFunc_({ |unit ...keyValuePairs|
			unit.synths.do({ |sn|
				switch( sn.defName.asString[..4].asSymbol,
					\wfsx_, {
						sn.set( *unit.def.env.getSetArgsFor( \wfsa, keyValuePairs, sn.server ) )
					},
					\wfsp_, {
						sn.set( *unit.def.env.getSetArgsFor( \wfsp, keyValuePairs, sn.server ) )
					}
				)
			})
		})
		.shouldPlayOnFunc_({ |unit, target|
			Server.default == target.asTarget.server; // safety measure: no output on wfs servers
		})
		.addUEnv
		.addUGlobalEQ
		.addUIO( UIn, \ar, 0, 1 );
	);

};